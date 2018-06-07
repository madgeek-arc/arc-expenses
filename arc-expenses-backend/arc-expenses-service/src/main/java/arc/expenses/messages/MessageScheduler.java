package arc.expenses.messages;

import arc.expenses.mail.EmailMessage;
import arc.expenses.mail.JavaMailer;
import arc.expenses.service.RequestServiceImpl;
import arc.expenses.service.UserServiceImpl;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableScheduling
public class MessageScheduler  {

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private JavaMailer javaMailer;

    Logger logger = Logger.getLogger(MessageScheduler.class);

    List<EmailMessage> queue = null;

    @Scheduled(cron = "${messages.cronExpr:0 0 8 * * ?}")
//    @Scheduled(cron = "* * * * * ?") // use this for debugging
    public void scheduledEmails() {
        logger.info("Sending scheduled emails");
        queue = new ArrayList<>();
        List<User> users = userService.getAll(new FacetFilter()).getResults();

        for (User user: users) {
            if ( "true".equals(user.getReceiveEmails()) && "false".equals(user.getImmediateEmails()) ) {
                List<Request> user_requests = requestService.getPendingRequests(user.getEmail());
                String text = createDigest(user_requests);
                queue.add(new EmailMessage(user.getEmail(), "[ARC-Expenses] Daily Digest", text));
            }
        }
        queue.forEach(email -> javaMailer.sendEmail(email.getRecipient(), email.getSubject(), email.getText()));
    }

    private String createDigest(List<Request> requests) {
        String text = "Εκκρεμεί η διεκπεραίωση των παρακάτω αιτημάτων: ";
        for (Request req: requests) {
            text += "\n" + req.getId();
        }
        return text;
    }

}
