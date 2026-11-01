package com.orderflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static com.orderflow.Contracts.*;
class ReservationRestartTests {
 @TempDir Path dir;
 private ConfigurableApplicationContext start(String url,boolean worker){return new SpringApplicationBuilder(OrderFlowApplication.class).run("--server.port=0","--spring.datasource.url="+url,"--spring.datasource.username=sa","--spring.datasource.password=","--orderflow.seed=false","--orderflow.public-demo=false","--orderflow.production=false","--orderflow.expiry-worker="+worker,"--orderflow.reservation-seconds=60");}
 @Test void coldRestartFindsDueRowsAndPreservesAlreadyPaidReservations(){
  String url="jdbc:h2:file:"+dir.resolve("restart")+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";String unpaid,paid;
  try(var first=start(url,false)){
   var service=first.getBean(OperationsService.class);var db=first.getBean(JdbcTemplate.class);
   String w=(String)service.createWarehouse(new WarehouseInput("Restart warehouse","HK")).get("id"),p=(String)service.createProduct(new ProductInput("RESTART","Restart product","Test",100)).get("id");service.receive(new StockInput(w,p,5,"Opening stock"),"restart-receipt-key");
   unpaid=(String)service.createOrder(new OrderInput("Unpaid buyer",w,List.of(new LineInput(p,2))),"restart-unpaid-key").get("id");paid=(String)service.createOrder(new OrderInput("Paid buyer",w,List.of(new LineInput(p,1))),"restart-paid-key").get("id");service.payment(new PaymentInput("restart-paid-event",paid,100,"SUCCEEDED"));
   // Persist overdue deadlines without waiting in real time; no worker runs in this process.
   db.update("UPDATE orders SET reservation_expires_at_ms=0");assertEquals(3,OperationsService.number(service.inventory().getFirst(),"reserved"));
  }
  try(var second=start(url,true)){
   var service=second.getBean(OperationsService.class);assertEquals("RESERVATION_EXPIRED",service.order(unpaid).get("cancellation_reason"));assertEquals("PAID",service.order(paid).get("status"));assertEquals(1,OperationsService.number(service.inventory().getFirst(),"reserved"));assertEquals(5,OperationsService.number(service.inventory().getFirst(),"on_hand"));assertFalse(service.expireReservation(unpaid));
  }
 }
 @Test void v1UnpaidRowsMigrateToImmediateExpiryWithoutRewritingOldHistory(){
  String url="jdbc:h2:file:"+dir.resolve("legacy")+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
  Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration").target("1").load().migrate();
  var db=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));String date="2026-01-01T00:00:00Z";
  db.update("INSERT INTO warehouses VALUES('warehouse','Legacy warehouse','HK')");db.update("INSERT INTO products VALUES('product','LEGACY','Legacy product','Test',100,?)",date);db.update("INSERT INTO inventory VALUES('warehouse','product',5,2)");
  db.update("INSERT INTO orders VALUES('legacy-order','LEGACY-001','Legacy buyer','warehouse','RESERVED',200,0,?,?)",date,date);db.update("INSERT INTO order_lines VALUES('line','legacy-order','product','LEGACY','Legacy product',100,2,0)");
  try(var app=start(url,true)){
   var service=app.getBean(OperationsService.class);var order=service.order("legacy-order");assertEquals("RESERVATION_EXPIRED",order.get("cancellation_reason"));assertEquals(date,order.get("created_at"));assertEquals(0,OperationsService.number(service.inventory().getFirst(),"reserved"));assertEquals(5,OperationsService.number(service.inventory().getFirst(),"on_hand"));
  }
 }
}
