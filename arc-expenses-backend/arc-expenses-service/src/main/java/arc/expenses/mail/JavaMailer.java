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
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;


@Component
public class JavaMailer {
    @Value("${mail.username}")
    private String address;

    @Value("${mail.password}")
    private String password;

    @Value("${mail.host:mail.ilsp.gr}")
    private String smtpHost;

    @Value("${mail.port:2525}")
    private String smtpPort;

    @Value("${mail.debug:false}")
    private boolean mailDebug;

    @Value("${mail.debug.address:test.athenarc.gr}")
    private String debugAddress;

    @Value("${mail.smtp.ssl:false}")
    private String mailSSL;

    @Value("${mail.smtp.sasl:false}")
    private String mailSASL;

    @Value("${mail.noreply.email:no-reply@athena-innovation.gr}")
    private String noReplyMail;

    @Value("${mail.noreply.from:Athena Research & Innovation Center}")
    private String noReplyFrom;


    private Properties properties;
    private static Logger logger = LogManager.getLogger(JavaMailer.class);


    @PostConstruct
    public void init() {
        properties = new Properties();
        properties.put("mail.smtp.ssl.enable", mailSSL);
        properties.put("mail.smtp.sasl.enable", mailSASL);
        properties.put("mail.debug", mailDebug);
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);
    }

    public void sendEmail(String to, String subject, String text) {
        Session session = Session.getInstance(properties, null);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(noReplyMail, noReplyFrom));
            if (mailDebug) {
                msg.setRecipients(Message.RecipientType.TO, debugAddress);
                msg.setText(text + debugText(to));
            } else {
                msg.setRecipients(Message.RecipientType.TO, to);
                msg.setText(text);
            }
            msg.setSubject(subject);
            msg.setSentDate(new Date());

            Transport transport = session.getTransport("smtp");
            if (mailSSL == "true") {
                transport.connect(smtpHost, address, password);
                transport.sendMessage(msg,msg.getAllRecipients());
//                Transport.send(msg, msg.getAllRecipients(), address, password);
            } else {
                Transport.send(msg, msg.getAllRecipients());
            }

        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void sendEmail(String fromEmail, String from, String to, String subject, String text) {
        Session session = Session.getInstance(properties);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromEmail, from));
            if (mailDebug) {
                msg.setRecipients(Message.RecipientType.TO, debugAddress);
                msg.setText(text + debugText(to));
            } else {
                msg.setRecipients(Message.RecipientType.TO, to);
                msg.setText(text);
            }
            msg.setSubject(subject);
            msg.setSentDate(new Date());

            Transport transport = session.getTransport("smtp");
            if (mailSSL == "true" ) {
                transport.connect(smtpHost, address, password);
                transport.sendMessage(msg,msg.getAllRecipients());
//                Transport.send(msg, msg.getAllRecipients(), address, password);
            } else {
                Transport.send(msg, msg.getAllRecipients());
            }

        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void sendEmail(List<String> mailList, String subject, String text) {
        Session session = Session.getInstance(properties);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(noReplyMail, noReplyFrom));
            if (mailDebug) {
                msg.setRecipients(Message.RecipientType.TO, debugAddress);
                msg.setText(text + debugText(addressListToString(mailList)));
            } else {
                msg.setRecipients(Message.RecipientType.TO, addressList(mailList));
                msg.setText(text);
            }
            msg.setSubject(subject);
            msg.setSentDate(new Date());

            Transport transport = session.getTransport("smtp");
            if (mailSSL == "true") {
                transport.connect(smtpHost, address, password);
                transport.sendMessage(msg,msg.getAllRecipients());
//                Transport.send(msg, msg.getAllRecipients(), address, password);
            } else {
                Transport.send(msg, msg.getAllRecipients());
            }

        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void sendEmailWithCC(String to, String subject, String text, String cc_addr, boolean bcc) {
        Session session = Session.getInstance(properties);

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(noReplyMail, noReplyFrom));
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
            if (mailSSL == "true") {
                transport.connect(smtpHost, address, password);
                transport.sendMessage(msg,msg.getAllRecipients());
//                Transport.send(msg, msg.getAllRecipients(), address, password);
            } else {
                Transport.send(msg, msg.getAllRecipients());
            }

        } catch (MessagingException mex) {
            logger.error("sendEmail failed, exception: " + mex);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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

    private InternetAddress[] addressList(List<String> list) {
        List<InternetAddress> addresses = new ArrayList<>();
        list.forEach(addr -> {
            try {
                addresses.add(new InternetAddress(addr));
            } catch (AddressException e) {
                e.printStackTrace();
            }
        });
        return (InternetAddress[]) addresses.toArray();
    }

    private String debugText(String email) {
        return "\n\n Προορισμός μηνύματος: " + email;
    }
}
