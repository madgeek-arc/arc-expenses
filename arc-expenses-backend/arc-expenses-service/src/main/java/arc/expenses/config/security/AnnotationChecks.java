package arc.expenses.config.security;

import arc.expenses.PolicyCheckerService;
import arc.expenses.RequestApprovalServiceImpl;
import arc.expenses.RequestPaymentServiceImpl;
import arc.expenses.RequestServiceImpl;
import eu.openminted.registry.core.domain.Browsing;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnnotationChecks {

    @Autowired
    RequestServiceImpl requestService;

    @Autowired
    RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    RequestPaymentServiceImpl requestPaymentService;

    @Autowired
    PolicyCheckerService policyCheckerService;

    public boolean isValidRequest(RequestApproval requestApproval, String email){
        return requestApproval!=null && isValidRequest(requestService.get(requestApproval.getRequestId()),email.toLowerCase());
    }

    public boolean isValidRequest(RequestPayment requestPayment, String email){
        return requestPayment!=null && isValidRequest(requestService.get(requestPayment.getRequestId()),email.toLowerCase());
    }

    public boolean isValidRequest(Browsing<RequestPayment> requestPayment, String email){

        List<RequestPayment> rs = requestPayment.getResults();
        for(RequestPayment rp : rs){
            if(!isValidRequest(requestService.get(rp.getRequestId()),email.toLowerCase()))
                return false;
        }
        return true;
    }


    public boolean isValidRequest(Request request , String email){
        return  request!=null && policyCheckerService.isRequestor(request,email.toLowerCase()) ||
                policyCheckerService.isSuppliesOfficeMemberOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isViceDirectorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isScientificCoordinator(request,email.toLowerCase()) ||
                policyCheckerService.isPOYOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isInstituteDirectorOrDelegate(request,email.toLowerCase())||
                policyCheckerService.isOperatorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isAccountingRegistratorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isAccountingPaymentOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isDiaugeiaOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isDiataktisOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isMemberOfABOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isOrganizationDirectorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isTravelManagerOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isAdmin(request,email.toLowerCase()) ||
                policyCheckerService.isTravelManagerOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isInspectionTeamOrDelegate(request,email.toLowerCase());
    }

    public boolean validateDownload(String requestId,String mode,String email){

        Request request =  null;
        if(mode.equals("approval"))
            request = requestService.get(requestApprovalService.get(requestId).getRequestId());
        else if(mode.equals("payment"))
            request = requestService.get(requestPaymentService.get(requestId).getRequestId());
        else
            request = requestService.get(requestId);
        //TODO change authorization
        return request!=null && policyCheckerService.isRequestor(request,email.toLowerCase()) ||
                policyCheckerService.isSuppliesOfficeMemberOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isViceDirectorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isScientificCoordinator(request,email.toLowerCase()) ||
                policyCheckerService.isPOYOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isInstituteDirectorOrDelegate(request,email.toLowerCase())||
                policyCheckerService.isAccountingRegistratorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isAccountingPaymentOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isDiaugeiaOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isDiataktisOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isMemberOfABOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isTravelManagerOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isOrganizationDirectorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isAdmin(request,email.toLowerCase()) ||
                policyCheckerService.isTravelManagerOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isOperatorOrDelegate(request,email.toLowerCase()) ||
                policyCheckerService.isInspectionTeamOrDelegate(request,email.toLowerCase());
    }

}
