package com.orderflow;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class ReservationPolicy {
 private final long seconds;
 public ReservationPolicy(@Value("${orderflow.reservation-seconds:0}")long configured,@Value("${orderflow.public-demo:false}")boolean demo){
  seconds=configured==0?(demo?60:900):configured;
  if(seconds<1||seconds>86400)throw new IllegalArgumentException("Reservation lifetime must be 1–86400 seconds (0 selects the mode default).");
 }
 public long seconds(){return seconds;}
}
