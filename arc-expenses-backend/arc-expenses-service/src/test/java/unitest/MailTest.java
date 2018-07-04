package unitest;

import arc.expenses.messages.MessageScheduler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/applicationContext.xml")
@WebAppConfiguration
public class MailTest {

    @Autowired
    MessageScheduler messageScheduler;


    @Test
    public void scheduledEmails(){
        messageScheduler.scheduledEmails();
    }

}
