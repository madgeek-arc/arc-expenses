package arc.expenses.messages;

import arc.expenses.mail.EmailMessage;
import arc.expenses.service.UserServiceImpl;
import gr.athenarc.domain.User;
import org.apache.logging.log4j.core.config.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scheduled
public class MessageScheduler {

    @Autowired
    UserServiceImpl userService;
    Map<User, List<EmailMessage>> queue = null;

    public void addToQueue(List<EmailMessage> emails) {
        List<EmailMessage> messages = null;
        if (queue == null) {
            Map<User, List<EmailMessage>> queue = new HashMap<>();
        }
        for (EmailMessage email: emails) {
            User user = userService.get(email.getRecipient());
            if (queue.containsKey(user)) {
                messages = queue.get(user);
                messages.add(email);
                queue.put(user, messages);
            } else {
                messages = new ArrayList<>();
                messages.add(email);
                queue.put(user, messages);
            }
        }
    }
}
