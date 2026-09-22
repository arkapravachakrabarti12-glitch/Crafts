package com.teachnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeachNetApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeachNetApplication.class, args);
    }
}
