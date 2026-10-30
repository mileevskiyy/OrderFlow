package com.orderflow;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.*;
import java.net.http.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:publicdemo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","orderflow.seed=false","orderflow.public-demo=true"})
class PublicDemoTests {
 @LocalServerPort int port;
 @Test void disposableDemoLoginRequiresCsrfAndCreatesUsableSession()throws Exception{
  var cookies=new CookieManager();cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);var client=HttpClient.newBuilder().cookieHandler(cookies).build();String base="http://127.0.0.1:"+port;
  var status=client.send(HttpRequest.newBuilder(URI.create(base+"/api/session")).GET().build(),HttpResponse.BodyHandlers.ofString());assertTrue(status.body().contains("\"publicDemo\":true"));var matcher=Pattern.compile("\"csrfToken\":\"([^\"]+)\"").matcher(status.body());assertTrue(matcher.find());
  assertEquals(403,client.send(HttpRequest.newBuilder(URI.create(base+"/api/demo-login")).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
  assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(base+"/api/demo-login")).header("X-CSRF-TOKEN",matcher.group(1)).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
  assertEquals(200,client.send(HttpRequest.newBuilder(URI.create(base+"/api/orders")).GET().build(),HttpResponse.BodyHandlers.ofString()).statusCode());
 }
}
