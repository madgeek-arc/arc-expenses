package arc.expenses.service;

import com.bazaarvoice.jolt.CardinalityTransform;
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
import java.util.List;
import java.util.Map;

@Component
public class MigrationService {

    @Autowired
    SearchService searchService;


    @PostConstruct
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
                for(Resource resource:rs){
//                    System.out.println(resource.getPayload());
                    InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("multipleAttachments.json");
                    List specs = null;
                    try {
                        specs = (List)mapper.readValue(inputStream, List.class);
                        CardinalityTransform cardinalityTransform = new CardinalityTransform(specs.get(0));
                        Object transformedOutput = cardinalityTransform.transform( resource.getPayload() );
                        System.out.println( transformedOutput );


                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }


            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }


}
