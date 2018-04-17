package controller;

import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.validation.ResourceValidator;
import org.springframework.beans.factory.annotation.Autowired;
import service.RequestServiceImpl;

public class RequestController {

    @Autowired
    RequestServiceImpl requestServiceImpl;
    @Autowired
    ResourceTypeService resourceTypeService;
//    @Autowired
//    PoolParser poolParser;
    @Autowired
    ResourceValidator resourceValidator;

}
