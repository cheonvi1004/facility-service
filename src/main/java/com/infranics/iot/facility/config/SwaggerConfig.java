package com.infranics.iot.facility.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {
   // TODO Auto-generated constructor stub
   
   @Bean
   OpenAPI openAPI() {
      Info info = new Info()
            .title("IOT facility Api Documentation")
            .version("v0.0.1")
            .description("IOT facility Api Documentation");
      return new OpenAPI()
            .components(new Components())
            .info(info);
   }
}
