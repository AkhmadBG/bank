package ru.practicum.front;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {
        "ru.practicum.interaction",
        "ru.practicum.front"
})
@EnableDiscoveryClient
@SpringBootApplication
public class FrontApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontApplication.class, args);
    }

}