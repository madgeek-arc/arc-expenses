package arc.expenses.config.security;

import arc.expenses.service.PolicyCheckerService;
import arc.expenses.service.RequestServiceImpl;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnnotationChecks {

    @Autowired
    RequestServiceImpl requestService;

    @Autowired
    PolicyCheckerService policyCheckerService;

    public boolean isValidRequest(RequestApproval requestApproval, String email){
        return requestApproval!=null && isValidRequest(requestService.get(requestApproval.getRequestId()),email);
    }

    public boolean isValidRequest(RequestPayment requestPayment, String email){
        return requestPayment!=null && isValidRequest(requestService.get(requestPayment.getRequestId()),email);
    }

    public boolean isValidRequest(Request request , String email){
        return  request!=null && policyCheckerService.isRequestor(request,email) ||
                policyCheckerService.isSuppliesOfficeMemberOrDelegate(request,email) ||
                policyCheckerService.isScientificCoordinator(request,email) ||
                policyCheckerService.isPOYOrDelegate(request,email) ||
                policyCheckerService.isInstituteDirectorOrDelegate(request,email)||
                policyCheckerService.isAccountingRegistratorOrDelegate(request,email) ||
                policyCheckerService.isAccountingPaymentOrDelegate(request,email) ||
                policyCheckerService.isDiaugeiaOrDelegate(request,email) ||
                policyCheckerService.isDiataktisOrDelegate(request,email) ||
                policyCheckerService.isMemberOfABOrDelegate(request,email) ||
                policyCheckerService.isInspectionTeamOrDelegate(request,email);
    }

    public boolean validateDownload(String requestId,String email){
        Request request = requestService.get(requestId);
        //TODO change authorization
        return request!=null && policyCheckerService.isRequestor(request,email) ||
                policyCheckerService.isSuppliesOfficeMemberOrDelegate(request,email) ||
                policyCheckerService.isScientificCoordinator(request,email) ||
                policyCheckerService.isPOYOrDelegate(request,email) ||
                policyCheckerService.isInstituteDirectorOrDelegate(request,email)||
                policyCheckerService.isAccountingRegistratorOrDelegate(request,email) ||
                policyCheckerService.isAccountingPaymentOrDelegate(request,email) ||
                policyCheckerService.isDiaugeiaOrDelegate(request,email) ||
                policyCheckerService.isDiataktisOrDelegate(request,email) ||
                policyCheckerService.isMemberOfABOrDelegate(request,email) ||
                policyCheckerService.isInspectionTeamOrDelegate(request,email);
    }

}
