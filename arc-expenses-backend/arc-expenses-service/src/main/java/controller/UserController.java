package controller;

import config.SAMLAuthentication;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/user")
@Api(description = "User Login API  ",  tags = {"user login"})
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @Value("${webapp.front}")
    private String frontEndURI;

    @RequestMapping(value =  "/logout", method = RequestMethod.GET)
    public String logOut() {
        return null;
    }

    @RequestMapping(value =  "/login", method = RequestMethod.GET)
    public ResponseEntity<Object> logIn() {

        SAMLAuthentication authentication = (SAMLAuthentication) SecurityContextHolder.getContext().getAuthentication();
        Map<String,Object> body = new HashMap<>();

        body.put("firstname",authentication.getFirstname());
        body.put("lastname",authentication.getLastname());
        body.put("email",authentication.getEmail());
        body.put("uid",authentication.getUid());
//        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
//        body.put("role",roles);

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

}
