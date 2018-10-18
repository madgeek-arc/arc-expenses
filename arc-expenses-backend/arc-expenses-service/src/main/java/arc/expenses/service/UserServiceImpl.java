package arc.expenses.service;

import arc.expenses.config.StoreRestConfig;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    public String getRole(String email) {

        String role = null;
        int count;

        if(admins.contains(email))
            return "ROLE_ADMIN";

        count =  new NamedParameterJdbcTemplate(dataSource)
                .queryForObject(createQuery(),new MapSqlParameterSource("email",email),Integer.class);

        if(count > 0)
            return "ROLE_EXECUTIVE";
        return "ROLE_USER";

    }

    private String createQuery() {

        return "select count(*) as count from project_view " +
                " where :email = ANY( project_operator ) or " +
               // " project_operator_delegates = :email or " +
                " project_scientificCoordinator = :email or " +
                " project_organization_POI  = :email or " +
              //  " organization_POI_delegate =  :email or " +
                " project_institute_accountingRegistration   = :email or " +
                " project_institute_diaugeia  = :email or " +
                " project_institute_accountingPayment  = :email or " +
                " project_institute_accountingDirector  = :email or " +
              //  " project_institute_accountingDirector_delegate = :email or " +
              //  " project_institute_accountingRegistration_delegate =  :email or " +
              //  " project_institute_accountingPayment_delegate = :email or " +
              //  " project_institute_diaugeia_delegate =  :email or " +
                " project_organization_director  = :email "; //or " +
              //  " project_organization_director_delegate = :email ";

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

    public String getSignatureArchiveID() {
        return DS_ARCHIVE;
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
}
