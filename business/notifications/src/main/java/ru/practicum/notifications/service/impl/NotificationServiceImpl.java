package ru.practicum.notifications.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.notifications.entity.Notification;
import ru.practicum.notifications.repository.NotificationRepository;
import ru.practicum.notifications.service.NotificationService;
import ru.practicum.interaction.notification.dto.NotificationRequest;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .recipient(request.recipient())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

}