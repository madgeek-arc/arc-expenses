package arc.expenses.utils;

import gr.athenarc.domain.BaseInfo;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;

public class Converter {


    public static BaseInfo toBaseInfo(RequestApproval requestApproval){
        return getBaseInfo(requestApproval.getId(), requestApproval.getRequestId(), requestApproval.getCreationDate(), requestApproval.getStage(), requestApproval.getStatus());
    }

    public static BaseInfo toBaseInfo(RequestPayment requestPayment){
        return getBaseInfo(requestPayment.getId(), requestPayment.getRequestId(), requestPayment.getCreationDate(), requestPayment.getStage(), requestPayment.getStatus());
    }

    private static BaseInfo getBaseInfo(String id, String requestId, String creationDate, String stage, String status) {
        BaseInfo baseInfo = new BaseInfo();
        baseInfo.setId(id);
        baseInfo.setRequestId(requestId);
        baseInfo.setCreationDate(creationDate);
        baseInfo.setStage(stage);
        baseInfo.setStatus(status);
        return baseInfo;
    }

}
