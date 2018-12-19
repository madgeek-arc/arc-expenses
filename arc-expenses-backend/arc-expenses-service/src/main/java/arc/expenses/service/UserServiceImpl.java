package arc.expenses.service;

import arc.expenses.config.StoreRestConfig;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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


        List<Integer> count =  new NamedParameterJdbcTemplate(dataSource)
                .query(createExecutiveQuery(email),countMapper);
        if(count.get(0) > 0)
            roles.add(new SimpleGrantedAuthority("ROLE_EXECUTIVE"));

        List<String> operators = new NamedParameterJdbcTemplate(dataSource)
                .query(createOperatorQuery(email),emailMapper);
        if(operators.contains(email))
            roles.add(new SimpleGrantedAuthority("ROLE_OPERATOR"));

        roles.add(new SimpleGrantedAuthority("ROLE_USER"));

        return roles;

    }

    private String createOperatorQuery(String email) {

        return "  select distinct(regexp_replace(email,'[^[:alpha:]]','')) as email\n" +
                " from (select split_part(regexp_matches(((payload::json)->>'operator')::text,'(?:\"email\":\")(.*?)(?:\",\"firstname\":\"(.*?)(?:\",\"lastname\":\"(.*?)(?:\")))','g')::text,',',1) as email\n" +
                "      from resource\n" +
                "      where fk_name = 'project') as foo";

    }

    private String createExecutiveQuery(String email) {

        return "select count(*) as count from request_view r \n" +
                "where  r.request_project_operator @> '{\"" + email + "\"}' or  r.request_project_operator_delegate @> '{\"" + email + "\"}' "
                + "  or  r.request_project_scientificCoordinator = '" + email
                + "' or  r.request_organization_poy = '" + email + "' or  r.request_organization_poy_delegate @>  '{\"" + email + "\"}' or  r.request_institute_accountingRegistration = '" + email
                + "' or  r.request_institute_diaugeia = '" + email + "' or  r.request_institute_accountingPayment = '" + email + "' or  r.request_institute_accountingDirector = '" + email
                + "' or  r.request_institute_accountingDirector_delegate @>  '{\"" + email + "\"}' or  r.request_institute_accountingRegistration_delegate @>  '{\"" + email + "\"}' "
                + "  or  r.request_institute_accountingPayment_delegate @>  '{\"" + email + "\"}' or  r.request_institute_diaugeia_delegate @>  '{\"" + email + "\"}' "
                + "  or  r.request_organization_director = '" + email + "' or  r.request_institute_director = '" + email
                + "' or  r.request_organization_director_delegate @>  '{\"" + email + "\"}'  or  r.request_institute_director_delegate @>  '{\"" + email + "\"}'  "
                + "  or  r.request_organization_inspectionTeam @> '{\"" + email +  "\"}'     or  request_organization_inspectionTeam_delegate @> '{\"" + email + "\"}' "
                + "  or  r.request_institute_travelmanager =  '" + email + "' or r.request_institute_suppliesoffice =  '" + email
                + "' or  request_institute_travelmanager_delegate @>  '{\"" + email +  "\"}'  or request_institute_suppliesoffice_delegate @> '{\"" + email +  "\"}'";

    }

    private RowMapper<Integer> countMapper = (rs, i) ->
            Integer.valueOf(rs.getString("count"));

    private RowMapper<String> emailMapper = (rs, i) ->
            (rs.getString("email"));

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
