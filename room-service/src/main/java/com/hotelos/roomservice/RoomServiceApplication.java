package com.hotelos.roomservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = { "com.hotelos.shared.entities" })
@EnableJpaRepositories(basePackages = { "com.hotelos.roomservice.repository" })
@ComponentScan(basePackages = { "com.hotelos.roomservice", "com.hotelos.shared" })
@EnableScheduling
public class RoomServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoomServiceApplication.class, args);
    }
}
