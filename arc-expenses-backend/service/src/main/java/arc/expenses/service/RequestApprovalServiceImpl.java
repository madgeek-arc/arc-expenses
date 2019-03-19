package arc.expenses.service;

import arc.expenses.domain.RequestResponse;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("requestApproval")
public class RequestApprovalServiceImpl extends GenericService<RequestApproval> {

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private ProjectServiceImpl projectService;

    @Autowired
    private InstituteServiceImpl instituteService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private DataSource dataSource;

    public RequestApprovalServiceImpl() {
        super(RequestApproval.class);
    }

    @Override
    public String getResourceType() {
        return "approval";
    }

    private Logger LOGGER = Logger.getLogger(RequestApprovalServiceImpl.class);


    public RequestApproval getApproval(String id) throws Exception {
        return getByField("request_id",id);
    }

    public RequestApproval getApproval(List<String> stage, List<String> status, String id) throws Exception {
        return null;
    }


    public RequestResponse getRequestResponse(String id) throws Exception {
        RequestApproval requestApproval = get(id);
        if(requestApproval == null)
            throw new ServiceException("Request approval not found");

        Request request = requestService.get(requestApproval.getRequestId());
        Project project = projectService.get(request.getProjectId());
        Institute institute = instituteService.get(project.getInstituteId());

        Map<String, Stage> stages = new HashMap<>();

        RequestResponse requestResponse = new RequestResponse();

        BaseInfo baseInfo = new BaseInfo();
        baseInfo.setId(requestApproval.getId());
        baseInfo.setCreationDate(requestApproval.getCreationDate());
        baseInfo.setRequestId(requestApproval.getRequestId());
        baseInfo.setStage(requestApproval.getStage());
        baseInfo.setStatus(requestApproval.getStatus());

        List<Class> stagesClasses = Arrays.stream(RequestApproval.class.getDeclaredFields()).filter(p-> Stage.class.isAssignableFrom(p.getType())).flatMap(p -> Stream.of(p.getType())).collect(Collectors.toList());
        for(Class stageClass : stagesClasses){
            if(RequestApproval.class.getMethod("get"+stageClass.getSimpleName()).invoke(requestApproval)!=null)
                 stages.put(stageClass.getSimpleName().replace("Stage",""),(Stage) RequestApproval.class.getMethod("get"+stageClass.getSimpleName()).invoke(requestApproval));
        }
        requestResponse.setBaseInfo(baseInfo);
        requestResponse.setRequesterPosition(request.getRequesterPosition());
        requestResponse.setType(request.getType());
        requestResponse.setRequestStatus(request.getRequestStatus());
        requestResponse.setStages(stages);
        requestResponse.setProjectAcronym(project.getAcronym());
        requestResponse.setInstituteName(institute.getName());
        requestResponse.setRequesterFullName(request.getUser().getFirstname() + " " + request.getUser().getLastname());
        if(request.getOnBehalfOf()!=null) {
            requestResponse.setOnBehalfFullName(request.getOnBehalfOf().getFirstname() + " " + request.getOnBehalfOf().getLastname());
        }

        if(request.getTrip()!=null)
            requestResponse.setTripDestination(request.getTrip().getDestination());

        requestResponse.setCanEdit(canEdit(request.getId()));

        return requestResponse;
    }


    public String generateID(String requestId) {
        //String maxID = getMaxID();
        //if(maxID == null)
        return requestId+"-a1";
        //else
         //   return requestId+"-a"+(Integer.valueOf(maxID.split("-a")[1])+1);

    }


    public boolean canEdit(String requestId){
        String roles = "";
        for(GrantedAuthority grantedAuthority : SecurityContextHolder.getContext().getAuthentication().getAuthorities()){
            roles = roles.concat(" or acl_sid.sid='"+grantedAuthority.getAuthority()+"'");
        }
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String aclEntriesQuery = "SELECT object_id_identity, canEdit FROM acl_object_identity INNER JOIN (select distinct acl_object_identity, CASE WHEN mask=32 THEN true ELSE false END AS canEdit from acl_entry INNER JOIN acl_sid ON acl_sid.id=acl_entry.sid where acl_sid.sid='"+email+"' and acl_entry.mask=32) as acl_entries ON acl_entries.acl_object_identity=acl_object_identity.id where acl_object_identity.object_id_identity='"+requestId+"'";
        return new JdbcTemplate(dataSource).query(aclEntriesQuery , rs -> {

            if(rs.next())
                return rs.getBoolean("canEdit");
            else
                return false;
        });

    }




    @Override
    public RequestApproval getByField(String key, String value) throws Exception {
        return super.getByField(key, value);
    }
}
