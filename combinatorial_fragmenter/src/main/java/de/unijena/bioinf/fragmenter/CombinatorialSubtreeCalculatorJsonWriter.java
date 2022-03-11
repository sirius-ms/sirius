package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

public class CombinatorialSubtreeCalculatorJsonWriter {

    public static String writeResultsToString(CombinatorialSubtreeCalculator subtreeCalc) throws CDKException, IOException {
        StringWriter writer = new StringWriter();
        write2Json(subtreeCalc, writer);
        return writer.toString();
    }

    public static void writeResultsToFile(CombinatorialSubtreeCalculator subtreeCalc, File file) throws IOException, CDKException {
        try(BufferedWriter bw = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset())) {
            write2Json(subtreeCalc, bw);
        }
    }

    public static void write2Json(@NotNull CombinatorialSubtreeCalculator subtreeCalc,@NotNull Writer writer) throws IOException, CDKException {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        JsonGenerator jsonGenerator = factory.createGenerator(writer);
        write2Json(subtreeCalc, jsonGenerator);
        jsonGenerator.close();
    }

    public static void write2Json(@NotNull CombinatorialSubtreeCalculator subtreeCalc, @NotNull JsonGenerator jsonGenerator) throws IOException, CDKException {
        jsonGenerator.useDefaultPrettyPrinter();
        jsonGenerator.writeStartObject();

        MolecularGraph molecule = subtreeCalc.getMolecule();
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Generic);
        InChIGenerator inchiGen = InChIGeneratorFactory.getInstance().getInChIGenerator(molecule.getMolecule());
        int[] atomOrder = new int[molecule.natoms];

        // some general informations:
        jsonGenerator.writeStringField("molecularFormula", molecule.getFormula().toString());
        jsonGenerator.writeStringField("smiles", smiGen.create(molecule.getMolecule(), atomOrder));
        jsonGenerator.writeStringField("inchi", inchiGen.getInchi());
        jsonGenerator.writeStringField("inchiKey", inchiGen.getInchiKey());
        jsonGenerator.writeStringField("method", subtreeCalc.getMethodName());
        jsonGenerator.writeNumberField("score", subtreeCalc.getScore());

        // assignments and computed subtree:
        writeAssignmentsToJson(subtreeCalc, atomOrder, jsonGenerator);
        jsonGenerator.writeFieldName("tree");
        CombinatorialSubtreeJsonWriter.tree2Json(subtreeCalc.getSubtree(), jsonGenerator);
        jsonGenerator.flush();
    }

    private static BitSet permutateBitSet(BitSet bitSet, int[] order){
        BitSet newBitSet = new BitSet(order.length);
        for(int i = 0; i < order.length; i++){
            newBitSet.set(order[i], bitSet.get(i));
        }
        return newBitSet;
    }

    private static void writeBitSetToJson(String fieldName, BitSet bitSet, JsonGenerator jsonGenerator) throws IOException {
        long[] longs = bitSet.toLongArray();
        jsonGenerator.writeFieldName(fieldName);
        jsonGenerator.writeStartArray();
        for(int i = 0; i < longs.length; i++){
            jsonGenerator.writeNumber(longs[i]);
        }
        jsonGenerator.writeEndArray();
    }

    private static void writeAssignmentsToJson(CombinatorialSubtreeCalculator subtreeCalc, int[] order, JsonGenerator jsonGenerator) throws IOException {
        final HashMap<Fragment, ArrayList<CombinatorialFragment>> mapping = subtreeCalc.computeMapping();
        final FragmentAnnotation<AnnotatedPeak> ano = subtreeCalc.getFTree().getFragmentAnnotationOrNull(AnnotatedPeak.class);

        jsonGenerator.writeFieldName("assignments");
        jsonGenerator.writeStartArray();
        for(Fragment ftFrag : mapping.keySet()){
            ArrayList<CombinatorialFragment> assignedFragments = mapping.get(ftFrag);

            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("ftFragVertexId", ftFrag.getVertexId());
            jsonGenerator.writeStringField("molecularFormula", ftFrag.getFormula().toString());

            if(ano != null) {
                jsonGenerator.writeNumberField("mz", ano.get(ftFrag).getMass());
                jsonGenerator.writeNumberField("relativeIntensity", ano.get(ftFrag).getRelativeIntensity());
            }

            jsonGenerator.writeFieldName("assignedFragments");
            jsonGenerator.writeStartArray();
            for(CombinatorialFragment fragment : assignedFragments){
                jsonGenerator.writeStartObject();
                writeBitSetToJson("bitset", permutateBitSet(fragment.bitset, order), jsonGenerator);
                jsonGenerator.writeStringField("molecularFormula", fragment.getFormula().toString());
                jsonGenerator.writeNumberField("hydrogenRearrangements", Math.abs(fragment.hydrogenRearrangements(ftFrag.getFormula())));
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}
