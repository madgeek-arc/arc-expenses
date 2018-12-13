package arc.expenses.service;

import arc.expenses.domain.RequestSummary;
import gr.athenarc.domain.Delegate;
import gr.athenarc.domain.PersonOfInterest;
import gr.athenarc.domain.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("policyCheckerSevice")
public class PolicyCheckerService {

    @Value("#{'${admin.emails}'.split(',')}")
    private List<String> admins;

    public List<Request> searchFilter(List<Request> rs, String email) {

        return rs.stream().filter(request ->
                        isRequestor(request,email) ||
                        isSuppliesOfficeMemberOrDelegate(request,email) ||
                        isScientificCoordinator(request,email) ||
                        isPOYOrDelegate(request,email) ||
                        isInstituteDirectorOrDelegate(request,email)||
                        isAccountingRegistratorOrDelegate(request,email) ||
                        isAccountingPaymentOrDelegate(request,email) ||
                        isInspectionTeamOrDelegate(request,email)
        ).collect(Collectors.toList());
    }

    public boolean isScientificCoordinator(Request request, String email) {
        return request.getProject().getScientificCoordinator().getEmail().equals(email);
    }

    public boolean isInspectionTeamOrDelegate(Request request, String email) {

        List<PersonOfInterest> inspectionTeam = request.getProject().getInstitute().getOrganization().getInspectionTeam();

        if(inspectionTeam!=null){
            for(PersonOfInterest operator:inspectionTeam){

                if(operator.getEmail().equals(email))
                    return true;

                if(isDelegate(operator.getDelegates(),email))
                    return true;

            }
        }
        return false;
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
        return request.getProject().getInstitute().getOrganization().getPoy().getEmail().equals(email)
            || isDelegate(request.getProject().getInstitute().getOrganization().getPoy().getDelegates(),email);
    }

    public boolean isSuppliesOfficeMemberOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getSuppliesOffice().getEmail().equals(email);
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
        return request.getUser().getEmail().equals(email);
    }

    public boolean updateFilter(RequestSummary requestSummary, String email) {

        Boolean value = false;
        Request request = requestSummary.getRequest();
        switch (requestSummary.getBaseInfo().getStage()) {
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
            case "5a":
                value = isDiataktisOrDelegate(request,email);
                break;
            case "5b":
                value = isMemberOfABOrDelegate(request,email);
                break;
            case "6":
                value = isDiaugeiaOrDelegate(request,email);
                break;
            case "7":
                if(request.getType().equals("trip"))
                    value = isTravelManagerOrDelegate(request,email) || isRequestor(request,email);
                else
                    value = isSuppliesOfficeMemberOrDelegate(request,email) || isRequestor(request,email);
                break;
            case "8":
                value = isInspectionTeamOrDelegate(request,email);
                break;
            case "9":
                value = isPOYOrDelegate(request,email);
                break;
            case "10":
                value = isDiataktisOrDelegate(request,email);
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

    public Boolean isTravelManagerOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getTravelManager().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getTravelManager().getDelegates(),email);
    }

    public Boolean isDiataktisOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getDiataktis().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getOrganization().getDirector().getDelegates(),email);
    }

    public Boolean isDiaugeiaOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getDiaugeia().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getDiaugeia().getDelegates(), email);
    }

    public Boolean isMemberOfABOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getDelegates(), email);
    }

    public boolean isOrganizationDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getDirector().getEmail().equals(email)
                || isDelegate(request.getProject().getInstitute().getOrganization().getDirector().getDelegates(),email);
    }

    public boolean isAdmin(Request request, String email) {
        return admins.contains(email);
    }

    public boolean isOperatorOrDelegate(Request request, String email) {

        List<PersonOfInterest> operators = request.getProject().getOperator();

        if(operators!=null){
            for(PersonOfInterest operator:operators){

                if(operator.getEmail().equals(email))
                    return true;

                if(isDelegate(operator.getDelegates(),email))
                    return true;

            }
        }
        return false;

    }
}
