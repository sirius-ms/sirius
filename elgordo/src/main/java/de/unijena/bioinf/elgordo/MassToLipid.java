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

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MassToLipid {

    private final MassToFormulaDecomposer cho, chno, chnops;
    private final PrecursorIonType[] possibleIonTypes;

    private final FormulaConstraints chainConstraints;
    private final MolecularFormula SPHINGOSIN_HEAD;
    private final Deviation deviation;

    public MassToLipid(Deviation deviation) {
        this.deviation = deviation;
        this.cho = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHO").elementArray()));
        this.chno = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNO").elementArray()));
        this.chnops = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNOPS").elementArray()));
        this.chainConstraints = new FormulaConstraints("C[1-]H[2-]N[0]O[0-]");

        this.possibleIonTypes = new PrecursorIonType[]{
                PrecursorIonType.fromString("[M+H]+"),
                PrecursorIonType.fromString("[M+Na]+"),
                PrecursorIonType.fromString("[M+NH3+H]+")};
        this.SPHINGOSIN_HEAD = new LipidChain(LipidChain.Type.SPHINGOSIN, 5, 1).getFormula();
    }

    public <T extends Spectrum<Peak>> AnnotatedLipidSpectrum<T> annotateSpectrum(LipidCandidate candidate, T spectrum) {
        final ArrayList<LipidAnnotation> annotations = new ArrayList<>();
        final TIntArrayList indizes = new TIntArrayList();
        search(annotations,indizes, spectrum,candidate.ionMass, ()->new PrecursorAnnotation(candidate.lipidFormula, candidate.ionType));
        FragmentLib.FragmentSet set = candidate.getFragmentSet();
        for (MolecularFormula f : set.fragments) {
            search(annotations, indizes, spectrum, candidate.ionType.addIonAndAdduct(f.getMass()), () -> new HeadGroupFragmentAnnotation(
                    LipidAnnotation.Target.FRAGMENT, f, f, candidate.ionType, candidate.possibleClass.headgroup
            ));
            if (!candidate.ionType.isPlainProtonationOrDeprotonation()) {
                // consider adduct switch
                search(annotations, indizes, spectrum, adductSwitch(candidate,candidate.ionType.addIonAndAdduct(f.getMass()),false), () -> new HeadGroupFragmentAnnotation(
                        LipidAnnotation.Target.FRAGMENT, f, adductSwitch(candidate, f, false).getCandidate(), adductSwitch(candidate.ionType), candidate.possibleClass.headgroup
                ));
            }
        }
        for (MolecularFormula f : set.losses) {
            if (!f.isEmpty()) {
                search(annotations, indizes, spectrum, candidate.ionMass - f.getMass(), () -> new HeadGroupFragmentAnnotation(
                        LipidAnnotation.Target.LOSS, f, candidate.lipidFormula.subtract(f), candidate.ionType, candidate.possibleClass.headgroup
                ));
            }
            if (!candidate.ionType.isPlainProtonationOrDeprotonation()) {
                // consider adduct switch
                search(annotations, indizes, spectrum, adductSwitch(candidate,candidate.ionMass - f.getMass(),true), () -> new HeadGroupFragmentAnnotation(
                        LipidAnnotation.Target.LOSS, f, candidate.lipidFormula.subtract(f), adductSwitch(candidate.ionType), candidate.possibleClass.headgroup
                ));
            }
        }

        {
            // todo: replace annotation/index array from the beginning
            final HashSet<IndexedPeak> peaks = new HashSet<>();
            for (int k=0; k < annotations.size(); ++k) {
                final int i = indizes.get(k);
                final LipidAnnotation a = annotations.get(k);
                final IndexedPeak e = new IndexedPeak(i, spectrum.getMzAt(i), spectrum.getIntensityAt(i),
                        a.getMeasuredPeakFormula(),
                        annotations.get(k)
                        );
                peaks.add(e);
            }

            final FragmentMap map = new FragmentMap(spectrum, candidate.lipidFormula, chnops, candidate.ionType);
            final List<List<LipidTreeNode>> bestChains;
            if (candidate.sphingosinChains > 0) {
                final List<LipidTreeNode> nodes = searchForSphingosin(spectrum, candidate, map, set);
                bestChains = nodes.isEmpty() ? new ArrayList<>() : new ArrayList<>(Collections.singleton(nodes));
            } else {
                final LipidTreeNode root = makeRoot(candidate,spectrum,map,set,
                        peaks.stream().filter(x->x.annotation instanceof HeadGroupFragmentAnnotation && x.annotation.getTarget()== LipidAnnotation.Target.LOSS).collect(Collectors.toList())
                        //  Collections.emptyList()
                );
                expand(candidate,spectrum,map,set,root);
                bestChains = findBestAnnotation(candidate, root);
            }
            final Set<LipidChain> chains = new HashSet<>();
            if (!bestChains.isEmpty()) {
                for (List<LipidTreeNode> annos : bestChains) {
                    for (LipidTreeNode node : annos) {
                        peaks.addAll(node.peaks);
                        chains.add(node.candidate);
                    }
                }
            }
            final IndexedPeak[] sorted = peaks.stream().sorted(Comparator.comparingInt(x->x.index)).toArray(IndexedPeak[]::new);
            final int[] peakindizes = new TIntHashSet(Arrays.stream(sorted).mapToInt(x->x.index).toArray()).toArray();
            Arrays.sort(peakindizes);
            LipidAnnotation[][] finalAnnotations = new LipidAnnotation[peakindizes.length][];
            {
                final TIntIntHashMap indexmap = new TIntIntHashMap();
                for (int i=0; i < peakindizes.length; ++i) indexmap.put(peakindizes[i], i);
                int[] lengths = new int[peakindizes.length];
                for (IndexedPeak p : sorted) ++lengths[indexmap.get(p.index)];
                for (int i=0; i < finalAnnotations.length; ++i) finalAnnotations[i] = new LipidAnnotation[lengths[i]];
                Arrays.fill(lengths, 0);
                for (int i=0; i < sorted.length; ++i) {
                    final int index = indexmap.get(sorted[i].index);
                    finalAnnotations[index][lengths[index]++] = sorted[i].annotation;
                }
            }

            return new AnnotatedLipidSpectrum<>(spectrum, candidate.lipidFormula, candidate.ionMass,candidate.ionType, new LipidSpecies(candidate.possibleClass,
                    bestChains.stream().map(x->x.get(0).candidate).toArray(LipidChain[]::new)),
                    finalAnnotations, peakindizes
                    );
        }
    }

    private PrecursorIonType adductSwitch(PrecursorIonType ionType) {
        if (ionType.equals(Sodium)) return Proton;
        else if (!ionType.getAdduct().isEmpty()) return ionType.withoutAdduct();
        else return ionType;
    }

    private <T extends Spectrum<Peak>> void annotateCarboHydrogens(T spectrum, LipidCandidate candidate, LipidChainCandidate lipidChainCandidate, ArrayList<LipidAnnotation> annotations, TIntArrayList indizes) {
        final TIntHashSet alreadyAnnotated = new TIntHashSet(indizes);
        nextPeak:
        for (int k = 0; k < spectrum.size(); ++k) {
            if (!alreadyAnnotated.contains(k)) {
                // TODO: adduct switch
                final List<MolecularFormula> molecularFormulas = cho.decomposeNeutralMassToFormulas(candidate.ionType.subtractIonAndAdduct(spectrum.getMzAt(k)), deviation);
                if (!candidate.ionType.isPlainProtonationOrDeprotonation()) {
                    molecularFormulas.addAll(cho.decomposeNeutralMassToFormulas(adductSwitch(candidate, candidate.ionType.subtractIonAndAdduct(spectrum.getMzAt(k)),false), deviation));
                }
                for (MolecularFormula f : molecularFormulas) {
                    if (f.numberOfHydrogens() % 2 != 0) continue;
                    for (LipidChain c : lipidChainCandidate.chains) {
                        if (c.getFormula().isSubtractable(f) && LipidChain.fromFormula(f).map(x -> x.numberOfDoubleBonds).orElse(Integer.MAX_VALUE) <= c.numberOfDoubleBonds) {
                            annotations.add(new ChainFragmentAnnotation(LipidAnnotation.Target.FRAGMENT, f, f, candidate.ionType));
                            indizes.add(k);
                            continue nextPeak;
                        }
                    }
                }


            }
        }
    }


    private <T extends Spectrum<Peak>> void annotateCarboHydrogens(T spectrum, LipidCandidate candidate, Set<LipidChain> chains, Set<IndexedPeak> peaks) {
        final TIntHashSet alreadyAnnotated = new TIntHashSet(peaks.stream().mapToInt(x->x.index).toArray());
        nextPeak:
        for (int k = 0; k < spectrum.size(); ++k) {
            if (!alreadyAnnotated.contains(k)) {
                // TODO: adduct switch
                final List<MolecularFormula> molecularFormulas = cho.decomposeNeutralMassToFormulas(candidate.ionType.subtractIonAndAdduct(spectrum.getMzAt(k)), deviation);
                if (!candidate.ionType.isPlainProtonationOrDeprotonation()) {
                    molecularFormulas.addAll(cho.decomposeNeutralMassToFormulas(adductSwitch(candidate, candidate.ionType.subtractIonAndAdduct(spectrum.getMzAt(k)),false), deviation));
                }
                for (MolecularFormula f : molecularFormulas) {
                    if (f.numberOfHydrogens() % 2 != 0) continue;
                    for (LipidChain c : chains) {
                        if (c.getFormula().isSubtractable(f) && LipidChain.fromFormula(f).map(x -> x.numberOfDoubleBonds).orElse(Integer.MAX_VALUE) <= c.numberOfDoubleBonds) {
                            final IndexedPeak p = new IndexedPeak(k, spectrum.getMzAt(k), spectrum.getIntensityAt(k), f, new ChainFragmentAnnotation(LipidAnnotation.Target.FRAGMENT, f, f, candidate.ionType));
                            continue nextPeak;
                        }
                    }
                }


            }
        }
    }

    private static class LipidChainCandidate {
        private final TIntArrayList peakIndizes;
        private final ArrayList<LipidAnnotation> annotations;
        private final List<LipidChain> chains;
        final boolean complete;

        public LipidChainCandidate(TIntArrayList peakIndizes, ArrayList<LipidAnnotation> annotations, List<LipidChain> chains, boolean complete) {
            this.peakIndizes = peakIndizes;
            this.annotations = annotations;
            this.chains = chains;
            this.complete = complete;
            Collections.sort(chains);
        }

        public LipidChainCandidate copy() {
            return new LipidChainCandidate(new TIntArrayList(peakIndizes), new ArrayList<>(annotations), new ArrayList<>(chains), complete);
        }

        public void combine(LipidChainCandidate bestSubL) {
            peakIndizes.addAll(bestSubL.peakIndizes);
            annotations.addAll(bestSubL.annotations);
            chains.addAll(bestSubL.chains);
            Collections.sort(chains);
        }

        public boolean isMergeable(LipidChainCandidate c) {
            return chains.equals(c.chains);
        }

        public void merge(LipidChainCandidate c) {
            final TIntHashSet alreadyKnown = new TIntHashSet(peakIndizes);
            for (int k = 0; k < c.peakIndizes.size(); ++k) {
                if (alreadyKnown.add(c.peakIndizes.getQuick(k))) {
                    peakIndizes.add(c.peakIndizes.getQuick(k));
                    annotations.add(c.annotations.get(k));
                }
            }
        }
    }

    private <T extends Spectrum<Peak>> LipidChainCandidate searchChains(T spectrum, LipidCandidate candidate, FragmentLib.FragmentSet lib) {
        if (candidate.alkylChains+candidate.acylChains+candidate.sphingosinChains==1) {
            final Optional<LipidChain> lipidChain = LipidChain.fromFormula(candidate.chainFormula);
            return lipidChain.map(x->annotateLipidChain(spectrum,candidate,lib,x,candidate.ionMass)).orElse(null);
        }
        return searchChains(spectrum, candidate, lib, candidate.alkylChains, candidate.acylChains, candidate.ionMass, MolecularFormula.emptyFormula());
    }

    private <T extends Spectrum<Peak>> LipidChainCandidate searchChains(T spectrum, LipidCandidate candidate, FragmentLib.FragmentSet lib, int nalkyl, int nacyl, double precursor, MolecularFormula chainFormulaSoFar) {
        // alle möglichen chains enumerieren
        List<LipidChain> chains = new ArrayList<>(findAllChains(spectrum, candidate, lib, nacyl, nalkyl, precursor));
        // für jede Chain: rufe search chain rekursiv auf, gib Anzahl
        // erklärter peaks zurück
        LipidChainCandidate bestChainSoFar = null;
        int bestN = 0;
        for (LipidChain chain : chains) {
            int acylNow = nacyl;
            int alkylNow = nalkyl;
            if (chain.type == LipidChain.Type.ALKYL) --alkylNow;
            else if (chain.type == LipidChain.Type.ACYL) --acylNow;
            if (acylNow + alkylNow > 1) {
                // we have to do a recursive call
                final MolecularFormula[] modifs = chain.type == LipidChain.Type.ACYL ? lib.acylLosses : lib.alkylLosses;
                LipidChainCandidate bestSubL = null;
                for (MolecularFormula m : modifs) {
                    final LipidChainCandidate best = searchChains(spectrum, candidate, lib, alkylNow, acylNow, precursor - chain.getFormula().getMass() - m.getMass(), chain.formula);
                    if (best==null) continue;
                    if (bestSubL == null || best.annotations.size() > bestSubL.annotations.size()) bestSubL = best;
                }
                if (bestSubL == null) continue;
                LipidChainCandidate c = annotateLipidChain(spectrum, candidate, lib, chain, precursor);
                if (bestChainSoFar == null || c.annotations.size() * bestSubL.annotations.size() >= bestN) {
                    bestN = c.annotations.size() * bestSubL.annotations.size();
                    c.combine(bestSubL);
                    Collections.sort(c.chains);
                    if (bestChainSoFar!=null && bestChainSoFar.isMergeable(c)) {
                        c.merge(bestChainSoFar);
                    }
                    bestChainSoFar = c;
                }
            } else {
                LipidChainCandidate c = annotateLipidChain(spectrum, candidate, lib, chain, precursor);
                MolecularFormula f = chainFormulaSoFar;
                for (LipidChain lchain : c.chains) f = f.add(lchain.formula);
                if (acylNow == 1) {
                    Optional<LipidChain> d = LipidChain.fromFormula(candidate.chainFormula.subtract(f));
                    if (!d.isPresent() || d.get().type != LipidChain.Type.ACYL) {
                        continue;
                    } else {
                        c.chains.add(d.get());
                    }
                } else if (alkylNow == 1) {
                    Optional<LipidChain> d = LipidChain.fromFormula(candidate.chainFormula.subtract(f));
                    if (!d.isPresent() || d.get().type != LipidChain.Type.ALKYL) {
                        continue;
                    } else {
                        c.chains.add(d.get());
                    }
                }
                Collections.sort(c.chains);
                // is chain compatible to previous one
                if (bestChainSoFar != null && bestChainSoFar.isMergeable(c)) {
                    bestChainSoFar.merge(c);
                } else if (bestChainSoFar == null || c.annotations.size() >= bestN) {
                    bestChainSoFar = c;
                    bestN = bestChainSoFar.annotations.size();
                }
            }
        }
        return bestChainSoFar;
    }

    private <T extends Spectrum<Peak>> LipidChainCandidate annotateLipidChain(T spectrum, LipidCandidate candidate, FragmentLib.FragmentSet lib, LipidChain chain, double precursor) {
        final LipidChainCandidate C = new LipidChainCandidate(new TIntArrayList(), new ArrayList<>(), new ArrayList<>(), false);
        C.chains.add(chain);
        // losses
        boolean[] adductSwitch = ( lib.isAdductSwitch() && !candidate.ionType.isPlainProtonationOrDeprotonation()) ? new boolean[]{true,false} : new boolean[]{false};
        {
            final MolecularFormula[] modifs = chain.type == LipidChain.Type.ACYL ? lib.acylLosses : lib.alkylLosses;
            for (MolecularFormula formula : modifs) {
                for (boolean aswitch : adductSwitch) {
                    double mz = precursor - (formula.getMass() + chain.getFormula().getMass());
                    if (aswitch) mz = adductSwitch(candidate, mz, true);
                    final int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, deviation);
                    if (i >= 0) {
                        C.peakIndizes.add(i);
                        final MolecularFormula loss = chain.getFormula().add(formula);
                        C.annotations.add(new ChainAnnotation(LipidAnnotation.Target.LOSS, loss, aswitch ? adductSwitch(candidate, loss, true).getCandidate() : candidate.lipidFormula.subtract(loss), aswitch ? adductSwitch(candidate.ionType) : candidate.ionType, chain));
                    }
                }
            }
        }
        // fragments
        {
            final MolecularFormula[] modifs = chain.type == LipidChain.Type.ACYL ? lib.acylFragments : lib.alkylFragments;
            for (MolecularFormula formula : modifs) {
                for (boolean aswitch : adductSwitch) {
                    double mz = candidate.ionType.addIonAndAdduct(formula.getMass() + chain.getFormula().getMass());
                    if (aswitch) mz = adductSwitch(candidate,mz,false);
                    final int i = Spectrums.mostIntensivePeakWithin(spectrum, mz, deviation);
                    if (i >= 0) {
                        C.peakIndizes.add(i);
                        final MolecularFormula loss = chain.getFormula().add(formula);
                        C.annotations.add(new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, loss, aswitch ? adductSwitch(candidate,loss, true).getCandidate() : candidate.lipidFormula.subtract(loss), aswitch ? adductSwitch(candidate.ionType) : candidate.ionType, chain));
                    }
                }
            }
        }
        return C;
    }

    private <T extends Spectrum<Peak>> Set<LipidChain> findAllChains(T spectrum, LipidCandidate candidate, FragmentLib.FragmentSet lib, int nacyl, int nalkyl, double precursor) {
        final HashSet<LipidChain> chains = new HashSet<>();
        for (int k = 0; k < spectrum.size(); ++k) {
            final double mz = spectrum.getMzAt(k);
            if (mz >= precursor) break;
            final List<MolecularFormula> molecularFormulas = cho.decomposeNeutralMassToFormulas(precursor - mz, deviation);
            if (lib.isAdductSwitch() && !candidate.ionType.isPlainProtonationOrDeprotonation()) {
                molecularFormulas.addAll(cho.decomposeNeutralMassToFormulas(adductSwitch(candidate, precursor - mz, true), deviation));
            }
            for (MolecularFormula formula : molecularFormulas) {
                // acyl
                if (nacyl > 0) {
                    for (MolecularFormula modification : lib.acylLosses) {
                        MolecularFormula loss = formula.subtract(modification);
                        if (!loss.isAllPositiveOrZero()) continue;
                        Optional<LipidChain> chain = LipidChain.fromFormula(loss);
                        if (chain.isPresent() && chain.get().type == LipidChain.Type.ACYL) {
                            chains.add(chain.get());
                        }
                    }
                }
                // alkyl
                if (nalkyl > 0) {
                    for (MolecularFormula modification : lib.alkylLosses) {
                        MolecularFormula loss = formula.subtract(modification);
                        if (!loss.isAllPositiveOrZero()) continue;
                        Optional<LipidChain> chain = LipidChain.fromFormula(loss);
                        if (chain.isPresent() && chain.get().type == LipidChain.Type.ALKYL) {
                            chains.add(chain.get());
                        }
                    }
                }
            }
            final List<MolecularFormula> fragmentFormulas = cho.decomposeNeutralMassToFormulas(candidate.ionType.subtractIonAndAdduct(mz), deviation);
            if (lib.isAdductSwitch() && !candidate.ionType.isPlainProtonationOrDeprotonation()) {
                fragmentFormulas.addAll(cho.decomposeNeutralMassToFormulas(adductSwitch(candidate, mz, false), deviation));
            }
            for (MolecularFormula formula : fragmentFormulas) {
                // acyl
                if (nacyl > 0) {
                    for (MolecularFormula modification : lib.acylFragments) {
                        MolecularFormula loss = formula.subtract(modification);
                        if (!loss.isAllPositiveOrZero()) continue;
                        Optional<LipidChain> chain = LipidChain.fromFormula(loss);
                        if (chain.isPresent() && chain.get().type == LipidChain.Type.ACYL) {
                            chains.add(chain.get());
                        }
                    }
                }
                // alkyl
                if (nalkyl > 0) {
                    for (MolecularFormula modification : lib.alkylFragments) {
                        MolecularFormula loss = formula.subtract(modification);
                        if (!loss.isAllPositiveOrZero()) continue;
                        Optional<LipidChain> chain = LipidChain.fromFormula(loss);
                        if (chain.isPresent() && chain.get().type == LipidChain.Type.ALKYL) {
                            chains.add(chain.get());
                        }
                    }
                }
            }
        }
        return chains;
    }


    private final static PrecursorIonType Sodium = PrecursorIonType.getPrecursorIonType("[M+Na]+"), Proton = PrecursorIonType.getPrecursorIonType("[M+H]+");

    private double adductSwitch(LipidCandidate candidate, double mz, boolean isLoss) {
        if (candidate.ionType.equals(Sodium)) {
            return mz - Sodium.getModificationMass() + Proton.getModificationMass();
        } else if (!candidate.ionType.getAdduct().isEmpty()) {
            return mz - candidate.ionType.getAdduct().getMass();
        } else return mz;
    }
    private Decomposition adductSwitch(LipidCandidate candidate, MolecularFormula formula, boolean isLoss) {
        if (isLoss) {
               if (candidate.ionType.getAdduct().isEmpty()) {
                   return new Decomposition(candidate.lipidFormula.subtract(formula), candidate.ionType.equals(Sodium) ? PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization() : candidate.ionType.getIonization(), 0d);
               } else {
                   return new Decomposition(candidate.lipidFormula.subtract(formula.add(candidate.ionType.getAdduct())), candidate.ionType.getIonization(), 0d);
               }
        }
            if (candidate.ionType.equals(Sodium)) {
                return new Decomposition(formula, PrecursorIonType.getPrecursorIonType("[M+H]+").getIonization(), 0d);
            } else if (!candidate.ionType.getAdduct().isEmpty()) {
                return new Decomposition(formula.subtract(candidate.ionType.getAdduct()), candidate.ionType.getIonization(), 0d);
            } else return new Decomposition(formula, candidate.ionType.getIonization(), 0d);
    }

    private <T extends Spectrum<Peak>> void search(ArrayList<LipidAnnotation> annotations, TIntArrayList indizes, T spectrum, double peakMass, Supplier<LipidAnnotation> supl) {
        final int i = Spectrums.mostIntensivePeakWithin(spectrum, peakMass, deviation);
        if (i >= 0) {
            annotations.add(supl.get());
            indizes.add(i);
        }
    }

    public List<LipidCandidate> analyzePrecursor(final double precursorMass) {
        final ArrayList<LipidCandidate> candidates = new ArrayList<>();
        for (HeadGroup group : LipidClass.getHeadGroups()) {
            final LipidClass[] classes = LipidClass.getClassesFor(group);
            final boolean maybeSphingosin = Arrays.stream(classes).anyMatch(x->x.isSphingolipid());
            final int maxNumberOfChains = Arrays.stream(classes).mapToInt(x -> x.chains).max().orElse(0);
            for (PrecursorIonType ionType : possibleIonTypes) {
                double remainingMass = ionType.subtractIonAndAdduct(precursorMass) - group.molecularFormula.getMass();
                int sphingosinChains = 0;
                if (maybeSphingosin) {
                    // subtract sphingosin head
                    remainingMass -= SPHINGOSIN_HEAD.getMass();
                    if (remainingMass <= 0) continue;
                    sphingosinChains = 1;
                }
                if (remainingMass>12){
                    List<MolecularFormula> chains = cho.decomposeNeutralMassToFormulas(remainingMass, deviation.absoluteFor(precursorMass), chainConstraints);
                    for (MolecularFormula formula : chains) {
                        if (formula.numberOfHydrogens() % 2 != 0 || formula.numberOfCarbons() < 2 || (sphingosinChains == 0 && formula.numberOfHydrogens() > (formula.numberOfCarbons() * 2)))
                            continue;
                        // add lipid candidate
                        int numberOfAcylChains = formula.numberOfOxygens();
                        for (LipidClass c : classes) {
                            int numberOfAlkylChains = c.chains - sphingosinChains - numberOfAcylChains;
                            if (numberOfAlkylChains >= 0) {
                                final MolecularFormula chainFormula = sphingosinChains > 0 ? formula.add(SPHINGOSIN_HEAD) : formula;
                                candidates.add(new LipidCandidate(c, ionType, precursorMass, chainFormula.add(group.molecularFormula), chainFormula, numberOfAlkylChains, numberOfAcylChains, sphingosinChains));
                            }
                        }
                    }

                }
            }
        }
        return candidates;
    }

    public static class LipidCandidate {
        public final double ionMass;
        public final LipidClass possibleClass;
        public final int alkylChains, acylChains, sphingosinChains;
        public final MolecularFormula chainFormula, lipidFormula;
        public final PrecursorIonType ionType;

        public LipidCandidate(LipidClass possibleClass, PrecursorIonType ionType, double ionMass, MolecularFormula lipidFormula, MolecularFormula chainFormula, int alkylChains, int acylChains, int sphingosinChains) {
            this.ionMass = ionMass;
            this.lipidFormula = lipidFormula;
            this.possibleClass = possibleClass;
            this.alkylChains = alkylChains;
            this.acylChains = acylChains;
            this.sphingosinChains = sphingosinChains;
            this.ionType = ionType;
            this.chainFormula = chainFormula;
        }

        public String toString() {
            return possibleClass.abbr() + " (" + ionType.toString() + ")";
        }

        public FragmentLib.FragmentSet getFragmentSet() {
            return possibleClass.fragmentLib == null ? FragmentLib.FragmentSet.empty() : possibleClass.fragmentLib.getFor(ionType).orElseGet(FragmentLib.FragmentSet::empty);
        }
    }


    static class IndexedPeak implements Peak {

        private final double mass, intensity;
        private final int index;
        private LipidAnnotation annotation;
        private MolecularFormula formula;

        public IndexedPeak(int index, double mass, double intensity, MolecularFormula formula, LipidAnnotation annotation) {
            this.mass = mass;
            this.intensity = intensity;
            this.index = index;
            this.formula = formula;
            this.annotation = annotation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IndexedPeak that = (IndexedPeak) o;
            return index == that.index && Objects.equals(annotation, that.annotation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, annotation);
        }

        @Override
        public double getMass() {
            return mass;
        }

        @Override
        public double getIntensity() {
            return intensity;
        }
    }

    private <T extends Spectrum<Peak>> LipidTreeNode makeRoot(LipidCandidate c,T spectrum, FragmentMap fragmentMap, FragmentLib.FragmentSet library, List<IndexedPeak> headGroupLosses) {
        final LipidTreeNode lipidTreeNode = new LipidTreeNode(null, c.chainFormula, 0, new OpenChains(c));
        IndexedPeak parentPeak = new IndexedPeak(-1, c.ionMass, 0d, c.lipidFormula, new PrecursorAnnotation(c.lipidFormula, c.ionType));
        lipidTreeNode.peaks.add(parentPeak);
        lipidTreeNode.peaks.addAll(headGroupLosses);
        return lipidTreeNode;
    }

    private static class OpenChains {
        int remainingAcyl, remainingAlkyl;

        public OpenChains(LipidCandidate c) {
            final FragmentLib.FragmentSet fragmentSet = c.getFragmentSet();
            if (fragmentSet.alkylFragments.length+fragmentSet.alkylLosses.length <= 0) {
                this.remainingAlkyl = 0;
            } else {
                this.remainingAlkyl = c.alkylChains;
            }
            if (fragmentSet.acylFragments.length+fragmentSet.acylLosses.length <= 0) {
                this.remainingAcyl = 0;
            } else {
                this.remainingAcyl = c.acylChains;
            }


        }

        public OpenChains(int remainingAcyl, int remainingAlkyl) {
            this.remainingAcyl = remainingAcyl;
            this.remainingAlkyl = remainingAlkyl;
        }

        public OpenChains decrementAlkyl() {
            return new OpenChains(remainingAcyl, remainingAlkyl-1);
        }
        public OpenChains decrementAcyl() {
            return new OpenChains(remainingAcyl-1, remainingAlkyl);
        }

        public OpenChains decrement(LipidChain lipidChain) {
            switch (lipidChain.getType()) {
                case ALKYL:
                    return decrementAlkyl();
                case ACYL:
                    return decrementAcyl();
                default: throw new UnsupportedOperationException("Not implemented yet: " + String.valueOf(lipidChain));
            }
        }

        public boolean isValid() {
            return remainingAcyl>=0 && remainingAlkyl>=0;
        }

        public int remaining(LipidChain.Type type) {
            switch (type) {
                case ALKYL:
                    return remainingAlkyl;
                case ACYL:
                    return remainingAcyl;
                default: return 0;
            }
        }
    }

    <T extends Spectrum<Peak>> void expand(LipidCandidate candidate, T spectrum, FragmentMap fragmentMap, FragmentLib.FragmentSet library, LipidTreeNode root) {
        // first check if remaining chain formula matches a single chain
        {
            final Optional<LipidChain> chain = LipidChain.fromFormula(root.remainingChainFormula);
            if (chain.isPresent()) {
                IndexedPeak p=null;
                final Optional<IndexedPeak> any = root.peaks.stream().filter(x -> x.annotation.getTarget() == LipidAnnotation.Target.LOSS).findAny();
                if (any.isPresent()) {
                    final IndexedPeak q = any.get();
                    p = new IndexedPeak(q.index, q.getMass(), q.getIntensity(), chain.get().formula, new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, chain.get().formula, q.formula, q.annotation.getIonType(), chain.get()));
                } else {
                    final Optional<IndexedPeak> any2 = root.peaks.stream().filter(x -> x.annotation.getTarget() == LipidAnnotation.Target.FRAGMENT).findAny();
                    if (any2.isPresent()) {
                        final IndexedPeak q = any2.get();
                        p = new IndexedPeak(q.index, q.getMass(), q.getIntensity(), any2.get().formula, new ChainAnnotation(LipidAnnotation.Target.LOSS, chain.get().formula,q.formula, q.annotation.getIonType(), chain.get()));
                    }
                }
                OpenChains c = root.openChains.decrement(chain.get());
                if (c.isValid()) {
                    final LipidTreeNode e = new LipidTreeNode(chain.get(), MolecularFormula.emptyFormula(), root.depth + 1, c);
                    if (p != null) {
                        e.peaks.add(p);
                    }
                    root.childNodes.add(e);
                }
            }
        }

        final HashMap<LipidChain, LipidTreeNode> nodes = new HashMap<>();
        for (IndexedPeak p : root.peaks) {
            final boolean precursor = (p.annotation instanceof PrecursorAnnotation);
            if (!precursor && p.annotation.getTarget() == LipidAnnotation.Target.FRAGMENT) {
                // we cannot expand fragments any further
                continue;
            }
            // precursor formula
            final MolecularFormula precursorIon = p.formula;//p.annotation.getTarget()== LipidAnnotation.Target.FRAGMENT ? p.annotation.getFormula() : candidate.lipidFormula.subtract(p.annotation.getFormula());
            // losses
            for (int k=0; k < spectrum.size(); ++k) {
                if (spectrum.getMzAt(k)>=p.mass) continue;
                final MolecularFormula[] peakFormulas = fragmentMap.formulasPerPeak[k];
                for (int i=0; i < peakFormulas.length; ++i) {
                    final PrecursorIonType ionType = fragmentMap.ionTypesPerPeak[k][i];
                    final MolecularFormula lossFormula = precursorIon.subtract(peakFormulas[i]);
                    final MolecularFormula fragmentFormula = peakFormulas[i];
                    for (LipidChain.Type type : LipidChain.Type.values()) {
                        if (root.openChains.remaining(type)<=0) continue;
                        for (MolecularFormula modification : precursor ? library.lossesFor(type) : new MolecularFormula[]{MolecularFormula.emptyFormula()}) {
                            final MolecularFormula finalFormula = lossFormula.subtract(modification);
                            if (finalFormula.isAllPositiveOrZero()) {
                                Optional<LipidChain> chain = LipidChain.fromFormula(finalFormula);
                                if (chain.isPresent() && chain.get().type.equals(type)) {
                                    // add node
                                    final LipidTreeNode node = nodes.computeIfAbsent(chain.get(), (x)->new LipidTreeNode(chain.get(), root.remainingChainFormula.subtract(chain.get().formula), root.depth+1, root.openChains.decrement(chain.get())));
                                    final IndexedPeak peak = new IndexedPeak(k,spectrum.getMzAt(k),spectrum.getIntensityAt(k), peakFormulas[i], new ChainAnnotation(LipidAnnotation.Target.LOSS, finalFormula,peakFormulas[i], ionType, chain.get()));
                                    node.peaks.add(peak);

                                }
                            }
                        }
                        // fragments are only valid for precursor ions
                        if (precursor) {
                            for (MolecularFormula modification : library.fragmentsFor(type)) {
                                final MolecularFormula finalFormula = fragmentFormula.add(modification);
                                if (finalFormula.isAllPositiveOrZero()) {
                                    Optional<LipidChain> chain = LipidChain.fromFormula(finalFormula);
                                    if (chain.isPresent() && chain.get().type.equals(type)) {
                                        // add node
                                        final LipidTreeNode node = nodes.computeIfAbsent(chain.get(), (x)->new LipidTreeNode(chain.get(),root.remainingChainFormula.subtract(chain.get().formula), root.depth+1,root.openChains.decrement(chain.get())));
                                        final IndexedPeak peak = new IndexedPeak(k,spectrum.getMzAt(k),spectrum.getIntensityAt(k),
                                        peakFormulas[i],new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, finalFormula,peakFormulas[i], ionType, chain.get()));
                                        node.peaks.add(peak);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // node expand every node further
        for (LipidTreeNode node : nodes.values()) {
            expand(candidate,spectrum,fragmentMap,library,node);
            root.childNodes.add(node);
        }
    }

    class FragmentMap {
        LinkedHashMap<PrecursorIonType, HashMap<MolecularFormula, Integer>> map;
        MolecularFormula[][] formulasPerPeak;
        PrecursorIonType[][] ionTypesPerPeak;

        public <T extends Spectrum<Peak>> FragmentMap(T spectrum, MolecularFormula parent, MassToFormulaDecomposer decomposer, PrecursorIonType ionType) {
            map = new LinkedHashMap<>();
            final FormulaConstraints subsets = FormulaConstraints.allSubsetsOf(parent);
            formulasPerPeak = new MolecularFormula[spectrum.size()][0];
            ionTypesPerPeak = new PrecursorIonType[spectrum.size()][0];
            LinkedHashSet<PrecursorIonType> ions = new LinkedHashSet<>();
            ions.add(ionType);
            ions.add(ionType.withoutAdduct());
            if (ionType.equals(Sodium)) ions.add(PrecursorIonType.getPrecursorIonType("[M+H]+"));
            for (PrecursorIonType ion : ions) {
                final HashMap<MolecularFormula, Integer> amap =new HashMap<>();
                for (int k=0; k < spectrum.size(); ++k) {
                    final List<MolecularFormula> molecularFormulas = decomposer.decomposeNeutralMassToFormulas(ion.subtractIonAndAdduct(spectrum.getMzAt(k)), deviation, subsets);
                    int i = formulasPerPeak[k].length;
                    formulasPerPeak[k] = Arrays.copyOf(formulasPerPeak[k], formulasPerPeak[k].length + molecularFormulas.size());
                    ionTypesPerPeak[k] = Arrays.copyOf(ionTypesPerPeak[k], ionTypesPerPeak[k].length + molecularFormulas.size());
                    for (MolecularFormula formula : molecularFormulas) {
                        amap.put(formula, k);
                        formulasPerPeak[k][i] = formula;
                        ionTypesPerPeak[k][i] = ion;
                        ++i;
                    }
                }
                map.put(ion, amap);
            }
        }

        public MolecularFormula[] formulasPerPeak(int peakIndex) {
            return formulasPerPeak[peakIndex];
        }
        public PrecursorIonType[] ionsPerPeak(int peakIndex) {
            return ionTypesPerPeak[peakIndex];
        }

        public int lookup(MolecularFormula formula) {
            for (Map<MolecularFormula, Integer> m : map.values()) {
                Integer i = m.get(formula);
                if (i!=null) return i;
            }
            return -1;
        }
        public int[] lookupAll(MolecularFormula formula) {
            int[] xs = new int[map.size()];
            int k=0;
            for (Map<MolecularFormula, Integer> m : map.values()) {
                Integer i = m.get(formula);
                if (i!=null) xs[k++] = i;
            }
            return Arrays.copyOf(xs, k);
        }
    }

    private static List<List<LipidTreeNode>> findBestAnnotation(LipidCandidate candidate, LipidTreeNode root) {
        final HashMap<LipidChain, Float> hints = new HashMap<>();
        final HashMap<LipidChain, Integer> chainCounts = new HashMap<>();
        for (LipidTreeNode node : root.childNodes) {
            HashMap<LipidChain, Integer> cc = new HashMap<>();
            node.populateHints(hints, cc);
            for (LipidChain l : cc.keySet()) chainCounts.put(l, Math.max(cc.get(l),chainCounts.getOrDefault(l,0)));
        }
        final LipidChain[] chains;
        {
            final ArrayList<LipidChain> cc = new ArrayList<>();
            for (LipidChain l : chainCounts.keySet()) {
                for (int k=0; k < chainCounts.get(l); ++k) cc.add(l);
            }
            chains = cc.toArray(LipidChain[]::new);
            Arrays.sort(chains, Comparator.comparingDouble(hints::get));
        }
        // stupid greedy search, just to have something running
        final LipidChain[] selected = findKBest(candidate, chains);
        if (selected==null) return new ArrayList<>();
        List<List<LipidTreeNode>> nodePerChain = new ArrayList<>();
        Arrays.sort(selected);
        int rep=0;LipidChain before=null;
        for (LipidChain chain : selected) {
            if (chain==before) {
                ++rep;
            } else {
                before = chain;
                rep=0;
            }
            nodePerChain.add(root.collectAllChainNodes(chain, rep));
        }
        return nodePerChain;
    }

    private static LipidChain[] findKBest(LipidCandidate candidate, LipidChain[] chainsSortedLowToBest) {
        HashMap<ChainWithFormula, BitSet> dp = new HashMap<>();
        final ChainWithFormula search = new ChainWithFormula(candidate.chainFormula, candidate.possibleClass.chains);
        for (int k=chainsSortedLowToBest.length-1; k >= 0; --k) {
            final int K = k;
            final HashMap<ChainWithFormula, BitSet> dpNextRow = new HashMap<>(dp);
            final LipidChain c = chainsSortedLowToBest[k];
            final BitSet single = new BitSet();
            single.set(K);
            dpNextRow.put(new ChainWithFormula(c.formula, 1), single);
            dp.forEach((key, bitSet) -> {
                final BitSet union = (BitSet)bitSet.clone();
                union.set(K);
                final int len = union.cardinality();
                if (len <=candidate.possibleClass.chains) {
                    dpNextRow.put(new ChainWithFormula(key.formula.add(c.getFormula()), len), union);
                }
            });
            // check if we find a solution
            final BitSet b = dpNextRow.get(search);
            if (b!=null) {
                return b.stream().mapToObj(x->chainsSortedLowToBest[x]).toArray(LipidChain[]::new);
            }
            dp = dpNextRow;
        }
        return null;
    }

    private static final class ChainWithFormula {
        private final MolecularFormula formula;
        private final int numberOfChains;

        private ChainWithFormula(MolecularFormula formula, int numberOfChains) {
            this.formula = formula;
            this.numberOfChains = numberOfChains;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChainWithFormula that = (ChainWithFormula) o;
            return numberOfChains == that.numberOfChains && formula.equals(that.formula);
        }

        @Override
        public int hashCode() {
            return Objects.hash(formula, numberOfChains);
        }
    }

    static class LipidTreeNode {
        final LipidChain candidate;
        final List<IndexedPeak> peaks;
        final List<LipidTreeNode> childNodes;
        final MolecularFormula remainingChainFormula;
        OpenChains openChains;
        final int depth;

        public LipidTreeNode(LipidChain candidate, MolecularFormula remainingChainFormula, int depth, OpenChains openChains) {
            this.candidate = candidate;
            this.peaks = new ArrayList<>();
            this.childNodes = new ArrayList<>();
            this.remainingChainFormula = remainingChainFormula;
            this.openChains = openChains;
            this.depth = depth;
        }

        public String toString() {
            if (candidate==null) return "root with " +peaks.size() + " peaks";
            return candidate.toString() + " |"+depth+"|" + peaks.size() + " peaks";
        }

        public boolean valid() {
            if (childNodes.isEmpty()) {
                return (remainingChainFormula.isEmpty() || LipidChain.validFormulaForAcylChains(remainingChainFormula, openChains.remainingAcyl));
            } else {
                return childNodes.stream().anyMatch(LipidTreeNode::valid);
            }
        }

        // delete all subtrees which are incomplete
        // return if self is complete or any subtree is complete
        public boolean prune() {
            childNodes.removeIf(lipidTreeNode -> !lipidTreeNode.prune());
            return valid();
        }

        public void populateHints(HashMap<LipidChain, Float> hints, HashMap<LipidChain, Integer> count) {
            if (candidate!=null) {
                hints.put(candidate, hints.getOrDefault(candidate,0.0f)+score());
                count.put(candidate, count.getOrDefault(candidate, 1)+1);
            }
            for (LipidTreeNode child : childNodes) {
                child.populateHints(hints, count);
            }
        }

        private float score() {
            return (float)peaks.stream().mapToDouble(x->x.intensity).sum() + chainPrior();
        }

        // just to resolve ties
        private float chainPrior() {
            if (candidate==null) return 0f;
            /*
            float prior = 0f;
            prior -= Math.max(0,candidate.numberOfDoubleBonds-6)/10000f;
            prior += candidate.chainLength/10000f;
            prior -= Math.max(0,12-candidate.chainLength)/1000f;
            if (candidate.getType()== LipidChain.Type.ACYL) prior += 1e-6;
            return prior;
             */
            final int n = (openChains.remainingAcyl+ openChains.remainingAlkyl);
            if (n==0) return 0f;
            int dist = remainingChainFormula.numberOfCarbons()/n;
            return -Math.abs(candidate.chainLength - dist)/1000f;


        }

        public List<LipidTreeNode> collectAllChainNodes(LipidChain chain, int minCount) {
            final ArrayList<LipidTreeNode> nodes = new ArrayList<>();
            collect(nodes, chain, minCount);
            return nodes;
        }


        private void collect(List<LipidTreeNode> nodes, LipidChain chain, int minCount) {
            if (candidate!=null && this.candidate.equals(chain)) {
                if (minCount >= 0) {
                    nodes.add(this);
                } else {
                    --minCount;
                }
            }
            for (LipidTreeNode child : childNodes) child.collect(nodes,chain,minCount);
        }
    }

    public <T extends Spectrum<Peak>> List<LipidTreeNode> searchForSphingosin(T spectrum, LipidCandidate candidate, FragmentMap map, FragmentLib.FragmentSet lib) {
        // we know there is exactly one sphingosin chain and one other chain
        final HashMap<LipidChain, List<IndexedPeak>> indicators = new HashMap<>();
        for (int k=0; k < map.formulasPerPeak.length; ++k) {
            final MolecularFormula[] fs = map.formulasPerPeak(k);
            for (int j=0; j< fs.length; ++j) {
                final MolecularFormula f = fs[j];
                for (MolecularFormula modification : lib.sphingosinFragments) {
                    final MolecularFormula finalFormula = f.add(modification);
                    if (LipidChain.validFormulaForSphingosinChains(finalFormula)) {
                        final MolecularFormula remain = candidate.chainFormula.subtract(finalFormula);
                        if (LipidChain.validFormulaForAcylChains(remain, 1)) {
                            final LipidChain lipidChain = LipidChain.fromFormula(finalFormula).get();
                            indicators.computeIfAbsent(lipidChain, (x)->new ArrayList<>()).add(new IndexedPeak(
                                    k, spectrum.getMzAt(k), spectrum.getIntensityAt(k),f, new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, f,f, map.ionTypesPerPeak[k][j], lipidChain)));
                        }
                    }
                }
                for (MolecularFormula modification : lib.sphingosinLosses) {
                    final MolecularFormula finalFormula = candidate.lipidFormula.subtract(f).subtract(modification);
                    if (LipidChain.validFormulaForSphingosinChains(finalFormula)) {
                        final MolecularFormula remain = candidate.chainFormula.subtract(finalFormula);
                        if (LipidChain.validFormulaForAcylChains(remain, 1)) {
                            final LipidChain lipidChain = LipidChain.fromFormula(finalFormula).get();
                            indicators.computeIfAbsent(lipidChain, (x)->new ArrayList<>()).add(new IndexedPeak(
                                    k, spectrum.getMzAt(k), spectrum.getIntensityAt(k),f, new ChainAnnotation(LipidAnnotation.Target.LOSS, finalFormula.add(modification),f, map.ionTypesPerPeak[k][j], lipidChain)));
                        }
                    }
                }
            }
        }
        final Optional<LipidTreeNode> best = indicators.keySet().stream().map(x -> {
            final LipidTreeNode root = new LipidTreeNode(x, candidate.chainFormula.subtract(x.getFormula()), 1, new OpenChains(0, 0));
            root.peaks.addAll(indicators.get(x));
            final LipidTreeNode child = new LipidTreeNode(LipidChain.fromFormula(root.remainingChainFormula).get(), MolecularFormula.emptyFormula(), 2, new OpenChains(0, 0));
            root.childNodes.add(child);
            return root;
        }).max(Comparator.comparingDouble(LipidTreeNode::score));
        return best.map(x->new ArrayList<>(Arrays.asList(x,x.childNodes.get(0)))).orElseGet(ArrayList::new);
    }
















}
