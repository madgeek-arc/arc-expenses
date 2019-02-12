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
                        isRequestor(request,email.toLowerCase()) ||
                        isSuppliesOfficeMemberOrDelegate(request,email.toLowerCase()) ||
                        isScientificCoordinator(request,email.toLowerCase()) ||
                        isPOYOrDelegate(request,email.toLowerCase()) ||
                        isInstituteDirectorOrDelegate(request,email.toLowerCase())||
                        isAccountingRegistratorOrDelegate(request,email.toLowerCase()) ||
                        isAccountingPaymentOrDelegate(request,email.toLowerCase()) ||
                        isInspectionTeamOrDelegate(request,email.toLowerCase())
        ).collect(Collectors.toList());
    }

    public boolean isScientificCoordinator(Request request, String email) {
        return request.getProject().getScientificCoordinator().getEmail().equals(email);
    }

    public boolean isInspectionTeamOrDelegate(Request request, String email) {

        List<PersonOfInterest> inspectionTeam = request.getProject().getInstitute().getOrganization().getInspectionTeam();

        if(inspectionTeam!=null){
            for(PersonOfInterest operator:inspectionTeam){

                if(operator.getEmail().equals(email.toLowerCase()))
                    return true;

                if(isDelegate(operator.getDelegates(),email.toLowerCase()))
                    return true;

            }
        }
        return false;
    }

    public boolean isAccountingPaymentOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingPayment().getEmail().equals(email.toLowerCase())
            || isDelegate(request.getProject().getInstitute().getAccountingPayment().getDelegates(),email.toLowerCase());
    }

    public boolean isAccountingRegistratorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getAccountingRegistration().getEmail().equals(email.toLowerCase())
            || isDelegate(request.getProject().getInstitute().getAccountingRegistration().getDelegates(),email.toLowerCase());
    }



    public boolean isInstituteDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getDirector().getEmail().equals(email.toLowerCase())
            || isDelegate(request.getProject().getInstitute().getDirector().getDelegates(),email.toLowerCase());
    }

    public boolean isPOYOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getPoy().getEmail().equals(email.toLowerCase())
            || isDelegate(request.getProject().getInstitute().getOrganization().getPoy().getDelegates(),email.toLowerCase());
    }

    public boolean isSuppliesOfficeMemberOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getSuppliesOffice().getEmail().equals(email.toLowerCase());
    }

    public boolean isDelegate(List<Delegate> delegates, String email) {
        if(delegates!=null){
            for(Delegate delegate:delegates)
                if(delegate.getEmail().equals(email.toLowerCase()))
                    return true;
        }
        return false;
    }

    public boolean isRequestor(Request request, String email) {
        return request.getUser().getEmail().equals(email.toLowerCase());
    }

    public boolean updateFilter(RequestSummary requestSummary, String email) {

        Boolean value = false;
        Request request = requestSummary.getRequest();
        switch (requestSummary.getBaseInfo().getStage()) {
            case "1":
                value = isRequestor(request,email.toLowerCase());
                break;
            case "2":
                value =  isScientificCoordinator(request,email.toLowerCase());
                break;
            case "3":
                value = isOperatorOrDelegate(request,email.toLowerCase());
                break;
            case "4":
                value = isPOYOrDelegate(request,email.toLowerCase());
                break;
            case "5a":
                if(request.getType().equals("trip")){
                    if ( (request.getUser().getEmail() == request.getProject().getInstitute().getDiataktis().getEmail())
                            || (request.getTrip().getEmail() == request.getProject().getInstitute().getDiataktis().getEmail())) {
                        if ( (request.getUser().getEmail() == request.getProject().getInstitute().getOrganization().getDirector().getEmail()) ||
                                (request.getTrip().getEmail() == request.getProject().getInstitute().getOrganization().getDirector().getEmail()))
                            value = isViceDirectorOrDelegate(request,email.toLowerCase());
                        else
                            value = isInstituteDirectorOrDelegate(request,email.toLowerCase());
                    }
                }else
                    value = isDiataktisOrDelegate(request,email.toLowerCase());
                break;
            case "5b":
                value = isMemberOfABOrDelegate(request,email.toLowerCase());
                break;
            case "6":
                value = isDiaugeiaOrDelegate(request,email.toLowerCase());
                break;
            case "7":
                if(request.getType().equals("trip"))
                    value = isTravelManagerOrDelegate(request,email.toLowerCase()) || isRequestor(request,email.toLowerCase());
                else
                    value = isSuppliesOfficeMemberOrDelegate(request,email.toLowerCase()) || isRequestor(request,email.toLowerCase());
                break;
            case "8":
                value = isInspectionTeamOrDelegate(request,email.toLowerCase());
                break;
            case "9":
                value = isPOYOrDelegate(request,email.toLowerCase());
                break;
            case "10":
                if(request.getType().equals("trip")){
                    if ( (request.getUser().getEmail() == request.getProject().getInstitute().getDiataktis().getEmail())
                            || (request.getTrip().getEmail() == request.getProject().getInstitute().getDiataktis().getEmail())) {
                        if ( (request.getUser().getEmail() == request.getProject().getInstitute().getOrganization().getDirector().getEmail()) ||
                                (request.getTrip().getEmail() == request.getProject().getInstitute().getOrganization().getDirector().getEmail()))
                            value = isViceDirectorOrDelegate(request,email.toLowerCase());
                        else
                            value = isInstituteDirectorOrDelegate(request,email.toLowerCase());
                    }
                }else
                    value = isDiataktisOrDelegate(request,email.toLowerCase());
                break;
            case "11":
                value = isDiaugeiaOrDelegate(request,email.toLowerCase());
                break;
            case "12":
                value = isAccountingRegistratorOrDelegate(request,email.toLowerCase());
                break;
            case "13":
                value = isAccountingPaymentOrDelegate(request,email.toLowerCase());
                break;
        }
        return value;
    }

    private Boolean isViceDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getViceDirector().getEmail().equals(email.toLowerCase())
                || isDelegate(request.getProject().getInstitute().getOrganization().getViceDirector().getDelegates(),email.toLowerCase());
    }

    public Boolean isTravelManagerOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getTravelManager().getEmail().equals(email.toLowerCase())
                || isDelegate(request.getProject().getInstitute().getTravelManager().getDelegates(),email.toLowerCase());
    }

    public Boolean isDiataktisOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getDiataktis().getEmail().equals(email.toLowerCase())
                || isDelegate(request.getProject().getInstitute().getOrganization().getDirector().getDelegates(),email.toLowerCase());
    }

    public Boolean isDiaugeiaOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getDiaugeia().getEmail().equals(email.toLowerCase())
                || isDelegate(request.getProject().getInstitute().getDiaugeia().getDelegates(), email.toLowerCase());
    }

    public Boolean isMemberOfABOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getEmail().equals(email.toLowerCase())
                || isDelegate(request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getDelegates(), email.toLowerCase());
    }

    public boolean isOrganizationDirectorOrDelegate(Request request, String email) {
        return request.getProject().getInstitute().getOrganization().getDirector().getEmail().equals(email.toLowerCase())
                || isDelegate(request.getProject().getInstitute().getOrganization().getDirector().getDelegates(),email.toLowerCase());
    }

    public boolean isAdmin(Request request, String email) {
        return admins.contains(email.toLowerCase());
    }

    public boolean isOperatorOrDelegate(Request request, String email) {

        List<PersonOfInterest> operators = request.getProject().getOperator();

        if(operators!=null){
            for(PersonOfInterest operator:operators){

                if(operator.getEmail().equals(email.toLowerCase()))
                    return true;

                if(isDelegate(operator.getDelegates(),email.toLowerCase()))
                    return true;

            }
        }
        return false;

    }
}
