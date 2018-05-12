package service;

import gr.athenarc.domain.Delegate;
import gr.athenarc.domain.POY;
import gr.athenarc.domain.Request;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("policyCheckerSevice")
public class PolicyCheckerService {

    public List<Request> searchFilter(List<Request> rs, String email) {

        return rs.stream().filter(request ->
                        isRequestor(request,email) ||
                        isOperatorOrDelegate(request,email) ||
                        isScientificCoordinatorOrDelegate(request,email) ||
                        isPOYOrDelegate(request,email) ||
                        isDirectorOrDelegate(request,email)||
                        isAccountingRegistratorOrDelegate(request,email) ||
                        isAccountingPaymentOrDelegate(request,email) ||
                        isAccountingDirectorOrDelegate(request,email)
        ).collect(Collectors.toList());
    }

    private boolean  isScientificCoordinatorOrDelegate(Request request, String email) {
        return request.getProject().getScientificCoordinator().getEmail().equals(email)
            || isDelegate(request.getProject().getScientificCoordinator().getDelegates(),email);
    }

    private boolean isAccountingDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingDirector().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getAccountingDirector().getDelegates(),email);
    }

    private boolean isAccountingPaymentOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingPayment().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getAccountingPayment().getDelegates(),email);
    }

    private boolean isAccountingRegistratorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingRegistration().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getAccountingRegistration().getDelegates(),email);
    }



    private boolean isDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getDirector().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getOrganization().getDirector().getDelegates(),email);
    }

    private boolean isPOYOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getPOY().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getOrganization().getPOY().getDelegates(),email);
    }

    private boolean isOperatorOrDelegate(Request request, String email) {
        List<POY> operators = request.getProject().getOperator();

        if(operators!=null){
            for(POY operator:operators){

                if(operator.getEmail().equals(email))
                    return true;

                if(isDelegate(operator.getDelegates(),email))
                    return true;

            }
        }
        return false;

    }

    private boolean isDelegate(List<Delegate> delegates, String email) {
        if(delegates!=null){
            for(Delegate delegate:delegates)
                if(delegate.getEmail().equals(email))
                    return true;
        }
        return false;
    }

    private boolean isRequestor(Request request, String email) {
        return request.getRequester().getEmail().equals(email);
    }

    public boolean updateFilter(Request request, String email) {

        Boolean value = false;
        switch (request.getStage()) {
            case "2":
                value =  isScientificCoordinatorOrDelegate(request,email);
                break;
            case "3":
                value = isOperatorOrDelegate(request,email);
                break;
            case "3a":
                value = isDirectorOrDelegate(request,email);
                break;
            case "3b":
                value = isMemberOfAB(request,email);
                break;
            case "4":
                value = isPOYOrDelegate(request,email);
                break;
            case "5":
                value = isDirectorOrDelegate(request,email);
                break;
            case "6":
                value = isOrganizationDiaugeia(request,email);
                break;
            case "7":
                value = isAccountingDirectorOrDelegate(request,email);
                break;
            case "8":
                value = isPOYOrDelegate(request,email);
                break;
            case "9":
                value = isAccountingRegistratorOrDelegate(request,email);
                break;
            case "10":
                value = isAccountingPaymentOrDelegate(request,email);
                break;
        }
        return value;
    }

    private Boolean isOrganizationDiaugeia(Request request, String email) {
        return request.getStage6().getOrganizationDiaugeia().getEmail().equals(email);
    }

    private Boolean isMemberOfAB(Request request, String email) {
        return request.getStage5b().getDioikitikoSumvoulio().getEmail().equals(email);
    }
}
