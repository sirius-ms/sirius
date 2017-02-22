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
package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.IonTreeUtils;
import de.unijena.bioinf.ChemistryBase.ms.ft.RecalibrationFunction;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class IdentificationResult implements Cloneable {

    protected FTree tree, beautifulTree, resolvedBeautifulTree;
    protected MolecularFormula formula;
    protected int rank;
    protected double score;
    protected HashMap<Class<?>, Object> annotations;

    private final static Pattern NeedToEscape = Pattern.compile("[\t\n\"]");
    public static void writeIdentifications(Writer writer, Ms2Experiment input, List<IdentificationResult> results) throws IOException {
        final StringBuilder buffer = new StringBuilder();
        String name = input.getName();
        // escape quotation marks
        {
            if (name.indexOf('"') >=0) {
                name = "\"" + name.replaceAll("\"","\"\"") + "\"";
            } else if (name.indexOf('\t')>=0 || name.indexOf('\n')>=0) {
                name="\""+name+"\"";
            }
        }
        buffer.append(name);
        buffer.append('\t');
        buffer.append(input.getIonMass());
        buffer.append('\t');
        buffer.append(input.getPrecursorIonType().toString());
        for (IdentificationResult r : results) {
            buffer.append('\t');
            buffer.append(r.getMolecularFormula().toString());
            buffer.append('\t');
            buffer.append(r.getScore());
        }
        buffer.append('\n');
        writer.write(buffer.toString());
    }

    public IdentificationResult(FTree tree, int rank) {
        this.tree = tree;
        tree.normalizeStructure();
        this.score = tree==null ? 0d : tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
        this.formula = tree.getRoot().getFormula();
        this.rank = rank;

        final IonTreeUtils.Type type =tree.getAnnotationOrNull(IonTreeUtils.Type.class);
        if (type == IonTreeUtils.Type.RESOLVED) {
            this.formula = tree.getRoot().getFormula();
        } else if (type == IonTreeUtils.Type.IONIZED) {
            this.formula = tree.getAnnotationOrThrow(PrecursorIonType.class).precursorIonToNeutralMolecule(tree.getRoot().getFormula());
        } else {
            this.formula = tree.getAnnotationOrThrow(PrecursorIonType.class).measuredNeutralMoleculeToNeutralMolecule(tree.getRoot().getFormula());
        }
        this.annotations = new HashMap<>();
    }

    public int getRank() {
        return rank;
    }

    public MolecularFormula getMolecularFormula() {
        return formula;
    }

    public RecalibrationFunction getRecalibrationFunction() {
        final RecalibrationFunction f = (RecalibrationFunction) getRawTree().getAnnotations().get(RecalibrationFunction.class);
        if (f==null) return RecalibrationFunction.identity();
        else return f;
    }

    public double getScore() {
        return score;
    }

    /**
     * true if a beautiful (bigger, better explaining spectrum) tree is available
     * @return
     */
    public boolean isBeautiful(){
        return beautifulTree!=null;
    }

    public FTree getRawTree() {
        if (isBeautiful()){
            return beautifulTree;
        } else {
            return tree;
        }
    }

    public FTree getResolvedTree() {
        if (resolvedBeautifulTree==null) {
            resolvedBeautifulTree = new IonTreeUtils().treeToNeutralTree(new FTree(getRawTree()));
        }
        return resolvedBeautifulTree;
    }

    public FTree getStandardTree() {
        return tree;
    }

    public FTree getBeautifulTree() {
        return beautifulTree;
    }

    public void setBeautifulTree(FTree beautifulTree) {
        this.resolvedBeautifulTree = null;
        this.beautifulTree = beautifulTree;
        TreeScoring beautifulScoring = this.beautifulTree.getAnnotationOrThrow(TreeScoring.class);
        TreeScoring treeScoring = this.tree.getAnnotationOrThrow(TreeScoring.class);
        beautifulScoring.setBeautificationPenalty(beautifulScoring.getOverallScore()-treeScoring.getOverallScore());
        beautifulScoring.setOverallScore(treeScoring.getOverallScore());

    }

    public double getTreeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getOverallScore() - treeScore.getAdditionalScore(Sirius.ISOTOPE_SCORE);
    }

    public void writeTreeToFile(File target) throws IOException {
        final String name = target.getName();
        if (name.endsWith(".dot")) {
            new FTDotWriter().writeTreeToFile(target, getRawTree());
        } else new FTJsonWriter().writeTreeToFile(target, getRawTree());
    }

    public String getNeutralizedJSONTree() {
        final StringWriter sw = new StringWriter(1024);
        try {
            new FTJsonWriter().writeTree(sw,getResolvedTree());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public String getRawJSONTree() {
        final StringWriter sw = new StringWriter(1024);
        try {
            new FTJsonWriter().writeTree(sw,getRawTree());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public void writeAnnotatedSpectrumToFile(File target) throws IOException {
        new AnnotatedSpectrumWriter().writeFile(target, getRawTree());
    }

    public double getIsotopeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getAdditionalScore(Sirius.ISOTOPE_SCORE);
    }

    public IdentificationResult clone() {
        final IdentificationResult r = new IdentificationResult(new FTree(tree), rank);
        if (beautifulTree!=null) r.beautifulTree = new FTree(beautifulTree);
        r.score = score;
        return r;
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T ano = (T) annotations.get(klass);
        if (ano == null) throw new NullPointerException("No annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrNull(Class<T> klass) {
        return (T) annotations.get(klass);
    }

    public boolean removeAnnotation(Class<?> klass) {
        return annotations.remove(klass) != null;
    }


    public <T> boolean setAnnotation(Class<T> klass, T annotation) {
        return annotations.put(klass, annotation) == annotation;
    }


}
