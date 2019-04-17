package arc.expenses.service;


import arc.expenses.acl.ArcPermission;
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
                    if(acl.getEntries().get(i).getSid().equals(sid) && !acl.getEntries().get(i).getPermission().equals(ArcPermission.READ)){
                        acl.deleteAce(i);
                        i = (i==1 ? 0:i--);
                    }
                }
            }
            updateAcl(acl);
        } catch (NotFoundException nfe) {
            logger.error("Could not delete acl entries" ,nfe);
        }

    }

    public void removePermissionFromSid(List<Permission> permissions, List<Sid> principals, String id, Class persistentClass) {
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(persistentClass, id);
        try {
            MutableAcl acl = (MutableAcl) readAclById(objectIdentity);
            for(int i=0; i<acl.getEntries().size();i++){
                for(Permission permission: permissions) {
                    for(Sid principal : principals){
                        if (acl.getEntries().get(i).getSid().equals(principal) && acl.getEntries().get(i).getPermission() == permission) {
                            acl.deleteAce(i);
                            i--;
                        }
                    }
                }
            }
            updateAcl(acl);
        } catch (NotFoundException nfe) {
            logger.error("Could not delete acl entries" ,nfe);
        }

    }

    public void updateAclEntries(List<Sid> oldPrincipal, List<Sid> newPrincipal, String id, Class persistentClass){
        deleteEntries(oldPrincipal,id, persistentClass);

        AclImpl acl = (AclImpl) readAclById(new ObjectIdentityImpl(persistentClass, id));
        for(Sid principal : newPrincipal){
            acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, principal, true);
            acl.insertAce(acl.getEntries().size(), ArcPermission.READ, principal, true);
        }
        updateAcl(acl);
    }

    public void removeWrite(String id, Class persistentClass){
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(persistentClass, id);
        try {
            MutableAcl acl = (MutableAcl) readAclById(objectIdentity);
            for(int i=0; i<acl.getEntries().size();i++){
                if(acl.getEntries().get(i).getPermission().equals(ArcPermission.WRITE)){
                    acl.deleteAce(i);
                    i--;
                }
            }
            updateAcl(acl);
        } catch (NotFoundException nfe) {
            logger.error("Could not delete acl entries" ,nfe);
        }
    }

    public void addWrite(List<Sid> principals, String id, Class persistentClass){
        AclImpl acl = (AclImpl) readAclById(new ObjectIdentityImpl(persistentClass, id));
        for(Sid principal : principals){
            acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, principal, true);
        }
        updateAcl(acl);
    }


}
