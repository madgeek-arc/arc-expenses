package arc.expenses.service;

import au.com.bytecode.opencsv.CSVReader;
import gr.athenarc.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@Service("bulkImport")
class BulkImportService {

    private org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(BulkImportService.class);


    @Value("${bulkImport.path}")
    private String path;

    @Value("${bulkImport.operation}")
    private String bulkImportOperation;

    @Autowired
    OrganizationServiceImpl organizationService;
    @Autowired
    InstituteServiceImpl instituteService;
    @Autowired
    ProjectServiceImpl projectService;


    @PostConstruct
    public void init() throws Exception {
        if(Boolean.parseBoolean(bulkImportOperation)){
            initializeOrganizations();
            initializeInstitutes();
            initializeProjects();
        }
    }

    private void initializeOrganizations() throws IOException {

        CSVReader reader = new CSVReader(new FileReader(path + "organization.csv"));
        String [] line;
        line = reader.readNext();
        while ((line = reader.readNext()) != null) {
            Organization organization = new Organization();
            organization.setId(line[0].trim());
            organization.setName(line[1].trim());

            organization.setPoy(parserPOY(line[2]));
            organization.getPoy().setDelegates(delegateParser(line[2]));

            organization.setDirector(parserPOY(line[3]));
            organization.getDirector().setDelegates(delegateParser(line[3]));

            organization.setDioikitikoSumvoulio(parserPOY(line[4]));
            organization.getDioikitikoSumvoulio().setDelegates(delegateParser(line[4]));

            organization.setInspectionTeam(parser(line[5]));


            organization.setViceDirector(parserPOY(line[6]));
            organization.getViceDirector().setDelegates(delegateParser(line[6]));

            organizationService.add(organization,null); //TODO(Check for authentication here)
        }
    }

    private List<Delegate> delegateParser(String s) {

        if(s.split("#").length <= 1)
            return null;

        String delegates[] = (s.split("#"))[1].split(",");
        List<Delegate> rs = new ArrayList<>();

        for(String delegate:delegates){
            Delegate d = new Delegate();
            String details[] = delegate.split(";");
            d.setEmail(details[1].trim());
            d.setFirstname((details[0].split(" "))[0].trim());
            d.setLastname((details[0].split(" "))[1].trim());
            rs.add(d);
        }
        return rs;
    }

    private PersonOfInterest parserPOY(String s) {
        PersonOfInterest poy = new PersonOfInterest();
        String details[] = s.split(";");
        String email = details[1].split("#")[0];
        poy.setEmail(email.trim());
        poy.setFirstname((details[0].split(" "))[0].trim());
        poy.setLastname((details[0].split(" "))[1].trim());
        return poy;

    }

    private void initializeInstitutes() throws Exception {

        CSVReader reader = new CSVReader(new FileReader(path+"institute.csv"));
        String [] line;
        line = reader.readNext();
        while ((line = reader.readNext()) != null) {
            Institute institute = new Institute();
            institute.setId(line[0].trim());
            institute.setName(line[1].trim());
            institute.setOrganization(organizationService.getByField("organization_name",line[2].trim()));

            institute.setDirector(parserPOY(line[3]));
            institute.getDirector().setDelegates(delegateParser(line[3]));

            institute.setAccountingRegistration(parser(line[4]).get(0));
            institute.getAccountingRegistration().setDelegates(delegateParser(line[4]));

            institute.setAccountingPayment(parser(line[5]).get(0));
            institute.getAccountingPayment().setDelegates(delegateParser(line[5]));

            institute.setAccountingDirector(parser(line[6]).get(0));
            institute.getAccountingDirector().setDelegates(delegateParser(line[6]));

            institute.setDiaugeia(parserPOY(line[7]));
            institute.getDiaugeia().setDelegates(delegateParser(line[7]));

            institute.setSuppliesOffice(parserPOY(line[8]));
            institute.getSuppliesOffice().setDelegates(delegateParser(line[8]));


            institute.setDiataktis(parserPOY(line[9]));
            institute.getDiataktis().setDelegates(delegateParser(line[9]));

            institute.setTravelManager(parserPOY(line[10]));
            institute.getTravelManager().setDelegates(delegateParser(line[10]));

            instituteService.add(institute,null); //TODO(Check for authentication here)
        }

    }

    private URLConnection connect(String filename){
        URL url = null;
        URLConnection urlConnection = null;
        try {
            url = new URL(path + filename);
            return url.openConnection();
        } catch (IOException e) {
            LOGGER.debug(e);
        }
        return null;
    }

    private void initializeProjects() throws IOException {

        CSVReader reader = new CSVReader(new FileReader(path + "project.csv"));
        String [] line;
        line = reader.readNext();
        while ((line = reader.readNext()) != null) {
            Project project = new Project();
            project.setId(line[0].trim());
            project.setName(line[1].trim());
            project.setAcronym(line[2].trim());
            project.setInstitute(instituteService.get(line[3].trim()));
            project.setParentProject(line[4].trim());

            project.setScientificCoordinator(parserPOY(line[5]));
            project.getScientificCoordinator().setDelegates(delegateParser(line[5]));

            project.setOperator(parser(line[6]));

            project.setStartDate(line[7].trim());
            project.setEndDate(line[8].trim());
            project.setTotalCost(line[9].trim());

            project.setScientificCoordinatorAsDiataktis(parseBoolean(line[10].trim()));
            projectService.add(project,null); //TODO(Check for authentication here)
        }
    }

    private Boolean parseBoolean(String s) {

        if(s.equals("ΟΧΙ"))
            return false;
        return true;

    }

    private List<PersonOfInterest> parser(String s) {

        String op[] = s.split(",");
        List<PersonOfInterest> operators = new ArrayList<>();

        for(String operator:op)
            operators.add(parserPOY(operator));
        return operators;
    }


}
