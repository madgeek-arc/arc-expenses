package listener;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import gr.athenarc.domain.Request;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utils.ParserPool;

import java.util.concurrent.ExecutionException;

@Component
public class StageResourceListener implements ResourceListener {

    private Logger logger = Logger.getLogger(StageResourceListener.class);

    @Autowired
    ParserPool parserPool;

    @Override
    public void resourceAdded(Resource resource) {
        // TODO
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        try {
            Request previousRequest = parserPool.deserialize(previousResource, Request.class).get();
            Request newRequest = parserPool.deserialize(previousResource, Request.class).get();
            if (previousRequest.getStage() != newRequest.getStage()) {
                logger.info("Stage changed from '"+previousRequest.getStage()+"' to '"+newRequest.getStage()+"'");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void resourceDeleted(Resource resource) {
        // TODO
    }
}