package arc.expenses.acl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ArcVoter implements AccessDecisionVoter {

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private PermissionEvaluator permissionEvaluator;


    @Override
    public boolean supports(ConfigAttribute configAttribute) {
        return false;
    }

    @Override
    public int vote(Authentication authentication, Object o, Collection collection) {
        return 0;
    }

    @Override
    public boolean supports(Class aClass) {
        return false;
    }
}
