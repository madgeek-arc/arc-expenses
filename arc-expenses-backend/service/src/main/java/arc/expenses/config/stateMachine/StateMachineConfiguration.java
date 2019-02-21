package arc.expenses.config.stateMachine;

import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import arc.expenses.service.*;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfiguration extends EnumStateMachineConfigurerAdapter<Stages, StageEvents> {

    private static Logger logger = LogManager.getLogger(StateMachineConfiguration.class);

    @Autowired
    private RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    AclService aclService;

    @Autowired
    ProjectServiceImpl projectService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    private InstituteServiceImpl instituteService;

    @Autowired
    private OrganizationServiceImpl organizationService;

    @Autowired
    private MailService mailService;


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
                .guard(stateContext -> checkContains(stateContext.getMessage(), Stage2.class))
                .and()
                .withExternal()
                .source(Stages.Stage1)
                .target(Stages.CANCELLED)
                .event(StageEvents.CANCEL)
                .action(stateContext -> {
                    try {
                        cancelRequest(stateContext,"1");
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
                    MultipartHttpServletRequest req = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);
                    String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");
                    if(comment.isEmpty()) {
                        context.getStateMachine().setStateMachineError(new ServiceException("We need a comment!"));
                        throw new ServiceException("We need a comment!");
                    }
                    try {
                        modifyRequest(context, request.getStage1(),"1", BaseInfo.Status.UNDER_REVIEW);
                        aclService.updateAclEntries(
                                Collections.singletonList(new PrincipalSid(projectService.get(request.getProjectId()).getScientificCoordinator().getEmail())),
                                Collections.singletonList(new PrincipalSid(request.getUser().getEmail())),
                                request.getId());
                        mailService.sendMail("Downgrade", Collections.singletonList(request.getUser().getEmail()));
                    } catch (Exception e) {
                        logger.error("Error occurred on approval of request " + request.getId(),e);
                        throw new ServiceException(e.getMessage());
                    }
                })
                .and()
                .withExternal()
                    .source(Stages.Stage2)
                    .target(Stages.Stage3)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> checkContains(stateContext.getMessage(), Stage2.class))
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            Stage2 stage2 = new Stage2(true,true,true);
                            stage2.setDate(LocalDate.now().toEpochDay()+"");
                            modifyRequest(context, stage2, "3", BaseInfo.Status.PENDING);

                            List<String> pois = request.getPois();

                            Project project = projectService.get(request.getProjectId());
                            List<Sid> revokeAccess = new ArrayList<>();
                            revokeAccess.add(new PrincipalSid(project.getScientificCoordinator().getEmail()));
                            project.getScientificCoordinator().getDelegates().forEach(person -> revokeAccess.add(new PrincipalSid(person.getEmail())));
                            List<Sid> whoCanAccess = new ArrayList<>();
                            project.getOperator().forEach(entry -> {
                                whoCanAccess.add(new PrincipalSid(entry.getEmail()));
                                if(!pois.contains(entry.getEmail()))
                                    pois.add(entry.getEmail());

                                entry.getDelegates().forEach( person -> {
                                    whoCanAccess.add(new PrincipalSid(person.getEmail()));
                                    if(!pois.contains(person.getEmail()))
                                        pois.add(person.getEmail());
                                });
                            });

                            request.setPois(pois);
                            requestService.update(request,request.getId());
                            aclService.updateAclEntries(
                                    revokeAccess,
                                    whoCanAccess,
                                    request.getId());

                            mailService.sendMail("Approved",whoCanAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));
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
                            cancelRequest(stateContext,"2");
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
                            modifyRequest(context, stage2,"2", BaseInfo.Status.REJECTED);
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
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        String comment = Optional.ofNullable(context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class).getHeader("comment")).orElse("");
                        if(comment.isEmpty())
                            throw new ServiceException("We need a comment!");
                        try {
                            Stage2 stage2 = new Stage2(true,true,false);
                            stage2.setDate(LocalDate.now().toEpochDay()+"");
                            modifyRequest(context, stage2,"2", BaseInfo.Status.UNDER_REVIEW);

                            PersonOfInterest scientificCoordinator = projectService.get(request.getProjectId()).getScientificCoordinator();
                            List<Sid> revokeAccess = new ArrayList<>();
                            revokeAccess.add(new PrincipalSid(scientificCoordinator.getEmail()));
                            scientificCoordinator.getDelegates().forEach(person -> revokeAccess.add(new PrincipalSid(person.getEmail())));
                            aclService.updateAclEntries(
                                    revokeAccess,
                                    Collections.singletonList(new PrincipalSid(request.getUser().getEmail())),
                                    request.getId());
                            mailService.sendMail("Approved",Collections.singletonList(request.getUser().getEmail()));
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withExternal()
                    .source(Stages.Stage3)
                    .target(Stages.Stage4)
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> checkContains(stateContext.getMessage(), Stage3.class))
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
                            modifyRequest(context,stage3, "4", BaseInfo.Status.PENDING);
                            Project project = projectService.get(request.getProjectId());
                            Institute institute = instituteService.get(project.getInstituteId());
                            Organization organization = organizationService.get(institute.getOrganizationId());

                            List<String> pois = request.getPois();

                            List<Sid> revokeAccess = new ArrayList<>();
                            project.getOperator().forEach(entry -> {
                                revokeAccess.add(new PrincipalSid(entry.getEmail()));
                                entry.getDelegates().forEach(person -> {
                                    revokeAccess.add(new PrincipalSid(person.getEmail()));
                                });
                            });

                            List<Sid> whoCanAccess = new ArrayList<>();
                            whoCanAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                            if(!pois.contains(organization.getPoy().getEmail()))
                                pois.add(organization.getPoy().getEmail());

                            organization.getPoy().getDelegates().forEach(delegate -> {
                                whoCanAccess.add(new PrincipalSid(delegate.getEmail()));
                                if(!pois.contains(delegate.getEmail()));
                                    pois.add(delegate.getEmail());
                            });

                            request.setPois(pois);
                            requestService.update(request,request.getId());

                            aclService.updateAclEntries(
                                    revokeAccess,
                                    whoCanAccess,
                                    request.getId());

                            mailService.sendMail("Approve", whoCanAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));
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
                            cancelRequest(stateContext,"3");
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
                            modifyRequest(context, stage3,"3", BaseInfo.Status.REJECTED);
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
                        String comment = Optional.ofNullable(context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class).getParameter("comment")).orElse("");
                        if(comment.isEmpty())
                            throw new ServiceException("We need a comment!");
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage3 stage3 = (requestApproval.getStage3() == null ? new Stage3() : requestApproval.getStage3());
                            stage3.setApproved(false);
                            modifyRequest(context, stage3,"3", BaseInfo.Status.UNDER_REVIEW);

                            Project project = projectService.get(request.getProjectId());
                            Institute institute = instituteService.get(project.getInstituteId());
                            Organization organization = organizationService.get(institute.getOrganizationId());

                            List<Sid> revokeAccess = new ArrayList<>();
                            revokeAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                            organization.getPoy().getDelegates().forEach(delegate -> {
                                revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                            });


                            List<Sid> grantAccess = new ArrayList<>();
                            project.getOperator().forEach(entry -> {
                                grantAccess.add(new PrincipalSid(entry.getEmail()));
                                entry.getDelegates().forEach(person -> {
                                    grantAccess.add(new PrincipalSid(person.getEmail()));
                                });
                            });

                            aclService.updateAclEntries(
                                    revokeAccess,
                                    grantAccess,
                                    request.getId());
                            mailService.sendMail("Downgrade", grantAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));
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
                    .guard(stateContext -> checkContains(stateContext.getMessage(), Stage4.class))
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            Stage4 stage4 = new Stage4(true,true,true);
                            modifyRequest(context,stage4, "5a", BaseInfo.Status.PENDING);

                            Project project = projectService.get(request.getProjectId());
                            Institute institute = instituteService.get(project.getInstituteId());
                            Organization organization = organizationService.get(institute.getOrganizationId());

                            List<String> pois = request.getPois();

                            List<Sid> revokeAccess = new ArrayList<>();
                            revokeAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                            organization.getPoy().getDelegates().forEach(delegate -> {
                                revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                            });

                            List<Sid> grantAccess = new ArrayList<>();
                            grantAccess.add(new PrincipalSid(request.getDiataktis().getEmail()));
                            if(!pois.contains(request.getDiataktis().getEmail()))
                                pois.add(request.getDiataktis().getEmail());
                            request.getDiataktis().getDelegates().forEach( delegate -> {
                                grantAccess.add(new PrincipalSid(delegate.getEmail()));
                                if(!pois.contains(delegate.getEmail()))
                                    pois.add(delegate.getEmail());
                            });
                            aclService.updateAclEntries(
                                    revokeAccess,
                                    grantAccess,
                                    request.getId());
                            request.setPois(pois);
                            requestService.update(request,request.getId());
                            mailService.sendMail("Approve", grantAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
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
                            cancelRequest(stateContext,"4");
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
                            modifyRequest(context, stage4,"4", BaseInfo.Status.REJECTED);
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
                        String comment = Optional.ofNullable(context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class).getParameter("comment")).orElse("");
                        if(comment.isEmpty())
                            throw new ServiceException("We need a comment!");
                        try {
                            RequestApproval requestApproval = requestApprovalService.getApproval(request.getId());
                            Stage4 stage4 = (requestApproval.getStage4() == null ? new Stage4() : requestApproval.getStage4());
                            stage4.setApproved(false);
                            modifyRequest(context, stage4,"4", BaseInfo.Status.UNDER_REVIEW);

                            Project project = projectService.get(request.getProjectId());
                            Institute institute = instituteService.get(project.getInstituteId());
                            Organization organization = organizationService.get(institute.getOrganizationId());

                            List<Sid> revokeAccess = new ArrayList<>();
                            revokeAccess.add(new PrincipalSid(request.getDiataktis().getEmail()));
                            request.getDiataktis().getDelegates().forEach( delegate -> {
                                revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                            });

                            List<Sid> grantAccess = new ArrayList<>();
                            grantAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                            organization.getPoy().getDelegates().forEach(delegate -> {
                                grantAccess.add(new PrincipalSid(delegate.getEmail()));
                            });

                            aclService.updateAclEntries(
                                    revokeAccess,
                                    grantAccess,
                                    request.getId());
                            mailService.sendMail("Downgrade", grantAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));
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
                            modifyRequest(context, stage5a,"5a", BaseInfo.Status.REJECTED);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId(),e);
                            throw new ServiceException(e.getMessage());
                        }
                    })
                    .and()
                .withChoice()
                    .source(Stages.Stage5a)
                    .first(Stages.Stage5b, stateContext -> {
                        logger.info("Guard of 5b");
                        if(!checkContains(stateContext.getMessage(),Stage5a.class))
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
                    }, context -> actionForStage5(context,"5b"))
                    .last(Stages.Stage6, context -> actionForStage5(context,"6"))
                    .and()
                .withExternal()
                    .source(Stages.Stage5)
                    .target(Stages.Stage5a)
                    .event(StageEvents.APPROVE);

    }


    private boolean checkContains(Message<StageEvents> message, Class stageClass){
        Request request = message.getHeaders().get("requestObj", Request.class);

        if(request == null) {
            return false;
        }

        HttpServletRequest req = message.getHeaders().get("restRequest", HttpServletRequest.class);
        if(req == null) {
            return false;
        }
        List<String> requiredFields = Arrays.stream(stageClass.getDeclaredFields()).filter(p -> p.isAnnotationPresent(NotNull.class)).flatMap(p -> Stream.of(p.getName())).collect(Collectors.toList());
        Map<String, String[]> parameters = req.getParameterMap();
        for(String field: requiredFields){
            if(!parameters.containsKey(field)) {
                return false;
            }
        }

        return true;
    }


    private void actionForStage5(StateContext<Stages, StageEvents> context ,String stage){
        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        logger.info("Got into action for stage "+ stage);
        try {
            Stage5a stage5a = new Stage5a(true);
            modifyRequest(context, stage5a, stage, BaseInfo.Status.PENDING);

            Project project = projectService.get(request.getProjectId());
            Institute institute = instituteService.get(project.getInstituteId());
            Organization organization = organizationService.get(institute.getOrganizationId());

            List<String> pois = request.getPois();

            List<Sid> revokeAccess = new ArrayList<>();
            revokeAccess.add(new PrincipalSid(request.getDiataktis().getEmail()));
            request.getDiataktis().getDelegates().forEach( delegate -> {
                revokeAccess.add(new PrincipalSid(delegate.getEmail()));
            });


            List<Sid> grantAccess = new ArrayList<>();
            if(stage.equals("5b")) {
                grantAccess.add(new PrincipalSid(organization.getDioikitikoSumvoulio().getEmail()));
                if (!pois.contains(organization.getDioikitikoSumvoulio().getEmail()))
                    pois.add(organization.getDioikitikoSumvoulio().getEmail());
                organization.getDioikitikoSumvoulio().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                    if (!pois.contains(delegate.getEmail()))
                        pois.add(delegate.getEmail());
                });
            }else if(stage.equals("6")){
                grantAccess.add(new PrincipalSid(institute.getDiaugeia().getEmail()));
                if (!pois.contains(institute.getDiaugeia().getEmail()))
                    pois.add(institute.getDiaugeia().getEmail());
                institute.getDiaugeia().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                    if (!pois.contains(delegate.getEmail()))
                        pois.add(delegate.getEmail());
                });
            }

            aclService.updateAclEntries(
                    revokeAccess,
                    grantAccess,
                    request.getId());

            request.setPois(pois);
            requestService.update(request,request.getId());

            mailService.sendMail("Approve", grantAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("Error occurred on approval of request " + request.getId(),e);
            throw new ServiceException(e.getMessage());
        }
    }


    private void modifyRequest(
            StateContext<Stages, StageEvents> context,
            Stage stage,
            String stageString,
            BaseInfo.Status status) throws Exception {

        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        MultipartHttpServletRequest multiPartRequest = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);

        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");

        ArrayList<Attachment> attachments = new ArrayList<>();
        for(MultipartFile file : multiPartRequest.getFiles("files")){
            storeRESTClient.storeFile(file.getBytes(), request.getArchiveId(), file.getOriginalFilename());
            attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage1"));
        }

        RequestApproval requestApproval = requestApprovalService.getByField("request_id",request.getId());
        stage.setAttachments(attachments);
        stage.setComment(comment);
        try {
            User user = userService.getByField("user_email",(String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            stage.setUser(user);
        } catch (Exception e) {
            throw new ServiceException("User not found");
        }

        if(stage instanceof Stage1)
            request.setStage1((Stage1) stage);
        else if(stage instanceof Stage2)
            requestApproval.setStage2((Stage2) stage);
        else if(stage instanceof Stage3)
            requestApproval.setStage3((Stage3) stage);
        else if(stage instanceof Stage4)
            requestApproval.setStage4((Stage4) stage);
        else if(stage instanceof Stage5a)
            requestApproval.setStage5a((Stage5a) stage);



        requestApproval.setStage(stageString);
        requestApproval.setStatus(status);
        requestApprovalService.update(requestApproval,requestApproval.getId());

        if(status == BaseInfo.Status.REJECTED){
            aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
            mailService.sendMail("Rejected", request.getPois());
        }
    }

    private void cancelRequest(
            StateContext<Stages, StageEvents> context,
            String stage) throws Exception {

        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        request.setRequestStatus(Request.RequestStatus.CANCELLED);

        RequestApproval requestApproval = requestApprovalService.getByField("request_id", request.getId());
        requestApproval.setStage(stage+"");
        requestApproval.setStatus(BaseInfo.Status.CANCELLED);
        requestApprovalService.update(requestApproval,requestApproval.getId());

        mailService.sendMail("Canceled",request.getPois());

        requestService.update(request, request.getId());
        aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
    }


}
