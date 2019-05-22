package gr.athenarc.mailer.domain;


import gr.athenarc.mailer.mailEntities.TemplateLoader;
import gr.athenarc.mailer.mailEntities.ApproveEmailEntity;

import java.io.IOException;

public class ApproveMessage extends MailMessage {

    public ApproveMessage(String from, String fromName, String to, String subject, String body) {
        super(from, fromName, to, subject, body);
    }

    public ApproveMessage(String to, String request_id, String project_acronym, String creation_date, String final_amount, String subject, String url ) throws IOException {
        super("4485helpdesk@athena-innovation.gr", "ARC Helpdesk", to, "Ενημέρωση σχετικά με το αίτημα " + request_id, "");
        String format = TemplateLoader.loadFilledTemplate(new ApproveEmailEntity(request_id,project_acronym,creation_date,final_amount,subject,url), "emails/approve.html");
        setBody(format);
    }
}
