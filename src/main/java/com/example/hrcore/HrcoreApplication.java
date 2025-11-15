package com.example.hrcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HrcoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrcoreApplication.class, args);
    }

}
