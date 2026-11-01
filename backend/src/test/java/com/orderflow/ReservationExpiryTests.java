package com.orderflow;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static com.orderflow.Contracts.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DATABASE_URL:jdbc:h2:mem:expiry;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000}","orderflow.seed=false","orderflow.expiry-worker=false","orderflow.reservation-seconds=60"})
@Import(ReservationExpiryTests.Time.class)
class ReservationExpiryTests {
 static class MutableClock extends Clock {
  final AtomicReference<Instant> instant=new AtomicReference<>(Instant.parse("2026-10-31T00:00:00Z"));
  public ZoneId getZone(){return ZoneOffset.UTC;}
  public Clock withZone(ZoneId zone){return Clock.fixed(instant(),zone);}
  public Instant instant(){return instant.get();}
  void advance(long millis){instant.updateAndGet(i->i.plusMillis(millis));}
 }
 @TestConfiguration static class Time {@Bean @Primary MutableClock testClock(){return new MutableClock();}}
 @Autowired OperationsService service;@Autowired JdbcTemplate db;@Autowired MutableClock clock;
 String warehouse,product;
 @BeforeEach void reset(){clock.instant.set(Instant.parse("2026-10-31T00:00:00Z"));for(String table:List.of("return_lines","returns","payment_events","idempotency_keys","activity","stock_movements","order_lines","orders","inventory","products","warehouses"))db.update("DELETE FROM "+table);warehouse=(String)service.createWarehouse(new WarehouseInput("Expiry warehouse","Hong Kong")).get("id");product=(String)service.createProduct(new ProductInput("EXPIRY-001","Expiry test product","Test",100)).get("id");service.receive(new StockInput(warehouse,product,10,"Opening test stock"),"opening-expiry-stock");}
 Map<String,Object> create(){return service.createOrder(new OrderInput("Expiry buyer",warehouse,List.of(new LineInput(product,2))),"expiry-order-request");}
 Map<String,Object> stock(){return service.inventory().getFirst();}
 long releases(){return db.queryForObject("SELECT COUNT(*) FROM stock_movements WHERE kind='RELEASE'",Long.class);}
 @Test void deadlineBoundaryReleasesOnceAndReplayCannotExtendReservation(){
  var o=create();String id=(String)o.get("id");long due=OperationsService.number(o,"reservation_expires_at_ms");assertEquals(clock.millis()+60000,due);
  clock.advance(59999);assertFalse(service.expireReservation(id));assertEquals(2,OperationsService.number(stock(),"reserved"));
  assertEquals(due,OperationsService.number(create(),"reservation_expires_at_ms"));
  clock.advance(1);assertTrue(service.expireReservation(id));assertFalse(service.expireReservation(id));service.cancel(id);
  var expired=service.order(id);assertEquals("CANCELLED",expired.get("status"));assertEquals("RESERVATION_EXPIRED",expired.get("cancellation_reason"));assertNotNull(expired.get("cancelled_at"));assertEquals(10,OperationsService.number(stock(),"on_hand"));assertEquals(0,OperationsService.number(stock(),"reserved"));assertEquals(1,releases());
  assertEquals("CANCELLED",create().get("status"));assertEquals(1,service.orders().size());
 }
 @Test void successfulPaymentBeforeDeadlineSurvivesEveryLaterSweep(){
  var o=create();String id=(String)o.get("id");clock.advance(59999);service.payment(new PaymentInput("before-deadline-event",id,200,"SUCCEEDED"));clock.advance(100000);
  assertFalse(service.expireReservation(id));assertTrue(service.dueReservations().isEmpty());assertEquals("PAID",service.order(id).get("status"));assertEquals(2,OperationsService.number(stock(),"reserved"));assertEquals(0,releases());
 }
 @Test void paymentEnforcesDeadlineEvenWhenWorkerHasNotRun(){
  var o=create();String id=(String)o.get("id");clock.advance(60000);
  var payment=new PaymentInput("late-payment-request",id,200,"SUCCEEDED");var result=service.payment(payment);
  assertEquals("LATE_SUCCESS_REVIEW",((Map<?,?>)result.get("event")).get("effect"));assertEquals("CANCELLED",service.order(id).get("status"));assertEquals(0,OperationsService.number(stock(),"reserved"));assertEquals(1,releases());
  assertEquals(1,OperationsService.number(service.orders().getFirst(),"payment_review_events"));assertEquals(true,service.payment(payment).get("replayed"));assertFalse(service.expireReservation(id));assertEquals(1,releases());assertThrows(BusinessException.class,()->service.fulfill(id));
 }
 @Test void simultaneousSweepersAndLatePaymentCannotReleaseOrConfirmTwice()throws Exception{
  var o=create();String id=(String)o.get("id");clock.advance(60000);var gate=new CountDownLatch(1);
  try(var workers=Executors.newFixedThreadPool(3)){
   var first=workers.submit(()->{gate.await();return service.expireReservation(id);});
   var second=workers.submit(()->{gate.await();return service.expireReservation(id);});
   var payment=workers.submit(()->{gate.await();return service.payment(new PaymentInput("race-late-payment",id,200,"SUCCEEDED"));});
   gate.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);assertEquals("LATE_SUCCESS_REVIEW",((Map<?,?>)payment.get(15,TimeUnit.SECONDS).get("event")).get("effect"));
  }
  assertEquals(1,releases());assertEquals(0,OperationsService.number(stock(),"reserved"));assertEquals("RESERVATION_EXPIRED",service.order(id).get("cancellation_reason"));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM activity WHERE action='order.expired'",Long.class));
 }
 @Test void expiryRollsBackAllStockAndHistoryWhenActivityCannotCommit(){
  String id=(String)create().get("id");clock.advance(60000);db.execute("ALTER TABLE activity ADD CONSTRAINT reject_expiry CHECK(action <> 'order.expired')");
  try{assertThrows(RuntimeException.class,()->service.expireReservation(id));assertEquals("RESERVED",service.order(id).get("status"));assertEquals(2,OperationsService.number(stock(),"reserved"));assertEquals(0,releases());}
  finally{db.execute("ALTER TABLE activity DROP CONSTRAINT reject_expiry");}
  assertTrue(service.expireReservation(id));assertEquals(1,releases());
 }
 @Test void workerRecoversPersistedDueReservationAndFailedPaymentDoesNotReviveIt(){
  String id=(String)create().get("id");clock.advance(60000);new ReservationExpiryWorker(service).sweep();
  assertEquals("CANCELLED",service.order(id).get("status"));var result=service.payment(new PaymentInput("expired-failed-payment",id,200,"FAILED"));assertEquals("IGNORED_EXPIRED",((Map<?,?>)result.get("event")).get("effect"));assertEquals(1,releases());
 }
 @Test void multiLineExpiryReconcilesLedgerAndManualCancellationKeepsItsReason(){
  var p=service.createProduct(new ProductInput("EXPIRY-002","Second product","Test",200));String other=(String)p.get("id");service.receive(new StockInput(warehouse,other,5,"Second opening stock"),"second-opening-stock");
  var o=service.createOrder(new OrderInput("Multi buyer",warehouse,List.of(new LineInput(product,2),new LineInput(other,3))),"multi-expiry-order");clock.advance(60000);assertTrue(service.expireReservation((String)o.get("id")));assertEquals(0,db.queryForObject("SELECT SUM(reserved) FROM inventory",Integer.class));assertEquals(0,db.queryForObject("SELECT SUM(reserved_delta) FROM stock_movements",Long.class));assertEquals(15,db.queryForObject("SELECT SUM(on_hand) FROM inventory",Long.class));
  var cancelled=create();service.cancel((String)cancelled.get("id"));assertEquals("OPERATOR_CANCELLED",service.order((String)cancelled.get("id")).get("cancellation_reason"));
 }
}
