package controller;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.SearchService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@RestController("searchcontroller")
@RequestMapping(value = "/search")
@Api(description = "Search API  ",  tags = {"Search requests"})
public class SearchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    public SearchController() {
    }

    @RequestMapping(path = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging getAll(@RequestParam(value = "resourceType") String resourceType,
                         @RequestParam(value = "keyword",required=false) String keyword,
                         @RequestParam(value = "from",required=false) String from,
                         @RequestParam(value = "quantity",required=false) String quantity,
                         @RequestParam(value = "order",required=false) String orderD,
                         @RequestParam(value = "orderField",required=false) String orderF,
                         @RequestParam(value = "email",required=false) String email) {

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(resourceType != null ? resourceType : "");
        filter.setKeyword(keyword != null ? keyword : "");
        filter.setFrom(from != null ? Integer.parseInt(from) : 0);
        filter.setQuantity(quantity != null ? Integer.parseInt(quantity) : 10);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = orderD != null ? orderD : "asc";
        String orderField = orderF != null ? orderF : null;

        if (orderField != null) {
            order.put("order",orderDirection);
            sort.put(orderField, order);
            filter.setOrderBy(sort);
        }
//        filter.setFilter(null);

        try {
            return searchService.search(filter);
        } catch (UnknownHostException e) {
            LOGGER.debug("Error on search controller",e);
        }
        return null;
    }

}
