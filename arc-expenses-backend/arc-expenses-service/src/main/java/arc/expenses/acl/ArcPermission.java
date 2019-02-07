package arc.expenses.acl;

import org.springframework.security.acls.domain.BasePermission;

public class ArcPermission extends BasePermission {

    public static final ArcPermission UPGRADE = new ArcPermission(1 << 5, 'A');

    public static final ArcPermission DOWNGRADE = new ArcPermission(1 << 6, 'B');

    public static final ArcPermission CANCEL = new ArcPermission(1 << 7, 'X');


    protected ArcPermission(int mask) {
        super(mask);
    }

    protected ArcPermission(int mask, char code) {
        super(mask, code);
    }
}
