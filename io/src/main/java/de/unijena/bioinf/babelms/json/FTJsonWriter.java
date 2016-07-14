/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class FTJsonWriter {


    private DescriptorRegistry registry = DescriptorRegistry.getInstance();

    public String treeToJsonString(FTree tree) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(tree2json(tree));
    }

    public void writeTree(Writer writer, FTree tree) throws IOException {
        writer.write(treeToJsonString(tree));
    }

    public void writeTreeToFile(File f, FTree tree) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(f.toPath(), Charset.defaultCharset())) {
            writeTree(bw,tree);
        }
    }

    protected JsonObject tree2json(FTree tree){
        final JSONDocumentType JSON = new JSONDocumentType();
        final JsonObject j = new JsonObject();
        final PrecursorIonType generalIonType = tree.getAnnotationOrNull(PrecursorIonType.class);
        final FragmentAnnotation<PrecursorIonType> ionPerFragment = tree.getFragmentAnnotationOrNull(PrecursorIonType.class);
        if (generalIonType!=null) {
            final MolecularFormula formula = tree.getRoot().getFormula();
            final PrecursorIonType fragmentIon = getFragmentIon(ionPerFragment, tree.getRoot(), generalIonType);
            final MolecularFormula neutral = fragmentIon.measuredNeutralMoleculeToNeutralMolecule(formula);
            j.addProperty("molecularFormula", neutral.toString());
            j.addProperty("root", formula.toString());
        } else {
            final String f = tree.getRoot().getFormula().toString();
            j.addProperty("molecularFormula", f);
            j.addProperty("root", f);
        }

        final JsonObject ano = new JsonObject();
        j.add("annotations", ano);

        for (Map.Entry<Class<Object>, Object> anot : tree.getAnnotations().entrySet()) {
            Descriptor<Object> d = registry.get(FTree.class, anot.getKey());
            if (d != null) {
                d.write(JSON, ano, anot.getValue());
            }
        }

        final JsonArray fragmentList = new JsonArray();
        j.add("fragments", fragmentList);

        final List<FragmentAnnotation<Object>> fragmentAnnotations = tree.getFragmentAnnotations();
        for (Fragment f : tree.getFragments()) {
            final JsonObject fragment = new JsonObject();
            fragmentList.add(fragment);
            fragment.addProperty("id", f.getVertexId());
            fragment.addProperty("molecularFormula", f.getFormula().toString());
            for (FragmentAnnotation<Object> fano : fragmentAnnotations) {
                if (fano.get(f)!=null) {
                    Descriptor<Object> d = registry.get(Fragment.class, fano.getAnnotationType());
                    if (d != null)
                        d.write(JSON, fragment, fano.get(f));
                }
            }
        }

        final JsonArray lossList = new JsonArray();
        j.add("losses", lossList);

        final List<LossAnnotation<Object>> lossAnnotations = tree.getLossAnnotations();
        for (Loss l : tree.losses()) {
            final JsonObject loss = new JsonObject();
            lossList.add(loss);
            loss.addProperty("source", l.getSource().getFormula().toString());
            loss.addProperty("target", l.getTarget().getFormula().toString());
            loss.addProperty("molecularFormula", l.getFormula().toString());
            for (LossAnnotation<Object> lano : lossAnnotations) {
                if (lano.get(l)!=null) {
                    Descriptor<Object> d = registry.get(Loss.class, lano.getAnnotationType());
                    if (d != null)
                        d.write(JSON, loss, lano.get(l));
                }
            }
        }

        return j;
    }

    private PrecursorIonType getFragmentIon(FragmentAnnotation<PrecursorIonType> ionPerFragment, Fragment vertex, PrecursorIonType generalIonType) {
        if (ionPerFragment==null || ionPerFragment.get(vertex)==null) return generalIonType;
        else return ionPerFragment.get(vertex);
    }

}
