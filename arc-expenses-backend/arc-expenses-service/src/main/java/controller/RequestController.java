package controller;

import eu.openminted.registry.core.service.ResourceTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import service.RequestService;

public class RequestController {

    @Autowired
    RequestService requestService;
    @Autowired
    ResourceTypeService resourceTypeService;
   /* @Autowired
    PoolParser poolParser;
    @Autowired
    ResourceValidator resourceValidator;*/


}
