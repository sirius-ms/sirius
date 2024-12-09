//package sirius.transformations;
import biotransformer.btransformers.*;
import biotransformer.biosystems.*;
import biotransformer.transformation.Biotransformation;
import biotransformer.transformation.MetabolicReaction;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;


import java.util.*;

/*TODO: config.json File einbinden
        Inhalt von config.json beschreiben
*/
public class BiotransformerWrapper implements TransformationMethod {



    private Biotransformer biotransformer;
    private Cyp450BTransformer cyp450BTransformer;
    private ECBasedBTransformer ecBasedBTransformer;
    private EnvMicroBTransformer envMicroBTransformer;
    private HGutBTransformer hGutBTransformer;
    private Phase2BTransformer phase2BTransformer;
    private SimulateHumanMetabolism simulateHumanMetabolism;
    private SmilesExtractor extractSmiles;
    String getConfigFilePath = "src/main/resources/config.json";
    /**
     * Konstruktor, der die Konfiguration lädt.
     * @param  Pfad zur Config-Datei (JSON-Format).
     * @param bioSystem Name des BioSystems (z.B. HUMAN, ENV, HGUT).
     * @param useDB         Gibt an, ob eine Datenbank verwendet werden soll.
     * @param useSubstitution Gibt an, ob Substitutionsmethoden verwendet werden sollen.
     * @param transformerTypes gibt die Transformertypen an
     */
    public BiotransformerWrapper( BioSystem.BioSystemName bioSystem, boolean useDB, boolean useSubstitution, Set<String> transformerTypes) {
        if (bioSystem == null) {
            throw new IllegalArgumentException("BioSystem darf nicht null sein.");
        }

        this.extractSmiles = new SmilesExtractor();

        try {
            for (String transformerType : transformerTypes) {
                switch (transformerType.toLowerCase()) {
                    case "biotransformer":
                        this.biotransformer = new Biotransformer(bioSystem, useDB, useSubstitution);
                        break;
                    case "cyp450":
                        this.cyp450BTransformer = new Cyp450BTransformer(bioSystem, useDB, useSubstitution);
                        break;
                    case "ecbased":
                        this.ecBasedBTransformer = new ECBasedBTransformer(bioSystem, useDB, useSubstitution);
                        break;
                    case "envmicro":
                        this.envMicroBTransformer = new EnvMicroBTransformer();
                        break;
                    case "hgut":
                        this.hGutBTransformer = new HGutBTransformer(useDB, useSubstitution);
                        break;
                    case "phase2":
                        this.phase2BTransformer = new Phase2BTransformer(bioSystem, useDB, useSubstitution);
                        break;
                    case "simulatehumanmetabolism":
                        // Standardwerte können hier angepasst werden
                        this.simulateHumanMetabolism = new SimulateHumanMetabolism(1, useDB, "hmdb", false, 10, useSubstitution);
                        break;
                    default:
                        throw new IllegalArgumentException("Unbekannter Transformer-Typ: " + transformerType);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Initialisieren eines Transformers: " + e.getMessage(), e);
        }
    }


    /**
     * Simuliert den menschlichen Metabolismus eines Moleküls über mehrere Iterationen.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param iterations Anzahl der Iterationen.
     * @return Liste der SMILES-Strings der Transformationsprodukte.
     */
    public List<String> simulateHumanMetabolism(String inputMolecule, int iterations) {
        if (simulateHumanMetabolism == null){
            throw new IllegalStateException("SimulateHumanMetabolism is not initialized");
        }
        List<String> results = new ArrayList<>();
        try {
            SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            IAtomContainer molecule = parser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = simulateHumanMetabolism.simulateHumanMetabolism(molecule, iterations);

            results = extractSmiles.extractSmilesFromTransformation(transformations,simulateHumanMetabolism.getInChIGenFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }



    /**
     * Berechnet die Biotransformationsprodukte für ein gegebenes Molekül.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @return Liste der SMILES-Strings der Biotransformationsprodukte.
     */
    @Override
    public List<String> calculateTransformations(String inputMolecule) {
        if (biotransformer == null){
            throw new IllegalStateException("Biotransformer not initialized");
        }
        // Simuliert den Aufruf von Biotransformer mit einem Molekül
        List<String> result = new ArrayList<>();
        try {
            SmilesParser smilesParser = biotransformer.getSmiParser();
            IAtomContainer molecule = smilesParser.parseSmiles(inputMolecule);

            LinkedHashMap<String, ArrayList<MetabolicReaction>> reactionsMap = biotransformer.getReactionsList();

            List<MetabolicReaction> allReactions = new ArrayList<>();
            for (ArrayList<MetabolicReaction> reactionGroup : reactionsMap.values()) {
                allReactions.addAll(reactionGroup);
            }

            ArrayList<Biotransformation> transformations = new ArrayList<>();
            for (MetabolicReaction reaction : allReactions) {
                transformations.addAll(
                        biotransformer.applyReactionAndReturnBiotransformations(
                                molecule,
                                reaction,   // Einzelne Reaktion übergeben
                                true,       // preprocess
                                0.5         // scoreThreshold
                        )
                );
            }

            result = extractSmiles.extractSmilesFromTransformation(transformations,biotransformer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Berechnet CYP450-spezifische Biotransformationsprodukte für ein Molekül.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param mode          Modus für CYP450-Berechnung (1, 2 oder 3).
     * @return Liste der SMILES-Strings der CYP450-Biotransformationsprodukte.
     */
    public List<String> calculateCYP450Transformations(String inputMolecule, int mode) {
        if(cyp450BTransformer == null){
            throw new IllegalStateException("Cyp450BTransformer not initialized");
        }
        List<String> result = new ArrayList<>();
        try {
            SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            IAtomContainer molecule = smilesParser.parseSmiles(inputMolecule);

            // Wendet die Transformationen mit dem Cyp450BTransformer an
            ArrayList<Biotransformation> transformations = cyp450BTransformer.predictCyp450BiotransformationsByMode(
                    molecule,
                    mode,
                    true, // preprocess
                    true, // filter
                    0.5   // threshold
            );

            // Extrahiert die Ergebnisse und konvertiert sie zu SMILES
            result = extractSmiles.extractSmilesFromTransformation(transformations,cyp450BTransformer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Berechnet EC-basierte Biotransformationsprodukte.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param type          Typ der EC-Transformation ("Deconjugation", "Transformation", "Conjugation").
     * @return Liste der SMILES-Strings der EC-basierten Produkte.
     */
    public List<String> calculateECBasedTransformations(String inputMolecule, String type) {
        if (ecBasedBTransformer == null){
            throw new IllegalStateException("EcBasedTranformer not initialized");
        }
        List<String> result = new ArrayList<>();
        try {
            SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            IAtomContainer molecule = smilesParser.parseSmiles(inputMolecule);
            ArrayList<Biotransformation> transformations;

            switch (type.toLowerCase()) {
                case "deconjugation":
                    transformations = ecBasedBTransformer.applyEcBasedDeconjugations(molecule, true, true, 1, 0.5);
                    break;
                case "transformation":
                    transformations = ecBasedBTransformer.applyEcBasedTransformations(molecule, true, true, 0.5);
                    break;
                case "conjugation":
                    transformations = ecBasedBTransformer.applyEcBasedConjugations(molecule, true, true, 0.5);
                    break;
                default:
                    throw new IllegalArgumentException("Ungültiger Transformationstyp: " + type);
            }

            result = extractSmiles.extractSmilesFromTransformation(transformations,ecBasedBTransformer);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Berechnet mikrobiell basierte Umwelt-Biotransformationsprodukte.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param nrOfSteps     Anzahl der Transformationen in der Kette.
     * @return Liste der SMILES-Strings der Umwelt-mikrobiellen Biotransformationsprodukte.
     */
    public List<String> calculateEnvMicrobialTransformations(String inputMolecule, int nrOfSteps) {
        if (envMicroBTransformer == null){
            throw new IllegalStateException("EnvMircoTransformer not initialized");
        }
        List<String> result = new ArrayList<>();
        try {
            SmilesParser smilesParser = envMicroBTransformer.getSmiParser();
            IAtomContainer molecule = smilesParser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = envMicroBTransformer.applyEnvMicrobialTransformationsChain(
                    molecule,
                    true,  // preprocess
                    true,  // filter
                    nrOfSteps,
                    0.5    // scoreThreshold
            );

            result = extractSmiles.extractSmilesFromTransformation(transformations,biotransformer);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * Führt eine Simulation der Redoxreaktionen des Darmmikrobioms durch.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param nrOfSteps Anzahl der Transformationsschritte.
     * @return Liste der SMILES-Strings der Transformationsprodukte.
     */
    public List<String> simulateGutMicrobialRedox(String inputMolecule, int nrOfSteps) {
        if (hGutBTransformer == null){
            throw new IllegalStateException("HGutBTransformer not initialized");
        }
        List<String> results = new ArrayList<>();
        try {
            SmilesParser parser = hGutBTransformer.getSmiParser();
            IAtomContainer molecule = parser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = hGutBTransformer.applyGutMicrobialRedoxChain(
                    molecule,
                    true, // preprocess
                    true, // filter
                    nrOfSteps,
                    0.5   // scoreThreshold
            );

            for (Biotransformation transformation : transformations) {
                results.add(hGutBTransformer.smiGen.create(transformation.getProducts().getAtomContainer(0)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Führt eine Simulation von Phase-II-Reaktionen des Darmmikrobioms durch.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @return Liste der SMILES-Strings der Transformationsprodukte.
     */
    public List<String> simulateGutMicrobialPhaseII(String inputMolecule) {
        if (hGutBTransformer== null){
            throw new IllegalStateException("HGutBTransformer not initialized");
        }
        List<String> results = new ArrayList<>();
        try {
            SmilesParser parser = hGutBTransformer.getSmiParser();
            IAtomContainer molecule = parser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = hGutBTransformer.applyGutMicrobialConjugations(
                    molecule,
                    true, // preprocess
                    true, // filter
                    0.5   // scoreThreshold
            );

            for (Biotransformation transformation : transformations) {
                results.add(hGutBTransformer.smiGen.create(transformation.getProducts().getAtomContainer(0)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Führt eine Simulation von De-Konjugationen des Darmmikrobioms durch.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param nrOfSteps Anzahl der Transformationsschritte.
     * @return Liste der SMILES-Strings der Transformationsprodukte.
     */
    public List<String> simulateGutMicrobialDeconjugation(String inputMolecule, int nrOfSteps) {
        if (hGutBTransformer== null){
            throw new IllegalStateException("HGutBTransformer not initialized");
        }
        List<String> results = new ArrayList<>();
        try {
            SmilesParser parser = hGutBTransformer.getSmiParser();
            IAtomContainer molecule = parser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = hGutBTransformer.applyGutMicrobialDeconjugationsChain(
                    molecule,
                    true, // preprocess
                    true, // filter
                    nrOfSteps,
                    0.5   // scoreThreshold
            );

            for (Biotransformation transformation : transformations) {
                results.add(hGutBTransformer.smiGen.create(transformation.getProducts().getAtomContainer(0)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Führt eine vollständige Simulation der Darmmikrobiom-Metabolismus durch.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param nrOfSteps Anzahl der Transformationsschritte.
     * @return Liste der SMILES-Strings der Transformationsprodukte.
     */
    public List<String> simulateFullGutMicrobialMetabolism(String inputMolecule, int nrOfSteps) {
        if (hGutBTransformer== null){
            throw new IllegalStateException("HGutBTransformer not initialized");
        }
        List<String> results = new ArrayList<>();
        try {
            SmilesParser parser = hGutBTransformer.getSmiParser();
            IAtomContainer molecule = parser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = hGutBTransformer.simulateGutMicrobialMetabolism(
                    molecule,
                    true, // preprocess
                    true, // filter
                    nrOfSteps,
                    0.5   // scoreThreshold
            );

            for (Biotransformation transformation : transformations) {
                results.add(hGutBTransformer.smiGen.create(transformation.getProducts().getAtomContainer(0)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Führt Phase-II-Biotransformationen durch.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param precheck Gibt an, ob eine Vorprüfung durchgeführt werden soll.
     * @param nrOfSteps Anzahl der Transformationsschritte.
     * @return Liste der SMILES-Strings der Transformationsprodukte.
     */
    public List<String> applyPhase2Transformations(String inputMolecule, boolean precheck, int nrOfSteps) {
        if (phase2BTransformer== null){
            throw new IllegalStateException("Phase2Transformer not initialized");
        }
        List<String> results = new ArrayList<>();
        try {
            SmilesParser parser = phase2BTransformer.getSmiParser();
            IAtomContainer molecule = parser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = phase2BTransformer.applyPhase2TransformationsChainAndReturnBiotransformations(
                    molecule,
                    precheck,  // Vorprüfung
                    true,      // Preprocessing
                    true,      // Filter
                    nrOfSteps,
                    0.5        // Score-Threshold
            );

            for (Biotransformation transformation : transformations) {
                results.add(phase2BTransformer.smiGen.create(transformation.getProducts().getAtomContainer(0)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Führt spezifische Phase-II-Biotransformationen wie Glucuronidierung oder Sulfonierung durch.
     *
     * @param inputMolecule Molekül im SMILES-Format.
     * @param reactionType Typ der Reaktion (z. B. "Glucuronidation", "Sulfonation").
     * @param nrOfSteps Anzahl der Transformationsschritte.
     * @return Liste der SMILES-Strings der Transformationsprodukte.
     */
    public List<String> applySpecificPhase2Transformation(String inputMolecule, String reactionType, int nrOfSteps) {
        if (phase2BTransformer== null){
            throw new IllegalStateException("Phase2Transformer not initialized");
        }
        List<String> results = new ArrayList<>();
        try {
            SmilesParser parser = phase2BTransformer.getSmiParser();
            IAtomContainer molecule = parser.parseSmiles(inputMolecule);

            ArrayList<Biotransformation> transformations = phase2BTransformer.applyPhase2TransformationsChainAndReturnBiotransformations(
                    molecule,
                    true,      // Vorprüfung
                    true,      // Preprocessing
                    true,      // Filter
                    nrOfSteps,
                    0.5        // Score-Threshold
            );

            // Filter Ergebnisse nach Reaktionstyp
            for (Biotransformation transformation : transformations) {
                if (transformation.getReactionType().contains(reactionType)) {
                    results.add(phase2BTransformer.smiGen.create(transformation.getProducts().getAtomContainer(0)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }


    /**
     * Gibt die unterstützten EC-Transformationstypen zurück.
     *
     * @return Liste von Transformationstypen.
     */
    public List<String> getSupportedECTransformations() {
        return List.of("Deconjugation", "Transformation", "Conjugation");
    }

    /**
     * Ruft alle unterstützten Reaktionen für das aktuelle BioSystem ab.
     *
     * @return Eine Liste von Reaktionsnamen.
     */
    public List<String> getSupportedReactions() {
        List<String> reactions = new ArrayList<>();
        biotransformer.getReactionsList().forEach((key, reaction) -> reactions.add(key));
        return reactions;
    }

    /**
     * Gibt die unterstützten CYP450-Enzyme zurück.
     *
     * @return Eine Liste von CYP450-Enzymnamen.
     */
    public List<String> getCYP450Enzymes() {
        List<String> enzymes = new ArrayList<>();
        for (String enzyme : cyp450BTransformer.cyp450Enzymes) {
            enzymes.add(enzyme);
        }
        return enzymes;
    }


}
