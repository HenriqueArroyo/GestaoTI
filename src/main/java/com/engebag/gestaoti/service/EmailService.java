package com.engebag.gestaoti.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remetente;

    public void enviarEmailTexto(String destinatario, String assunto, String mensagem) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(remetente);
        email.setTo(destinatario);
        email.setSubject(assunto);
        email.setText(mensagem);

        mailSender.send(email);
    }
}