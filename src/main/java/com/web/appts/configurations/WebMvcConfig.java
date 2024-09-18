
package com.web.appts.configurations;

import java.time.LocalDate;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private LocalDate allowedEndDate;
  private String key;
  @PersistenceContext
  private EntityManager entityManager;

  public WebMvcConfig() {
    this.allowedEndDate = LocalDate.MAX;
    this.key = "RYGB";
  }

  @Bean
  public EntityManager getEntityManager() {
    return this.entityManager;
  }

  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ExpiryInterceptor(this.allowedEndDate, this.key));
  }

  public String getKey() {
    return this.key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
