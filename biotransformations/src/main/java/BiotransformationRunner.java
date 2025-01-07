import java.io.IOException;
import java.util.List;

public class BiotransformationRunner {
    public static void main(String[] args) {
        try {
            // Erstellen einer Instanz des BiotransformerWrapper
            BiotransformerWrapper biotransformer = new BiotransformerWrapper();

            // Beispiel-SMILES-String eines Moleküls (z. B. Ethanol)
            String inputMolecule = "CCO"; // Ethanol im SMILES-Format

            // Das gewünschte Biosystem (z. B. HUMAN, GUTMICRO usw.)
            String biosystem = "HUMAN";

            // Liste der zu verwendenden Enzyme
            List<String> enzymes = List.of("CYP2D6", "CYP3A4");

            // Debug-Ausgabe der Eingabeparameter
            System.out.println("Eingegebenes Molekül: " + inputMolecule);
            System.out.println("Biosystem: " + biosystem);
            System.out.println("Enzyme: " + enzymes);


            // Biotransformationen durchführen
            List<String> results = biotransformer.calculateTransformations(inputMolecule, biosystem, enzymes);

            // Ergebnisse anzeigen
            if (results != null && !results.isEmpty()) {
                System.out.println("Biotransformationsprodukte:");
                results.forEach(System.out::println);
            } else {
                System.out.println("Keine Biotransformationsprodukte gefunden oder ein Fehler ist aufgetreten.");
            }

        } catch (IOException e) {
            System.err.println("Fehler beim Initialisieren des Biotransformers: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Ein unerwarteter Fehler ist aufgetreten: " + e.getMessage());
            e.printStackTrace();
        }
    }
}