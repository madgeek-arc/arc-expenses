package arc.expenses.service.transformations;

import arc.expenses.service.Transformation;
import com.bazaarvoice.jolt.Shiftr;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class CustomShiftTransform implements Transformation {


    @Override
    public Object transform(Object toTransform) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream transform = CustomCardinalityTransform.class.getClassLoader().getResourceAsStream("renamefield.json");
//            InputStream inputStream = new FileInputStream(path);
            Object specs;
//            Object input;
            specs = mapper.readValue(transform, Object.class);
//            input = mapper.readValue(inputStream,Object.class);

            Shiftr shiftr = new Shiftr(specs);
            return shiftr.transform(toTransform);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
