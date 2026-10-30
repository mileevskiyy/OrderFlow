package com.orderflow;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import java.util.*;
import static com.orderflow.Contracts.*;

@Component
public class DemoData implements ApplicationRunner {
 private final OperationsService service;private final boolean enabled;
 public DemoData(OperationsService service,@Value("${orderflow.seed}")boolean enabled){this.service=service;this.enabled=enabled;}
 @Override public void run(ApplicationArguments args){if(!enabled||!service.products().isEmpty())return;
  var hk=service.createWarehouse(new WarehouseInput("Hong Kong studio","Hong Kong SAR"));var sg=service.createWarehouse(new WarehouseInput("Singapore hub","Singapore"));
  var inputs=List.of(new ProductInput("AU-101","Studio wireless headphones","Audio",14900),new ProductInput("DK-201","Mechanical keyboard","Desk",11900),new ProductInput("DK-202","Precision wireless mouse","Desk",6900),new ProductInput("CN-301","USB-C desktop hub","Connectivity",8900),new ProductInput("AU-102","Desktop microphone","Audio",12900),new ProductInput("DK-203","Aluminium laptop stand","Desk",5900),new ProductInput("CN-302","Braided USB-C cable","Connectivity",1900),new ProductInput("VD-401","4K conference webcam","Video",15900));
  var products=new ArrayList<Map<String,Object>>();int i=0;for(var input:inputs){var p=service.createProduct(input);products.add(p);service.receive(new StockInput((String)hk.get("id"),(String)p.get("id"),i==6?4:16+i*3,"Fictional opening stock"),"seed-hk-"+i);service.receive(new StockInput((String)sg.get("id"),(String)p.get("id"),8+i,"Fictional opening stock"),"seed-sg-"+i);i++;}
  var customers=List.of("Northstar Studio · demo","Paper & Pixel · demo","Harbour Design · demo","Orbit Works · demo","Monday Supply · demo","Elm Office · demo");
  for(i=0;i<customers.size();i++){var p=products.get(i);var order=service.createOrder(new OrderInput(customers.get(i),(String)hk.get("id"),List.of(new LineInput((String)p.get("id"),2))),"seed-order-"+i);String id=(String)order.get("id");if(i==5){service.cancel(id);continue;}if(i==2)continue;service.payment(new PaymentInput("seed-payment-"+i,id,OperationsService.number(order,"total_cents"),"SUCCEEDED"));if(i!=3)service.fulfill(id);if(i==4){@SuppressWarnings("unchecked")var lines=(List<Map<String,Object>>)order.get("lines");service.returnOrder(id,new ReturnInput("Fictional partial return",List.of(new ReturnLineInput((String)lines.getFirst().get("id"),1))),"seed-return-"+i);}}
 }
}
