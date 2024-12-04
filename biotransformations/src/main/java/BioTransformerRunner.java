import biotransformer.biosystems.BioSystem;
import biotransformer.btransformers.*;
import biotransformer.transformation.Biotransformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class BioTransformerRunner implements TransformationMethod {

    private Cyp450BTransformer cypTransformer;
    private ECBasedBTransformer ecTransformer;
    private Phase2BTransformer phase2Transformer;
    private EnvMicroBTransformer envMicroTransformer;
    private HGutBTransformer gutTransformer;

    private boolean useDB = false;
    private boolean useSubstitution = false;
    private Set<String> activeModules;

    /**
     * Konstruktor mit Standardoptionen.
     */
    public BioTransformerRunner() {
        this(false, false, "HUMAN", new HashSet<>());
    }

    /**
     * Konstruktor mit konfigurierbaren Optionen.
     */
    public BioTransformerRunner(boolean useDB, boolean useSubstitution, String bioSystem, Set<String> modules) {
        this.useDB = useDB;
        this.useSubstitution = useSubstitution;
        this.activeModules = modules;
        initializeTransformers(bioSystem);
    }



    /**
     * Initialisiert die BioTransformer-Module.
     */
    private void initializeTransformers(String bioSystem) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (inputStream == null) {
                throw new RuntimeException("Die Datei config.json konnte nicht gefunden werden.");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            LinkedHashMap<String, Object> config = objectMapper.readValue(inputStream, LinkedHashMap.class);
            System.out.println("Konfiguration erfolgreich geladen: " + config);

            boolean useDBFromConfig = (Boolean) config.getOrDefault("useDB", useDB);
            boolean useSubstitutionFromConfig = (Boolean) config.getOrDefault("useSubstitution", useSubstitution);



            cypTransformer = new Cyp450BTransformer(BioSystem.BioSystemName.valueOf(bioSystem), useDBFromConfig, useSubstitutionFromConfig);
            ecTransformer = new ECBasedBTransformer(BioSystem.BioSystemName.valueOf(bioSystem), useDBFromConfig, useSubstitutionFromConfig);
            phase2Transformer = new Phase2BTransformer(BioSystem.BioSystemName.valueOf(bioSystem), useDBFromConfig, useSubstitutionFromConfig);
            envMicroTransformer = new EnvMicroBTransformer();
            gutTransformer = new HGutBTransformer(useDBFromConfig, useSubstitutionFromConfig);
        }
        catch (IOException e) {
            throw new RuntimeException("Fehler beim Laden der Konfigurationsdatei: " + e.getMessage(), e);
        }catch (Exception e) {
            throw new RuntimeException("Fehler beim Initialisieren der Transformer: " + e.getMessage());
        }
    }

    /**
     * Aktiviert ein spezifisches Modul.
     */
    public void activateModule(String moduleName) {
        activeModules.add(moduleName.toUpperCase());
    }

    /**
     * Aktiviert alle verfügbaren Module.
     */
    public void activateAllModules() {
        activeModules.add("CYP450");
        activeModules.add("EC");
        activeModules.add("PHASE2");
        activeModules.add("ENVMICRO");
        activeModules.add("GUT");
    }

    /**
     * Führt Transformationen auf einer Liste von Molekülen aus.
     *
     * @param inputs Liste von Molekülen als Eingabe
     * @return Liste von Transformationsprodukten
     */
    @Override
    public List<Biotransformation> transform(List<IAtomContainer> inputs) {
        List<Biotransformation> results = new ArrayList<>();

        if (activeModules.isEmpty()) {
            throw new IllegalStateException("Es wurden keine Module aktiviert.");
        }

        for (IAtomContainer molecule : inputs) {
            try {
                if (activeModules.contains("CYP450")) {
                    results.addAll(cypTransformer.predictCyp450BiotransformationChain(molecule, true, true, 1, 0.0));
                }
                if (activeModules.contains("EC")) {
                    results.addAll(ecTransformer.applyEcBasedTransformationsChain(molecule, true, true, 1, 0.0));
                }
                if (activeModules.contains("PHASE2")) {
                    results.addAll(phase2Transformer.applyPhase2TransformationsChainAndReturnBiotransformations(molecule, true, true, true, 1, 0.0));
                }
                if (activeModules.contains("ENVMICRO")) {
                    results.addAll(envMicroTransformer.applyEnvMicrobialTransformationsChain(molecule, true, true, 1, 0.0));
                }
                if (activeModules.contains("GUT")) {
                    results.addAll(gutTransformer.applyGutMicrobialMetabolismHydrolysisAndRedoxChain(molecule, true, true, 1, 0.0));
                }
            } catch (Exception e) {
                throw new RuntimeException("Fehler bei der Transformation mit Modul X für Molekül Y: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Hilfsmethode zum Parsen eines SMILES-Strings.
     */
    public static IAtomContainer parseSmiles(String smiles) {
        try {
            SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            return sp.parseSmiles(smiles);
        } catch (Exception e) {
            throw new IllegalArgumentException("Fehler beim Parsen von SMILES: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            BioTransformerRunner runner = new BioTransformerRunner();

            // Aktivieren Sie die gewünschten Module
            runner.activateModule("CYP450");
            runner.activateModule("EC");

            // Beispieleingabe: Liste von SMILES
            List<IAtomContainer> inputs = List.of(
                    parseSmiles("CC(C)C1=CC=CC=C1"), // Beispiel-Molekül
                    parseSmiles("C1=CC=CC=C1")      // Beispiel-Molekül
            );

            // Transformationen ausführen
            List<Biotransformation> results = runner.transform(inputs);

            // Ergebnisse ausgeben
            System.out.println("Gefundene Transformationen: " + results.size());
            for (Biotransformation result : results) {
                System.out.println(result.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
