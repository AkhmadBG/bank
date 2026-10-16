package ru.practicum.notifications.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.notifications.entity.Notification;
import ru.practicum.notifications.repository.NotificationRepository;
import ru.practicum.notifications.service.NotificationService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void send(NotificationMessage message) {
        log.info("NOTIFICATION for {} [{}]: {}", message.login(), message.type(), message.message());

        Notification notification = Notification.builder()
                .recipient(message.login())
                .description(message.message())
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

}