package gr.athenarc.mailer.domain;


import gr.athenarc.mailer.mailEntities.CancelEmailEntity;
import gr.athenarc.mailer.mailEntities.TemplateLoader;

import java.io.IOException;

public class CancelMessage extends MailMessage {

    public CancelMessage(String from, String fromName, String to, String subject, String body) {
        super(from, fromName, to, subject, body);
    }

    public CancelMessage(String to, String request_id, String project_acronym, String creation_date, String final_amount, String subject, String url ) throws IOException {
        super("4485helpdesk@athena-innovation.gr", "ARC Helpdesk", to, "Ακύρωση του αιτήματος " + request_id, "");
        String format = TemplateLoader.loadFilledTemplate(new CancelEmailEntity(request_id,project_acronym,creation_date,final_amount,subject,url), "emails/cancel.html");
        setBody(format);
    }
}
