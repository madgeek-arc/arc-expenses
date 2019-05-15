package arc.expenses.config.stateMachine;

import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import arc.expenses.service.*;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
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
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

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

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private MailService mailService;


    @Override
    public void configure(StateMachineStateConfigurer<Stages, StageEvents> states) throws Exception {
        states.withStates()
                .initial(Stages.Stage1)
                .choice(Stages.Stage5a)
                .choice(Stages.Stage6ChoiceDowngrade)
                .choice(Stages.Stage7aOr8)
                .choice(Stages.Stage8ChoiceDowngrade)
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
    @DependsOn("factory")
    public void configure(StateMachineTransitionConfigurer<Stages, StageEvents> transitions) throws Exception {

        transitions.withExternal()
                .source(Stages.Stage1)
                .target(Stages.Stage2)
                .event(StageEvents.APPROVE)
                .guard(stateContext -> transitionService.checkContains(stateContext, Stage1.class))
                .action(context -> {
                    RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                    MultipartHttpServletRequest req = (MultipartHttpServletRequest) context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                    try {
                        Stage1 stage1 = requestApproval.getStage1();
                        stage1.setAmountInEuros(Double.parseDouble(req.getParameter("amountInEuros")));
                        stage1.setFinalAmount(stage1.getAmountInEuros());
                        stage1.setSubject(req.getParameter("subject"));
                        stage1.setSupplier(Optional.ofNullable(req.getParameter("supplier")).orElse(stage1.getSupplier()));
                        stage1.setSupplierSelectionMethod(Optional.ofNullable(Stage1.SupplierSelectionMethod.fromValue(req.getParameter("supplierSelectionMethod"))).orElse(stage1.getSupplierSelectionMethod()));
                        transitionService.approveApproval(context,"1","2",stage1);
                    } catch (Exception e) {
                        logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
                        context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                        throw new ServiceException(e.getMessage());
                    }
                })
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
                .target(Stages.Stage2)
                .event(StageEvents.EDIT)
                .guard(stateContext -> transitionService.checkContains(stateContext, Stage1.class))
                .action(context -> {
                    RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                    try {
                        transitionService.editApproval(context, requestApproval.getStage1(), "1");
                    } catch (Exception e) {
                        logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
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
                    RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                    MultipartHttpServletRequest req = (MultipartHttpServletRequest) context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                    try {
                        Stage2 stage2 = new Stage2(Optional.ofNullable(Boolean.parseBoolean(req.getParameter("checkFeasibility"))).orElse(false),Optional.ofNullable(Boolean.parseBoolean(req.getParameter("checkNecessity"))).orElse(false),true);
                        stage2.setDate(new Date().toInstant().toEpochMilli());
                        transitionService.downgradeApproval(context,"2","1",stage2);
                    } catch (Exception e) {
                        logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
                        context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                        throw new ServiceException(e.getMessage());
                    }
                })
                .and()
                .withExternal()
                    .source(Stages.Stage2)
                    .target(Stages.Stage3)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage2.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage2 stage2 = new Stage2(true,true,true);
                            stage2.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approveApproval(context,"2","3",stage2);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage2 stage2 = new Stage2(true,true,true);
                            stage2.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.rejectApproval(context, stage2,"2");
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of request approval " + requestApproval.getId());
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        Stage3 stage3 = Optional.ofNullable(requestApproval.getStage3()).orElse(new Stage3());
                        stage3.setDate(new Date().toInstant().toEpochMilli());
                        transitionService.downgradeApproval(context,"3","2",stage3);
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.Stage4)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage3.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);

                            Stage3 stage3 = new Stage3(true,true,false,"",true);
                            stage3.setDate(new Date().toInstant().toEpochMilli());
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
                            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
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
                    .target(Stages.Stage3)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage2.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            transitionService.editApproval(context, requestApproval.getStage2(), "2");
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage3 stage3 = Optional.ofNullable(requestApproval.getStage3()).orElse(new Stage3());
                            stage3.setApproved(false);
                            transitionService.rejectApproval(context, stage3,"3");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .and()
                    .withExternal()
                    .source(Stages.Stage4)
                    .target(Stages.Stage4)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage3.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            transitionService.editApproval(context, requestApproval.getStage3(), "3");
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage4 stage4 = Optional.ofNullable(requestApproval.getStage4()).orElse(new Stage4());
                            stage4.setApproved(false);
                            transitionService.downgradeApproval(context,"4","3",stage4);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }

                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage4)
                    .target(Stages.Stage5)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage4.class))
                    .action(context -> {
                        try {
                            Stage4 stage4 = new Stage4(true,true,true);
                            stage4.setDate(new Date().toInstant().toEpochMilli());
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
                    .and()
                    .withExternal()
                    .source(Stages.Stage5)
                    .target(Stages.Stage5)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage4.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            transitionService.editApproval(context, requestApproval.getStage4(), "4");
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage4 stage4 = Optional.ofNullable(requestApproval.getStage4()).orElse(new Stage4());
                            stage4.setApproved(false);
                            transitionService.rejectApproval(context, stage4,"4");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage5a stage5a = Optional.ofNullable(requestApproval.getStage5a()).orElse(new Stage5a());
                            stage5a.setApproved(false);
                            transitionService.downgradeApproval(context,"5a","4",stage5a);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage5a stage5a = Optional.ofNullable(requestApproval.getStage5a()).orElse(new Stage5a());
                            stage5a.setApproved(false);
                            transitionService.rejectApproval(context, stage5a,"5a");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
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
                        if(!transitionService.checkContains(context,Stage5a.class))
                            return false;

                        try {
                            RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                            Request request = requestService.get(requestApproval.getRequestId());
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
                            Stage5a stage5a = new Stage5a(true);
                            stage5a.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approveApproval(context,"5a","5b",stage5a);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.Stage6, context -> {
                        try {
                            Stage5a stage5a = new Stage5a(true);
                            stage5a.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approveApproval(context,"5a","6",stage5a);
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
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage5b.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                        try {
                            Stage1 stage1 = requestApproval.getStage1();
                            stage1.setAmountInEuros(Double.parseDouble(req.getParameter("amountInEuros")));
                            stage1.setFinalAmount(stage1.getAmountInEuros());
                            stage1.setSupplier(Optional.ofNullable(req.getParameter("supplier")).orElse(stage1.getSupplier()));
                            requestApproval.setStage1(stage1);
                            requestApprovalService.update(requestApproval, requestApproval.getId());

                            Stage5b stage5b = new Stage5b(true);
                            stage5b.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approveApproval(context,"5b","6",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage5b)
                    .target(Stages.Stage5b)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage5a.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                        try {
                            transitionService.editApproval(context, requestApproval.getStage5a(), "5a");
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage5b stage5b = Optional.ofNullable(requestApproval.getStage5b()).orElse(new Stage5b());
                            stage5b.setApproved(false);
                            transitionService.rejectApproval(context, stage5b,"5b");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
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
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage5b stage5b = Optional.ofNullable(requestApproval.getStage5b()).orElse(new Stage5b());
                            stage5b.setApproved(false);
                            transitionService.downgradeApproval(context,"5b","5a",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withChoice()
                    .source(Stages.Stage6ChoiceDowngrade)
                    .first(Stages.Stage5b, stateContext -> {

                        try {
                            RequestApproval requestApproval = stateContext.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                            if(requestApproval.getStage5b()!=null)
                                return true;

                            return false;
                        } catch (Exception e) {
                            logger.error("Failed to downgradeApproval 6->5b",e);
                            return false;
                        }
                    }, context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage6 stage6 = Optional.ofNullable(requestApproval.getStage6()).orElse(new Stage6());
                            transitionService.downgradeApproval(context,"6","5b",stage6);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.Stage5, context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage6 stage6 = Optional.ofNullable(requestApproval.getStage6()).orElse(new Stage6());
                            transitionService.downgradeApproval(context,"6","5a",stage6);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage6)
                    .target(Stages.Stage6)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> {
                        RequestApproval requestApproval = stateContext.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        if(requestApproval.getStage5b()!=null)
                            return transitionService.checkContains(stateContext, Stage5b.class);
                        else{
                            return transitionService.checkContains(stateContext, Stage5a.class);
                        }
                    })
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            if(requestApproval.getStage5b()!=null) {
                                HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                                Stage1 stage1 = requestApproval.getStage1();
                                stage1.setAmountInEuros(Double.parseDouble(req.getParameter("amountInEuros")));
                                stage1.setFinalAmount(stage1.getAmountInEuros());
                                stage1.setSupplier(Optional.ofNullable(req.getParameter("supplier")).orElse(stage1.getSupplier()));
                                requestApproval.setStage1(stage1);
                                requestApprovalService.update(requestApproval, requestApproval.getId());

                                transitionService.editApproval(context, requestApproval.getStage5b(), "5b");
                            }else
                                transitionService.editApproval(context, requestApproval.getStage5a(),"5a");
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage6)
                    .target(Stages.Stage6ChoiceDowngrade)
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
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        Request request = requestService.get(requestApproval.getRequestId());
                        try {
                            Stage6 stage6 = Optional.ofNullable(requestApproval.getStage6()).orElse(new Stage6());
                            stage6.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.modifyRequestApproval(context, stage6, "6", BaseInfo.Status.ACCEPTED);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage7 stage7 = Optional.ofNullable(payment.getStage7()).orElse(new Stage7());
                            stage7.setApproved(false);
                            stage7.setDate(new Date().toInstant().toEpochMilli());
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
                    .target(Stages.Stage7)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage6.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            transitionService.editApproval(context, requestApproval.getStage6(), "6");
                            transitionService.editApproval(context, requestApproval.getStage1(), "1");
                        } catch (Exception e) {
                            logger.error("Error occurred on editing of approval " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                .and()
                .withExternal()
                    .source(Stages.Stage7)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
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
                    .target(Stages.FINISHED)
                    .event(StageEvents.FINALIZE)
                    .action(stateContext -> {
//                        Request request = stateContext.getMessage().getHeaders().get("requestObj", Request.class);
//
//                        List<String> sendFinal = new ArrayList<>();
//                        sendFinal.add(request.getUser().getEmail());
//                        if(request.getOnBehalfOf()!=null)
//                            sendFinal.add(request.getOnBehalfOf().getEmail());
//
//
//
//
//
//                        mailService.sendMail("Finalized",sendFinal);
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7)
                    .target(Stages.Stage7aOr8)
                    .event(StageEvents.APPROVE)
                .and()
                    .withChoice()
                    .source(Stages.Stage7aOr8)
                    .first(Stages.Stage7a, stateContext -> {
                        RequestPayment requestPayment = stateContext.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(requestPayment.getRequestId());
                            return requestApproval.getStage3().getLoan();
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestPayment.getId(),e);
                            stateContext.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    }, context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                            RequestApproval requestApproval = requestApprovalService.getApproval(requestPayment.getRequestId());
                            Stage1 stage1 = requestApproval.getStage1();
                            String finalAmount = Optional.ofNullable(req.getParameter("finalAmount")).orElse("");
                            if(!finalAmount.isEmpty())
                                stage1.setFinalAmount(Double.parseDouble(finalAmount));

                            requestApproval.setStage1(stage1);
                            requestApprovalService.update(requestApproval, requestApproval.getId());

                            Stage7 stage7 = Optional.ofNullable(requestPayment.getStage7()).orElse(new Stage7());
                            stage7.setApproved(true);
                            stage7.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"7","7a",stage7);
                        } catch (Exception e) {
                            logger.error("Error occurred on upgrading payment of request " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.Stage8, context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                            RequestApproval requestApproval = requestApprovalService.getApproval(requestPayment.getRequestId());
                            Stage1 stage1 = requestApproval.getStage1();
                            String finalAmount = Optional.ofNullable(req.getParameter("finalAmount")).orElse("");
                            if(!finalAmount.isEmpty())
                                stage1.setFinalAmount(Double.parseDouble(finalAmount));
                            requestApproval.setStage1(stage1);
                            requestApprovalService.update(requestApproval, requestApproval.getId());

                            Stage7 stage7 = Optional.ofNullable(requestPayment.getStage7()).orElse(new Stage7());
                            stage7.setApproved(true);
                            stage7.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"7","8",stage7);
                        } catch (Exception e) {
                            logger.error("Error occurred on upgrading payment of request " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7a)
                    .target(Stages.Stage8)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage7a.class))
                    .action(context -> {
                        try {
                            Stage7a stage7a = new Stage7a(true);
                            stage7a.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"7a","8",stage7a);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7a)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage7a stage7a = Optional.ofNullable(payment.getStage7a()).orElse(new Stage7a());
                            stage7a.setApproved(false);
                            transitionService.rejectPayment(context, stage7a,"7a");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage7a)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            transitionService.cancelRequestPayment(context, "7a");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage7a)
                    .target(Stages.Stage7a)
                    .event(StageEvents.EDIT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
                            RequestApproval requestApproval = requestApprovalService.getApproval(payment.getRequestId());
                            Stage1 stage1 = requestApproval.getStage1();
                            String finalAmount = Optional.ofNullable(req.getParameter("finalAmount")).orElse("");
                            if(!finalAmount.isEmpty())
                                stage1.setFinalAmount(Double.parseDouble(finalAmount));
                            requestApproval.setStage1(stage1);
                            requestApprovalService.update(requestApproval, requestApproval.getId());


                            transitionService.editPayment(context, payment.getStage7(),"7");
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7a)
                    .target(Stages.Stage7)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage7a stage7a = Optional.ofNullable(requestPayment.getStage7a()).orElse(new Stage7a());
                            stage7a.setApproved(false);
                            stage7a.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.downgradePayment(context,"7a","7",stage7a);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading payment " + requestPayment.getId(),e);
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
                    .target(Stages.Stage8)
                    .event(StageEvents.EDIT)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);

                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(requestPayment.getRequestId());
                            if(requestApproval.getStage3().getLoan())
                                transitionService.editPayment(context, requestPayment.getStage7a(), "7a");
                            else
                                transitionService.editPayment(context, requestPayment.getStage7(), "7");
                        } catch (Exception e) {
                            logger.error("Error occurred on editing of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage8)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestPayment(context,"8");
                        } catch (Exception e) {
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withChoice()
                    .source(Stages.Stage8ChoiceDowngrade)
                    .first(Stages.Stage7a, stateContext -> {
                        RequestPayment requestPayment = stateContext.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(requestPayment.getRequestId());
                            return requestApproval.getStage3().getLoan();
                        }catch (Exception e) {
                            logger.error("Error occurred on downgradePayment of payment " + requestPayment.getId(),e);
                            stateContext.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    },context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage8 stage8 = Optional.ofNullable(requestPayment.getStage8()).orElse(new Stage8());
                            stage8.setApproved(false);
                            transitionService.downgradePayment(context,"8","7a",stage8);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradePayment of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    }).last(Stages.Stage7, context -> {
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
                    .and()
                .withExternal()
                    .source(Stages.Stage8)
                    .target(Stages.Stage8ChoiceDowngrade)
                    .event(StageEvents.DOWNGRADE)
                    .and()
                .withExternal()
                    .source(Stages.Stage8)
                    .target(Stages.Stage9)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage8.class))
                    .action(context -> {
                        try {
                            Stage8 stage8 = new Stage8(true, true, true);
                            stage8.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"8","9",stage8);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage9)
                    .target(Stages.Stage9)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage8.class))
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            transitionService.editPayment(context, requestPayment.getStage8(), "8");
                        } catch (Exception e) {
                            logger.error("Error occurred on editing of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage9)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage9 stage9 = Optional.ofNullable(payment.getStage9()).orElse(new Stage9());
                            stage9.setApproved(false);
                            transitionService.rejectPayment(context, stage9,"9");
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage9)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestPayment(context,"9");
                        } catch (Exception e) {
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                .and()
                .withExternal()
                    .source(Stages.Stage9)
                    .target(Stages.Stage8)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage9 stage9 = Optional.ofNullable(requestPayment.getStage9()).orElse(new Stage9());
                            stage9.setApproved(false);
                            transitionService.downgradePayment(context,"9","8",stage9);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage9)
                    .target(Stages.Stage10)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage9.class))
                    .action(context -> {
                        try {
                            Stage9 stage9 = new Stage9(true, true, true);
                            stage9.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"9","10",stage9);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage10)
                    .target(Stages.Stage10)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage9.class))
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            transitionService.editPayment(context, requestPayment.getStage9(), "9");
                        } catch (Exception e) {
                            logger.error("Error occurred on editing of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                  .and()
                .withExternal()
                    .source(Stages.Stage10)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestPayment(context,"10");
                        } catch (Exception e) {
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                .and()
                .withExternal()
                    .source(Stages.Stage10)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage10 stage10 = Optional.ofNullable(payment.getStage10()).orElse(new Stage10());
                            stage10.setApproved(false);
                            transitionService.rejectPayment(context, stage10,"10");
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage10)
                    .target(Stages.Stage9)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage10 stage10 = Optional.ofNullable(requestPayment.getStage10()).orElse(new Stage10());
                            stage10.setApproved(false);
                            transitionService.downgradePayment(context,"10","9",stage10);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage10)
                    .target(Stages.Stage11)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage10.class))
                    .action(context -> {
                        try {
                            Stage10 stage10 =new Stage10(true);
                            stage10.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"10","11", stage10);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage11)
                    .target(Stages.Stage11)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage10.class))
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            transitionService.editPayment(context, requestPayment.getStage10(), "10");
                        } catch (Exception e) {
                            logger.error("Error occurred on editing of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                 .and()
                .withExternal()
                    .source(Stages.Stage11)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestPayment(context,"11");
                        } catch (Exception e) {
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                .and()
                .withExternal()
                    .source(Stages.Stage11)
                    .target(Stages.Stage10)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage11 stage11 = Optional.ofNullable(requestPayment.getStage11()).orElse(new Stage11());
                            transitionService.downgradePayment(context,"11","10",stage11);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage11)
                    .target(Stages.Stage12)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage11.class))
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage11 stage11 = Optional.ofNullable(requestPayment.getStage11()).orElse(new Stage11());
                            stage11.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"11","12",stage11);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage12)
                    .target(Stages.Stage11)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage12 stage12 = Optional.ofNullable(requestPayment.getStage12()).orElse(new Stage12());
                            stage12.setApproved(false);
                            transitionService.downgradePayment(context,"12","11",stage12);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage12)
                    .target(Stages.Stage12)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage11.class))
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            transitionService.editPayment(context, requestPayment.getStage11(), "11");
                        } catch (Exception e) {
                            logger.error("Error occurred on editing of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                 .and()
                .withExternal()
                    .source(Stages.Stage12)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestPayment(context,"12");
                        } catch (Exception e) {
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                .and()
                .withExternal()
                    .source(Stages.Stage12)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage12 stage12 = Optional.ofNullable(payment.getStage12()).orElse(new Stage12());
                            transitionService.rejectPayment(context, stage12,"12");
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage12)
                    .target(Stages.Stage13)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage12.class))
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage12 stage12 = Optional.ofNullable(requestPayment.getStage12()).orElse(new Stage12(true));
                            stage12.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"12","13",stage12);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage13)
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        try {
                            transitionService.cancelRequestPayment(context,"13");
                        } catch (Exception e) {
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                .and()
                .withExternal()
                    .source(Stages.Stage13)
                    .target(Stages.Stage12)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage13 stage13 = Optional.ofNullable(requestPayment.getStage13()).orElse(new Stage13());
                            stage13.setApproved(false);
                            transitionService.downgradePayment(context,"13","12",stage13);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                    .withExternal()
                    .source(Stages.Stage13)
                    .target(Stages.Stage13)
                    .event(StageEvents.EDIT)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage12.class))
                    .action(context -> {
                        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            transitionService.editPayment(context, requestPayment.getStage12(), "12");
                        } catch (Exception e) {
                            logger.error("Error occurred on editing of payment " + requestPayment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage13)
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage13 stage13 = Optional.ofNullable(payment.getStage13()).orElse(new Stage13());
                            transitionService.rejectPayment(context, stage13,"13");
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of payment " + payment.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage13)
                    .target(Stages.FINISHED)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage13.class))
                    .action(context -> {
                        RequestPayment payment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
                        try {
                            Stage13 stage13 = Optional.ofNullable(payment.getStage13()).orElse(new Stage13(true));
                            stage13.setDate(new Date().toInstant().toEpochMilli());
                            transitionService.approvePayment(context,"13","13",stage13);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request ",e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })

        ;
    }

}
