import biotransformer.biosystems.BioSystem;
import biotransformer.transformation.MetabolicReaction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BiotransformerTest {

    @BeforeAll
    public static void setUpConfigPath() {
       // Setze die JVM-Systemeigenschaft für die Konfiguration

        System.out.println("Config Path: " + System.getProperty("biotransformer.config.path"));
    }

    @Test
    public void testCalculateTransformations() {
        // Initialisiere den Wrapper
        BiotransformerWrapper wrapper = new BiotransformerWrapper(
                BioSystem.BioSystemName.HUMAN, true, false, Set.of("biotransformer")
        );

        // Testmolekül (Isopropylbenzol)
        String inputMolecule = "CC(C)C1=CC=CC=C1";

        // Gebe Informationen zur Konfiguration aus
        System.out.println("Starte Transformationen für Molekül: " + inputMolecule);
        System.out.println("Geladene Reaktionen: " + wrapper.getSupportedReactions().size());

        // Führt die Transformationen durch
        List<String> results = wrapper.calculateTransformations(inputMolecule);

        // Debug-Ausgaben für bessere Analyse
        System.out.println("Biotransformer Result Count: " + (results == null ? 0 : results.size()));
        if (results != null && results.isEmpty()) {
            System.err.println("Warnung: Keine Ergebnisse für Molekül. Mögliche Ursachen:");
            System.err.println("- Keine passenden Reaktionen für Molekül gefunden.");
            System.err.println("- Probleme in der Transformationsextraktion (extractSmiles).");
        }

        // Tests
        assertNotNull(results, "Die Ergebnisliste sollte nicht null sein.");
        assertFalse(results.isEmpty(), "Die Ergebnisliste sollte nicht leer sein.");

        // Gebe die Ergebnisse aus
        System.out.println("Ergebnisse: " + results);
    }
}
