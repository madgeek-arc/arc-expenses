package arc.expenses.service;

import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.BaseInfo;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestPayment;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@Service("requestPayment")
public class RequestPaymentServiceImpl extends GenericService<RequestPayment> {


    private static Logger logger = LogManager.getLogger(RequestPaymentServiceImpl.class);

    public RequestPaymentServiceImpl() {
        super(RequestPayment.class);
    }

    @Override
    public String getResourceType() {
        return "payment";
    }


    @Autowired
    private StateMachineFactory<Stages, StageEvents> factory;


    private StateMachine<Stages, StageEvents> build(RequestPayment payment){

        StateMachine<Stages, StageEvents> sm = this.factory.getStateMachine(payment.getId());
        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {

                    sma.addStateMachineInterceptor( new StateMachineInterceptorAdapter<Stages, StageEvents>(){

                        @Override
                        public void postStateChange(State state, Message message, Transition transition, StateMachine stateMachine) {
                            Optional.ofNullable(message).ifPresent(msg -> {
                                Optional.ofNullable((RequestPayment) msg.getHeaders().get("paymentObj"))
                                        .ifPresent(payment ->{
                                            payment.setCurrentStage(state.getId()+""); // <-- casting to String causes uncertain behavior. Keep it this way
                                            try {
                                                logger.info("Updating "+ payment.getId()+" payment's stage to " + state.getId());
                                                update(payment, payment.getId());
                                            } catch (ResourceNotFoundException e) {
                                                throw new ServiceException("Request with id " + payment.getId() + " not found");
                                            }
                                            msg.getHeaders().replace("paymentObj",payment);
                                        });
                            });
                        }
                    });

                    sma.resetStateMachine(new DefaultStateMachineContext<>(
                            Stages.valueOf((payment.getCurrentStage() == null ? Stages.Stage7.name() : payment.getCurrentStage())), null, null, null));

                    logger.info("Resetting machine of payment " + payment.getId() + " at state " + sm.getState().getId());
                });

        sm.start();
        return sm;
    }


    @PreAuthorize("hasPermission(#request,'EDIT')")
    public void approve(Request request, RequestPayment requestPayment, HttpServletRequest req) {

        logger.info("Approving payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.APPROVE)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }


    @PreAuthorize("hasPermission(#request,'EDIT')")
    public void reject(Request request, RequestPayment requestPayment, HttpServletRequest req) {
        logger.info("Rejecting payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.REJECT)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }


    @PreAuthorize("hasPermission(#request,'EDIT')")
    public void downgrade(Request request, RequestPayment requestPayment, HttpServletRequest req) {
        logger.info("Downgrading payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.DOWNGRADE)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }

    @PreAuthorize("hasPermission(#request,'CANCEL')")
    public void cancel(Request request, RequestPayment requestPayment) throws Exception {
        logger.info("Canceling payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.CANCEL)
                .setHeader("paymentObj", requestPayment)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();
    }

    public RequestPayment createPayment(Request request){
        RequestPayment requestPayment = new RequestPayment();
        requestPayment.setId(request.getId()+"-p1");
        requestPayment.setRequestId(request.getId());
        requestPayment.setCreationDate(new Date().toInstant().toEpochMilli());
        requestPayment.setStage("7");
        requestPayment.setStatus(BaseInfo.Status.PENDING);
        requestPayment.setCurrentStage(Stages.Stage7.name());
        return add(requestPayment,null);
    }

    public Browsing<RequestPayment> getPayments(String id, Authentication u) throws Exception {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());
        filter.addFilter("request_id",id);

        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1000);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "creation_date";

        order.put("order",orderDirection);
        sort.put(orderField, order);
        filter.setOrderBy(sort);

        return getAll(filter,u);
    }

    public String generateID(String requestId) {
        String maxID = getMaxID();
        if(maxID == null)
            return requestId+"-p1";
        else
            return requestId+"-p"+(Integer.valueOf(maxID.split("-p")[1])+1);
    }


    private String getMaxID() {

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());
        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "payment_id";

        order.put("order",orderDirection);
        sort.put(orderField, order);
        filter.setOrderBy(sort);

        try {
            List rs = searchService.search(filter).getResults();
            Resource payment;
            if(rs.size() > 0) {
                payment = ((Resource) rs.get(0));
                return parserPool.deserialize(payment, RequestPayment.class).getId();
            }
        } catch (IOException e) {
            logger.debug("Error on search controller",e);
        }
        return null;
    }

}

