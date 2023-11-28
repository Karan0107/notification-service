package com.kcompany.notification.controller;

import com.kcompany.notification.service.NotificationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/notification")
public class NotificationController {
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationServiceImpl notificationService;

    @GetMapping
    public String printString() {
        return "Hello Karan";
    }

    @PostMapping("/{templateName}")
    public void sendEmailNotification(@RequestBody Map<String, String> map, @PathVariable String templateName) {
        try {
            notificationService.sendEmail(map, templateName);
        } catch(Exception e) {
            logger.error("Error Occurred while sending email to {}", map.get("email"));
        }
    }
}
