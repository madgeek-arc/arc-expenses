package arc.expenses.utils;

import arc.expenses.domain.RequestFatClass;
import gr.athenarc.domain.BaseInfo;
import gr.athenarc.domain.Request;
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

    public static RequestFatClass toRequestFatClass(Request request, RequestApproval newRequestApproval) {

        RequestFatClass requestFatClass = new RequestFatClass();


        requestFatClass.setRequest_id(request.getId());
        requestFatClass.setId(newRequestApproval.getId());
        requestFatClass.setType(request.getType());
        requestFatClass.setUser(request.getUser());
        requestFatClass.setProject(request.getProject());
        requestFatClass.setId(newRequestApproval.getId());
        requestFatClass.setStage1(request.getStage1());
        requestFatClass.setStage2(newRequestApproval.getStage2());
        requestFatClass.setStage3(newRequestApproval.getStage3());
        requestFatClass.setStage4(newRequestApproval.getStage4());
        requestFatClass.setStage5(newRequestApproval.getStage5());
        requestFatClass.setStage5a(newRequestApproval.getStage5a());
        requestFatClass.setStage5b(newRequestApproval.getStage5b());
        requestFatClass.setStage6(newRequestApproval.getStage6());

        return requestFatClass;
    }

    public static RequestFatClass toRequestFatClass(Request request, RequestPayment requestPayment) {

        RequestFatClass requestFatClass = new RequestFatClass();

        requestFatClass.setRequest_id(request.getId());
        requestFatClass.setId(requestPayment.getId());
        requestFatClass.setType(request.getType());
        requestFatClass.setUser(request.getUser());
        requestFatClass.setProject(request.getProject());
        requestFatClass.setId(requestPayment.getId());
        requestFatClass.setStage1(request.getStage1());
        requestFatClass.setStage7(requestPayment.getStage7());
        requestFatClass.setStage8(requestPayment.getStage8());
        requestFatClass.setStage9(requestPayment.getStage9());
        requestFatClass.setStage10(requestPayment.getStage10());
        requestFatClass.setStage11(requestPayment.getStage11());
        requestFatClass.setStage12(requestPayment.getStage12());
        requestFatClass.setStage13(requestPayment.getStage13());

        return requestFatClass;
    }

}
