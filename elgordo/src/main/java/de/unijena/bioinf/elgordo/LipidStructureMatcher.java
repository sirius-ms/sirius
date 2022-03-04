/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 * Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.elgordo;

import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.matchers.Expr;
import org.openscience.cdk.isomorphism.matchers.QueryAtom;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smarts.SmartsPattern;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class LipidStructureMatcher {

    public static void main(String[] args) {
        try {
            IAtomContainer molecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles("CCCCCCCCCCCCCCCC(=O)OCC(COP(=O)(O)OCCN)O");
            System.out.println(new LipidStructureMatcher(LipidClass.LPE, molecule).getMatchedSpecies());


            molecule = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles("CCCCCCCCCCCCCCCC(=O)OCC(COP(=O)(O)OCCN)O");
            System.out.println(new LipidStructureMatcher(LipidClass.LPE, molecule).getMatchedSpecies());

        } catch (InvalidSmilesException e) {
            e.printStackTrace();
        }
    }

    LipidClass lipidClass;
    IAtomContainer molecule;
    LipidChain[] chains;
    int[] chainLocalizations;
    boolean valid;

    private static int ACETYL_CHAIN_PROP = 1;
    private static int ALKYL_CHAIN_PROP = 2;
    private static int SPHINGOSIN_CHAIN_PROP = 3;

    public LipidStructureMatcher(LipidClass lipidClass, IAtomContainer molecule) {
        molecule = AtomContainerManipulator.removeHydrogens(molecule);
        this.lipidClass = lipidClass;
        this.molecule = molecule;
        this.chains = new LipidChain[lipidClass.chains];
        this.chainLocalizations = new int[chains.length];
        Arrays.fill(chainLocalizations, -1);
        if (lipidClass.getSmiles().isPresent()) {
            if (lipidClass.isSphingolipid()) this.valid = computeSphingosin();
            else this.valid = computeNoSphingosin();
        } else this.valid = false;
    }

    public boolean isMatched() {
        return valid;
    }

    public Optional<LipidSpecies> getMatchedSpecies() {
        return valid ? Optional.of(new LipidSpecies(lipidClass, chains)) : Optional.empty();
    }

    private boolean computeNoSphingosin() {
        String smiles = lipidClass.getSmiles().get();
        // hijack smarts
        for (int chainId=0; chainId<chains.length; ++chainId) {
            smiles = smiles.replace("R"+(chainId+1), "[C," +"#"+(101+chainId)+  "]");
        }

        final SmartsPattern pattern = SmartsPattern.create(smiles);
        final Iterator<Map<IAtom, IAtom>> iterator = pattern.matchAll(molecule).toAtomMap().iterator();
        if (!iterator.hasNext()) return false;
        eachMatch:
        while (iterator.hasNext()) {
            Arrays.fill(chainLocalizations,-1);
            final Map<IAtom, IAtom> mapping = iterator.next();
            boolean[] usedAtoms = new boolean[molecule.getAtomCount()];
            // step 1: all atoms that are part of the headgroup or the linkage atom are flagged
            for (Map.Entry<IAtom,IAtom> entry : mapping.entrySet()) {
                usedAtoms[entry.getValue().getIndex()] = true;
                QueryAtom query = (QueryAtom) entry.getKey();
                if (query.getExpression().type()== Expr.Type.OR && query.getExpression().right().type()==Expr.Type.ELEMENT) {
                    final int id = query.getExpression().right().value();
                    if (id>100) {
                        chainLocalizations[id-101] = entry.getValue().getIndex();
                    }
                }
            }
            // check if all chains are localized
            for (int i=0; i < chains.length; ++i) {
                if (chainLocalizations[i]<0) continue eachMatch;
            }
            // now follow the chains
            for (int i=0; i < chains.length; ++i) {
                int[] chainProperties = new int[]{0,0,0};
                boolean valid = followAcetylOrAlkyl(molecule.getAtom(chainLocalizations[i]), usedAtoms, chainProperties);
                if (!valid) continue eachMatch;
                chains[i] = new LipidChain(chainProperties[2]==ACETYL_CHAIN_PROP ? LipidChain.Type.ACYL : LipidChain.Type.ALKYL, chainProperties[0], chainProperties[1]);
            }
            for (boolean atom : usedAtoms) {
                if (!atom) continue eachMatch;
            }
            return true;
        }
        return false;
    }

    private boolean computeSphingosin() {
        String smiles = lipidClass.getSmiles().get();
        // hijack smarts
        smiles = smiles.replace("X", "[O,#101]");
        for (int chainId=2; chainId<chains.length; ++chainId) {
            smiles = smiles.replace("R"+(chainId+1), "[C," +"#"+(101+chainId)+  "]");
        }

        final SmartsPattern pattern = SmartsPattern.create(smiles);
        final Iterator<Map<IAtom, IAtom>> iterator = pattern.matchAll(molecule).toAtomMap().iterator();
        if (!iterator.hasNext()) return false;
        eachMatch:
        while (iterator.hasNext()) {
            Arrays.fill(chainLocalizations,-1);
            final Map<IAtom, IAtom> mapping = iterator.next();
            boolean[] usedAtoms = new boolean[molecule.getAtomCount()];
            // step 1: all atoms that are part of the headgroup or the linkage atom are flagged
            for (Map.Entry<IAtom,IAtom> entry : mapping.entrySet()) {
                usedAtoms[entry.getValue().getIndex()] = true;
                QueryAtom query = (QueryAtom) entry.getKey();
                if (query.getExpression().type()== Expr.Type.OR && query.getExpression().right().type()==Expr.Type.ELEMENT) {
                    final int id = query.getExpression().right().value();
                    if (id>100) {
                        chainLocalizations[id-101] = entry.getValue().getIndex();
                    }
                }
            }

            // check if all chains are localized. Note that one chain is linked to
            // the sphingosin and is not localized yet
            int localized=0;
            for (int i=0; i < chains.length; ++i) {
                if (chainLocalizations[i]>=0) ++localized;
            }
            if (localized < chains.length-1) continue eachMatch;
            // now follow the sphingosin chain
            {
                int[] chainProperties = new int[]{0,0,0};
                boolean valid = followSphingosin(molecule.getAtom(chainLocalizations[0]), usedAtoms, chainProperties, chainLocalizations);
                if (!valid) continue eachMatch;
                chains[0] = new LipidChain(LipidChain.Type.SPHINGOSIN, chainProperties[0], chainProperties[1]);
            }
            // then follow other chains
            for (int i=1; i < chains.length; ++i) {
                int[] chainProperties = new int[]{0,0,0};
                boolean valid = followAcetylOrAlkyl(molecule.getAtom(chainLocalizations[i]), usedAtoms, chainProperties);
                if (!valid) continue eachMatch;
                chains[i] = new LipidChain(chainProperties[2]==ACETYL_CHAIN_PROP ? LipidChain.Type.ACYL : LipidChain.Type.ALKYL, chainProperties[0], chainProperties[1]);
            }
            return true;
        }
        return false;
    }

    // O, C(N), Acetyl
    private boolean followSphingosin(IAtom atom, boolean[] usedAtoms, int[] chainProperties, int[] chainLocalizations) {
        IAtom[] a2 = new IAtom[2];
        IAtom[] a1 = new IAtom[1];
        if (!walk(atom, usedAtoms, a1, 6)) return false;
        atom = a1[0];
        if (!walk(atom, usedAtoms, a1, 6)) return false;
        atom = a1[0];
        if (!walk(atom, usedAtoms, a2, 6,7)) return false;
        IAtom nitrogen = a2[1];
        IAtom carbon = a2[0];
        if (walk(nitrogen, usedAtoms, a1, 6)) {
            chainLocalizations[1] = a1[0].getIndex();
        } else return false;
        if(!walk(carbon, usedAtoms, a2, 6,8)) return false;
        carbon = a2[0];
        chainProperties[0] += 3;
        return followAlkyl(carbon, usedAtoms, chainProperties);
    }

    private boolean walk(IAtom startingPoint, boolean[] usedAtoms, IAtom[] storage, int... expectedElements ) {
        Arrays.fill(storage,null);
        nextBond:
        for (IBond b : startingPoint.bonds()) {
            IAtom freeAtom = null;
            if (!usedAtoms[b.getAtom(0).getIndex()]) {
                freeAtom = b.getAtom(0);
            } else if (!usedAtoms[b.getAtom(1).getIndex()]) {
                freeAtom = b.getAtom(1);
            }
            if (freeAtom==null) continue;
            for (int i=0; i < storage.length; ++i) {
                if (storage[i]==null && freeAtom.getAtomicNumber() == expectedElements[i]) {
                    storage[i] = freeAtom;
                    usedAtoms[freeAtom.getIndex()]=true;
                    continue nextBond;
                }
            }
            return false;
        }
        for (IAtom a : storage) {
            if (a == null) return false;
        }
        return true;


    }

    private boolean followAcetylOrAlkyl(IAtom atom, boolean[] usedAtoms, int[] chainProperties) {
        IAtom oxygenNeighbour = null;
        IAtom carbonNeighbour = null;
        for (IBond b : atom.bonds()) {
            IAtom freeAtom = null;
            if (!usedAtoms[b.getAtom(0).getIndex()]) {
                freeAtom = b.getAtom(0);
            } else if (!usedAtoms[b.getAtom(1).getIndex()]) {
                freeAtom = b.getAtom(1);
            }
            if (freeAtom==null) continue;
            if (freeAtom.getAtomicNumber()==6) {
                if (carbonNeighbour!=null) return false;
                carbonNeighbour = freeAtom;
            }
            else if (freeAtom.getAtomicNumber()==8) {
                if (oxygenNeighbour!=null) return false;
                oxygenNeighbour = freeAtom;
            } else return false;
        }
        if (oxygenNeighbour!=null && carbonNeighbour!=null) {
            usedAtoms[oxygenNeighbour.getIndex()]=true;
            usedAtoms[carbonNeighbour.getIndex()]=true;
            chainProperties[2] = ACETYL_CHAIN_PROP;
            ++chainProperties[0];
            return followAlkyl(carbonNeighbour, usedAtoms, chainProperties);
        } else if (oxygenNeighbour==null && carbonNeighbour!=null) {
            usedAtoms[carbonNeighbour.getIndex()]=true;
            chainProperties[2] = ALKYL_CHAIN_PROP;
            ++chainProperties[0];
            return followAlkyl(carbonNeighbour, usedAtoms, chainProperties);
        }
        return false;
    }

    private boolean followAcetyl(IAtom atom, boolean[] usedAtoms, int[] chainProperties) {
        IAtom oxygenNeighbour = null;
        IAtom carbonNeighbour = null;
        for (IBond b : atom.bonds()) {
            IAtom freeAtom = null;
            if (!usedAtoms[b.getAtom(0).getIndex()]) {
                freeAtom = b.getAtom(0);
            } else if (!usedAtoms[b.getAtom(1).getIndex()]) {
                freeAtom = b.getAtom(1);
            }
            if (freeAtom==null) continue;
            if (freeAtom.getAtomicNumber()==6) {
                if (carbonNeighbour!=null) return false;
                carbonNeighbour = freeAtom;
            }
            else if (freeAtom.getAtomicNumber()==8) {
                if (oxygenNeighbour!=null) return false;
                oxygenNeighbour = freeAtom;
            } else return false;
        }
        if (oxygenNeighbour!=null && carbonNeighbour!=null) {
            usedAtoms[oxygenNeighbour.getIndex()]=true;
            usedAtoms[carbonNeighbour.getIndex()]=true;
            ++chainProperties[0];
            return followAlkyl(carbonNeighbour, usedAtoms, chainProperties);
        } else return false;
    }

    private boolean followAlkyl(IAtom atom, boolean[] usedAtoms, int[] chainProperties) {
        nextAtom:
        while (true) {
            ++chainProperties[0];
            IAtom freeAtom = null;
            for (IBond b : atom.bonds()) {
                if (!usedAtoms[b.getAtom(0).getIndex()]) {
                    if (freeAtom!=null) return false;
                    freeAtom = b.getAtom(0);
                    if (b.getOrder()== IBond.Order.DOUBLE) {
                        ++chainProperties[1];
                    }
                } else if (!usedAtoms[b.getAtom(1).getIndex()]) {
                    if (freeAtom!=null) return false;
                    freeAtom = b.getAtom(1);
                    if (b.getOrder()== IBond.Order.DOUBLE) {
                        ++chainProperties[1];
                    }
                }
            }
            if (freeAtom!=null) {
                if (freeAtom.getAtomicNumber()==6) {
                    usedAtoms[freeAtom.getIndex()]=true;
                    atom = freeAtom;
                    continue nextAtom;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

}
