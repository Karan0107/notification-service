package com.kcompany.notification.configuration;

import com.kcompany.notification.entity.notification.Notification;
import com.kcompany.notification.repository.notification.NotificationRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;

import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.util.List;

public class FreeMarkerConfig {
    public static freemarker.template.Configuration configuration = null;

    private FreeMarkerConfig(NotificationRepository notificationRepository) {
        configuration = loadTemplate(notificationRepository);
    }

    public static freemarker.template.Configuration getConfiguration(NotificationRepository notificationRepository) {
        if(configuration == null) {
            new FreeMarkerConfig(notificationRepository);
        }
        return configuration;
    }

    public freemarker.template.Configuration loadTemplate(NotificationRepository notificationRepository) {
        StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        try {
            List<Notification> notificationList = notificationRepository.findAll();
            notificationList.forEach(details -> {
                Blob template = details.template;
                byte[] bytes = new byte[0];
                try {
                    bytes = template.getBytes(1, (int) template.length());
                } catch(Exception e) {
                    e.printStackTrace();
                }
                stringTemplateLoader.putTemplate(details.name, new String(bytes, StandardCharsets.UTF_8));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        freemarker.template.Configuration config = new Configuration(Configuration.VERSION_2_3_27);
        config.setTemplateLoader(stringTemplateLoader);
        return config;
    }
}
