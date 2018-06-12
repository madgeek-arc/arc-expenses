package arc.expenses;

import arc.expenses.CoordinatorInterface;
import gr.athenarc.domain.POI;
import gr.athenarc.domain.Request;

import java.util.List;

public class ApplicationStages extends Request implements CoordinatorInterface {

    @Override
    public List<POI> getCoordinator(String stage) {
        List<POI> coordinator;
        switch (stage) {
            case "1":
                coordinator = null;
                break;
            case "2":
                coordinator = (List<POI>) getProject().getScientificCoordinator();
                break;
            case "3":
                coordinator = getProject().getOperator();
                break;
            case "4":
                coordinator = (List<POI>) getProject().getInstitute().getOrganization().getPOI();
                break;
            case "5":
                coordinator = (List<POI>) getProject().getInstitute().getDirector();
                break;
            case "5a":
                coordinator = (List<POI>) getProject().getInstitute().getOrganization().getDirector();
                break;
            case "5b":
                coordinator = (List<POI>) getProject().getInstitute().getOrganization().getDioikitikoSumvoulio();
                break;
            case "6":
                coordinator = (List<POI>) getProject().getInstitute().getDiaugeia();
                break;
            case "7":
                coordinator = getProject().getOperator();
                break;
            case "8":
                coordinator = (List<POI>) getProject().getInstitute().getAccountingDirector();
                break;
            case "9":
                coordinator = (List<POI>) getProject().getInstitute().getOrganization().getPOI();
                break;
            case "10":
                coordinator = (List<POI>) getProject().getInstitute().getOrganization().getDirector();
                break;
            case "11":
                coordinator = (List<POI>) getProject().getInstitute().getDiaugeia();
                break;
            case "12":
                coordinator = (List<POI>) getProject().getInstitute().getAccountingRegistration();
                break;
            case "13":
                coordinator = (List<POI>) getProject().getInstitute().getAccountingPayment();
                break;
            default:
                return null;
        }
        return coordinator;
    }
}
