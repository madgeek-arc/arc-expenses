package arc.expenses.controller;

import arc.expenses.config.SAMLAuthenticationToken;
import arc.expenses.UserServiceImpl;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.User;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/user")
@Api(description = "User API  ",  tags = {"Manage users"})
public class UserController {

    private org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(UserController.class);

    @Autowired
    UserServiceImpl userService;

    @RequestMapping(value =  "/getUserInfo", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getUserInfo() {

        if(SecurityContextHolder.getContext().getAuthentication() instanceof SAMLAuthenticationToken){
            SAMLAuthenticationToken authentication = (SAMLAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            Map<String,Object> body = new HashMap<>();
            User user = null;
            try {
                user = userService.getByField("user_email",authentication.getEmail());
                if(user == null){
                    User u = new User();
                    u.setId(authentication.getEmail().toLowerCase());
                    u.setEmail(authentication.getEmail().toLowerCase());
                    u.setFirstnameLatin(authentication.getFirstname());
                    u.setFirstname("null");
                    u.setLastnameLatin(authentication.getLastname());
                    u.setLastname("null");
                    u.setSignatureArchiveId(userService.getSignatureArchiveID());
                    u.setImmediateEmails("false");
                    u.setReceiveEmails("false");
                    u.setAttachments(null);
                    user = userService.add(u,authentication);
                }
                body.put("user",user);
                body.put("role",userService.getRole(authentication.getEmail()));
            } catch (Exception e) {
                LOGGER.error("Error getting user", e);
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(body, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value =  "/idp_login", method = RequestMethod.GET)
    public void idpLogin() {}

    @RequestMapping(value =  "/update", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public User update(@RequestBody User user) throws ResourceNotFoundException {
        return userService.update(user,user.getId());
    }


    @RequestMapping(value =  "/getUsersWithImmediateEmailPreference", method = RequestMethod.GET)
    public List<User> getUsersWithImmediateEmailPreference() {
        return userService.getUsersWithImmediateEmailPreference();
    }

   /* @RequestMapping(value = "/store/uploadSignatureFile", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> upLoadSignatureFile(@RequestParam("email") String email,
                                             @RequestParam("file") MultipartFile file) throws IOException {
        return userService.upLoadSignatureFile(email,file);
    }*/
}
