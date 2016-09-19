package de.unijena.bioinf.ms.sirius;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 19.09.16.
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusCore {

    private static final Properties USER_PROPERTIES = new Properties();
    public final static Path WORKSPACE;

    //creating
    static {
        final Path DEFAULT_WORKSPACE = Paths.get(System.getProperty("user.home")).resolve(".sirius");
        final Map<String, String> env = System.getenv();
        String ws =  env.get("SIRIUS_WORKSPACE");

        if (ws != null){
             Path wsDir = Paths.get(ws);
            if (Files.isDirectory(wsDir)){
                WORKSPACE = wsDir;
            }else if (Files.notExists(wsDir)){
                try {
                    Files.createDirectories(wsDir);
                } catch (IOException e) {
                    System.err.println("Could not create Workspace set in environment variable! Falling back to default Workspace - " + DEFAULT_WORKSPACE.toString());
                    //todo use logging
                    e.printStackTrace();
                    wsDir = DEFAULT_WORKSPACE;
                }finally {
                    WORKSPACE = wsDir;
                }
            }else{
                System.err.println(wsDir.toString() + " is not a directory! Falling back to default Workspace - " + DEFAULT_WORKSPACE.toString());//todo use logging (warning)
                WORKSPACE = DEFAULT_WORKSPACE;
            }
        }else{
            WORKSPACE = DEFAULT_WORKSPACE;
        }
        if (Files.notExists(WORKSPACE)){
            try {
                Files.createDirectories(WORKSPACE);
            } catch (IOException e) {
                System.err.println("Could NOT create Workspace");//todo use logging (error)
                e.printStackTrace();
            }//todo Close application?
        }

        final Path USER_PROPERTIES_FILE = WORKSPACE.resolve("sirius.properties");
        if (Files.notExists(USER_PROPERTIES_FILE)) {
            try {
                final Path DEFAULT_PROPERTIES_FILE = Paths.get(SiriusCore.class.getClassLoader().getResource("sirius.properties").getFile());
                Files.copy(DEFAULT_PROPERTIES_FILE, USER_PROPERTIES_FILE);
            } catch (IOException e) {
                System.err.println("Could NOT create Properties file");//todo use logging (error)
                e.printStackTrace();
            }//todo Close application?
        }

        try {
            USER_PROPERTIES.load(Files.newInputStream(USER_PROPERTIES_FILE));
        } catch (IOException e) {
            System.err.println("Could NOT load Properties form user properties file");//todo use logging (error)
            e.printStackTrace();
        }

        USER_PROPERTIES.setProperty("de.unijena.bioinf.sirius.workspace",WORKSPACE.toAbsolutePath().toString());
        System.setProperties(USER_PROPERTIES);
    }



    public static void addDefaultPropteries(File properties) throws IOException {
        addDefaultPropteries(properties.toPath());
    }

    public static void addDefaultPropteries(Path properties) throws IOException {
        Properties p = new Properties();
        p.load(Files.newInputStream(properties));
        addDefaultPropteries(p);
    }

    public static void addDefaultPropteries(Properties properties) {
        System.setProperties(properties);
        System.setProperties(USER_PROPERTIES);
    }

    public static void addDefaultProptery(String propertyName, String propertyValue) {
        System.setProperty(propertyName,propertyValue);
        System.setProperties(USER_PROPERTIES);
    }


}
