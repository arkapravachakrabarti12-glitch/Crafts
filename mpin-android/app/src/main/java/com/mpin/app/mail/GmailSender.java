package com.mpin.app.mail;

import com.mpin.app.core.EmailContent;
import com.mpin.app.data.EmailSettings;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Sends email through Gmail's SMTP server with JavaMail — the same API that
 * Spring's JavaMailSender wraps. Gmail needs an App Password for this
 * (Google Account → Security → 2-Step Verification → App passwords).
 *
 * Blocking: call from a background thread.
 */
public final class GmailSender {

    private static final String HOST = "smtp.gmail.com";
    private static final String PORT = "587";

    static {
        // JavaMail on Android doesn't always find its content handlers; register them explicitly.
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        CommandMap.setDefaultCommandMap(mc);
    }

    private GmailSender() {
    }

    public static void send(EmailSettings settings, EmailContent content) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        Thread.currentThread().setContextClassLoader(GmailSender.class.getClassLoader());
        MimeMessage message = buildMessage(Session.getInstance(props), settings, content);

        // Google shows app passwords as "abcd efgh ijkl mnop"; the spaces aren't part of it.
        Transport.send(message, settings.senderEmail, settings.appPassword.replace(" ", ""));
    }

    static MimeMessage buildMessage(Session session, EmailSettings settings, EmailContent content)
            throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(settings.senderEmail, "MPIN Generator", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(settings.senderEmail));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.recipient()));
        message.setSubject(content.subject, "UTF-8");

        MimeBodyPart text = new MimeBodyPart();
        text.setText(content.text, "UTF-8");
        MimeBodyPart html = new MimeBodyPart();
        html.setContent(content.html, "text/html; charset=UTF-8");
        MimeMultipart body = new MimeMultipart("alternative");
        body.addBodyPart(text);
        body.addBodyPart(html);
        message.setContent(body);
        message.saveChanges();
        return message;
    }

    public static String describe(MessagingException e) {
        if (e instanceof javax.mail.AuthenticationFailedException) {
            return "Gmail rejected the login. Check the Gmail address and use an App Password.";
        }
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        if (cause instanceof java.net.UnknownHostException || cause instanceof java.net.SocketTimeoutException
                || cause instanceof java.net.ConnectException) {
            return "No connection to Gmail. Check your internet connection.";
        }
        return e.getMessage() != null ? e.getMessage() : "Sending failed";
    }
}
