package controller;

import config.SAMLAuthentication;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.User;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import service.UserServiceImpl;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/user")
@Api(description = "User API  ",  tags = {"Manage users"})
public class UserController {

    private org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(UserController.class);

    @Autowired
    UserServiceImpl userService;

    @RequestMapping(value =  "/logout", method = RequestMethod.GET)
    public String logOut() {
        return null;
    }

    @RequestMapping(value =  "/getUserInfo", method = RequestMethod.GET)
    public ResponseEntity<Object> getUserInfo() {

        SAMLAuthentication authentication = (SAMLAuthentication) SecurityContextHolder.getContext().getAuthentication();
        Map<String,Object> body = new HashMap<>();

        body.put("firstnameLatin",authentication.getFirstname());
        body.put("lastnameLatin",authentication.getLastname());
        body.put("email",authentication.getEmail());
        body.put("uid",authentication.getUid());

        User user = null;
        try {
            user = userService.getByField("user_email",authentication.getEmail());
            body.put("firstname",user !=null ? user.getFirstname():null);
            body.put("lastname",user !=null ? user.getLastname():null);
        } catch (Exception e) {
            LOGGER.fatal(e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }

//        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
//        body.put("role",roles);

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @RequestMapping(value =  "/idp_login", method = RequestMethod.GET)
    public void idpLogin() {}

    @RequestMapping(value =  "/update", method = RequestMethod.PUT,consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public User update(@RequestBody User user) {
        try {
            return userService.update(user, user.getEmail(), "email");
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    // TODO: remove method
    @RequestMapping(value =  "/add", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public User add(@RequestBody User user) {
        return userService.add(user);
    }

    // TODO: remove method
    @RequestMapping(value =  "/show/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Browsing<User> showUsers() {
        return userService.getAll(new FacetFilter());
    }

}
