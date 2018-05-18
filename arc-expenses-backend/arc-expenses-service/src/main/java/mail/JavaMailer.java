package mail;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;


@Component
public class JavaMailer {
    @Value("${mail.username}")
    private String username;

    @Value("${mail.host}")
    private String smtpHost;

    @Value("${mail.password}")
    private String password;

    @Value("${mail.debug}")
    private boolean mailDebug;


    private Properties properties;
    private static Logger logger = LogManager.getLogger(JavaMailer.class);


    @PostConstruct
    public void init() {
        properties = new Properties();
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.sasl.enable", "true");
        properties.put("mail.debug", mailDebug);
        properties.put("mail.smtp.host", smtpHost);
    }

    public void sendEmail(String to, String subject, String text) {
        Session session =Session.getInstance(properties);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(username);
            msg.setRecipients(Message.RecipientType.TO, to);
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setText(text);
            Transport transport = session.getTransport("smtp");
            transport.connect(smtpHost, username, password);
            Transport.send(msg, msg.getAllRecipients(), username, password);
        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        }
    }
}
