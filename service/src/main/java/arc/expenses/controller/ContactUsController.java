package arc.expenses.controller;

import gr.athenarc.domain.ContactUsMail;
import io.swagger.annotations.Api;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/contactUs")
@Api(description = "Contact Us API  ",  tags = {"contact-us"})
public class ContactUsController {

    Logger logger = Logger.getLogger(ContactUsController.class);

    @Autowired
    @Qualifier("jmsQueueTemplate")
    private JmsTemplate jmsTemplate;

    @Value("${contact.address:4485helpdesk@athena-innovation.gr}")
    private String contactAddress;

    @RequestMapping(value =  "/sendMail", method = RequestMethod.POST)
    public void contactUs(@RequestBody ContactUsMail mail) {


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message_type","CONTACT");
        jsonObject.put("subject",mail.getSubject());
        jsonObject.put("body",mail.getMessage());
        jsonObject.put("fullname",mail.getName() + " " + mail.getSurname());
        jsonObject.put("email", mail.getEmail());

        jmsTemplate.convertAndSend("mailbox",jsonObject.toString());

        logger.info("Contact Us email was sent from: " + mail.getEmail());
    }

}