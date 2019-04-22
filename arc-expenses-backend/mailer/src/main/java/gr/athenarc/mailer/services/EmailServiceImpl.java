package gr.athenarc.mailer.services;

import gr.athenarc.mailer.domain.MailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.List;

@Component
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Override
    public void sendMail(List<MailMessage> mailMessages) {
        for(MailMessage mailMessage : mailMessages) {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
                logger.info(mailMessage.getBody());
                mimeMessage.setContent(mailMessage.getBody(), "text/html; charset=utf-8");
                helper.setFrom(mailMessage.getFrom(), mailMessage.getFromName());
                helper.setTo(mailMessage.getTo());
                helper.setSubject(mailMessage.getSubject());
                logger.info((String) mimeMessage.getContent());
//                javaMailSender.send(mimeMessage);
            } catch (MessagingException | IOException e) {
                logger.error("Could not send message", e);
            }
        }
    }
}
