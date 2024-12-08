package de.unijena.bioinf.babelms.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FTJsonWriterTest {

    @Test
    public void testCompoundFormula() throws JsonProcessingException {
        Ionization protonation = PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization();
        PrecursorIonType ionType = PrecursorIonType.getPrecursorIonType(protonation);
        MolecularFormula precursorMF = MolecularFormula.parseOrThrow("C6H12NO6");
        double exactMz = protonation.addToMass(precursorMF.getMass());
        AnnotatedPeak precursorPeak = new AnnotatedPeak(precursorMF, exactMz, exactMz, 0, protonation, new Peak[]{new SimplePeak(exactMz, 0)}, new CollisionEnergy[]{new CollisionEnergy(10d)}, new int[]{0});

        FTree baseTree = new FTree(precursorMF, protonation);
        FragmentAnnotation<AnnotatedPeak> anno = baseTree.getOrCreateFragmentAnnotation(AnnotatedPeak.class);
        anno.set(baseTree.getRoot(), precursorPeak);

        //[M+H]+ tree
        testForIonType(precursorMF, baseTree, ionType, false);

        //[M+NH4]+ tree unresolved
        ionType = PrecursorIonType.fromString("[M+NH4]+");
        testForIonType(precursorMF, baseTree, ionType, false);


        //[M+NH4]+ tree resolved
        ionType = PrecursorIonType.fromString("[M+NH4]+");
        testForIonType(precursorMF, baseTree, ionType, true);

        //[M-H2O+H]+ tree unresolved
        ionType = PrecursorIonType.fromString("[M-H2O+H]+");
        testForIonType(precursorMF, baseTree, ionType, false);

        //[M-H2O+H]+ tree resolved
        ionType = PrecursorIonType.fromString("[M-H2O+H]+");
        testForIonType(precursorMF, baseTree, ionType, true);

        //[M-H2O+NH4]+ tree unresolved
        ionType = PrecursorIonType.fromString("[M-H2O+NH4]+");
        testForIonType(precursorMF, baseTree, ionType, false);

        //[M-H2O+NH4]+ tree resolved
        ionType = PrecursorIonType.fromString("[M-H2O+NH4]+");
        testForIonType(precursorMF, baseTree, ionType, true);
    }

    private static void testForIonType(MolecularFormula precursorMF, FTree baseTree, PrecursorIonType ionType, boolean resolveTree) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        FTJsonWriter writer = new FTJsonWriter();

        MolecularFormula compoundFormula = ionType.measuredNeutralMoleculeToNeutralMolecule(precursorMF);
        MolecularFormula rootFormula = resolveTree ? compoundFormula : precursorMF;

        FTree tree = new FTree(baseTree);
        tree.setAnnotation(PrecursorIonType.class, ionType);

        if (resolveTree)
            tree = new IonTreeUtils().treeToNeutralTree(tree);

        String json = writer.treeToJsonString(tree);
        System.out.println(json);

        JsonNode jsonRoot = mapper.readTree(json);
        assertTrue(jsonRoot.has("root"));
        assertTrue(jsonRoot.has("molecularFormula"));
        assertTrue(jsonRoot.has("annotations"));

        JsonNode annotations = jsonRoot.get("annotations");
        assertTrue(annotations.has("precursorIonType"));

        assertEquals(rootFormula, MolecularFormula.parseOrThrow(jsonRoot.get("root").asText()));
        assertEquals(compoundFormula, MolecularFormula.parseOrThrow(jsonRoot.get("molecularFormula").asText()));
        assertEquals(ionType, PrecursorIonType.fromString(annotations.get("precursorIonType").asText()));
    }


}
