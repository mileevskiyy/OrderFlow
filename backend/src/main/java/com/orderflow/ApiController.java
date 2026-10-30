package com.orderflow;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.util.*;
import static com.orderflow.Contracts.*;

@RestController @RequestMapping("/api")
public class ApiController {
 private final OperationsService service;
 public ApiController(OperationsService service){this.service=service;}
 @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","ok","application","OrderFlow","payments","simulated");}
 @GetMapping("/session") public Map<String,Object> session(Authentication auth,CsrfToken csrf){return Map.of("authenticated",auth!=null&&auth.isAuthenticated(),"username",auth==null?"":auth.getName(),"csrfToken",csrf.getToken());}
 @GetMapping("/overview") public Object overview(){return service.overview();}
 @GetMapping("/products") public Object products(){return service.products();}
 @PostMapping("/products") public ResponseEntity<?> product(@Valid @RequestBody ProductInput input){return ResponseEntity.status(201).body(service.createProduct(input));}
 @GetMapping("/warehouses") public Object warehouses(){return service.warehouses();}
 @PostMapping("/warehouses") public ResponseEntity<?> warehouse(@Valid @RequestBody WarehouseInput input){return ResponseEntity.status(201).body(service.createWarehouse(input));}
 @GetMapping("/inventory") public Object inventory(){return service.inventory();}
 @PostMapping("/inventory/receive") public Object receive(@Valid @RequestBody StockInput input,@RequestHeader(value="Idempotency-Key",required=false)String key){return service.receive(input,key);}
 @GetMapping("/orders") public Object orders(){return service.orders();}
 @GetMapping("/orders/{id}") public Object order(@PathVariable String id){return service.order(id);}
 @PostMapping("/orders") public ResponseEntity<?> create(@Valid @RequestBody OrderInput input,@RequestHeader(value="Idempotency-Key",required=false)String key){return ResponseEntity.status(201).body(service.createOrder(input,key));}
 @PostMapping("/orders/{id}/cancel") public Object cancel(@PathVariable String id){return service.cancel(id);}
 @PostMapping("/orders/{id}/fulfill") public Object fulfill(@PathVariable String id){return service.fulfill(id);}
 @PostMapping("/orders/{id}/returns") public Object returns(@PathVariable String id,@Valid @RequestBody ReturnInput input,@RequestHeader(value="Idempotency-Key",required=false)String key){return service.returnOrder(id,input,key);}
 @PostMapping("/payments/simulate") public Object payment(@Valid @RequestBody PaymentInput input){return service.payment(input);}
 @GetMapping("/movements") public Object movements(){return service.movements();}
 @GetMapping("/activity") public Object activity(){return service.activity();}
}
