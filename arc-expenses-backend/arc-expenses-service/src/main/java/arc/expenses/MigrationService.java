package arc.expenses;

import com.bazaarvoice.jolt.CardinalityTransform;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MigrationService {

    @Autowired
    SearchService searchService;


//    @PostConstruct
    public void startMigration() {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType("payment");
        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1000);
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Resource> rs = searchService.search(filter).getResults();
            if(rs.size() > 0) {
//                for(Resource resource:rs){
//                    System.out.println(resource.getPayload());
                    InputStream transform = this.getClass().getClassLoader().getResourceAsStream("multipleAttachments.json");
                    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("input.json");
                    Object specs = null;
                    Object input = null;
                    try {
                        specs = (List)mapper.readValue(transform, Object.class);
                        input = mapper.readValue(inputStream,Object.class);


                        CardinalityTransform cardinalityTransform = new CardinalityTransform(((List) specs).get(0));
                        Object transformedOutput = cardinalityTransform.transform( input );
//                        Chainr chainr = Chainr.fromSpec(specs);
//                        Object transformedOutput = chainr.transform(input);
                        System.out.println( JsonUtils.toPrettyJsonString(transformedOutput) );
                        System.out.println("finish!");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }


//            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }


}
