/*
package unitest;

import arc.expenses.config.ARCServiceConfiguration;
import RequestServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes= ARCServiceConfiguration.class)
@WebAppConfiguration
public class GenerateIDTest {

    @Autowired
    RequestServiceImpl requestService;

    @Test
    public void generateID(){

        String maxID = requestService.getMaxID();
        if(maxID == null)
            System.out.println(new SimpleDateFormat("yyyyMMdd").format(new Date())+"-1");
        else{
            String number[] = maxID.split("-");
            if(number[0].equals(new SimpleDateFormat("yyyyMMdd").format(new Date())))
                System.out.println( number[0]+"-"+(Integer.parseInt(number[1])+1));
            else
                System.out.println(new SimpleDateFormat("yyyyMMdd").format(new Date())+"-1");
        }


    }

}

*/
