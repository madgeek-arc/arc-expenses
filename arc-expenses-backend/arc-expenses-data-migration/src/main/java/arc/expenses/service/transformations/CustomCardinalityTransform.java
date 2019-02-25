package arc.expenses.service.transformations;

import arc.expenses.service.Transformation;
import com.bazaarvoice.jolt.CardinalityTransform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class CustomCardinalityTransform implements Transformation {

    @Override
    public Object transform(Object toTransform) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream transform = CustomCardinalityTransform.class.getClassLoader().getResourceAsStream("cardinality.json");
            Object specs;
            specs = mapper.readValue(transform, Object.class);
            CardinalityTransform cardinalityTransform = new CardinalityTransform(specs);
            return cardinalityTransform.transform( toTransform );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
