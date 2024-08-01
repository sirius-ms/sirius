package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import de.unijena.bioinf.ChemistryBase.chem.InChI;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.chemdb.InChISMILESUtils;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;

public class CombinatorialSubtreeCalculatorJsonWriter {

    public static String writeResultForVisualization(FTree tree, CombinatorialSubtreeCalculator subtreeCalc) throws IOException {
        final HashMap<MolecularFormula, AnnotatedPeak[]> formula2peak = new HashMap<>();
        final FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for (Fragment f : tree) {
            if (formula2peak.containsKey(f.getFormula())) {
                AnnotatedPeak a = ano.get(f);
                AnnotatedPeak[] bs = (formula2peak.get(f.getFormula()));
                AnnotatedPeak[] cs = Arrays.copyOf(bs, bs.length+1);
                cs[bs.length] = a;
                formula2peak.put(f.getFormula(), cs);
            } else {
                formula2peak.put(f.getFormula(), new AnnotatedPeak[]{ano.get(f)});
            }
        }
        StringWriter writer = new StringWriter();
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        JsonGenerator jsonGenerator = factory.createGenerator(writer);
        jsonGenerator.writeStartArray();
        for (CombinatorialNode node : subtreeCalc.subtree) {
            if (!node.fragment.isInnerNode()) {
                final CombinatorialNode frag = node.getIncomingEdges().get(0).getSource();
                final AnnotatedPeak[] peaks = formula2peak.getOrDefault(node.fragment.formula, new AnnotatedPeak[0]);
                for (AnnotatedPeak peak : peaks) {
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeStringField("formula", node.fragment.getFormula().toString());
                    jsonGenerator.writeNumberField("peakmass", peak.getMass());
                    jsonGenerator.writeNumberField("totalScore", subtreeCalc.subtree.getScore());
                    jsonGenerator.writeNumberField("score", node.totalScore);
                    jsonGenerator.writeArrayFieldStart("atoms");
                    final Set<IAtom> atoms = new HashSet<>();
                    for (IAtom a : frag.fragment.getAtoms()) {
                        atoms.add(a);
                        jsonGenerator.writeNumber(a.getIndex());
                    }
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeArrayFieldStart("bonds");
                    for (IBond b : subtreeCalc.molecule.bonds) {
                        if (atoms.contains(b.getAtom(0)) && atoms.contains(b.getAtom(1))) {
                            jsonGenerator.writeNumber(b.getIndex());
                        }
                    }
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeArrayFieldStart("cuts");
                    CombinatorialEdge e = frag.getIncomingEdges().isEmpty() ? null : frag.incomingEdges.get(0);
                    while (e != null) {
                        if (e.cut1!=null) jsonGenerator.writeNumber(e.cut1.getIndex());
                        if (e.cut2!=null) jsonGenerator.writeNumber(e.cut2.getIndex());
                        e = e.getSource().incomingEdges.isEmpty() ? null : e.getSource().getIncomingEdges().get(0);
                    }
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeEndObject();
                }
            }
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.flush();
        return writer.toString();
    }

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
        InChI inChI = InChISMILESUtils.getInchi(molecule.getMolecule(), false);
        Objects.requireNonNull(inChI);
        int[] atomOrder = new int[molecule.natoms];


        // some general informations:
        jsonGenerator.writeStringField("molecularFormula", molecule.getFormula().toString());
        jsonGenerator.writeStringField("smiles", smiGen.create(molecule.getMolecule(), atomOrder));
        jsonGenerator.writeStringField("inchi", inChI.in3D);
        jsonGenerator.writeStringField("inchiKey", inChI.key);
        jsonGenerator.writeStringField("method", subtreeCalc.getMethodName());
        jsonGenerator.writeNumberField("score", subtreeCalc.getScore());
        jsonGenerator.writeNumberField("explainedPeaks", subtreeCalc.getSubtree().getNodes().stream().filter(x->!x.fragment.isInnerNode()).count());
        jsonGenerator.writeNumberField("numberOfPeaks", subtreeCalc.fTree.numberOfVertices());
        FragmentAnnotation<AnnotatedPeak> pa = subtreeCalc.fTree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        double totalIntensity = subtreeCalc.fTree.getFragments().stream().mapToDouble(x->pa.get(x, AnnotatedPeak::none).getRelativeIntensity()).sum();
        jsonGenerator.writeNumberField("explainedIntensity", subtreeCalc.getSubtree().getNodes().stream().filter(x->!x.fragment.isInnerNode()).mapToDouble(x->x.fragment.peakIntensity).sum() / totalIntensity);
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
