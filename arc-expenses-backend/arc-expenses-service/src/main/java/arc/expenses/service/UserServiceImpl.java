package arc.expenses.service;

import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

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

            for(int i = 1;i<16;i++)
                statement.setString(i,email);

            ResultSet rs = statement.executeQuery();
            while(rs.next())
                if(rs.getInt(0) > 0)
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
                " where project_operator = ? or " +
                " project_operator_delegates = ? or " +
                " project_scientificCoordinator = ? or " +
                " organization_POI = ? or " +
                " organization_POI_delegate =  ? or " +
                " institute_accountingRegistration = ? or " +
                " institute_diaugeia = ? or " +
                " institute_accountingPayment = ? or " +
                " institute_accountingDirector = ? or " +
                " institute_accountingDirector_delegate = ? or " +
                " institute_accountingRegistration_delegate =  ? or " +
                " institute_accountingPayment_delegate = ? or " +
                " institute_diaugeia_delegate =  ? or " +
                " organization_director = ? or " +
                " organization_director_delegate = ? ";

    }
}
