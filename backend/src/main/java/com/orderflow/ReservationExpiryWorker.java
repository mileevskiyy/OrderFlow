package com.orderflow;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name="orderflow.expiry-worker",havingValue="true",matchIfMissing=true)
public class ReservationExpiryWorker {
 private static final Logger log=LoggerFactory.getLogger(ReservationExpiryWorker.class);
 private final OperationsService service;
 public ReservationExpiryWorker(OperationsService service){this.service=service;}
 @EventListener(ApplicationReadyEvent.class)
 public void recover(){sweep();}
 @Scheduled(fixedDelayString="${orderflow.reservation-poll-ms:5000}",initialDelayString="${orderflow.reservation-poll-ms:5000}")
 public void sweep(){
  for(String id:service.dueReservations()){
   try{service.expireReservation(id);}catch(RuntimeException e){log.warn("Reservation expiry failed for {}; will retry: {}",id,e.getClass().getSimpleName());}
  }
 }
}
