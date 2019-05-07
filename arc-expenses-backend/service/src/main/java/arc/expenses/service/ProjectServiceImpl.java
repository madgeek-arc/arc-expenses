package arc.expenses.service;

import arc.expenses.domain.Vocabulary;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Project;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("projectService")
@CacheConfig(cacheNames = "vocabularies")
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

    @Override
    public Project add(Project project, Authentication u) {
        return super.add(project, u);
    }

    public List<Vocabulary> getAllProjectNames() {

        return new JdbcTemplate(dataSource)
                .query("select project_view.project_id ,project_view.project_acronym,project_view.project_institute, institute_view.institute_name from project_view inner join institute_view on project_view.project_institute=institute_view.institute_id; ",vocabularyRowMapper);

    }

    private RowMapper<Vocabulary> vocabularyRowMapper = (rs, i) ->
            new Vocabulary(rs.getString("project_id"),rs.getString("project_acronym"), rs.getString("project_institute"), rs.getString("institute_name"));


    public Paging<Project> getAllProjects(String from,String quantity,Authentication auth) {

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());

        filter.setKeyword("");
        filter.setFrom(Integer.parseInt(from));
        filter.setQuantity(Integer.parseInt(quantity));

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "creation_date";

        order.put("order",orderDirection);
        sort.put(orderField, order);
        filter.setOrderBy(sort);

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
    @CacheEvict(value = "executives", allEntries = true)
    public Project update(Project project, Authentication authentication) throws ResourceNotFoundException {
        update(project,project.getId());
        return project;
    }
}
