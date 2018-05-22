package arc.expenses.service;

import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.Project;
import org.apache.log4j.Logger;
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

    public Project getByAcronym(String acronym) {
        Project project;
        try {
            project = parserPool.deserialize(searchService.searchId("project",new SearchService.KeyValue("project_acronym", acronym)), Project.class).get();
        } catch (UnknownHostException | ExecutionException | InterruptedException e) {
            LOGGER.fatal(e);
            throw new ServiceException(e);
        }
        return project;
    }

    public List<String> getAllProjectNames() {

        List<String> acronyms = new ArrayList<>();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("select project_acronym from project_view");

            while(rs.next())
                acronyms.add(rs.getString("project_acronym"));

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
