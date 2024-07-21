/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.*;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class CompoundMatchHighlighter {

    protected int[] match, unsure, nomatch;

    public CompoundMatchHighlighter(FingerprintCandidateBean compound, ProbabilityFingerprint fp) {
        compound.parseAndPrepare();
        // we have to match all fingerprints against this compound
        final IAtomContainer container = compound.getMolecule();
        final FasterSmartsQueryTool fx = new FasterSmartsQueryTool("CC", SilentChemObjectBuilder.getInstance());
        final Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
        map.defaultReturnValue(0d);
        final Int2DoubleOpenHashMap unsureMap = new Int2DoubleOpenHashMap();
        map.defaultReturnValue(0d);
        try {
            for (FPIter mp : compound.getCandidateFingerprint().presentFingerprints()) {
                final double probability = fp.getProbability(mp.getIndex());
                final double weight;
                if (probability >= 0.5) {
                    weight = probability;
                } else if (probability < 0.05) {
                    weight = -(1d - probability);
                } else continue;
                final MolecularProperty property = mp.getMolecularProperty();
                if (property instanceof SubstructureProperty) {
                    final SubstructureProperty p = (SubstructureProperty) property;
                    final String smarts = p.getSmarts();
                    fx.setSmarts(smarts);
                    if (fx.matches(container)) {
                        final List<List<Integer>> matches = fx.getUniqueMatchingAtoms();
                        if (matches.size() <= 3) {
                            final double downscale = matches.size() * matches.size();
                            for (List<Integer> atoms : matches) {
                                for (int i : atoms) {
                                    map.addTo(i,weight / downscale);
//                                    map.adjustOrPutValue(i, weight / downscale, weight / downscale);
                                    if (weight < 0)
                                        unsureMap.addTo(i,weight / downscale);
//                                        unsureMap.adjustOrPutValue(i, weight / downscale, weight / downscale);
                                }
                            }
                        }
                    }
                } else if (property instanceof ExtendedConnectivityProperty) {
                    final int hash = ((ExtendedConnectivityProperty) property).getHash();
                    int index = Arrays.binarySearch(compound.ecfpHashs, hash);
                    if (index >= 0) {
                        int n = 0;
                        for (int i = index; i < compound.ecfpHashs.length && compound.ecfpHashs[i] == hash; ++i) {
                            ++n;
                        }
                        if (n <= 3) {
                            double score = weight / (n * n);
                            for (int i = index; i < compound.ecfpHashs.length && compound.ecfpHashs[i] == hash; ++i) {
                                for (int atom : compound.relevantFps[index].atoms) {
                                    map.addTo(atom,score);
//                                    map.adjustOrPutValue(atom, score, score);
                                    if (weight < 0)
                                        unsureMap.addTo(atom,score);
//                                        unsureMap.adjustOrPutValue(atom, score, score);
                                }
                            }
                        }
                    }
                }
            }
            final IntArrayList sure = new IntArrayList(), unsure = new IntArrayList(), no = new IntArrayList();
            map.keySet().forEach(a -> {
                double b = map.get(a);
                if (unsureMap.get(a) <= -1) {
                    if (b >= 1) unsure.add(a);
                    else if (b <= -1) no.add(a);
                } else if (b >= 1) {
                    sure.add(a);
                }
            });

            this.match = sure.toIntArray();
            this.nomatch = no.toIntArray();
            this.unsure = unsure.toIntArray();
            System.out.println(map);

        } catch (CDKException e) {
            LoggerFactory.getLogger(getClass()).error("Error: ", e);
        }
    }

    public void hightlight(FingerprintCandidateBean compound) {
        final IAtomContainer molecule = compound.getMolecule();
        for (IAtom atom : molecule.atoms()) atom.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        for (IBond bond : molecule.bonds()) bond.removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
        highlight(molecule, match, CandidateListDetailView.PRIMARY_HIGHLIGHTED_COLOR);
        highlight(molecule, unsure, CandidateListDetailView.INVERT_HIGHLIGHTED_COLOR2);
        highlight(molecule, nomatch, CandidateListDetailView.INVERT_HIGHLIGHTED_COLOR);
    }

    private void highlight(IAtomContainer molecule, int[] mapping, Color primaryHighlightedColor) {
        final HashSet<IAtom> atoms = new HashSet<>(mapping.length);
        for (int i : mapping) atoms.add(molecule.getAtom(i));
        for (int i : mapping) {
            if (molecule.getAtom(i).getProperty(StandardGenerator.HIGHLIGHT_COLOR) == null)
                molecule.getAtom(i).setProperty(StandardGenerator.HIGHLIGHT_COLOR, primaryHighlightedColor);
            for (IBond b : molecule.getConnectedBondsList(molecule.getAtom(i))) {
                if (atoms.contains(b.getAtom(0)) && atoms.contains(b.getAtom(1))) {
                    if (b.getProperty(StandardGenerator.HIGHLIGHT_COLOR) == null)
                        b.setProperty(StandardGenerator.HIGHLIGHT_COLOR, primaryHighlightedColor);
                }
            }

        }
    }


}
