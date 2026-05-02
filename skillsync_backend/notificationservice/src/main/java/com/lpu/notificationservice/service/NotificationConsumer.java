package com.lpu.notificationservice.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lpu.notificationservice.config.RabbitConfig;
import com.lpu.notificationservice.dto.NotificationEvent;
import com.lpu.notificationservice.entity.Notification;
import com.lpu.notificationservice.repository.NotificationRepository;

@Service
public class NotificationConsumer {

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationRepository repo;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void consume(NotificationEvent event) {
        String status = "SENT";

        try {
            emailService.sendEmail(event.getEmail(), event.getSubject(), event.getMessage());
            System.out.println("Email sent to: " + event.getEmail());
        } catch (Exception e) {
            status = "FAILED";
            System.out.println("Email failed for " + event.getEmail() + ": " + e.getMessage());
        }

        try {
            Notification notification = Notification.builder()
                    .userId(event.getUserId())
                    .email(event.getEmail())
                    .message(event.getMessage())
                    .type(event.getType())
                    .status(status)
                    .build();

            repo.save(notification);
        } catch (Exception e) {
            System.out.println("Notification save failed for " + event.getEmail() + ": " + e.getMessage());
        }
    }
}
