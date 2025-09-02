package com.edutest.webserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@ComponentScan(basePackages = "com.edutest")
@EnableJpaRepositories(basePackages = "com.edutest.persistance.repository")
@EntityScan(basePackages = "com.edutest.persistance.entity")
@EnableJpaAuditing
public class EdutestWebServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdutestWebServerApplication.class, args);
    }

}
