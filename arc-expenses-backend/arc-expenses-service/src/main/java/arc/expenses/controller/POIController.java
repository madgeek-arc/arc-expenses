package arc.expenses.controller;

import arc.expenses.domain.Executive;
import arc.expenses.service.POIServiceImpl;
import gr.athenarc.domain.PersonOfInterest;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/poi")
@Api(description = "POI API  ",  tags = {"Manage pois"})
public class POIController {


    private org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(UserController.class);

    @Autowired
    POIServiceImpl poiService;

    @RequestMapping(value = "/getPois", method = RequestMethod.GET)
    public List<Executive> getPois() {
        return poiService.getPois();
    }

}
