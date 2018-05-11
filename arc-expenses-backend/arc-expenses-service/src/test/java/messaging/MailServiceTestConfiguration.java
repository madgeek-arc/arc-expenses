package messaging;

import mail.JavaMailer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class MailServiceTestConfiguration {

    @Autowired
    private JavaMailer javaMailer;

    @Bean
    @Primary
    public JavaMailer javaMailer() {
//        return Mockito.mock(JavaMailer.class);
        return javaMailer;
    }
}