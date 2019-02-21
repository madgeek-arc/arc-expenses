package arc.expenses.service;

import com.bazaarvoice.jolt.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

@Component
public class MigrationProcess {

    @Autowired
    CustomCardinalityTransform customCardinalityTransform;
    Properties properties = new Properties();
    String[] folders = {"approval","payment","request"};
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
        Object transformed = customCardinalityTransform.transform(path.toString());
        writeObjectToFile(path,transformed);
    }

    public void writeObjectToFile(Path path,Object transformed) {
        try (FileWriter file = new FileWriter(properties.getProperty("output_path") + "/" + currentFolder + "/" + path.getFileName())) {
            file.write(JsonUtils.toPrettyJsonString(transformed));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
