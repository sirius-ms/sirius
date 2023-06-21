
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.JDKDocument;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class FTJsonWriter {


    private DescriptorRegistry registry = DescriptorRegistry.getInstance();

    public String treeToJsonString(@NotNull FTree tree, @Nullable Double precursorMass) {
        StringWriter w = new StringWriter();
        tree2json(tree,precursorMass,w);
        return w.toString();
    }

    public String treeToJsonString(@NotNull FTree tree) {
        return treeToJsonString(tree, null);
    }

    public void writeTree(Writer writer, FTree tree) throws IOException {
        tree2json(tree,null, writer);
    }

    public void writeTreeToFile(File f, FTree tree) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath(), Charset.defaultCharset())) {
            writeTree(bw, tree);
        }
    }

    protected void tree2json(@NotNull FTree tree, @Nullable Double precursorMass, Writer writer) {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        try {
            final JsonGenerator generator = factory.createGenerator(writer);
            tree2json(tree, precursorMass, generator);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void tree2json(@NotNull FTree tree, @Nullable Double precursorMass, @NotNull final JsonGenerator generator) throws IOException {
        generator.useDefaultPrettyPrinter();
        generator.writeStartObject();

        final PrecursorIonType generalIonType = tree.getAnnotationOrNull(PrecursorIonType.class);
        final FragmentAnnotation<PrecursorIonType> ionPerFragment = tree.getFragmentAnnotationOrNull(PrecursorIonType.class);
        final FragmentAnnotation<AnnotatedPeak> anoPeak = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        if (generalIonType != null) {
            final MolecularFormula formula = tree.getRoot().getFormula();
            final PrecursorIonType fragmentIon = getFragmentIon(ionPerFragment, tree.getRoot(), generalIonType);
            final MolecularFormula neutral = fragmentIon.measuredNeutralMoleculeToNeutralMolecule(formula);
            generator.writeStringField("molecularFormula", neutral.toString());
            generator.writeStringField("root", formula.toString());
        } else {
            final String f = tree.getRoot().getFormula().toString();
            generator.writeStringField("molecularFormula", f);
            generator.writeStringField("root", f);
        }
        generator.writeFieldName("annotations");
        generator.writeStartObject();

        final JDKDocument jdkDocument = new JDKDocument();

        for (Class<DataAnnotation> anot : tree.annotations()) {
            Descriptor<DataAnnotation> d = registry.get(FTree.class, anot);
            if (d != null) {
                final Map<String, Object> stringObjectMap = jdkDocument.newDictionary();
                d.write(jdkDocument,stringObjectMap, tree.getAnnotationOrThrow(anot));
                writeSimpleMap(generator, jdkDocument, stringObjectMap);
            } else {
                //hardCodedAnnotations(generator tree);
            }
        }
        generator.writeEndObject();

        generator.writeFieldName("fragments");
        generator.writeStartArray();

        final List<FragmentAnnotation<DataAnnotation>> fragmentAnnotations = tree.getFragmentAnnotations();
        for (Fragment f : tree.getFragments()) {

            generator.writeStartObject();

            generator.writeNumberField("id", f.getVertexId());
            generator.writeStringField("molecularFormula", f.getFormula().toString());
            if (f.getIonization()!=null)generator.writeStringField("ion", f.getIonization().toString());

            if (anoPeak.get(f).isMeasured()) {
                Deviation dev = tree.getMassError(f);
                if (f.isRoot() && precursorMass != null && dev.equals(Deviation.NULL_DEVIATION))
                    dev = tree.getMassErrorTo(f, precursorMass);

                Deviation rdev = tree.getRecalibratedMassError(f);
                if (f.isRoot() && precursorMass != null && dev.equals(Deviation.NULL_DEVIATION))
                    rdev = tree.getMassErrorTo(f, precursorMass);

                generator.writeStringField("massDeviation", dev.toString());
                generator.writeStringField("recalibratedMassDeviation", rdev.toString());
            }

            for (FragmentAnnotation<DataAnnotation> fano : fragmentAnnotations) {
                final Map<String,Object> fragment = jdkDocument.newDictionary();
                if (fano.get(f) != null) {
                    Descriptor<DataAnnotation> d = registry.get(Fragment.class, fano.getAnnotationType());
                    if (d != null) {
                        d.write(jdkDocument, fragment, fano.get(f));
                        writeSimpleMap(generator,jdkDocument,fragment);
                    }
                }
            }

            generator.writeEndObject();
        }

        generator.writeEndArray();

        generator.writeFieldName("losses");
        generator.writeStartArray();
        final List<LossAnnotation<DataAnnotation>> lossAnnotations = tree.getLossAnnotations();
        for (Loss l : tree.losses()) {

            generator.writeStartObject();

            generator.writeNumberField("source", l.getSource().getVertexId());
            generator.writeNumberField("target", l.getTarget().getVertexId());
            generator.writeStringField("molecularFormula", l.getFormula().toString());
            for (LossAnnotation<DataAnnotation> lano : lossAnnotations) {
                if (lano.get(l)!=null) {
                    Descriptor<DataAnnotation> d = registry.get(Loss.class, lano.getAnnotationType());
                    if (d != null) {
                        Map<String,Object> map = jdkDocument.newDictionary();
                        d.write(jdkDocument, map, lano.get(l));
                        writeSimpleMap(generator,jdkDocument,map);
                    }
                }
            }

            generator.writeEndObject();
        }
        generator.writeEndArray();

        generator.writeEndObject();

        generator.flush();
    }

    private void writeSimpleMap(JsonGenerator generator, JDKDocument doc, Map<String, Object> stringObjectMap) throws IOException {
        for (Map.Entry<String,Object> o : stringObjectMap.entrySet()) {
            generator.writeFieldName(o.getKey());
            final Object x = o.getValue();
            writeSimpleEntity(generator,doc,x);
        }
    }

    private void writeSimpleEntity(JsonGenerator generator, JDKDocument doc, Object x) throws IOException {
        if (doc.isDictionary(x)) {
            generator.writeStartObject();
            writeSimpleMap(generator,doc,doc.getDictionary(x));
            generator.writeEndObject();
        } else if (doc.isList(x)) {
            generator.writeStartArray();
            writeSimpleList(generator,doc,doc.getList(x));
            generator.writeEndArray();
        } else if (doc.isBoolean(x)) {
            generator.writeBoolean(doc.getBoolean(x));
        } else if (doc.isInteger(x)) {
            generator.writeNumber(doc.getInt(x));
        } else if (doc.isDouble(x)) {
            generator.writeNumber(doc.getDouble(x));
        } else if (doc.isNull(x)) {
            generator.writeNull();
        } else if (doc.isString(x)) {
            generator.writeString(doc.getString(x));
        } else throw new IllegalArgumentException("Unknown type of " + x);
    }

    private void writeSimpleList(JsonGenerator generator, JDKDocument doc, List<Object> x) throws IOException {
        for (int i=0, n = x.size(); i < n; ++i) {
            writeSimpleEntity(generator,doc,x.get(i));
        }
    }

    private PrecursorIonType getFragmentIon(FragmentAnnotation<PrecursorIonType> ionPerFragment, Fragment vertex, PrecursorIonType generalIonType) {
        if (ionPerFragment==null || ionPerFragment.get(vertex)==null) return generalIonType;
        else return ionPerFragment.get(vertex);
    }

}
