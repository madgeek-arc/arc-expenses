package arc.expenses.service;

import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends GenericService<User> {

    private Logger LOGGER = Logger.getLogger(UserServiceImpl.class);

    public UserServiceImpl() {
        super(User.class);
    }

    @Override
    public String getResourceType() {
        return "user";
    }

}
