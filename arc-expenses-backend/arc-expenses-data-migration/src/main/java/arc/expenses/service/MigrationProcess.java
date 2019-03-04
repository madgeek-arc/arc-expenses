package arc.expenses.service;

import arc.expenses.service.transformations.CustomChainrTransform;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

@Component
public class MigrationProcess {

    @Autowired
    CustomChainrTransform customChainrTransform;

    Properties properties = new Properties();
    String[] folders = {"approval","payment","request","user"};
    String currentFolder = null;

    @PostConstruct
    public void migrateData() throws IOException {
        properties.load(MigrationProcess.class.getClassLoader().getResourceAsStream("application.properties"));
        migrate(properties.getProperty("input_path"));
    }

    private void migrate(String filePath){
        for(String file : folders){
            currentFolder = file;
            try (Stream<Path> paths = Files.walk(Paths.get(filePath+"/"+file))) {
                paths
                        .filter(Files::isRegularFile)
                        .forEach(this::migrateFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void migrateFile(Path path) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new FileInputStream(path.toString());
            Object toTransform = mapper.readValue(inputStream,Object.class);
            Object transformed = applyTransformations(toTransform);
            writeObjectToFile(path,transformed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Object applyTransformations(Object toTransform) {
        return customChainrTransform.transform(toTransform,currentFolder);
    }

    public void writeObjectToFile(Path path,Object transformed) {
        try (FileWriter file = new FileWriter(properties.getProperty("output_path") + "/" + currentFolder + "/" + path.getFileName())) {
            file.write(JsonUtils.toPrettyJsonString(transformed));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
