package arc.expenses.config.security;

import arc.expenses.service.PolicyCheckerService;
import arc.expenses.service.RequestApprovalServiceImpl;
import arc.expenses.service.RequestPaymentServiceImpl;
import arc.expenses.service.RequestServiceImpl;
import eu.openminted.registry.core.domain.Browsing;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        return requestApproval!=null && isValidRequest(requestService.get(requestApproval.getRequestId()),email);
    }

    public boolean isValidRequest(RequestPayment requestPayment, String email){
        return requestPayment!=null && isValidRequest(requestService.get(requestPayment.getRequestId()),email);
    }

    public boolean isValidRequest(Browsing<RequestPayment> requestPayment, String email){

        List<RequestPayment> rs = requestPayment.getResults();
        for(RequestPayment rp : rs){
            if(!isValidRequest(requestService.get(rp.getRequestId()),email))
                return false;
        }
        return true;
    }


    public boolean isValidRequest(Request request , String email){
        return  request!=null && policyCheckerService.isRequestor(request,email) ||
                policyCheckerService.isSuppliesOfficeMemberOrDelegate(request,email) ||
                policyCheckerService.isScientificCoordinator(request,email) ||
                policyCheckerService.isPOYOrDelegate(request,email) ||
                policyCheckerService.isInstituteDirectorOrDelegate(request,email)||
                policyCheckerService.isOperatorOrDelegate(request,email) ||
                policyCheckerService.isAccountingRegistratorOrDelegate(request,email) ||
                policyCheckerService.isAccountingPaymentOrDelegate(request,email) ||
                policyCheckerService.isDiaugeiaOrDelegate(request,email) ||
                policyCheckerService.isDiataktisOrDelegate(request,email) ||
                policyCheckerService.isMemberOfABOrDelegate(request,email) ||
                policyCheckerService.isOrganizationDirectorOrDelegate(request,email) ||
                policyCheckerService.isTravelManagerOrDelegate(request,email) ||
                policyCheckerService.isAdmin(request,email) ||
                policyCheckerService.isInspectionTeamOrDelegate(request,email);
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
                policyCheckerService.isTravelManagerOrDelegate(request,email) ||
                policyCheckerService.isOrganizationDirectorOrDelegate(request,email) ||
                policyCheckerService.isAdmin(request,email) ||
                policyCheckerService.isInspectionTeamOrDelegate(request,email);
    }

}
