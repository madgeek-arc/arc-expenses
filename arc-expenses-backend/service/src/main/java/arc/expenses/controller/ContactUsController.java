package arc.expenses.controller;

import arc.expenses.mail.JavaMailer;
import gr.athenarc.domain.ContactUsMail;
import io.swagger.annotations.Api;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    JavaMailer javaMailer;

    @Value("${contact.address:4485helpdesk@athena-innovation.gr}")
    private String contactAddress;

    @RequestMapping(value =  "/sendMail", method = RequestMethod.POST)
    public void contactUs(@RequestBody ContactUsMail mail) {
        javaMailer.sendEmail(mail.getEmail(), mail.getName()+" "+mail.getSurname(),
                contactAddress, mail.getSubject(), mail.getMessage());

        logger.info("Contact Us email was sent from: " + mail.getEmail());
    }

}
