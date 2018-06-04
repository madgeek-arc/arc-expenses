package arc.expenses.service;

import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.Project;
import org.apache.log4j.Logger;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service("projectService")
public class ProjectServiceImpl extends GenericService<Project> {

    private Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class);

    @Autowired
    DataSource dataSource;


    public ProjectServiceImpl() {
        super(Project.class);
    }

    @Override
    public String getResourceType() {
        return "project";
    }

    public Project getByAcronym(String acronym,String institute) {

        Paging<Resource> rs = searchService.cqlQuery(
                "project_acronym = " + acronym + " and project_institute = " + institute,"project",
                10,0, "", SortOrder.ASC);

        List<Project> resultSet = new ArrayList<>();
        for(Resource resource:rs.getResults()) {
            try {
                resultSet.add(parserPool.deserialize(resource,typeParameterClass).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return resultSet.get(0);



    }

    public List<String> getAllProjectNames() {

        List<String> acronyms = new ArrayList<>();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("select project_acronym::text || ' (' || project_institute::text || ')' as projectName from project_view");

            while(rs.next())
                acronyms.add(rs.getString("projectName"));

            rs.close();
            statement.close();
            connection.close();
            return acronyms;

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
}
