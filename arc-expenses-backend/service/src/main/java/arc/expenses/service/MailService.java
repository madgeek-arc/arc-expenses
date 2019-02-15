package arc.expenses.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("mailService")
public class MailService {

    private static Logger logger = LogManager.getLogger(MailService.class);

    @Autowired
    @Qualifier("jmsQueueTemplate")
    JmsTemplate jmsTemplate;

    void sendMail(String type, List<String> whoTo){

        logger.info("Sending mail of type "+type + " to " + whoTo.stream().collect(Collectors.joining(",")));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message_type",type);
        jsonObject.put("to",whoTo);

        logger.info(jsonObject.toString());
        jmsTemplate.convertAndSend("mailbox", jsonObject.toString());
    }

}
