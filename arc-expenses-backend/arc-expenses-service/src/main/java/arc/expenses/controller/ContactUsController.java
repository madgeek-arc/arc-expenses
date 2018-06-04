package arc.expenses.controller;

import arc.expenses.mail.JavaMailer;
import com.google.common.base.Charsets;
import io.swagger.annotations.Api;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RequestMapping(value =  "/sendMail", method = RequestMethod.POST)
    public void contactUs(@RequestBody String body) {
        JSONObject json = new JSONObject(body);
        // TODO: write this code better
        String text = "Από: " + json.getString("name") + " " + json.getString("surname") + "\n\n\n" +
                json.getString("message");
        javaMailer.sendEmailWithBCC("${contact.address:test.athenarc@gmail.com}", json.getString("subject"),
                text, "${contact.address:spyroukostas@msn.com}");
        logger.info("Contact Us email was sent from: " + json.getString("email"));
    }

//    private String utf8Encode(String value) {
//        return new String(value.getBytes(Charsets.UTF_8), Charsets.UTF_8);
//    }

}
