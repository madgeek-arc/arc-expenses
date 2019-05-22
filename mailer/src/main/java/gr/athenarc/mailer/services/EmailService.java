package gr.athenarc.mailer.services;


import gr.athenarc.mailer.domain.MailMessage;

import java.util.List;

public interface EmailService {

    void sendMail(List<MailMessage> mailMessages);
}
