package gr.athenarc.mailer.services;

import gr.athenarc.mailer.domain.MailMessage;
import gr.athenarc.mailer.domain.MailType;
import gr.athenarc.mailer.domain.WelcomeMessage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class MailReceiver {

    private static final Logger logger = LoggerFactory.getLogger(MailReceiver.class);

    @Autowired
    EmailService emailService;

    @JmsListener(destination = "mailbox")
    public void receiver(JSONObject jsonObject) {
        logger.info(jsonObject.toString());
        MailType mailType = MailType.valueOf(jsonObject.getString("message_type"));
        List<MailMessage> mailMessages= new ArrayList<>();
        String name = "";
        try {
            switch (mailType){
                case Initial:
                    for(Object toMail : jsonObject.getJSONArray("to")) {
                        mailMessages.add(new WelcomeMessage((String) toMail, name));
                    }
                    break;
                default:
                    mailMessages=null;
            }
        } catch (IOException e) {
            logger.error("Could not create WelcomeMessage",e);
        }


        if(mailMessages==null)
           logger.debug("Unrecognised mail type received " + mailType.name() );
        else
            emailService.sendMail(mailMessages);
    }
}
