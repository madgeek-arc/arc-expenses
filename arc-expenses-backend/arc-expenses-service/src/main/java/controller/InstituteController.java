package controller;

import gr.athenarc.domain.Institute;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import service.InstituteServiceImpl;

import java.util.List;

@RestController
@RequestMapping(value = "/institute")
@Api(description = "Institute API  ",  tags = {"Manage institute"})
public class InstituteController {


    @Autowired
    InstituteServiceImpl instituteService;


    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    public Institute getById(@PathVariable("id") String id) {
        return instituteService.get(id);
    }


    @RequestMapping(value = "/add", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Institute addInstitute(@RequestBody Institute institute) {
        return instituteService.add(institute);
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public List<Institute> getAll() {
        return null;
    }


}
