package arc.expenses;

import gr.athenarc.domain.POI;
import gr.athenarc.domain.Request;

import java.util.ArrayList;
import java.util.List;

public class ApplicationStages extends Request implements CoordinatorInterface {

    @Override
    public List<POI> getPersonsOfInterest(String stage) {
        List<POI> coordinator = new ArrayList();
        switch (stage) {
            case "1":
                coordinator = null;
                break;
            case "2":
                coordinator.add(getProject().getScientificCoordinator());
                break;
            case "3":
                coordinator.addAll(getProject().getOperator());
                break;
            case "4":
                coordinator.add(getProject().getInstitute().getOrganization().getPOI());
                break;
            case "5":
                coordinator.add(getProject().getInstitute().getDirector());
                break;
            case "5a":
                coordinator.add(getProject().getInstitute().getOrganization().getDirector());
                break;
            case "5b":
                coordinator.add(getProject().getInstitute().getOrganization().getDioikitikoSumvoulio());
                break;
            case "6":
                coordinator.add(getProject().getInstitute().getDiaugeia());
                break;
            case "7":
                coordinator.addAll(getProject().getOperator());
                break;
            case "8":
                coordinator.addAll(getProject().getInstitute().getOrganization().getInspectionTeam());
                break;
            case "9":
                coordinator.add(getProject().getInstitute().getOrganization().getPOI());
                break;
            case "10":
                coordinator.add(getProject().getInstitute().getOrganization().getDirector());
                break;
            case "11":
                coordinator.add(getProject().getInstitute().getDiaugeia());
                break;
            case "12":
                coordinator.add(getProject().getInstitute().getAccountingRegistration());
                break;
            case "13":
                coordinator.add(getProject().getInstitute().getAccountingPayment());
                break;
            default:
                return null;
        }
        return coordinator;
    }
}
