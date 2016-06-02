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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class FTJsonWriter {

    private DescriptorRegistry registry = DescriptorRegistry.getInstance();

    public void writeTree(Writer writer, FTree tree) throws IOException {
        try {
            writer.write(tree2json(tree).toString(4));
        } catch (JSONException e) {
            throw new IOException(e);
        } finally {
            writer.close();
        }
    }

    public void writeTreeToFile(File f, FTree tree) throws IOException {
        try (final FileWriter fileWriter = new FileWriter(f)) {
            fileWriter.write(tree2json(tree).toString(4));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    protected JSONObject tree2json(FTree tree) throws JSONException {
        final JSONDocumentType JSON = new JSONDocumentType();
        final JSONObject j = new JSONObject();
        final PrecursorIonType generalIonType = tree.getAnnotationOrNull(PrecursorIonType.class);
        final FragmentAnnotation<PrecursorIonType> ionPerFragment = tree.getFragmentAnnotationOrNull(PrecursorIonType.class);
        if (generalIonType!=null) {
            final MolecularFormula formula = tree.getRoot().getFormula();
            final PrecursorIonType fragmentIon = getFragmentIon(ionPerFragment, tree.getRoot(), generalIonType);
            final MolecularFormula neutral = fragmentIon.measuredNeutralMoleculeToNeutralMolecule(formula);
            j.put("molecularFormula", neutral.toString());
            j.put("root", formula.toString());
        } else {
            final String f = tree.getRoot().getFormula().toString();
            j.put("molecularFormula", f);
            j.put("root", f);
        }

        final JSONObject ano = new JSONObject();
        j.put("annotations", ano);

        for (Map.Entry<Class<Object>, Object> anot : tree.getAnnotations().entrySet()) {
            Descriptor<Object> d = registry.get(FTree.class, anot.getKey());
            if (d != null) {
                d.write(JSON, ano, anot.getValue());
            }
        }

        final JSONArray fragmentList = new JSONArray();
        j.put("fragments", fragmentList);

        final List<FragmentAnnotation<Object>> fragmentAnnotations = tree.getFragmentAnnotations();
        for (Fragment f : tree.getFragments()) {
            final JSONObject fragment = new JSONObject();
            fragmentList.put(fragment);
            fragment.put("id", f.getVertexId());
            fragment.put("molecularFormula", f.getFormula().toString());
            for (FragmentAnnotation<Object> fano : fragmentAnnotations) {
                if (fano.get(f)!=null) {
                    Descriptor<Object> d = registry.get(Fragment.class, fano.getAnnotationType());
                    if (d != null)
                        d.write(JSON, fragment, fano.get(f));
                }
            }
        }

        final JSONArray lossList = new JSONArray();
        j.put("losses", lossList);

        final List<LossAnnotation<Object>> lossAnnotations = tree.getLossAnnotations();
        for (Loss l : tree.losses()) {
            final JSONObject loss = new JSONObject();
            lossList.put(loss);
            loss.put("source", l.getSource().getFormula());
            loss.put("target", l.getTarget().getFormula());
            loss.put("molecularFormula", l.getFormula().toString());
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
