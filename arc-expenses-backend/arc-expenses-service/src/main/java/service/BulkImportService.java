package service;

import au.com.bytecode.opencsv.CSVReader;
import gr.athenarc.domain.Institute;
import gr.athenarc.domain.Organization;
import gr.athenarc.domain.POY;
import gr.athenarc.domain.Project;
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
public class BulkImportService {

    private org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(BulkImportService.class);

    private String path = "/home/panagiotis/Desktop/uploadFiles/";

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
            organization.setPOY(parserPOY(line[2]));
            organization.setDirector(parserPOY(line[3]));
            organization.setDioikitikoSumvoulio(parserPOY(line[4]));
            organizationService.add(organization);
        }
    }

    private POY parserPOY(String s) {
        POY poy = new POY();
        String details[] = s.split(";");
        poy.setEmail(details[1].trim());
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
            institute.setAccountingRegistration(parserPOY(line[4]));
            institute.setAccountingPayment(parserPOY(line[5]));
            institute.setAccountingDirector(parserPOY(line[6]));
            institute.setDiaugeia(parserPOY(line[7]));
            instituteService.add(institute);
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
            project.setOperator(parser(line[6]));
            project.setStartDate(line[7].trim());
            project.setEndDate(line[8].trim());
            projectService.add(project);
        }
    }

    private List<POY> parser(String s) {

        String op[] = s.split(",");
        List<POY> operators = new ArrayList<>();

        for(String operator:op)
            operators.add(parserPOY(operator));
        return operators;
    }


}
