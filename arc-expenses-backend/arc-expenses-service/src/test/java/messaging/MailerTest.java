package messaging;

import mail.EmailMessage;
import mail.JavaMailer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MailServiceTestConfiguration.class})
public class MailerTest {

    @Autowired
    private JavaMailer javaMailer;

    @Test
    public void sendTestMail() {
        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setRecipient("spyroukostas@msn.com");
        emailMessage.setSubject("test");
        emailMessage.setText("this is a test message just to annoy you");
        javaMailer.sendEmail(emailMessage.getRecipient(), emailMessage.getSubject(), emailMessage.getText());
    }
}
