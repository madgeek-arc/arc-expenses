package gr.athenarc.mailer.domain;


import gr.athenarc.mailer.mailEntities.InitialEmailEntity;
import gr.athenarc.mailer.mailEntities.TemplateLoader;

import java.io.IOException;

public class InitialMessage extends MailMessage {

    public InitialMessage(String from, String fromName, String to, String subject, String body) {
        super(from, fromName, to, subject, body);
    }

    public InitialMessage(String to, String request_id, String project_acronym, String creation_date, String final_amount, String subject, String url ) throws IOException {
        super("4485helpdesk@athena-innovation.gr", "ARC Helpdesk", to, "Υποβολή αιτήματος " + request_id, "");
        String format = TemplateLoader.loadFilledTemplate(new InitialEmailEntity(request_id,project_acronym,creation_date,final_amount,subject,url), "emails/initial.html");
        setBody(format);
    }
}
