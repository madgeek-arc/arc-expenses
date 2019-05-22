package arc.expenses.acl;

import org.springframework.security.acls.domain.DefaultPermissionFactory;

public class ArcPermissionFactory extends DefaultPermissionFactory {

    public ArcPermissionFactory() {
        super();
        registerPublicPermissions(ArcPermission.class);
    }
}
