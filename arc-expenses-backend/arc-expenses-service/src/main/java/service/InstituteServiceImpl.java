package service;

import gr.athenarc.domain.Institute;
import org.springframework.stereotype.Service;

@Service("instituteService")
public class InstituteServiceImpl extends GenericService<Institute>{


    public InstituteServiceImpl() {
        super(Institute.class);
    }

    @Override
    public String getResourceType() {
        return "institute";
    }
}
