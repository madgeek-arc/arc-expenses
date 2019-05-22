package gr.athenarc.mailer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class AppStartRunner implements ApplicationRunner {
    private static Logger log = LoggerFactory.getLogger(AppStartRunner.class);

    @Autowired
    JmsTemplate jmsTemplate;

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {

    }
}