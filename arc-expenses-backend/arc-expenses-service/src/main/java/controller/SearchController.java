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

@RestController("searchcontroller")
@RequestMapping(value = "/search")
@Api(description = "Search API  ",  tags = {"Search requests"})
public class SearchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SearchService searchService;

    public SearchController() {
    }


    @RequestMapping(path = "/all", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging getAll(@RequestBody FacetFilter facetFilter) {

        /*FacetFilter filter = new FacetFilter();
        filter.setKeyword(allRequestParams.get("keyword") != null ? (String)allRequestParams.remove("keyword") : "");
        filter.setFrom(allRequestParams.get("from") != null ? Integer.parseInt((String)allRequestParams.remove("from")) : 0);
        filter.setQuantity(allRequestParams.get("quantity") != null ? Integer.parseInt((String)allRequestParams.remove("quantity")) : 10);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = allRequestParams.get("order") != null ? (String)allRequestParams.remove("order") : "asc";
        String orderField = allRequestParams.get("orderField") != null ? (String)allRequestParams.remove("orderField") : null;

        if (orderField != null) {
            order.put("order",orderDirection);
            sort.put(orderField, order);
            filter.setOrderBy(sort);
        }
        filter.setFilter(allRequestParams);*/
        try {
            return searchService.search(facetFilter);
        } catch (UnknownHostException e) {
            LOGGER.debug("Error on search controller",e);
        }
        //return new ResponseEntity<>(searchService.getAll(filter), HttpStatus.OK);
        return null;
    }

}
