package arc.expenses.service;

import gr.athenarc.domain.RequestApproval;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("requestApproval")
public class RequestApprovalServiceImpl extends GenericService<RequestApproval> {


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

    public String generateID(String requestId) {
        //String maxID = getMaxID();
        //if(maxID == null)
        return requestId+"-a1";
        //else
         //   return requestId+"-a"+(Integer.valueOf(maxID.split("-a")[1])+1);

    }

    @Override
    public RequestApproval getByField(String key, String value) throws Exception {
        return super.getByField(key, value);
    }
}
