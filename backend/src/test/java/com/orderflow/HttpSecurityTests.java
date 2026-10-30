package com.orderflow;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:http;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","orderflow.seed=false"})
class HttpSecurityTests {
 @LocalServerPort int port;
 @Test void loginRequiresCsrfAndPrivateEndpointsRequireSession()throws Exception{
  var cookies=new CookieManager();cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);var client=HttpClient.newBuilder().cookieHandler(cookies).build();String base="http://127.0.0.1:"+port;
  assertEquals(401,client.send(HttpRequest.newBuilder(URI.create(base+"/api/orders")).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
  var session=client.send(HttpRequest.newBuilder(URI.create(base+"/api/session")).GET().build(),HttpResponse.BodyHandlers.ofString());var matcher=Pattern.compile("\"csrfToken\":\"([^\"]+)\"").matcher(session.body());assertTrue(matcher.find());String csrf=matcher.group(1);
  var login=HttpRequest.newBuilder(URI.create(base+"/api/login")).header("Content-Type","application/x-www-form-urlencoded").header("X-CSRF-TOKEN",csrf).POST(HttpRequest.BodyPublishers.ofString("username=operator&password=local-demo-password")).build();assertEquals(200,client.send(login,HttpResponse.BodyHandlers.ofString()).statusCode());
  assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(base+"/api/orders")).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
  var afterLogin=client.send(HttpRequest.newBuilder(URI.create(base+"/api/session")).GET().build(),HttpResponse.BodyHandlers.ofString());var newToken=Pattern.compile("\"csrfToken\":\"([^\"]+)\"").matcher(afterLogin.body());assertTrue(newToken.find());
  var fractional=HttpRequest.newBuilder(URI.create(base+"/api/products")).header("Content-Type","application/json").header("X-CSRF-TOKEN",newToken.group(1)).POST(HttpRequest.BodyPublishers.ofString("{\"sku\":\"DECIMAL\",\"name\":\"Fractional cents\",\"category\":\"Test\",\"priceCents\":12.5}")).build();assertEquals(400,client.send(fractional,HttpResponse.BodyHandlers.ofString()).statusCode());
  assertEquals(403,client.send(HttpRequest.newBuilder(URI.create(base+"/api/warehouses")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Warehouse\",\"location\":\"HK\"}")).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
 }
}
