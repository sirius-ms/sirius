//package sirius.transformations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openscience.cdk.interfaces.IAtomContainerSet;
import reactantpredictor.ReactantPred;
import reactantpredictor.SdfToSample;
import weka.core.Instances;

/**
 * Wrapper-Klasse für BioTransformer mit Unterstützung für Biosystem-Erkennung.
 * Verwendet die Konfiguration aus einer config.json-Datei.
 */
public class BiotransformerWrapper implements TransformationMethod {

    private final ReactantPred reactantPredictor;
    private final JsonNode config;

    /**
     * Konstruktor: Initialisiert das ReactantPredictor-Objekt und lädt die Konfiguration.
     *
     * @throws IOException Falls die config.json-Datei nicht gelesen werden kann.
     */
    public BiotransformerWrapper() throws IOException {
        this.reactantPredictor = new ReactantPred();

        // Hinzufügen von ALLOW_COMMENTS, falls Kommentare verwendet wurden
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        // Laden der Konfiguration aus config.json
        this.config = objectMapper.readTree(new File("biotransformations/config.json")); // Pfad zur config.json ggf. anpassen
        System.out.println("Geladene Konfiguration: " + this.config.toPrettyString());
    }

    /**
     * Berechnet Biotransformationen basierend auf Biosystem und Enzymen.
     *
     * @param inputMolecule Die Struktur des Eingabemoleküls im SMILES-Format.
     * @param bioSystem      Das ausgewählte Biosystem (z. B. "HUMAN").
     * @param enzymes        Eine Liste der Enzyme (CYPs), die verwendet werden sollen.
     *                       Wenn `null` oder leer, werden alle Standard-Enzyme genutzt.
     * @return Eine Liste von Biotransformationsprodukten (Ergebnisse als Strings).
     */
    @Override
    public List<String> calculateTransformations(String inputMolecule, String bioSystem, List<String> enzymes) {
        try {

            // Debug: Ausgeben der Eingaben
            System.out.println("Transformation gestartet mit:");
            System.out.println("SMILES: " + inputMolecule);
            System.out.println("Biosystem: " + bioSystem);
            System.out.println("Enzyme: " + enzymes);

            if (enzymes == null || enzymes.isEmpty()) {
                enzymes = getAllEnzymesForBioSystem(bioSystem);
            }

            // Moleküle verarbeiten
            SdfToSample sdfTool = new SdfToSample();
            Instances testSet = generateInstancesFromSmiles(sdfTool,inputMolecule);

            // Debug: Überprüfen des erzeugten testSet
           // System.out.println("Erzeugtes Instances-Objekt: " + testSet);

            if (testSet == null || testSet.size() == 0) {
                return List.of("Fehler: Keine gültigen Moleküldaten generiert.");
            }

           /* // Überprüfen, ob das angegebene Biosystem in der Konfiguration existiert
            JsonNode bioSystemNode = config.path("database_locations").path("kb_biosystem_path").path(bioSystem.toUpperCase());
            if (bioSystemNode.isMissingNode()) {
                return List.of("Fehler: Ungültiges Biosystem: " + bioSystem);
            }

            // Extrahiere den Pfad des Biosystems
            String bioSystemPath = bioSystemNode.path("path").asText();
            if (bioSystemPath == null || bioSystemPath.isEmpty()) {
                return List.of("Fehler: Kein gültiger Pfad für Biosystem in der Konfiguration gefunden.");
            }
            System.out.println("Gefundener Pfad für Biosystem: " + bioSystemPath);*/


            ArrayList<HashMap<String, String>> predictedResult =
                    reactantPredictor.initPreResults(new ArrayList<>(), testSet.size());

            List<String> results = new ArrayList<>();

            for (String enzyme : enzymes) {
                System.out.println("Verarbeite Enzym: " + enzyme);

                // Pfad der Support-Datei in eine temporäre Datei schreiben
                String supportFilePath = copySupportFileToTemp(enzyme, "supportfile.csv");

                // Supportfile-Stream laden
                try (InputStream supportFileStream = getResourceStream(enzyme, "supportfile.csv")) {
                    if (!isEnzymeValidForBioSystem(bioSystem, enzyme)) {
                        results.add("Warnung: Enzym " + enzyme + " nicht für Biosystem " + bioSystem + " verfügbar.");
                        continue;
                    }

                    // Prediction durchführen
                    predictedResult = reactantPredictor.makeEnsemblePrediction(
                            enzyme, testSet, supportFilePath, predictedResult);

                    // Ergebnisse extrahieren
                    for (int i = 0; i < predictedResult.size(); i++) {
                        String prediction = predictedResult.get(i).getOrDefault(enzyme, "Keine Vorhersage");
                        results.add("Biosystem: " + bioSystem + ", Enzym: " + enzyme + " -> " + prediction);
                    }
                }
            }

            return results;

        } catch (Exception e) {
            System.err.println("Fehler bei der Transformation: " + e.getMessage());
            e.printStackTrace();
            return List.of("Fehler bei der Transformation: " + e.getMessage());
        }
    }

    /**
     * Lädt einen InputStream für eine Ressource aus der JAR-Datei.
     *
     * @param enzyme   Das Enzym (z. B. "CYP2C9").
     * @param fileName Der Dateiname (z. B. "supportfile.csv" oder "enzyme_NR.model").
     * @return Eine InputStream-Referenz zu der Ressource.
     * @throws IOException Falls die Ressource nicht gefunden werden kann.
     */
    private InputStream getResourceStream(String enzyme, String fileName) throws IOException {
        // Relativer Pfad innerhalb der JAR-Datei
        String resourcePath = "/resources/" + enzyme + "/" + fileName;

        // Laden der Ressource als Stream
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            throw new FileNotFoundException("Ressource nicht gefunden: " + resourcePath);
        }

        return resourceStream;
    }

    private String copySupportFileToTemp(String enzyme, String fileName) throws IOException {
        // Laden der Datei als Stream aus der JAR
        String resourcePath = "/" + enzyme + "/" + fileName;
        InputStream resourceStream = getClass().getResourceAsStream(resourcePath);
        System.out.println(resourceStream);

        if (resourceStream == null) {
            throw new IOException("Supportfile nicht gefunden: " + resourcePath);
        }

        // Temporäre Datei erstellen
        File tempFile = File.createTempFile("supportfile", ".csv");

        try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            while ((resourceStream.read(buffer)) > 0) {
                outputStream.write(buffer);
            }
        } finally {
            resourceStream.close();
        }
        // Automatisches Löschen der Datei beim Beenden der JVM
        tempFile.deleteOnExit();

        // Rückgabe des Pfads der temporären Datei
        return tempFile.getAbsolutePath();
    }


    /**
     * Gibt alle verfügbaren Enzyme für ein spezifisches Biosystem zurück.
     *
     * @param bioSystem Das Biosystem (z. B. "HUMAN").
     * @return Eine Liste der Enzyme.
     */
    private List<String> getAllEnzymesForBioSystem(String bioSystem) {
        JsonNode bioSystemNode = config.path("abstracted_models").path(bioSystem.toUpperCase()).path("ReactantPred");
        if (bioSystemNode.isMissingNode() || !bioSystemNode.isObject()) {
            return List.of(); // Keine Enzyme für dieses Biosystem
        }

        List<String> enzymes = new ArrayList<>();
        bioSystemNode.fieldNames().forEachRemaining(enzymes::add); // Alle Enzyme extrahieren
        return enzymes;
    }

    /**
     * Prüft, ob ein Enzym für ein spezifisches Biosystem verfügbar ist.
     *
     * @param bioSystem Das Biosystem (z. B. "HUMAN").
     * @param enzyme    Das Enzym (z. B. "CYP2C9").
     * @return True, wenn das Enzym unterstützt wird, sonst False.
     */
    private boolean isEnzymeValidForBioSystem(String bioSystem, String enzyme) {
        JsonNode enzymeNode = config.path("abstracted_models").path(bioSystem.toUpperCase()).path("ReactantPred").path(enzyme);
        return !enzymeNode.isMissingNode(); // Valid, wenn der Pfad existiert
    }

    /**
     * Generiert ein WEKA `Instances`-Objekt für den angegebenen SMILES-String.
     *
     * @param sdfTool        Instanz von `SdfToSample`.
     * @param inputSmiles    Eingabe-Molekül als SMILES-String.
     * @return Weka `Instances`, das die Molekülmerkmale enthält.
     */
    private Instances generateInstancesFromSmiles(SdfToSample sdfTool, String inputSmiles) {
        try {
            // Erst ein `IAtomContainerSet` aus dem SMILES-String erstellen
            IAtomContainerSet moleculeSet = sdfTool.createIAtomContainerSet("SMILES=" + inputSmiles);

            // Verwendet die Methode `generateAllInstances` von SdfToSample
            return sdfTool.generateAllInstances(moleculeSet);

        } catch (Exception e) {
            System.err.println("Fehler beim Generieren von Instances vom SMILES: " + e.getMessage());
            return null;
        }
    }

}
