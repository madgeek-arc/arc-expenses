package arc.expenses.service;

import arc.expenses.config.StoreRestConfig;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl extends GenericService<User> {

    @Autowired
    DataSource dataSource;

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    private StoreRestConfig storeRestConfig;

    @Value("${user.signature.archiveID}")
    private String DS_ARCHIVE;

    @Value("#{'${admin.emails}'.split(',')}")
    private List<String> admins;

    private Logger LOGGER = Logger.getLogger(UserServiceImpl.class);

    public UserServiceImpl() {
        super(User.class);
    }

    @PostConstruct
    private void createArchiveForSignatures(){
        storeRESTClient.createArchive("DS_ARCHIVE");
        LOGGER.info("DS archive created");
    }


    @Override
    public String getResourceType() {
        return "user";
    }

    public List<GrantedAuthority> getRole(String email) {

        List<GrantedAuthority> roles = new ArrayList<>();
        LOGGER.debug(admins);

        if(admins.contains(email))
            roles.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if(isExecutive(email))
            roles.add(new SimpleGrantedAuthority("ROLE_EXECUTIVE"));

        if(isOperator(email))
            roles.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));

        roles.add(new SimpleGrantedAuthority("ROLE_USER"));

        return roles;

    }

    private boolean isOperator(String email) {


        return searchService.cqlQuery("operator="+email,"project",1,0,"","ASC").getResults().size()>0;

    }

    private boolean isExecutive(String email) {

        String cqlQuery = "project_operator="+email+
                " or project_operator_delegate="+email+
                " or project_scientificCoordinator="+email+
                " or organization_poy="+email+
                " or poy_delegate="+email+
                " or accountingRegistration="+email+
                " or diaugeia="+email+
                " or accountingPayment="+email+
                " or accountingDirector="+email+
                " or accountingDirector_delegate="+email+
                " or accountingRegistration_delegate="+email+
                " or accountingPayment_delegate="+email+
                " or diaugeia_delegate="+email+
                " or director="+email+
                " or director_delegate="+email+
                " or inspectionTeam="+email+
                " or inspectionTeam_delegate="+email+
                " or travelmanager="+email+
                " or suppliesoffice="+email+
                " or travelmanager_delegate="+email+
                " or suppliesoffice_delegate="+email;


        return searchService.cqlQuery(cqlQuery,"*",1,0,"","ASC").getResults().size()>0;


    }

    public List<User> getUsersWithImmediateEmailPreference() {

        String query = " user_immediate_emails = \"true\" ";

        Paging<Resource> rs = searchService.cqlQuery(
                query,"user",
                1000,0,
                "", "ASC");


        List<User> resultSet = new ArrayList<>();
        for(Resource resource:rs.getResults()) {
            resultSet.add(parserPool.deserialize(resource,typeParameterClass));
        }
        return resultSet;
    }

    public ResponseEntity<Object> upLoadSignatureFile(String email,MultipartFile file) {

        if(Boolean.parseBoolean(storeRESTClient.fileExistsInArchive(DS_ARCHIVE,email).getResponse()))
            storeRESTClient.deleteFile(DS_ARCHIVE,email);

        try {
            storeRESTClient.storeFile(file.getBytes(),DS_ARCHIVE,email);
        } catch (IOException e) {
            LOGGER.info(e);
            return new ResponseEntity<>("ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(DS_ARCHIVE+"/"+email,
                HttpStatus.OK);
    }

    public boolean exists(String email) throws UnknownHostException {
        return  searchService.searchId(resourceType.getName(),
                new SearchService.KeyValue(String.format("%s_email", resourceType.getName()),email)) != null;
    }

    @Override
    public void delete(User user) throws ResourceNotFoundException {
        
    }
}
