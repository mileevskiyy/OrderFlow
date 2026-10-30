package com.orderflow;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return PasswordEncoderFactories.createDelegatingPasswordEncoder();}
 @Bean UserDetailsService users(PasswordEncoder encoder,@Value("${orderflow.username}")String username,@Value("${orderflow.password}")String password,@Value("${orderflow.production}")boolean production,@Value("${server.servlet.session.cookie.secure}")boolean secure){
  if(production&&(password.length()<16||password.equals("local-demo-password")||!secure))throw new IllegalStateException("Production requires an explicit 16+ character operator password and secure cookies.");
  return new InMemoryUserDetailsManager(User.withUsername(username).password(encoder.encode(password)).roles("OPERATOR").build());
 }
 @Bean SecurityFilterChain security(HttpSecurity http)throws Exception {
  http.authorizeHttpRequests(a->a.requestMatchers("/","/index.html","/assets/**","/favicon.ico","/api/health","/api/session","/error").permitAll().anyRequest().hasRole("OPERATOR"))
   .csrf(c->c.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
   .formLogin(f->f.loginPage("/").loginProcessingUrl("/api/login").successHandler((req,res,auth)->{res.setContentType("application/json");res.getWriter().write("{\"ok\":true}");}).failureHandler((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Incorrect username or password\"}");}).permitAll())
   .logout(l->l.logoutUrl("/api/logout").invalidateHttpSession(true).deleteCookies("JSESSIONID").logoutSuccessHandler((req,res,auth)->res.setStatus(204)))
   .exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Sign in to continue\"}");}).accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Session verification failed. Reload and retry.\"}");}));
  return http.build();
 }
}
