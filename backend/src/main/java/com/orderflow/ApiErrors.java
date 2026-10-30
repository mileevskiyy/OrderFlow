package com.orderflow;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(BusinessException.class) ResponseEntity<?> business(BusinessException e){return ResponseEntity.status(e.status()).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){String message=e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).findFirst().orElse("Invalid input");return ResponseEntity.badRequest().body(Map.of("error",message));}
 @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<?> json(){return ResponseEntity.badRequest().body(Map.of("error","Malformed request body"));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> integrity(){return ResponseEntity.status(409).body(Map.of("error","The operation conflicts with existing records or stock constraints."));}
 @ExceptionHandler(PessimisticLockingFailureException.class) ResponseEntity<?> contention(){return ResponseEntity.status(409).body(Map.of("error","The resource is busy. Retry with the same idempotency key."));}
}
