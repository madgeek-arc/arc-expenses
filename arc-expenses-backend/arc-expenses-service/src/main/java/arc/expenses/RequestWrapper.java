package arc.expenses;

import gr.athenarc.domain.POI;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.User;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * created by spyroukostas 13/06/18
 */
//public class RequestWrapper extends Request implements StageInterface {
public class RequestWrapper implements StageInterface {

    public static final String[] stages = {null,"1","2","3","4","5a","5b","6","7","8","9","10","11","12","13"};

    private Request request;

    public RequestWrapper(Request request) {
        super();
        this.request = request;
    }

    public static String getNextStage(Request request) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object stage = null;
        int index = Arrays.asList(stages).indexOf(request.getStage());
        do {
            index ++;
            stage = PropertyUtils.getProperty(request,"stage" + stages[index]);
        } while (stage == null);

        return stages[index];
    }

    public static String getPreviousStage(Request request) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object stage = null;
        int index = Arrays.asList(stages).indexOf(request.getStage());
        do {
            index --;
            stage = PropertyUtils.getProperty(request,"stage" + stages[index]);
        } while (stage == null);

        return stages[index];
    }

    public static String getPreviousStage(Request request, String stageFrom) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Object stage = null;
        int index = Arrays.asList(stages).indexOf(stageFrom);
        do {
            index --;
            stage = PropertyUtils.getProperty(request,"stage" + stages[index]);
        } while (stage == null);

        return stages[index];
    }

    public static boolean stageUnderReview(String previousStage, String nextStage) {
        if (Arrays.asList(stages).indexOf(previousStage) > Arrays.asList(stages).indexOf(nextStage)) {
            return true;
        }
        return false;
    }

    @Override
    public String getPreviousStage() {
        try {
            return getPreviousStage(request);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getNextStage() {
        try {
            return getNextStage(request);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User getUser(String stage) {
        User stageUser;
        switch (stage) {
            case "1":
                stageUser = request.getRequester();
                break;
            case "2":
                stageUser = request.getStage2().getUser();
                break;
            case "3":
                stageUser = request.getStage3().getUser();
                break;
            case "4":
                stageUser = request.getStage4().getUser();
                break;
            case "5":
                stageUser = request.getStage5().getUser();
                break;
            case "5a":
                stageUser = request.getStage5a().getUser();
                break;
            case "5b":
                stageUser = request.getStage5b().getUser();
                break;
            case "6":
                stageUser = request.getStage6().getUser();
                break;
            case "7":
                stageUser = request.getStage7().getUser();
                break;
            case "8":
                stageUser = request.getStage8().getUser();
                break;
            case "9":
                stageUser = request.getStage9().getUser();
                break;
            case "10":
                stageUser = request.getStage10().getUser();
                break;
            case "11":
                stageUser = request.getStage11().getUser();
                break;
            case "12":
                stageUser = request.getStage12().getUser();
                break;
            case "13":
                stageUser = request.getStage13().getUser();
                break;
            default:
                return null;
        }
        return stageUser;
    }

    @Override
    public List<POI> getPersonsOfInterest(String stage) {
        List<POI> personsOfInterest = new ArrayList();
        switch (stage) {
            case "1":
                personsOfInterest = null;
                break;
            case "2":
                personsOfInterest.add(request.getProject().getScientificCoordinator());
                break;
            case "3":
                personsOfInterest.addAll(request.getProject().getOperator());
                break;
            case "4":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getPOI());
                break;
            case "5":
                personsOfInterest.add(request.getProject().getInstitute().getDirector());
                break;
            case "5a":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getDirector());
                break;
            case "5b":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio());
                break;
            case "6":
                personsOfInterest.add(request.getProject().getInstitute().getDiaugeia());
                break;
            case "7":
                personsOfInterest.addAll(request.getProject().getOperator());
                break;
            case "8":
                personsOfInterest.addAll(request.getProject().getInstitute().getOrganization().getInspectionTeam());
                break;
            case "9":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getPOI());
                break;
            case "10":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getDirector());
                break;
            case "11":
                personsOfInterest.add(request.getProject().getInstitute().getDiaugeia());
                break;
            case "12":
                personsOfInterest.add(request.getProject().getInstitute().getAccountingRegistration());
                break;
            case "13":
                personsOfInterest.add(request.getProject().getInstitute().getAccountingPayment());
                break;
            default:
                return null;
        }
        return personsOfInterest;
    }

    @Override
    public String getDate(String stage) {
        String date;
        switch (stage) {
            case "1":
                date = request.getStage1().getRequestDate();
                break;
            case "2":
                date = request.getStage2().getDate();
                break;
            case "3":
                date = request.getStage3().getDate();
                break;
            case "4":
                date = request.getStage4().getDate();
                break;
            case "5":
                date = request.getStage5().getDate();
                break;
            case "5a":
                date = request.getStage5a().getDate();
                break;
            case "5b":
                date = request.getStage5b().getDate();
                break;
            case "6":
                date = request.getStage6().getDate();
                break;
            case "7":
                date = request.getStage7().getDate();
                break;
            case "8":
                date = request.getStage8().getDate();
                break;
            case "9":
                date = request.getStage9().getDate();
                break;
            case "10":
                date = request.getStage10().getDate();
                break;
            case "11":
                date = request.getStage11().getDate();
                break;
            case "12":
                date = request.getStage12().getDate();
                break;
            case "13":
                date = request.getStage13().getDate();
                break;
            default:
                return null;
        }
        return date;
    }

    @Override
    public String getComment(String stage) {
        String comment;
        switch (stage) {
            case "1":
                comment = request.getStage1().getSubject();
                break;
            case "2":
                comment = request.getStage2().getComment();
                break;
            case "3":
                comment = request.getStage3().getComment();
                break;
            case "4":
                comment = request.getStage4().getComment();
                break;
            case "5":
                comment = request.getStage5().getComment();
                break;
            case "5a":
                comment = request.getStage5a().getComment();
                break;
            case "5b":
                comment = request.getStage5b().getComment();
                break;
            case "6":
                comment = request.getStage6().getComment();
                break;
            case "7":
                comment = request.getStage7().getComment();
                break;
            case "8":
                comment = request.getStage8().getComment();
                break;
            case "9":
                comment = request.getStage9().getComment();
                break;
            case "10":
                comment = request.getStage10().getComment();
                break;
            case "11":
                comment = request.getStage11().getComment();
                break;
            case "12":
                comment = request.getStage12().getComment();
                break;
            case "13":
                comment = request.getStage13().getComment();
                break;
            default:
                return null;
        }
        return comment;
    }

    public Request getRequest() {
        return request;
    }
}
