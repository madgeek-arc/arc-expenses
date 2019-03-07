package arc.expenses;

import arc.expenses.domain.Vocabulary;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Institute;
import gr.athenarc.domain.Organization;
import gr.athenarc.domain.Project;
import gr.athenarc.domain.Request;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
                .query(projectsOfOperator(email.toLowerCase()),vocabularyRowMapper);
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
                "  where poi.email ilike  '%" + email.toLowerCase() + "%'";

    }

    @Override
    public Project update(Project project, Authentication authentication) throws ResourceNotFoundException {
        update(project,project.getId());
        cascadeService.cascadeAll(project,authentication);
        return project;
    }

    public void cascadeAll(Institute institute, Authentication authentication) {
        List<Resource> resources = getProjectsPerInstitute(institute.getId(),authentication);

        for(Resource resource:resources){
            Project project = parserPool.deserialize(resource,typeParameterClass);
            project.setInstitute(institute);
            try {
                update(project,project.getId());
            } catch (ResourceNotFoundException e) {
                LOGGER.debug("error on updating project ( " + project.getId() + " ) on cascade all ", e);
            }
        }
    }

    public void cascadeAll(Organization organization, Authentication authentication) {
        List<Resource> resources = getProjectsPerOrganization(organization.getId(),authentication);

        for(Resource resource:resources){
            Project project = parserPool.deserialize(resource,typeParameterClass);

            Project project_new = get(project.getId());
            project_new.getInstitute().setOrganization(organization);
            try {
                update(project_new,project_new.getId());
            } catch (ResourceNotFoundException e) {
                LOGGER.debug("error on updating project ( " + project.getId() + " ) on cascade all ", e);
            }
        }
    }

    public List<Resource> getProjectsPerOrganization(String id, Authentication authentication) {
        return getByValue("project_organization",id,authentication);
    }

    public List<Resource> getProjectsPerInstitute(String id,Authentication authentication) {
        return getByValue("project_institute",id,authentication);
    }


    private List<Resource> getByValue(String field,String id,Authentication authentication){

        String query = field + "= \"" + id + "\"";

        Paging<Resource> rs = searchService.cqlQuery(
                query,"project",
                1000,0,
                "", "ASC");
        return rs.getResults();
    }

    public double getApprovedRequestsByScientificCoordinator(Request request) {
        String scientificCoordinator = request.getProject().getScientificCoordinator().getEmail();
        float totalApprovals = getTotalApprovalsAmount(scientificCoordinator,request.getProject().getId());
        float totalPayments = getTotalPaymentsAmount(scientificCoordinator,request.getProject().getId());
        return totalApprovals+totalPayments;
    }

    private float getTotalPaymentsAmount(String scientificCoordinator,String projectId) {

        String query = "select sum(((rs.payload::json)->'stage1'->>'finalAmount')::float) as total\n" +
                "from request_view r,resource rs\n" +
                "where r.request_id in ( select request_id from payment_view where stage = '11'  )\n" +
                "and r.request_project_scientificcoordinator = '" + scientificCoordinator + "'"+
                "and r.id = rs.id and rs.fk_name = 'request' and cast(rs.payload::json->'project'->'id' as varchar) = '" + projectId + "'";

        return new JdbcTemplate(dataSource).query(query,floatRowMapper).get(0);
    }

    private float getTotalApprovalsAmount(String scientificCoordinator,String projectId) {

        String query = "select sum(((rs.payload::json)->'stage1'->>'finalAmount')::float) as total\n" +
                "from request_view r,resource rs\n" +
                "where r.request_id in ( select request_id from approval_view where stage = '6'\n" +
                "                        except\n" +
                "                        select request_id from payment_view where stage = '11'  )\n" +
                "and r.request_project_scientificcoordinator = '" + scientificCoordinator + "'"+
                "and r.id = rs.id and rs.fk_name = 'request' and cast(rs.payload::json->'project'->'id' as varchar) = '" + projectId + "'";

        return new JdbcTemplate(dataSource).query(query,floatRowMapper).get(0);
    }

    private RowMapper<Float> floatRowMapper = (rs, i) -> (rs.getFloat("total"));
}
