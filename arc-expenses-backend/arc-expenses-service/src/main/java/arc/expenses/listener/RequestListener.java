package arc.expenses.listener;

import arc.expenses.acl.ArcPermission;
import arc.expenses.service.RequestApprovalServiceImpl;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Aspect
@Component
public class RequestListener {

    final private static Logger logger = LogManager.getLogger(RequestListener.class);


    @Autowired
    RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    MutableAclService aclService;

    @AfterReturning(pointcut = "execution(* arc.expenses.service.RequestServiceImpl.add(..))", returning = "request")
    public void requestCreated(Request request) {
        logger.debug("Request with id " + request.getId() + " has just been created");

        RequestApproval requestApproval = new RequestApproval();
        requestApproval.setId(request.getId()+"-a1");
        requestApproval.setRequestId(request.getId());
        requestApproval.setCreationDate(new Date().getTime()+"");
        requestApproval.setStage("2");
        requestApproval.setStatus("pending");

        requestApproval = requestApprovalService.add(requestApproval, null);

        try{
            aclService.createAcl(new ObjectIdentityImpl(Request.class, request.getId()));
        }catch (AlreadyExistsException ex){
            logger.debug("Object identity already exists");
        }
        AclImpl acl = (AclImpl) aclService.readAclById(new ObjectIdentityImpl(Request.class, request.getId()));
        acl.insertAce(acl.getEntries().size(), ArcPermission.CANCEL, new PrincipalSid(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.CANCEL, new GrantedAuthoritySid("ROLE_ADMIN"), true);

        aclService.updateAcl(acl);

        aclService.createAcl(new ObjectIdentityImpl(RequestApproval.class, requestApproval.getId()));
        acl = (AclImpl) aclService.readAclById(new ObjectIdentityImpl(RequestApproval.class, requestApproval.getId()));
        acl.insertAce(acl.getEntries().size(), ArcPermission.DOWNGRADE, new PrincipalSid(request.getProject().getScientificCoordinator().getEmail()), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.UPGRADE, new PrincipalSid(request.getProject().getScientificCoordinator().getEmail()), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.DOWNGRADE, new GrantedAuthoritySid("ROLE_ADMIN"), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.UPGRADE, new GrantedAuthoritySid("ROLE_ADMIN"), true);

        aclService.updateAcl(acl);

    }


}
