package ru.practicum.accounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.practicum.interaction.kafka.NotificationPublisher;

@EnableFeignClients(basePackages = {
        "ru.practicum.interaction"
})
@Import(NotificationPublisher.class)
@SpringBootApplication
@EnableScheduling
public class AccountsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountsApplication.class, args);
    }

}