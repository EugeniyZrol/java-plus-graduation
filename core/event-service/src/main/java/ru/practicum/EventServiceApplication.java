package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum", "client"})
@EntityScan(basePackages = {"ru.practicum"})
@EnableJpaRepositories(basePackages = {"ru.practicum"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"ru.practicum.interaction.client", "ru.practicum.event.client", "client"})
public class EventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}