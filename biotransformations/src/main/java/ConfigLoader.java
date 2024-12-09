import java.io.InputStream;

public class ConfigLoader {
    public static void main(String[] args) {
        InputStream configStream = ConfigLoader.class.getClassLoader().getResourceAsStream("config.json");
        if (configStream == null) {
            System.out.println("config.json nicht gefunden!");
        } else {
            System.out.println("config.json wurde erfolgreich geladen.");
        }
    }
}
