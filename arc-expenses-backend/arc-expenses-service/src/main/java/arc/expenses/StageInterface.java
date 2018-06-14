package arc.expenses;

import gr.athenarc.domain.POI;
import gr.athenarc.domain.User;

import java.util.List;

/**
 * created by spyroukostas 13/06/18
 */
public interface StageInterface {

    User getUser(String stage);

    List<POI> getPersonsOfInterest(String stage);

    String getDate(String stage);

    String getComment(String stage);

    String getPreviousStage();

    String getNextStage();
}
