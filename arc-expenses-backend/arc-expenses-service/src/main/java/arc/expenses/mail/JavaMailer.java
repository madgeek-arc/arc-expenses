package arc.expenses.mail;

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
import java.util.List;
import java.util.Properties;


@Component
public class JavaMailer {
    @Value("${mail.username}")
    private String address;

    @Value("${mail.host}")
    private String smtpHost;

//    @Value("${mail.port}")
//    private String smtpPort;

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
//        properties.put("mail.smtp.port", smtpPort);
    }

    public void sendEmail(String to, String subject, String text) {
        Session session = Session.getInstance(properties);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(address);
            if (mailDebug) {
                msg.setRecipients(Message.RecipientType.TO, address);
                msg.setText(text + debugText(to));
            } else {
                msg.setRecipients(Message.RecipientType.TO, to);
                msg.setText(text);
            }
            msg.setSubject(subject);
            msg.setSentDate(new Date());
//            msg.setText(text);
            Transport transport = session.getTransport("smtp");
            transport.connect(smtpHost, address, password);
            Transport.send(msg, msg.getAllRecipients(), address, password);
        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        }
    }

    public void sendEmail(List<String> mailList, String subject, String text) {
        Session session = Session.getInstance(properties);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(address);
            if (mailDebug) {
                msg.setRecipients(Message.RecipientType.TO, address);
                msg.setText(text + debugText(addressListToString(mailList)));
            } else {
                msg.setRecipients(Message.RecipientType.TO, addressListToString(mailList));
                msg.setText(text);
            }
            msg.setSubject(subject);
            msg.setSentDate(new Date());
//            msg.setText(text);
            Transport transport = session.getTransport("smtp");
            transport.connect(smtpHost, address, password);
            Transport.send(msg, msg.getAllRecipients(), address, password);
        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        }
    }

    public void sendEmailWithCC(String to, String subject, String text, String cc_addr, boolean bcc) {
        Session session = Session.getInstance(properties);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(address);
            if (mailDebug) {
                msg.setRecipients(Message.RecipientType.TO, address);
            } else {
                msg.setRecipients(Message.RecipientType.TO, to);
                if (bcc) {
                    msg.setRecipients(Message.RecipientType.BCC, cc_addr);
                } else {
                    msg.setRecipients(Message.RecipientType.CC, cc_addr);
                }
            }
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setText(text);
            Transport transport = session.getTransport("smtp");
            transport.connect(smtpHost, address, password);
            Transport.send(msg, msg.getAllRecipients(), address, password);
        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        }
    }

    private String addressListToString(List<String> list) {
        String addresses = "";
        if (!list.isEmpty()) {
            for (int i = 0; i < list.size()-1; i++) {
                addresses += list.get(i) + ", ";
            }
            addresses += list.get(list.size()-1);
        }
        return addresses;
    }

    private String debugText(String email) {
        return "\n\n Προορισμός μηνύματος: " + email;
    }
}