package arc.expenses.controller;

import arc.expenses.service.RequestServiceImpl;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/temp/diataktis")
@Api(description = "User API  ",  tags = {"Update diataktis of request"})
public class TempController {

    private org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(TempController.class);

    @Autowired
    private RequestServiceImpl requestService;

    @RequestMapping(value =  "/", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public void updateDiataktis() throws Exception {
        requestService.updateDiataktis();
        requestService.updatePois();
    }

}
