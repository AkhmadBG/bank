package ru.practicum.cash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.interaction.kafka.NotificationPublisher;

@EnableFeignClients(basePackages = {
        "ru.practicum.interaction",
})
@Import(NotificationPublisher.class)
@SpringBootApplication
public class CashApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashApplication.class, args);
    }

}