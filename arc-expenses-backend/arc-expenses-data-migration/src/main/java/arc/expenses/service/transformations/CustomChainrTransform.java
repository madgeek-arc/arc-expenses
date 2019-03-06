package arc.expenses.service.transformations;

import arc.expenses.service.Transformation;
import com.bazaarvoice.jolt.Chainr;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class CustomChainrTransform implements Transformation {


    @Override
    public Object transform(Object toTransform,String resourceType) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream transform;
            transform = CustomChainrTransform.class.getClassLoader().getResourceAsStream("shiftTransformation.json");
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
