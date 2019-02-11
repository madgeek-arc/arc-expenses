package arc.expenses.service;


import arc.expenses.acl.ArcPermission;
import gr.athenarc.domain.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.*;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service("aclService")
public class AclService extends JdbcMutableAclService{


    private static Logger logger = LogManager.getLogger(AclService.class);


    public AclService(DataSource dataSource, LookupStrategy lookupStrategy, AclCache aclCache) {
        super(dataSource, lookupStrategy, aclCache);
    }

    public void deleteEntries(List<Sid> principals, String id, Class persistentClass) {
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(persistentClass, id);
        try {
            MutableAcl acl = (MutableAcl) readAclById(objectIdentity);
            for(int i=0; i<acl.getEntries().size();i++){
                for(Sid sid : principals) {
                    if(acl.getEntries().get(i).getSid().equals(sid)){
                        System.out.println("Removing "+sid);
                        acl.deleteAce(i);
                        i--;
                    }
                }
            }
            updateAcl(acl);
        } catch (NotFoundException nfe) {
            logger.error("Could not delete acl entries" ,nfe);
        }

    }

    public void updateAclEntries(List<Sid> oldPrincipal, List<Sid> newPrincipal, String id){
        deleteEntries(oldPrincipal,id, Request.class);

        AclImpl acl = (AclImpl) readAclById(new ObjectIdentityImpl(Request.class, id));
        for(Sid principal : newPrincipal){
            acl.insertAce(acl.getEntries().size(), ArcPermission.APPROVE, principal, true);
            acl.insertAce(acl.getEntries().size(), ArcPermission.REJECT, principal, true);
            acl.insertAce(acl.getEntries().size(), ArcPermission.DOWNGRADE, principal, true);
        }
        updateAcl(acl);
        deleteEntries(oldPrincipal,id, Request.class);
    }
}
