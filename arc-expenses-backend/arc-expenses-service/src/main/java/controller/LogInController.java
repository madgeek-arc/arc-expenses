package controller;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import service.LogInService;

@RestController
@RequestMapping(value = "/login")
@Api(description = "User Login API  ",  tags = {"user login"})
public class LogInController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogInController.class);

    @Autowired
    LogInService logInService;

    @Value("${idp.url}")
    private String IDP_URL;


    @Value("${ACS_URL}")
    private String assertionConsumerServiceUrl;

    @Value("${idp.issuer.idD}")
    private String IDP_ISSUER_ID;


    @RequestMapping(value =  "/", method = RequestMethod.GET)
    public String redirectToIDPWithAuthNRequest() {

        String redirectUrl, redirectString = null;
        redirectUrl = logInService.getAuthNRedirectUrl(IDP_URL, assertionConsumerServiceUrl, IDP_ISSUER_ID);
        LOGGER.debug("Redirecting to " + redirectUrl );
        return "redirect:" + redirectUrl;
    }

}
