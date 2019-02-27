package arc.expenses.config.stateMachine;

import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import arc.expenses.service.RequestApprovalServiceImpl;
import arc.expenses.service.RequestPaymentServiceImpl;
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

    @Autowired
    private RequestPaymentServiceImpl requestPaymentService;


    @Override
    public void configure(StateMachineStateConfigurer<Stages, StageEvents> states) throws Exception {
        states.withStates()
                .initial(Stages.Stage1)
                .choice(Stages.Stage5a)
                .choice(Stages.Stage6Choice)
                .end(Stages.FINISHED)
                .end(Stages.REJECTED)
                .end(Stages.CANCELLED)
                .states(EnumSet.allOf(Stages.class))
            ;
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
                .action(context -> {
                    try {
                        transitionService.cancelRequestApproval(context,"1");
                    } catch (Exception e) {
                        logger.error("Failed to cancel at Stage 1",e);
                        context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                        throw new ServiceException(e.getMessage());
                    }
                })
                .and()
                .withExternal()
                .source(Stages.Stage2)
                .target(Stages.Stage1)
                .event(StageEvents.DOWNGRADE)
                .action(context -> {
                    Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                    try {
                        RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                        Stage1 stage1 = Optional.ofNullable(requestApproval.getStage1()).orElse(new Stage1());
                        transitionService.downgradeApproval(context,"2","1",stage1);
                    } catch (Exception e) {
                        logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                        context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                        throw new ServiceException(e.getMessage());
                    }
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
                            stage2.setDate(LocalDate.now().toEpochDay());
                            transitionService.approveApproval(context,"2","3",stage2);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage2)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestApproval(context,"2");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 2",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
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
                            stage2.setDate(LocalDate.now().toEpochDay());
                            transitionService.rejectApproval(context, stage2,"2");
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of request " + request.getId());
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.Stage2)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        Stage2 stage2 = new Stage2(true,true,false);
                        stage2.setDate(LocalDate.now().toEpochDay());
                        transitionService.downgradeApproval(context,"3","2",stage2);
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
                                if(loanSource.isEmpty()) {
                                    context.getStateMachine().setStateMachineError(new ServiceException("Loan source cannot be empty"));
                                    throw new ServiceException("Loan source cannot be empty");

                                }
                                stage3.setLoanSource(loanSource);
                            }
                            transitionService.approveApproval(context,"3","4",stage3);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestApproval(context,"3");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 3",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
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
                            Stage3 stage3 = Optional.ofNullable(requestApproval.getStage3()).orElse(new Stage3());
                            stage3.setApproved(false);
                            transitionService.rejectApproval(context, stage3,"3");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
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
                            Stage3 stage3 = Optional.ofNullable(requestApproval.getStage3()).orElse(new Stage3());
                            stage3.setApproved(false);
                            transitionService.downgradeApproval(context,"4","3",stage3);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
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
                            transitionService.approveApproval(context,"4","5a",stage4);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage4)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestApproval(context,"4");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 4",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
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
                            Stage4 stage4 = Optional.ofNullable(requestApproval.getStage4()).orElse(new Stage4());
                            stage4.setApproved(false);
                            transitionService.rejectApproval(context, stage4,"4");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
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
                            Stage4 stage4 = Optional.ofNullable(requestApproval.getStage4()).orElse(new Stage4());
                            stage4.setApproved(false);
                            transitionService.downgradeApproval(context,"5a","4",stage4);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
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
                            Stage5a stage5a = Optional.ofNullable(requestApproval.getStage5a()).orElse(new Stage5a());
                            stage5a.setApproved(false);
                            transitionService.rejectApproval(context, stage5a,"5a");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestApproval(context,"5a");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 5a",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withChoice()
                    .source(Stages.Stage5a)
                    .first(Stages.Stage5b, context -> {
                        if(!transitionService.checkContains(context.getMessage(),Stage5a.class))
                            return false;

                        try {
                            Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            if(
                                    requestApproval.getStage1().getAmountInEuros()>20000 ||
                                            requestApproval.getStage1().getSupplierSelectionMethod() == Stage1.SupplierSelectionMethod.AWARD_PROCEDURE ||
                                            request.getType() == Request.Type.CONTRACT ||
                                            request.getType() == Request.Type.SERVICES_CONTRACT
                            )
                                return true;

                            return false;
                        } catch (Exception e) {
                            logger.error("Error occurred on choice of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }

                    }, context -> {
                        try {
                            Stage5b stage5b = new Stage5b(true);
                            transitionService.approveApproval(context,"5a","5b",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.Stage6, context -> {
                        try {
                            Stage6 stage6 = new Stage6();
                            transitionService.approveApproval(context,"5a","6",stage6);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
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
                            transitionService.approveApproval(context,"5b","6",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage5b)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestApproval(context,"5b");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 5b",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
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
                            Stage5b stage5b = Optional.ofNullable(requestApproval.getStage5b()).orElse(new Stage5b());
                            stage5b.setApproved(false);
                            transitionService.rejectApproval(context, stage5b,"5b");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
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
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage5b stage5b = Optional.ofNullable(requestApproval.getStage5b()).orElse(new Stage5b());
                            stage5b.setApproved(false);
                            transitionService.downgradeApproval(context,"5b","5a",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withChoice()
                    .source(Stages.Stage6Choice)
                    .first(Stages.Stage5b, stateContext -> {

                        try {
                            Request request = stateContext.getMessage().getHeaders().get("requestObj", Request.class);
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            if(requestApproval.getStage5b()!=null)
                                return true;

                            return false;
                        } catch (Exception e) {
                            logger.error("Failed to downgradeApproval 6->5b",e);
                            return false;
                        }
                    }, context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage5b stage5b = Optional.ofNullable(requestApproval.getStage5b()).orElse(new Stage5b());
                            stage5b.setApproved(false);
                            transitionService.downgradeApproval(context,"6","5b",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.Stage5, context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage5a stage5a = Optional.ofNullable(requestApproval.getStage5a()).orElse(new Stage5a());
                            stage5a.setApproved(false);
                            transitionService.downgradeApproval(context,"6","5a",stage5a);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage6)
                    .target(Stages.Stage6Choice)
                    .event(StageEvents.DOWNGRADE)
                    .and()
                .withExternal()
                    .source(Stages.Stage6)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestApproval(context,"6");
                        } catch (Exception e) {
                            logger.error("Failed to cancel at Stage 6",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage6)
                    .target(Stages.Stage7)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext.getMessage(), Stage6.class))
                    .action(context -> {
                        try {
                            transitionService.movingToStage7(context);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7)
                    .target(Stages.REJECTED)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage7 stage7 = Optional.ofNullable(payment.getStage7()).orElse(new Stage7());
                            stage7.setApproved(false);
                            transitionService.rejectPayment(context, stage7,"7");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7)
                    .target(Stages.CANCELLED)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestPayment(context,"7");
                        } catch (Exception e) {
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7)
                    .target(Stages.Stage8)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext.getMessage(), Stage7.class))
                    .action(context -> {
                        try {
                            Stage7 stage7 = new Stage7(true);
                            transitionService.approvePayment(context,"7","8",stage7);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage8)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage8 stage8 = Optional.ofNullable(payment.getStage8()).orElse(new Stage8());
                            stage8.setApproved(false);
                            transitionService.rejectPayment(context, stage8,"8");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage8)
                    .target(Stages.Stage7)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage8 stage8 = Optional.ofNullable(requestPayment.getStage8()).orElse(new Stage8());
                            stage8.setApproved(false);
                            transitionService.downgradePayment(context,"8","7",stage8);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradePayment of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                ;
    }

}
