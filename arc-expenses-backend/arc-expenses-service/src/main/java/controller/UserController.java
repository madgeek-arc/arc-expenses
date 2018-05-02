package controller;

import config.SAMLAuthentication;
import gr.athenarc.domain.User;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import service.UserServiceImpl;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = "/user")
@Api(description = "User Login API  ",  tags = {"user login"})
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
            user = userService.getByField("email",authentication.getEmail());
            body.put("firstname",user !=null ? user.getFirstname():null);
            body.put("lastname",user !=null ? user.getLastname():null);
        } catch (UnknownHostException | ExecutionException | InterruptedException e) {
            LOGGER.fatal(e);
        }

//        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
//        body.put("role",roles);

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @RequestMapping(value =  "/idp_login", method = RequestMethod.GET)
    public void idpLogin() {}

    @RequestMapping(value =  "/update", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public User update(User user) {
        return userService.add(user);
    }

}
