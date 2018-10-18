package arc.expenses.service;

import arc.expenses.domain.Vocabulary;
import gr.athenarc.domain.Project;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

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

    public List<Vocabulary> getAllProjectNames() {

        return new JdbcTemplate(dataSource)
                .query("select project_id ,project_acronym,project_institute from project_view",vocabularyRowMapper);

    }

    private RowMapper<Vocabulary> vocabularyRowMapper = (rs, i) ->
            new Vocabulary(rs.getString("project_id"),rs.getString("project_acronym"), rs.getString("project_institute"));


}
