package com.edutest.webserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.edutest")
public class EdutestWebServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdutestWebServerApplication.class, args);
    }

}
