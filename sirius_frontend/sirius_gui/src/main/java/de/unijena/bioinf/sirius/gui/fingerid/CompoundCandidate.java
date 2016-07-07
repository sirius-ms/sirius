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

import de.unijena.bioinf.ChemistryBase.fp.*;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.generators.HighlightGenerator;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CompoundCandidate {

    private final static double THRESHOLD_FP = 0.4;

    protected Compound compound;
    protected double score;
    protected int rank,index;

    protected FingerprintAgreement agreement, missings;
    protected DatabaseLabel[] labels;
    protected int highlightedIndex=-1;
    protected boolean atomCoordinatesAreComputed=false;
    protected ReentrantLock compoundLock = new ReentrantLock();

    public CompoundCandidate(Compound compound, double score, int rank, int index) {
        this.compound = compound;
        this.score = score;
        this.rank = rank;
        this.index = index;
        if (compound==null || compound.databases==null) {
            this.labels = new DatabaseLabel[0];
        } else {
            List<DatabaseLabel> labels = new ArrayList<>();
            for (String key : compound.databases.keySet()) {
                final Collection<String> values = compound.databases.get(key);
                labels.add(new DatabaseLabel(key, values.toArray(new String[values.size()]), new Rectangle(0,0,0,0)));
            }
            this.labels = labels.toArray(new DatabaseLabel[labels.size()]);
        }
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
        return compound.fingerprint.isSet(index);
    }

    public boolean highlightFingerprint(CSIFingerIdComputation computation, int absoluteIndex) {
        final FingerprintVersion version = compound.fingerprint.getFingerprintVersion();
        final IAtomContainer molecule = compound.getMolecule();
        if (!hasFingerprintIndex(absoluteIndex)) {
            molecule.setProperty(HighlightGenerator.ID_MAP, Collections.emptyMap());
            return false;
        }
        final MolecularProperty property = version.getMolecularProperty(absoluteIndex);
        if (property instanceof SubstructureProperty) {
            final String smarts = ((SubstructureProperty)property).getSmarts();
            final HashMap<IAtom, Integer> colorMap = new HashMap<>();

            int minCount;
            if (property instanceof SubstructureCountProperty) minCount = ((SubstructureCountProperty)property).getMinimalCount();
            else minCount = 1;

            molecule.setProperty(HighlightGenerator.ID_MAP, Collections.emptyMap());
            System.err.println(smarts);
            FasterSmartsQueryTool tool = new FasterSmartsQueryTool(smarts, DefaultChemObjectBuilder.getInstance());
            try {
                if (tool.matches(molecule)) {
                    final List<List<Integer>> mappings = tool.getUniqueMatchingAtoms();
                    for (List<Integer> mapping : mappings) {
                        --minCount;
                        for (Integer i : mapping) {
                            if (!colorMap.containsKey(molecule.getAtom(i)))
                                colorMap.put(molecule.getAtom(i), minCount>=0 ? 0 : 1);

                        }
                    }
                }
            } catch (CDKException e) {
                e.printStackTrace();
            }
            molecule.setProperty(HighlightGenerator.ID_MAP, colorMap);
            return true;
        } else return false;
    }

    public FingerprintAgreement getAgreement(CSIFingerIdComputation computations, ProbabilityFingerprint prediction) {
        if (agreement==null) agreement = FingerprintAgreement.getAgreement(prediction.getFingerprintVersion(), prediction.toProbabilityArray(), compound.fingerprint.toBooleanArray(), computations.performances, 1-THRESHOLD_FP, 0.25);
        return agreement;
    }

    public FingerprintAgreement getMissings(CSIFingerIdComputation computations,  ProbabilityFingerprint prediction) {
        if (missings==null) missings = FingerprintAgreement.getMissing(prediction.getFingerprintVersion(), prediction.toProbabilityArray(), compound.fingerprint.toBooleanArray(),computations.performances,  THRESHOLD_FP);
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
