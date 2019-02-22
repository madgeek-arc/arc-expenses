package arc.expenses.config.stateMachine;

import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import arc.expenses.service.RequestApprovalServiceImpl;
import arc.expenses.service.TransitionService;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfiguration extends EnumStateMachineConfigurerAdapter<Stages, StageEvents> {

    private static Logger logger = LogManager.getLogger(StateMachineConfiguration.class);

    @Autowired
    private TransitionService transitionService;

    @Autowired
    private RequestApprovalServiceImpl requestApprovalService;


    @Override
    public void configure(StateMachineStateConfigurer<Stages, StageEvents> states) throws Exception {
        states.withStates()
                .initial(Stages.Stage1)
                .choice(Stages.Stage5a)
                .end(Stages.Stage13)
                .states(EnumSet.allOf(Stages.class));
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<Stages, StageEvents> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(true)
                .listener(loggingListener());
    }

    public StateMachineListener<Stages, StageEvents> loggingListener() {
        return new StateMachineListenerAdapter<Stages, StageEvents>() {
            @Override
            public void stateChanged(State<Stages, StageEvents> from, State<Stages, StageEvents> to) {

            }

            @Override
            public void stateMachineError(StateMachine<Stages, StageEvents> stateMachine, Exception exception) {
                logger.info("Exception received from machine");
                stateMachine.getExtendedState().getVariables().put("error",exception.getMessage());
            }

            @Override
            public void eventNotAccepted(Message<StageEvents> event) {
                logger.error("Event not accepted: {}", event.getPayload());
            }
        };
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<Stages, StageEvents> transitions) throws Exception {

        transitions.withExternal()
                .source(Stages.Stage1)
                .target(Stages.Stage2)
                .event(StageEvents.APPROVE)
                .guard(stateContext -> transitionService.checkContains(stateContext.getMessage(), Stage2.class))
                .and()
                .withExternal()
                .source(Stages.Stage1)
                .target(Stages.CANCELLED)
                .event(StageEvents.CANCEL)
                .action(stateContext -> {
                    try {
                        transitionService.cancelRequest(stateContext,"1");
                    } catch (Exception e) {
                        logger.error("Failed to cancel at Stage 1",e);
                    }
                })
                .and()
                .withExternal()
                .source(Stages.Stage2)
                .target(Stages.Stage1)
                .event(StageEvents.DOWNGRADE)
                .action(context -> {
                    Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                    transitionService.downgrade(context,"2","1",request.getStage1());
                })
                .and()
                .withExternal()
                    .source(Stages.Stage2)
                    .target(Stages.Stage3)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext.getMessage(), Stage2.class))
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            Stage2 stage2 = new Stage2(true,true,true);
                            stage2.setDate(LocalDate.now().toEpochDay()+"");
                            transitionService.approve(context,"2","3",stage2);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage2)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(stateContext -> {
                        try {
                            transitionService.cancelRequest(stateContext,"2");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 2",e);
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage2)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            Stage2 stage2 = new Stage2(true,true,true);
                            stage2.setDate(LocalDate.now().toEpochDay()+"");
                            transitionService.reject(context, stage2,"2");
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of request " + request.getId());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.Stage2)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        Stage2 stage2 = new Stage2(true,true,false);
                        stage2.setDate(LocalDate.now().toEpochDay()+"");
                        transitionService.downgrade(context,"3","2",stage2);
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.Stage4)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext.getMessage(), Stage3.class))
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);

                            Stage3 stage3 = new Stage3(true,true,false,"",true);
                            stage3.setLoan(Boolean.parseBoolean(Optional.ofNullable(req.getParameter("loan")).orElse("false")));
                            if(stage3.getLoan()) {
                                String loanSource = Optional.ofNullable(req.getParameter("loanSource")).orElse("");
                                if(loanSource.isEmpty())
                                    throw new ServiceException("Loan source cannot be empty");
                                stage3.setLoanSource(loanSource);
                            }
                            transitionService.approve(context,"3","4",stage3);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(stateContext -> {
                        try {
                            transitionService.cancelRequest(stateContext,"3");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 3",e);
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage3 stage3 = (requestApproval.getStage3() == null ? new Stage3() : requestApproval.getStage3());
                            stage3.setApproved(false);
                            transitionService.reject(context, stage3,"3");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage4)
                    .target(Stages.Stage3)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage3 stage3 = (requestApproval.getStage3() == null ? new Stage3() : requestApproval.getStage3());
                            stage3.setApproved(false);
                            transitionService.downgrade(context,"4","3",stage3);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrade of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }

                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage4)
                    .target(Stages.Stage5)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext.getMessage(), Stage4.class))
                    .action(context -> {
                        try {
                            Stage4 stage4 = new Stage4(true,true,true);
                            transitionService.approve(context,"4","5a",stage4);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage4)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(stateContext -> {
                        try {
                            transitionService.cancelRequest(stateContext,"4");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 4",e);
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage4)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage4 stage4 = (requestApproval.getStage4() == null ? new Stage4() : requestApproval.getStage4());
                            stage4.setApproved(false);
                            transitionService.reject(context, stage4,"4");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5)
                    .target(Stages.Stage4)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage4 stage4 = (requestApproval.getStage4() == null ? new Stage4() : requestApproval.getStage4());
                            stage4.setApproved(false);
                            transitionService.downgrade(context,"5a","4",stage4);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrade of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage5a stage5a = (requestApproval.getStage5a() == null ? new Stage5a() : requestApproval.getStage5a());
                            stage5a.setApproved(false);
                            transitionService.reject(context, stage5a,"5a");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(stateContext -> {
                        try {
                            transitionService.cancelRequest(stateContext,"5a");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 5a",e);
                        }
                    })
                    .and()
                .withChoice()
                    .source(Stages.Stage5a)
                    .first(Stages.Stage5b, stateContext -> {
                        if(!transitionService.checkContains(stateContext.getMessage(),Stage5a.class))
                            return false;

                        Request request = stateContext.getMessage().getHeaders().get("requestObj", Request.class);
                        if(
                                request.getStage1().getAmountInEuros()>20000 ||
                                request.getStage1().getSupplierSelectionMethod() == Stage1.SupplierSelectionMethod.AWARD_PROCEDURE ||
                                request.getType() == Request.Type.CONTRACT ||
                                request.getType() == Request.Type.SERVICES_CONTRACT
                        )
                            return true;

                        return false;
                    }, context -> {
                        try {
                            Stage5b stage5b = new Stage5b(true);
                            transitionService.approve(context,"5a","5b",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.Stage6, context -> {
                        try {
                            Stage6 stage6 = new Stage6();
                            transitionService.approve(context,"5a","6",stage6);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5)
                    .target(Stages.Stage5a)
                    .event(StageEvents.APPROVE)//we create a single transition for 5->5a since 5a is a choice state and will automatically move us to 5b or 6
                    .and()
                .withExternal()
                    .source(Stages.Stage5b)
                    .target(Stages.Stage6)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext.getMessage(), Stage5b.class))
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            Stage5b stage5b = new Stage5b(true);
                            transitionService.approve(context,"5b","6",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5b)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(stateContext -> {
                        try {
                            transitionService.cancelRequest(stateContext,"5b");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 5b",e);
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5b)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage5b stage5b = (requestApproval.getStage5b() == null ? new Stage5b() : requestApproval.getStage5b());
                            stage5b.setApproved(false);
                            transitionService.reject(context, stage5b,"5b");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5b)
                    .target(Stages.Stage5)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        String comment = Optional.ofNullable(context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class).getParameter("comment")).orElse("");
                        if(comment.isEmpty())
                            throw new ServiceException("We need a comment!");
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage5b stage5b = (requestApproval.getStage5b() == null ? new Stage5b() : requestApproval.getStage5b());
                            stage5b.setApproved(false);
                            transitionService.downgrade(context,"5b","5a",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrade of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    });
    }

}
