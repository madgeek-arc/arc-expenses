package gr.athenarc.mailer.domain;


import gr.athenarc.mailer.mailEntities.ContactEmailEntity;
import gr.athenarc.mailer.mailEntities.TemplateLoader;

import java.io.IOException;

public class ContactMessage extends MailMessage {

    public ContactMessage(String from, String fromName, String to, String subject, String body) {
        super(from, fromName, to, subject, body);
    }

    public ContactMessage(String subject, String body, String fullname, String email ) throws IOException {
        super("4485helpdesk@athena-innovation.gr", "ARC Helpdesk", "4485helpdesk@athena-innovation.gr", subject, "");
        String format = TemplateLoader.loadFilledTemplate(new ContactEmailEntity(body,fullname,email), "emails/contact.html");
        setBody(format);
    }
}
