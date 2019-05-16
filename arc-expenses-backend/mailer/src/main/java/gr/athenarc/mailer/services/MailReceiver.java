package gr.athenarc.mailer.services;

import gr.athenarc.mailer.domain.*;
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
                case APPROVE:
                    for(Object toMail : jsonObject.getJSONArray("to")) {
                        mailMessages.add(new ApproveMessage((String) toMail,
                                jsonObject.getString("request_id"),
                                jsonObject.getString("project_acronym"),
                                jsonObject.getString("creation_date"),
                                jsonObject.getString("final_amount"),
                                jsonObject.getString("subject"),
                                jsonObject.getString("url")));
                    }
                    break;
                case INITIAL:
                    for(Object toMail: jsonObject.getJSONArray("to")){
                        mailMessages.add(new InitialMessage((String) toMail,
                                jsonObject.getString("request_id"),
                                jsonObject.getString("project_acronym"),
                                jsonObject.getString("creation_date"),
                                jsonObject.getString("final_amount"),
                                jsonObject.getString("subject"),
                                jsonObject.getString("url")));
                    }
                    break;
                case CANCEL:
                    for(Object toMail: jsonObject.getJSONArray("to")){
                        mailMessages.add(new CancelMessage((String) toMail,
                                jsonObject.getString("request_id"),
                                jsonObject.getString("project_acronym"),
                                jsonObject.getString("creation_date"),
                                jsonObject.getString("final_amount"),
                                jsonObject.getString("subject"),
                                jsonObject.getString("url")));
                    }
                    break;
                default:
                    mailMessages=null;
            }
        } catch (IOException e) {
            logger.error("Could not create ApproveMessage",e);
        }


        if(mailMessages==null)
           logger.debug("Unrecognised mail type received " + mailType.name() );
        else
            emailService.sendMail(mailMessages);
    }
}
