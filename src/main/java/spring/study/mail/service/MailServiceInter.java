package spring.study.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public interface MailServiceInter {
    MimeMessage createMessage(String to) throws MessagingException, UnsupportedEncodingException;

    String createkey();

    String sendSimpleMessage(String to) throws Exception;
}
