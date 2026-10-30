package com.orderflow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@ConditionalOnProperty(name="orderflow.public-demo",havingValue="true")
public class PublicDemoController {
 private final AuthenticationManager manager;private final String username,password;
 public PublicDemoController(AuthenticationManager manager,@Value("${orderflow.username}")String username,@Value("${orderflow.password}")String password){this.manager=manager;this.username=username;this.password=password;}
 @PostMapping("/api/demo-login") public Map<String,Object> login(HttpServletRequest request,HttpServletResponse response){
  var auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username,password));
  new ChangeSessionIdAuthenticationStrategy().onAuthentication(auth,request,response);
  new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository()).onAuthentication(auth,request,response);
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);
  new HttpSessionSecurityContextRepository().saveContext(context,request,response);
  return Map.of("ok",true);
 }
}
