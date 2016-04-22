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

package de.unijena.bioinf.sirius.gui.fingerid;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.generators.HighlightGenerator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CompoundCandidate {

    private final static double THRESHOLD_FP = 1d/3d;

    protected Compound compound;
    protected double score;
    protected int rank,index;

    protected FingerprintAgreement agreement, missings;
    protected int highlightedIndex=-1;
    protected boolean atomCoordinatesAreComputed=false;
    protected ReentrantLock compoundLock = new ReentrantLock();

    public CompoundCandidate(Compound compound, double score, int rank, int index) {
        this.compound = compound;
        this.score = score;
        this.rank = rank;
        this.index = index;
    }

    public boolean computeAtomCoordinates() {
        if (atomCoordinatesAreComputed) return false;
        try {
            StructureDiagramGenerator sdg = new StructureDiagramGenerator();
            sdg.setMolecule(compound.getMolecule(), false);
            sdg.generateCoordinates();
        } catch (CDKException e) {
            e.printStackTrace();
            return false;
        }
        atomCoordinatesAreComputed = true;
        return true;
    }

    public boolean hasFingerprintIndex(int index) {
        return compound.fingerprint[index];
    }

    public boolean highlightFingerprint(CSIFingerIdComputation computation, int index) {
        final IAtomContainer molecule = compound.getMolecule();
        if (!hasFingerprintIndex(index)) {
            molecule.setProperty(HighlightGenerator.ID_MAP, Collections.emptyMap());
            return false;
        }

        final HashMap<IAtom, Integer> colorMap = new HashMap<>();
        if (computation.relativeIndex2Smarts[index]==null || computation.relativeIndex2Smarts[index].isEmpty()) {
            molecule.setProperty(HighlightGenerator.ID_MAP, Collections.emptyMap());
            return false;
        }
        FasterSmartsQueryTool tool = new FasterSmartsQueryTool(computation.relativeIndex2Smarts[index], DefaultChemObjectBuilder.getInstance());
        try {
            if (tool.matches(molecule)) {
                final List<List<Integer>> mappings = tool.getUniqueMatchingAtoms();
                int k=0;
                for (List<Integer> mapping : mappings) {
                    for (Integer i : mapping) {
                        if (!colorMap.containsKey(molecule.getAtom(i)))
                            colorMap.put(molecule.getAtom(i), Math.min(1,k));

                    }
                    ++k;
                }
            }
        } catch (CDKException e) {
            e.printStackTrace();
        }
        molecule.setProperty(HighlightGenerator.ID_MAP, colorMap);
        return true;
    }

    public FingerprintAgreement getAgreement(CSIFingerIdComputation computations, double[] prediction) {
        if (agreement==null) agreement = FingerprintAgreement.getAgreement(prediction, compound.fingerprint, computations.getFScores(), 1-THRESHOLD_FP);
        return agreement;
    }

    public FingerprintAgreement getMissings(CSIFingerIdComputation computations,  double[] prediction) {
        if (missings==null) missings = FingerprintAgreement.getMissing(prediction, compound.fingerprint,computations.getFScores(),  THRESHOLD_FP);
        return missings;
    }


    public void parseAndPrepare() {
        try {
            System.out.println("parse and prepare " + compound.inchi.in2D);
            // we do not want to search anything in the compound but just "enforce initialization" of the molecule
            final FasterSmartsQueryTool tool = new FasterSmartsQueryTool("Br", DefaultChemObjectBuilder.getInstance());
            tool.matches(compound.getMolecule());
        } catch (CDKException e) {
            e.printStackTrace();
        }

    }
}
