package com.kcompany.notification.service;

import com.kcompany.notification.configuration.FreeMarkerConfig;
import com.kcompany.notification.repository.notification.NotificationRepository;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@Service
public class NotificationServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Value("${mail.sender.host}")
    private String mailHost;
    @Value("${mail.sender.password}")
    private String senderPassword;
    @Value("${mail.sender.userName}")
    private String senderUserName;
    @Value("${mail.sender.email}")
    private String senderEmail;
    @Value("${mail.sender.port}")
    private int senderPort;

    @Autowired
    private NotificationRepository notificationRepository;

    public void sendEmail(Map<String, String> map, String templateName) {
        String email = map.get("email");
        Map<String, Object> inputData = new HashMap<>(map);
        sendEmail(email, inputData, templateName);
    }

    private String[] getRecipientEmailList(String email) {
        String[] toList;
        if(email.contains(",")) toList = email.split(",");
        else if(email.contains(";")) toList = email.split(";");
        else toList = new String[] {email};
        return toList;
    }

    private String getTemplateName(String template) {
        return ((!template.endsWith(".ftl")) ? template.concat(".ftl") : template);
    }

    public void sendEmail(String email, Map<String, Object> inputData, String templateName) {
        if(email == null || email.trim().isEmpty()) {
//            ToDO: throw error as recipient email-id required
        }

        String[] toList = getRecipientEmailList(email);
        templateName = getTemplateName(templateName);

        try {
            Template bodyTemplate = FreeMarkerConfig.getConfiguration(notificationRepository).getTemplate(templateName);
            if(null == bodyTemplate) {
                logger.error("Unable to load html content for {}", templateName);
                return;
            }

            Map<String, Object> sanitizeInput = sanitizaInput(inputData);
            String htmlContent = getHtmlcontent(bodyTemplate, sanitizeInput);

            if(null == inputData.get("attachment")) {
                triggerMail(htmlContent, toList, null, null, null, null);
            } else {
                List<Integer> list = (ArrayList<Integer>) inputData.get("attachment");
                byte[] attachmentData = new byte[list.size()];
                int i = 0;
                for(int b : list) {
                    attachmentData[i++] = (byte) b;
                }
                String fileName = (String) inputData.get("attachment_name");
                triggerMail(htmlContent, toList, null, null, attachmentData, fileName);
            }

        } catch (Exception e) {
//            ToDo: throw error
        }
    }

    private Map<String, Object> sanitizaInput(Map<String, Object> inputData) {
        Map<String, Object> outputData = new HashMap<>(inputData.size());
        for(String key : inputData.keySet()) {
            Object value = inputData.get(key);
            if(null == value) {
                outputData.put(key, value);
                continue;
            }
            if(value instanceof String) {
                String strValue = ((String) value).trim();
                if(strValue.isEmpty()) {
                    outputData.put(key, value);
                    continue;
                }
                value = !strValue.replaceAll("<[^>]*>", "").isEmpty() ? strValue : "[XSS Content will not be displayed]";
            }
            outputData.put(key, value);
        }
        return outputData;
    }

    private String getHtmlcontent(Template bodyTemplate, Map<String, Object> inputData) throws TemplateException, IOException {
        Map<String, Object> templateMap = new HashMap<>(2);
        templateMap.putAll(inputData);
        templateMap.putAll(getStaticData());
        return FreeMarkerTemplateUtils.processTemplateIntoString(bodyTemplate, templateMap);
    }

    private Map<String, String> getStaticData() {
        Map<String, String> staticData = new HashMap<>();
        return staticData;
    }

    public static boolean isArrayEmpty(String[] array) {
        if(array == null) {
            return true;
        } else if(array.length == 0) {
            return true;
        } else {
            for(String arr: array) {
                if(arr != null) {
                    return false;
                }
            }
            return true;
        }
    }

    private static String getSubjectFromHtmlTemplate(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.title().trim().isEmpty() ? "Subject" : doc.title();
    }

    private static String getFallbackTextContent(String htmlContent) {
        return Jsoup.parse(htmlContent).text();
    }

    private String[] triggerMail(String htmlContent, String[] to, String[] cc, String[] bcc, byte[] attachment, String attachmentName) {
        boolean hasToAddress = !isArrayEmpty(to);
        boolean hasCcAddress = !isArrayEmpty(cc);
        boolean hasBccAddress = !isArrayEmpty(bcc);

        if(!hasToAddress && !hasCcAddress && !hasBccAddress) {
            logger.warn("No valid recipient found either in TO, CC or BCC list.");
            return new String[0];
        }

        String senderName = senderUserName;
        logger.info("SenderName: {} and senderEmail: {}", senderName, senderEmail);

        senderName = (null == senderName || senderName.trim().isEmpty() ? senderEmail: senderName);
        logger.info("Sending email using Host: smtp.gmail.com, Port: 587, From: {}", senderEmail);

        String finalSenderName = senderName;
        MimeMessagePreparator mailMessage = mimeMessage -> {
            String str = StandardCharsets.UTF_8.name();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, str);
            message.setFrom(senderEmail, finalSenderName);
            if(hasToAddress) message.setTo(to);
            if(hasCcAddress) message.setTo(cc);
            if(hasBccAddress) message.setTo(bcc);

            message.setReplyTo(senderEmail);
            message.setSubject(getSubjectFromHtmlTemplate(htmlContent));
            message.setText(getFallbackTextContent(htmlContent), htmlContent);
//            message.addInline("logo.png", new ClassPathResource("/images/logo.png"));

            if(null != attachment) {
                logger.info("Including an attachment file {}", attachmentName);
                message.addAttachment(attachmentName, new ByteArrayResource(attachment));
            }
        };
        try {
            getJavaMailSender().send(mailMessage);
            logger.info("Successfully sent email for the recepients, To[{}] {}, CC[{}] {}, BCC[{}] {}",
                    (null == to) ? 0: to.length, (null == to) ? "[]": to,
                    (null == cc) ? 0: cc.length, (null == cc) ? "[]": cc,
                    (null == bcc) ? 0: bcc.length, (null == bcc) ? "[]": bcc);
        } catch(MailException mex) {
            logger.warn("Unable to send email, reason {}", mex.getMessage());
//            ToDo: throw err
        }
        return Stream.of(to, cc, bcc)
                .filter(array -> !isArrayEmpty(array))
                .flatMap(Stream::of)
                .toArray(String[]::new);
    }

    private JavaMailSenderImpl getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(senderPort);
        mailSender.setUsername(senderEmail);
        mailSender.setPassword(senderPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.connectiontimeout", 50000);
        props.put("mail.smtp.timeout", 50000);
        props.put("mail.smtp.writetimeout", 50000);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.debug", true);

        return mailSender;
    }
}