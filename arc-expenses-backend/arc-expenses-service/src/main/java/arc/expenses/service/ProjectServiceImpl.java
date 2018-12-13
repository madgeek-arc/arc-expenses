package arc.expenses.service;

import arc.expenses.domain.Vocabulary;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Organization;
import gr.athenarc.domain.Project;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("projectService")
public class ProjectServiceImpl extends GenericService<Project> {

    private Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class);

    @Autowired
    DataSource dataSource;

    @Autowired
    CascadeService cascadeService;

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


    public Paging<Project> getAllProjects(String from, String quantity,Authentication auth) {

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());

        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1000);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "creation_date";

        order.put("order",orderDirection);
        sort.put(orderField, order);
        filter.setOrderBy(sort);

        filter.setFrom(Integer.parseInt(from));
        filter.setQuantity(Integer.parseInt(quantity));
        return getAll(filter,auth);


    }

    public List<Vocabulary> getProjectsOfOperator(String email) {
        return new JdbcTemplate(dataSource)
                .query(projectsOfOperator(email),vocabularyRowMapper);
    }

    private String projectsOfOperator(String email) {

        return  "  select distinct (project_id),project_acronym,project_institute\n" +
                "  from project_view , (  select split_part(poi::text,',',1) as email,\n" +
                "                              split_part(poi::text,',',2) as firstname,\n" +
                "                              regexp_replace(split_part(poi::text,',',3),'[^[:alpha:]]','') as lastname\n" +
                "                        from   (\n" +
                "                              select regexp_matches(payload,'(?:\"email\":\")(.*?)(?:\",\"firstname\":\"(.*?)(?:\",\"lastname\":\"(.*?)(?:\")))','g')\n" +
                "                              from resource\n" +
                "                              where fk_name = 'project'\n" +
                "                              )  as poi\n" +
                "                    ) as poi\n" +
                "  where poi.email ilike  '%" + email + "%'";

    }

    @Override
    public Project update(Project project, Authentication authentication) throws ResourceNotFoundException {
        update(project,project.getId());
        cascadeService.cascadeAll(project,authentication);
        return project;
    }
}
