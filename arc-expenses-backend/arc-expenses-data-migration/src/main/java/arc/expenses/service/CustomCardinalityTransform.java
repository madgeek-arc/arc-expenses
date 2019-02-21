package arc.expenses.service;

import com.bazaarvoice.jolt.CardinalityTransform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class CustomCardinalityTransform implements  Transformation {

    @Override
    public Object transform(String path) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream transform = CustomCardinalityTransform.class.getClassLoader().getResourceAsStream("cardinality.json");
            InputStream inputStream = new FileInputStream(path);
            Object specs;
            Object input;
            specs = mapper.readValue(transform, Object.class);
            input = mapper.readValue(inputStream,Object.class);
            CardinalityTransform cardinalityTransform = new CardinalityTransform(specs);
            return cardinalityTransform.transform( input );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
