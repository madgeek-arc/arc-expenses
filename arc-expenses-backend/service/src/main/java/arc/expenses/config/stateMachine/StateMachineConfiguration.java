package arc.expenses.config.stateMachine;

import arc.expenses.acl.ArcPermission;
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
import org.springframework.security.acls.domain.PrincipalSid;
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
import java.util.*;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfiguration extends EnumStateMachineConfigurerAdapter<Stages, StageEvents> {

    private static Logger logger = LogManager.getLogger(StateMachineConfiguration.class);

    @Autowired
    private TransitionService transitionService;

    @Autowired
    private RequestPaymentServiceImpl requestPaymentService;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private AclService aclService;

    @Autowired
    private MailService mailService;


    @Override
    public void configure(StateMachineStateConfigurer<Stages, StageEvents> states) throws Exception {
        states.withStates()
                .initial(Stages.Stage1)
                .choice(Stages.Stage5a)
                .choice(Stages.Stage6ChoiceDowngrade)
                .choice(Stages.FinalizeContracts)
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
                .guard(stateContext -> transitionService.checkContains(stateContext, Stage2.class))
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
                    RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                    try {
                        Stage1 stage1 = Optional.ofNullable(requestApproval.getStage1()).orElse(new Stage1());
                        transitionService.downgradeApproval(context,"2","1",stage1);
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
                        Stage2 stage2 = new Stage2(true,true,false);
                        stage2.setDate(new Date().toInstant().toEpochMilli());
                        transitionService.downgradeApproval(context,"3","2",stage2);
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
                    .source(Stages.Stage4)
                    .target(Stages.Stage3)
                    .event(StageEvents.DOWNGRADE)
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage3 stage3 = Optional.ofNullable(requestApproval.getStage3()).orElse(new Stage3());
                            stage3.setApproved(false);
                            transitionService.downgradeApproval(context,"4","3",stage3);
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
                            Stage4 stage4 = Optional.ofNullable(requestApproval.getStage4()).orElse(new Stage4());
                            stage4.setApproved(false);
                            transitionService.downgradeApproval(context,"5a","4",stage4);
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
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage5b.class))
                    .action(context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
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
                            Stage5b stage5b = Optional.ofNullable(requestApproval.getStage5b()).orElse(new Stage5b());
                            stage5b.setApproved(false);
                            transitionService.downgradeApproval(context,"6","5b",stage5b);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgrading approval of request " + requestApproval.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.Stage5, context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        try {
                            Stage5a stage5a = Optional.ofNullable(requestApproval.getStage5a()).orElse(new Stage5a());
                            stage5a.setApproved(false);
                            transitionService.downgradeApproval(context,"6","5a",stage5a);
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + requestApproval.getId(),e);
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
                .withChoice()
                    .source(Stages.FinalizeContracts)
                    .first(Stages.Stage7, context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        Request request = requestService.get(requestApproval.getRequestId());
                        if(!transitionService.checkContains(context, Stage6.class))
                            return false;

                        if(request.getType() != Request.Type.CONTRACT) {
                            return true;
                        }
                        return false;
                    }, context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        Request request = requestService.get(requestApproval.getRequestId());
                        try {
                            RequestPayment requestPayment = requestPaymentService.createPayment(request);
                            transitionService.updatingPermissions("6","7", request,"Approve",RequestApproval.class,requestApproval.getId());

                            aclService.removePermissionFromSid(Collections.singletonList(ArcPermission.CANCEL),new PrincipalSid(request.getUser().getEmail()),requestApproval.getId(),RequestApproval.class); //request can no longer get canceled
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .last(Stages.FINISHED, context -> {
                        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
                        Request request = requestService.get(requestApproval.getRequestId());
                        try {
                            transitionService.modifyRequestApproval(context, new Stage13(), "13", BaseInfo.Status.ACCEPTED);
                            aclService.removePermissionFromSid(Collections.singletonList(ArcPermission.CANCEL),new PrincipalSid(request.getUser().getEmail()),request.getId(),Request.class); //request can no longer get canceled
                        } catch (Exception e) {
                            logger.error("Error occurred on downgradeApproval of request " + request.getId(),e);
                            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage6)
                    .target(Stages.FinalizeContracts)
                    .event(StageEvents.APPROVE)
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
                        Request request = stateContext.getMessage().getHeaders().get("requestObj", Request.class);

                        List<String> sendFinal = new ArrayList<>();
                        sendFinal.add(request.getUser().getEmail());
                        if(request.getOnBehalfOf()!=null)
                            sendFinal.add(request.getOnBehalfOf().getEmail());

                        mailService.sendMail("Finalized",sendFinal);
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage7)
                    .target(Stages.Stage8)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> transitionService.checkContains(stateContext, Stage7.class))
                    .action(context -> {
                        try {

                            Stage7 stage7 = new Stage7(true);
                            stage7.setDate(new Date().toInstant().toEpochMilli());
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
                        try {
                            Stage11 stage11 = new Stage11();
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
                        try {
                            Stage12 stage12 = new Stage12(true);
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
                        try {
                            Stage13 stage13 = new Stage13(true);
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
