import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ConfigSetup {

    public static void copyConfigToWorkingDirectory() {
        try {
            Path sourcePath = Path.of("src/main/resources/config.json");
            Path targetPath = Path.of("config.json"); // Arbeitsverzeichnis

            if (!Files.exists(targetPath)) {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("config.json wurde ins Arbeitsverzeichnis kopiert.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Fehler beim Kopieren der config.json");
        }
    }
}
