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
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.RecalibrationFunction;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class IdentificationResult {

    protected FTree tree;
    protected int rank;
    protected double score;

    public IdentificationResult(FTree tree, int rank) {
        this.tree = tree;
        this.score = tree==null ? 0d : tree.getAnnotationOrThrow(TreeScoring.class).getOverallScore();
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public MolecularFormula getMolecularFormula() {
        return tree.getRoot().getFormula();
    }

    public RecalibrationFunction getRecalibrationFunction() {
        final RecalibrationFunction f = (RecalibrationFunction) tree.getAnnotations().get(RecalibrationFunction.class);
        if (f==null) return RecalibrationFunction.identity();
        else return f;
    }

    public double getScore() {
        return score;
    }

    public FTree getTree() {
        return tree;
    }

    public double getTreeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getOverallScore() - treeScore.getAdditionalScore(Sirius.ISOTOPE_SCORE);
    }

    public void writeTreeToFile(File target) throws IOException {
        final String name = target.getName();
        if (name.endsWith(".dot")) {
            new FTDotWriter().writeTreeToFile(target, tree);
        } else new FTJsonWriter().writeTreeToFile(target, tree);
    }

    public String getJSONTree() {
        final StringWriter sw = new StringWriter(1024);
        try {
            new FTJsonWriter().writeTree(sw,tree);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sw.toString();
    }

    public void writeAnnotatedSpectrumToFile(File target) throws IOException {
        new AnnotatedSpectrumWriter().writeFile(target, tree);
    }

    public double getIsotopeScore() {
        final TreeScoring treeScore = tree.getAnnotationOrThrow(TreeScoring.class);
        return treeScore.getAdditionalScore(Sirius.ISOTOPE_SCORE);
    }
}
