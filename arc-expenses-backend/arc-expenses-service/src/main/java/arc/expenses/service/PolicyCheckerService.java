package arc.expenses.service;

import gr.athenarc.domain.Delegate;
import gr.athenarc.domain.POI;
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
                        isScientificCoordinator(request,email) ||
                        isPOYOrDelegate(request,email) ||
                        isInstituteDirectorOrDelegate(request,email)||
                        isAccountingRegistratorOrDelegate(request,email) ||
                        isAccountingPaymentOrDelegate(request,email) ||
                        isAccountingDirectorOrDelegate(request,email)
        ).collect(Collectors.toList());
    }

    public boolean isScientificCoordinator(Request request, String email) {
        return request.getProject().getScientificCoordinator().getEmail().equals(email);
    }

    public boolean isAccountingDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingDirector().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getAccountingDirector().getDelegates(),email);
    }

    public boolean isAccountingPaymentOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingPayment().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getAccountingPayment().getDelegates(),email);
    }

    public boolean isAccountingRegistratorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingRegistration().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getAccountingRegistration().getDelegates(),email);
    }



    public boolean isInstituteDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getDirector().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getDirector().getDelegates(),email);
    }

    public boolean isPOYOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getPOI().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getOrganization().getPOI().getDelegates(),email);
    }

    public boolean isOperatorOrDelegate(Request request, String email) {
        List<POI> operators = request.getProject().getOperator();

        if(operators!=null){
            for(POI operator:operators){

                if(operator.getEmail().equals(email))
                    return true;

                if(isDelegate(operator.getDelegates(),email))
                    return true;

            }
        }
        return false;

    }

    public boolean isDelegate(List<Delegate> delegates, String email) {
        if(delegates!=null){
            for(Delegate delegate:delegates)
                if(delegate.getEmail().equals(email))
                    return true;
        }
        return false;
    }

    public boolean isRequestor(Request request, String email) {
        return request.getRequester().getEmail().equals(email);
    }

    public boolean updateFilter(Request request, String email) {

        Boolean value = false;
        switch (request.getStage()) {
            case "1":
                value = isRequestor(request,email);
                break;
            case "2":
                value =  isScientificCoordinator(request,email);
                break;
            case "3":
                value = isOperatorOrDelegate(request,email);
                break;
            case "4":
                value = isPOYOrDelegate(request,email);
                break;
            case "5":
                value = isInstituteDirectorOrDelegate(request,email);
                break;
            case "5a":
                value = isOrganizationDirectorOrDelegate(request,email);
                break;
            case "5b":
                value = isMemberOfABOrDelegates(request,email);
                break;
            case "6":
                value = isDiaugeiaOrDelegate(request,email);
                break;
            case "7":
                value = isOperatorOrDelegate(request,email);
                break;
            case "8":
                value = isAccountingDirectorOrDelegate(request,email);
                break;
            case "9":
                value = isPOYOrDelegate(request,email);
                break;
            case "10":
                value = isOrganizationDirectorOrDelegate(request,email);
                break;
            case "11":
                value = isDiaugeiaOrDelegate(request,email);
                break;
            case "12":
                value = isAccountingRegistratorOrDelegate(request,email);
                break;
            case "13":
                value = isAccountingPaymentOrDelegate(request,email);
                break;
        }
        return value;
    }

    public Boolean isOrganizationDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getDirector().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getOrganization().getDirector().getDelegates(),email);
    }

    public Boolean isDiaugeiaOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getDiaugeia().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getDiaugeia().getDelegates(), email);
    }

    public Boolean isMemberOfABOrDelegates(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getDelegates(), email);
    }
}
