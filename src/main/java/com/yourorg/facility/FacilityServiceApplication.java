package com.yourorg.facility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FacilityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FacilityServiceApplication.class, args);
    }
}
