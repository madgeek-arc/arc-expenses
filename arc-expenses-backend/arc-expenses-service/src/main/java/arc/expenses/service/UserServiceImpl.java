package arc.expenses.service;

import arc.expenses.utils.ParserPool;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.swing.text.html.parser.Parser;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserServiceImpl extends GenericService<User> {

    @Autowired
    DataSource dataSource;

    private Logger LOGGER = Logger.getLogger(UserServiceImpl.class);

    public UserServiceImpl() {
        super(User.class);
    }

    @Override
    public String getResourceType() {
        return "user";
    }

    public String getRole(String email) {

        Connection connection = null;
        PreparedStatement statement = null;

        try {
            String query = createQuery();
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(query);

            //for(int i = 1;i<16;i++)
            for(int i = 1;i<9;i++)
                statement.setString(i,email);

            ResultSet rs = statement.executeQuery();
            while(rs.next())
                if(Integer.parseInt(rs.getString("count")) > 0)
                    return "ROLE_EXECUTIVE";
                else
                    return "ROLE_USER";


            rs.close();
            statement.close();
            connection.close();


        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                assert statement != null;
                statement.close();
                connection.close();
            } catch (Exception e) { /* ignored */ }
        }
        return null;
    }

    private String createQuery() {

        return "select count(*) from project_view " +
                " where ? = ANY( project_operator ) or " +
               // " project_operator_delegates = ? or " +
                " project_scientificCoordinator = ? or " +
                " project_organization_POI  = ? or " +
              //  " organization_POI_delegate =  ? or " +
                " project_institute_accountingRegistration   = ? or " +
                " project_institute_diaugeia  = ? or " +
                " project_institute_accountingPayment  = ? or " +
                " project_institute_accountingDirector  = ? or " +
              //  " project_institute_accountingDirector_delegate = ? or " +
              //  " project_institute_accountingRegistration_delegate =  ? or " +
              //  " project_institute_accountingPayment_delegate = ? or " +
              //  " project_institute_diaugeia_delegate =  ? or " +
                " project_organization_director  = ? "; //or " +
              //  " project_organization_director_delegate = ? ";

    }

    public List<User> getUsersWithImmediateEmailPreference() {

        String query = " user_immediate_emails = true ";

        Paging<Resource> rs = searchService.cqlQuery(
                query,"user",
                1000,0,
                "", SortOrder.ASC);


        List<User> resultSet = new ArrayList<>();
        for(Resource resource:rs.getResults()) {
            try {
                resultSet.add(parserPool.deserialize(resource,typeParameterClass).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return resultSet;
    }
}
