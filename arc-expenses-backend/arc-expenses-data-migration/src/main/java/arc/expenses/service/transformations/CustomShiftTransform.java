package arc.expenses.service.transformations;

import arc.expenses.service.Transformation;
import com.bazaarvoice.jolt.Chainr;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class CustomShiftTransform implements Transformation {


    @Override
    public Object transform(Object toTransform,String resourceType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream transform = null;
            if(resourceType.equals("approval"))
                transform = CustomCardinalityTransform.class.getClassLoader().getResourceAsStream("renamefieldApproval.json");
            else if(resourceType.equals("payment"))
                transform = CustomCardinalityTransform.class.getClassLoader().getResourceAsStream("renamefieldPayment.json");
            else
                transform = CustomCardinalityTransform.class.getClassLoader().getResourceAsStream("renamefieldRequest.json");


            Object specs;
            specs = mapper.readValue(transform, Object.class);
            Chainr unit = Chainr.fromSpec( specs );
            return unit.transform(toTransform);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
