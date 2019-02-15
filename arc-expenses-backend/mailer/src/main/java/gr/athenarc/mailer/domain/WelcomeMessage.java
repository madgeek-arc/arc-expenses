package gr.athenarc.mailer.domain;


import gr.athenarc.mailer.mailEntities.TemplateLoader;
import gr.athenarc.mailer.mailEntities.WelcomeMailEntity;

import java.io.IOException;

public class WelcomeMessage extends MailMessage {

    public WelcomeMessage(String from, String fromName, String to, String subject, String body) {
        super(from, fromName, to, subject, body);
    }

    public WelcomeMessage(String to, String name) throws IOException {
        super("info@clicktotherapy.com", "ClicktoTherapy", to, "Καλώς ήρθες στο ClicktoTherapy!", "");
        String format = TemplateLoader.loadFilledTemplate(new WelcomeMailEntity(name), "emails/welcome.html");
        setBody(format);
    }
}
