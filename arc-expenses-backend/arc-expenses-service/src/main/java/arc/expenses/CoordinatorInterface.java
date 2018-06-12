package arc.expenses;

import gr.athenarc.domain.POI;

import java.util.List;

public interface CoordinatorInterface {

    List<POI> getCoordinator(String stage);
}
