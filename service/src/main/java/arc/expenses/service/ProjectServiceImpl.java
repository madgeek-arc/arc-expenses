package arc.expenses.service;

import arc.expenses.domain.Vocabulary;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.*;
import org.apache.log4j.Logger;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;

@Service("projectService")
@CacheConfig(cacheNames = "vocabularies")
public class ProjectServiceImpl extends GenericService<Project> {

    private Logger logger = Logger.getLogger(ProjectServiceImpl.class);

    @Autowired
    DataSource dataSource;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    private RequestPaymentServiceImpl requestPaymentService;

    @Autowired
    private AclService aclService;

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

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Project update(Project newProject) throws Exception {
        Project oldProject = get(newProject.getId());
        if(oldProject == null)
            throw new ServiceException("Project not found");

        update(newProject, (Authentication) null);


        if(oldProject.getScientificCoordinator() != newProject.getScientificCoordinator() || oldProject.getScientificCoordinatorAsDiataktis() != newProject.getScientificCoordinatorAsDiataktis()){
            FacetFilter filter = new FacetFilter();
            filter.setQuantity(10000);

            Map<String, Object> map = new HashMap<>();
            if(!oldProject.getScientificCoordinator().getEmail().equals(newProject.getScientificCoordinator().getEmail()))
                map.put("request_diataktis", oldProject.getScientificCoordinator().getEmail());
            else
                map.put("request_project",oldProject.getId());

            map.put("request_status", "PENDING");

            filter.setFilter(map);
            List<Request> requests = requestService.getAll(filter,null).getResults();

            Map<String, Sid> mappingOldSids = new HashMap<>();
            requests.forEach( request -> {
                logger.info("Old principals -- " + (request.getDiataktis() == null || request.getDiataktis().getEmail().isEmpty() ? "" : request.getDiataktis().getEmail()));
                mappingOldSids.put(request.getId(),(request.getDiataktis() == null || request.getDiataktis().getEmail().isEmpty() ? null : new PrincipalSid(request.getDiataktis().getEmail())));
            });

            logger.info("Found " + requests.size() + " requests, processing them..");

            requestService.updateDiataktis(requests,newProject);

            requests = requestService.getAll(filter,null).getResults();

            Map<String, Sid> mappingNewSids = new HashMap<>();
            requests.forEach( request -> {
                logger.info("New principals -- " + (request.getDiataktis() == null || request.getDiataktis().getEmail().isEmpty() ? "" : request.getDiataktis().getEmail()));
                mappingNewSids.put(request.getId(),(request.getDiataktis() == null || request.getDiataktis().getEmail().isEmpty() ? null : new PrincipalSid(request.getDiataktis().getEmail())));
            });

            for(Request request : requests) {

                if(mappingNewSids.get(request.getId()) == null) //Diataktis was not defined
                    continue;

                List<Sid> oldSids = new ArrayList<>();
                List<Sid> newSids = new ArrayList<>();

                if(mappingOldSids.get(request.getId()) != null){
                    oldSids.add(mappingOldSids.get(request.getId()));
                }

                newSids.add(mappingNewSids.get(request.getId()));

                RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                if(requestApproval == null)
                    continue;

                if(requestApproval.getStatus().equals(BaseInfo.Status.ACCEPTED)){
                    aclService.addRead(newSids,requestApproval.getId(),RequestApproval.class);
                }else{
                    aclService.updateAclEntries(oldSids,newSids,requestApproval.getId(),RequestApproval.class);
                }
                Browsing<RequestPayment> payments = requestPaymentService.getPayments(request.getId(),null);
                for(RequestPayment payment : payments.getResults()){
                    if(payment.getStatus().equals(BaseInfo.Status.ACCEPTED)){
                        aclService.addRead(newSids,payment.getId(),RequestPayment.class);
                    }else{
                        aclService.updateAclEntries(oldSids,newSids,payment.getId(),RequestPayment.class);
                    }
                }
            }

        }

        return newProject;
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
