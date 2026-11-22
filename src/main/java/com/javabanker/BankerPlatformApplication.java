package com.javabanker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BankerPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankerPlatformApplication.class, args);
    }
}
