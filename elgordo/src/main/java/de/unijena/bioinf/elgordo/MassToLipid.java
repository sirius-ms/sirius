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
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Decomposition;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MassToLipid {

    private final MassToFormulaDecomposer cho, chno, chnops;
    private final PrecursorIonType[] possibleIonTypes;

    private final FormulaConstraints chainConstraints;
    private final MolecularFormula SPHINGOSIN_HEAD;
    private final Deviation deviation;

    public static void main(String[] args) {

    }

    public MassToLipid(Deviation deviation, int polarity) {
        this.deviation = deviation;
        this.cho = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHO").elementArray()));
        this.chno = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNO").elementArray()));
        this.chnops = new MassToFormulaDecomposer(new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNOPS").elementArray()));
        this.chainConstraints = new FormulaConstraints("C[1-]H[2-]N[0]O[0-]");
        if (polarity>0) {
            this.possibleIonTypes = new PrecursorIonType[]{
                    PrecursorIonType.fromString("[M+H]+"),
                    PrecursorIonType.fromString("[M+Na]+"),
                    PrecursorIonType.fromString("[M+NH3+H]+"),
                    PrecursorIonType.fromString("[M+O+H]+")};
        } else {
            this.possibleIonTypes = new PrecursorIonType[]{
                    PrecursorIonType.fromString("[M-H]-"),
                    PrecursorIonType.fromString("[M+H2CO2-H]-"),
            PrecursorIonType.fromString("[M - CH2 - H]-")};
        }
        this.SPHINGOSIN_HEAD = new LipidChain(LipidChain.Type.SPHINGOSIN, 5, 1).getFormula();
    }

    public <T extends Spectrum<Peak>> SimpleSpectrum prepareSpectrum(T spectrum) {
        SimpleMutableSpectrum buf = new SimpleMutableSpectrum(spectrum);
        for (int i=0; i < buf.size(); ++i) buf.setIntensityAt(i, buf.getIntensityAt(i));
        return Spectrums.getNormalizedSpectrum(buf, Normalization.Max(1d));
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
                        LipidAnnotation.Target.FRAGMENT, f, f, adductSwitch(candidate.ionType), candidate.possibleClass.headgroup
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
            LipidChainCandidate bestCandidate = null;
            final LipidTreeNode root = makeRoot(candidate,spectrum,map,set,
                    peaks.stream().filter(x->x.annotation instanceof HeadGroupFragmentAnnotation && x.annotation.getTarget()== LipidAnnotation.Target.LOSS).collect(Collectors.toList())
                    //  Collections.emptyList()
            );
            final HashMap<LipidChain, ArrayList<IndexedPeak>> lipidChainFrags;
            final HashMap<LipidChain, ArrayList<IndexedPeak>> remainingAnnotations = new HashMap<>();
            if (candidate.sphingosinChains > 0) {
                final List<LipidTreeNode> nodes = searchForSphingosin(spectrum, candidate, map, set);
                root.childNodes.addAll(nodes);
                bestCandidate = root.getAllCompleteLipidChains(candidate).stream().sorted(Comparator.reverseOrder()).findFirst().orElse(null);
                root.putAllIn(remainingAnnotations);
            } else {
                lipidChainFrags = expand(candidate, spectrum, map, set, root);
                bestCandidate = findBestAnnotation(candidate, root, lipidChainFrags);
                remainingAnnotations.putAll(lipidChainFrags);
                root.putAllIn(remainingAnnotations);
            }
            if (bestCandidate==null) {
                final Optional<LipidChain> mergedFromFormula = LipidChain.getMergedFromFormula(candidate.chainFormula, candidate.possibleClass);
                if (mergedFromFormula.isPresent()) bestCandidate = new LipidChainCandidate(candidate, new LipidChain[]{mergedFromFormula.get()}, new IndexedPeak[0]);
                else return null;
            }
            peaks.addAll(Arrays.asList(bestCandidate.anotations));

            final IndexedPeak[] sorted = peaks.stream().sorted(Comparator.comparingInt(x->x.index)).toArray(IndexedPeak[]::new);
            //final IndexedPeak[] contradicting = findContradictingAnnotations(sorted, map, spectrum, candidate);
            final TIntHashSet PeakIndexSet = new TIntHashSet(Arrays.stream(sorted).mapToInt(x -> x.index).toArray());
            final int[] peakindizes = PeakIndexSet.toArray();
            Arrays.sort(peakindizes);

            List<IndexedPeak> contraAnnotations = new ArrayList<>();
            {
                TIntHashSet ci = new TIntHashSet();
                for (LipidChain c: remainingAnnotations.keySet()) {
                    if (!bestCandidate.hasChain(c)) {
                        for (IndexedPeak p : remainingAnnotations.get(c)) {
                            if (!PeakIndexSet.contains(p.index) && ci.add(p.index)) {
                                contraAnnotations.add(p);
                            }
                        }
                    }
                }
            }


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
            float undecomposable = 0f;
            int undecompsablePeaks = 0;
            float totalInt = 0f;
            double maxIntensity = Spectrums.getMaximalIntensity(spectrum);
            for (int i=0; i < map.formulasPerPeak.length; ++i) {
                totalInt += spectrum.getIntensityAt(i);
                if (map.formulasPerPeak[i].length==0) {
                    if ((spectrum.getIntensityAt(i) / maxIntensity) >= 0.05) {
                        ++undecompsablePeaks;
                    }
                    undecomposable += spectrum.getIntensityAt(i);
                }
            }
            undecomposable/=totalInt;

            return new AnnotatedLipidSpectrum<T>(spectrum, candidate.lipidFormula, candidate.ionMass,candidate.ionType, new LipidSpecies(candidate.possibleClass,
                    bestCandidate.chains), (float)bestCandidate.score,
                    finalAnnotations, peakindizes, contraAnnotations.stream().map(x->x.annotation).toArray(LipidAnnotation[]::new), contraAnnotations.stream().mapToInt(x->x.index).sorted().toArray(),contraAnnotations.stream().mapToDouble(IndexedPeak::getIntensity).sum(),
                    undecompsablePeaks, undecomposable
                    );
        }
    }

    private PrecursorIonType adductSwitch(PrecursorIonType ionType) {
        if (ionType.equals(Sodium)) return Proton;
        else if (!ionType.getAdduct().isEmpty()) return ionType.withoutAdduct();
        else return ionType;
    }

    private static class LipidChainCandidate implements Comparable<LipidChainCandidate> {
        private final LipidCandidate parent;
        private final LipidChain[] chains;
        private final IndexedPeak[] anotations;
        private final double score;

        public LipidChainCandidate(LipidCandidate parent, LipidChain[] chains, IndexedPeak[] anotations) {
            this.parent = parent;
            this.chains = chains;
            this.anotations = anotations;
            this.score = calcScore(parent,anotations);
        }

        private double calcScore(LipidCandidate parent, IndexedPeak[] anotations) {
            final TIntHashSet usedPeaks = new TIntHashSet();
            float intensity = 0f;
            for (IndexedPeak p : anotations) {
                if (usedPeaks.add(p.index)) intensity += p.getIntensity();
            }
            float prior = 0f;
            for (LipidChain chain : chains) {
                prior += LipidTreeNode.chainPrior(parent, chain);
            }
            return intensity+prior;
        }

        @Override
        public int compareTo(@NotNull MassToLipid.LipidChainCandidate o) {
            return Double.compare(this.score, o.score);
        }

        public boolean hasChain(LipidChain c) {
            for (LipidChain d : chains) if (d.equals(c)) return true;
            return false;
        }
    }

    private static class LipidChainCandidateLegacy {
        private final TIntArrayList peakIndizes;
        private final ArrayList<LipidAnnotation> annotations;
        private final List<LipidChain> chains;
        final boolean complete;

        public LipidChainCandidateLegacy(TIntArrayList peakIndizes, ArrayList<LipidAnnotation> annotations, List<LipidChain> chains, boolean complete) {
            this.peakIndizes = peakIndizes;
            this.annotations = annotations;
            this.chains = chains;
            this.complete = complete;
            Collections.sort(chains);
        }

        public LipidChainCandidateLegacy copy() {
            return new LipidChainCandidateLegacy(new TIntArrayList(peakIndizes), new ArrayList<>(annotations), new ArrayList<>(chains), complete);
        }

        public void combine(LipidChainCandidateLegacy bestSubL) {
            peakIndizes.addAll(bestSubL.peakIndizes);
            annotations.addAll(bestSubL.annotations);
            chains.addAll(bestSubL.chains);
            Collections.sort(chains);
        }

        public boolean isMergeable(LipidChainCandidateLegacy c) {
            return chains.equals(c.chains);
        }

        public void merge(LipidChainCandidateLegacy c) {
            final TIntHashSet alreadyKnown = new TIntHashSet(peakIndizes);
            for (int k = 0; k < c.peakIndizes.size(); ++k) {
                if (alreadyKnown.add(c.peakIndizes.getQuick(k))) {
                    peakIndizes.add(c.peakIndizes.getQuick(k));
                    annotations.add(c.annotations.get(k));
                }
            }
        }
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
                            if (c.fragmentLib==null || c.fragmentLib.getFor(ionType).isEmpty()) continue;
                            int numberOfAlkylChains = c.chains - sphingosinChains - numberOfAcylChains;
                            if (numberOfAlkylChains >= 0) {
                                if (numberOfAlkylChains==0 || c.fragmentLib.getFor(ionType).get().hasAlkyl()) {
                                    final MolecularFormula chainFormula = sphingosinChains > 0 ? formula.add(SPHINGOSIN_HEAD) : formula;
                                    candidates.add(new LipidCandidate(c, ionType, precursorMass, chainFormula.add(group.molecularFormula), chainFormula, numberOfAlkylChains, numberOfAcylChains, sphingosinChains));
                                }
                            }
                        }
                    }

                }
            }
        }
        return candidates.stream().filter(LipidCandidate::hasValidChainFormula).collect(Collectors.toList());
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

        public boolean hasValidChainFormula() {
            return (chainFormula.numberOfOxygens() == (acylChains + sphingosinChains*2)) && (chainFormula.numberOfNitrogens() == sphingosinChains);
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

        public boolean isDone() {
            return remainingAlkyl==0 && remainingAcyl==0;
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

    private static final MolecularFormula[] SPHINGO_MODIFS = new MolecularFormula[]{MolecularFormula.emptyFormula(), MolecularFormula.parseOrNull("NH3")};
    private static final MolecularFormula[] FATTY_MODIFS = new MolecularFormula[]{MolecularFormula.emptyFormula(), MolecularFormula.parseOrNull("H2O")};

    <T extends Spectrum<Peak>> HashMap<LipidChain, ArrayList<IndexedPeak>> expand(LipidCandidate candidate, T spectrum, FragmentMap fragmentMap, FragmentLib.FragmentSet library, LipidTreeNode root) {
        // first check if remaining chain formula matches a single chain
        {
            final Optional<LipidChain> chain = LipidChain.fromFormula(root.remainingChainFormula);
            if (chain.isPresent()) {
                IndexedPeak p=null;
                final Optional<IndexedPeak> any = root.peaks.stream().filter(x -> x.annotation.getTarget() == LipidAnnotation.Target.LOSS && x.formula.equals(chain.get().formula)).findAny();

                if (any.isPresent()) {
                    final IndexedPeak q = any.get();
                    p = new IndexedPeak(q.index, q.getMass(), q.getIntensity(), chain.get().formula, new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, chain.get().formula, q.formula, q.annotation.getIonType(), MolecularFormula.emptyFormula(), chain.get()));
                }
                /*
                else {
                    final Optional<IndexedPeak> any2 = root.peaks.stream().filter(x -> x.annotation.getTarget() == LipidAnnotation.Target.FRAGMENT && x.formula).findAny();
                    if (any2.isPresent()) {
                        final IndexedPeak q = any2.get();
                        p = new IndexedPeak(q.index, q.getMass(), q.getIntensity(), any2.get().formula, new ChainAnnotation(LipidAnnotation.Target.LOSS, chain.get().formula,q.formula, q.annotation.getIonType(), chain.get()));
                    }
                }
                 */

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
        final HashMap<LipidChain, ArrayList<IndexedPeak>> fragmentAnnotations = new HashMap<>();
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
                        MolecularFormula[] allowedModifs = library.lossesFor(type);
                        if (!precursor) {
                            // always allow NH3 in sphingosin and H2O in acyl/alkyl chains
                            if (type==LipidChain.Type.SPHINGOSIN) {
                                allowedModifs = SPHINGO_MODIFS;
                            } else {
                                allowedModifs = FATTY_MODIFS;
                            }
                        }
                        for (MolecularFormula modification : allowedModifs) {
                            final MolecularFormula finalFormula = lossFormula.subtract(modification);
                            if (finalFormula.isAllPositiveOrZero()) {
                                Optional<LipidChain> chain = LipidChain.fromFormula(finalFormula);
                                if (chain.isPresent() && chain.get().type.equals(type)) {
                                    final MolecularFormula rem = root.remainingChainFormula.subtract(chain.get().formula);
                                    final boolean done = root.openChains.decrement(chain.get()).isDone();
                                    if ((done && rem.isEmpty()) || (!done && !rem.isEmpty())) {
                                        final LipidTreeNode node = nodes.computeIfAbsent(chain.get(), (x)->new LipidTreeNode(chain.get(), rem, root.depth+1, root.openChains.decrement(chain.get())));
                                        final IndexedPeak peak = new IndexedPeak(k, spectrum.getMzAt(k), spectrum.getIntensityAt(k), peakFormulas[i], new ChainAnnotation(LipidAnnotation.Target.LOSS, finalFormula, peakFormulas[i], ionType, modification, chain.get()));
                                        node.peaks.add(peak);
                                    }
                                }
                            }
                        }
                        // fragments are only valid for precursor ions
                        if (precursor) {
                            for (MolecularFormula modification : library.fragmentsFor(type)) {
                                final MolecularFormula finalFormula = fragmentFormula.add(modification);
                                if (finalFormula.isAllPositiveOrZero()) {
                                    Optional<LipidChain> chain = LipidChain.fromFormula(finalFormula);
                                    if (chain.isPresent() && chain.get().type.equals(type) ) {
                                        final MolecularFormula rem = root.remainingChainFormula.subtract(chain.get().formula);
                                        final OpenChains decrement = root.openChains.decrement(chain.get());
                                        if (rem.isEmpty() && decrement.isDone()) {
                                            // add node
                                            final LipidTreeNode node = nodes.computeIfAbsent(chain.get(), (x) -> new LipidTreeNode(chain.get(), rem, root.depth + 1, decrement));
                                            final IndexedPeak peak = new IndexedPeak(k, spectrum.getMzAt(k), spectrum.getIntensityAt(k),
                                                    peakFormulas[i], new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, finalFormula, peakFormulas[i], ionType, modification, chain.get()));
                                            node.peaks.add(peak);
                                        } else {
                                            fragmentAnnotations.computeIfAbsent(chain.get(), (aa)->new ArrayList<>()).add(new IndexedPeak(k, spectrum.getMzAt(k), spectrum.getIntensityAt(k), peakFormulas[i], new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, finalFormula, peakFormulas[i], ionType, modification, chain.get())));
                                        }
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
        // and add remaining fragments
        root.attach(fragmentAnnotations);
        return fragmentAnnotations;
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

    // first search in the tree for a complete annotated lipid chain
    private static LipidChainCandidate findBestAnnotation(LipidCandidate candidate, LipidTreeNode root, HashMap<LipidChain, ArrayList<IndexedPeak>> otherAnnotations) {
        final List<LipidChainCandidate> candidates = root.getAllCompleteLipidChains(candidate);
        if (!candidates.isEmpty()) {
            return candidates.get(0);
        } else {
            final HashMap<LipidChain, Float> hints = new HashMap<>();
            final HashMap<LipidChain, Integer> chainCounts = new HashMap<>();
            for (LipidTreeNode node : root.childNodes) {
                HashMap<LipidChain, Integer> cc = new HashMap<>();
                node.populateHints(candidate, hints, cc);
                for (LipidChain l : cc.keySet()) chainCounts.put(l, Math.max(cc.get(l),chainCounts.getOrDefault(l,0)));
            }
            final LipidChain[] chains;
            {
                for (LipidChain other : otherAnnotations.keySet()) {
                    if (chainCounts.get(other)==null) {
                        chainCounts.put(other, 1);
                        hints.put(other, (float)otherAnnotations.get(other).stream().mapToDouble(IndexedPeak::getIntensity).sum() + LipidTreeNode.chainPrior(candidate,other));
                    }
                }
                final ArrayList<LipidChain> cc = new ArrayList<>();
                for (LipidChain l : chainCounts.keySet()) {
                    for (int k=0; k < chainCounts.get(l); ++k) cc.add(l);
                }
                chains = cc.toArray(LipidChain[]::new);
                Arrays.sort(chains, Comparator.comparingDouble(hints::get));
            }
            // stupid greedy search, just to have something running
            final LipidChain[] selected = findKBest(candidate, chains);
            if (selected==null) return null;
            final List<IndexedPeak> annotatedPeaks = new ArrayList<>();
            for (LipidChain c : selected) annotatedPeaks.addAll(root.collectAllChainNodes(c).stream().flatMap(x->x.peaks.stream()).collect(Collectors.toList()));
            return new LipidChainCandidate(candidate,
                    selected,annotatedPeaks.toArray(IndexedPeak[]::new)
            );

        }
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

        List<LipidChainCandidate> getAllCompleteLipidChains(LipidCandidate lipidCandidate) {
            ArrayList<LipidTreeNode> path = new ArrayList<>();
            ArrayList<LipidChainCandidate> results = new ArrayList<>();
            for (LipidTreeNode child : childNodes) {
                child.searchForCompletePath(lipidCandidate, path, results);
            }
            results.sort(Comparator.reverseOrder());
            return results;
        }

        private void searchForCompletePath(LipidCandidate lipidCandidate, ArrayList<LipidTreeNode> ancestors,ArrayList<LipidChainCandidate> candidates) {
            if (isComplete()) {
                ancestors.add(this);
                LipidChainCandidate candidate = new LipidChainCandidate(lipidCandidate,
                    ancestors.stream().map(x->x.candidate).filter(Objects::nonNull).toArray(LipidChain[]::new),
                        ancestors.stream().flatMap(x->x.peaks.stream()).toArray(IndexedPeak[]::new)
                );
                candidates.add(candidate);
                ancestors.remove(ancestors.size()-1);
            } else {
                ancestors.add(this);
                for (LipidTreeNode child : childNodes) {
                    child.searchForCompletePath(lipidCandidate, ancestors,candidates);
                }
                ancestors.remove(ancestors.size()-1);
            }
        }

        public String toString() {
            if (candidate==null) return "root with " +peaks.size() + " peaks";
            int peaks = peaksum();
            double intensity = this.peaks.stream().mapToDouble(x->x.intensity).sum();
            double intens = intsum();
            return String.format(Locale.US, "%s\tpeaks = %d + %d, int = %.3f + %.3f", this.candidate.toString(),this.peaks.size(), peaks-this.peaks.size(),
                    intensity, intens-intensity);
        }

        public List<IndexedPeak> getAllPeaks() {
            ArrayList<IndexedPeak> pks = new ArrayList<>();
            getAllPeaks(pks);
            return pks;
        }
        public List<IndexedPeak> getAllPeaks(List<IndexedPeak> ls) {
            ls.addAll(peaks);
            for (LipidTreeNode n : childNodes) {
                n.getAllPeaks(ls);
            }
            return ls;
        }

        private int peaksum() {
            return peaks.size() + childNodes.stream().mapToInt(LipidTreeNode::peaksum).max().orElse(0);
        }
        private double intsum() {
            return peaks.stream().mapToDouble(x->x.intensity).sum() + childNodes.stream().mapToDouble(LipidTreeNode::intsum).max().orElse(0d);
        }

        public boolean isComplete() {
            return childNodes.isEmpty() && remainingChainFormula.isEmpty();
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

        public void populateHints(LipidCandidate parent, HashMap<LipidChain, Float> hints, HashMap<LipidChain, Integer> count) {
            if (candidate!=null) {
                hints.put(candidate, hints.getOrDefault(candidate,0.0f)+score(parent));
                count.put(candidate, count.getOrDefault(candidate, 0)+1);
            }
            for (LipidTreeNode child : childNodes) {
                child.populateHints(parent, hints, count);
            }
        }

        private float score(LipidCandidate parent) {
            return (float)peaks.stream().mapToDouble(x->x.intensity).sum() + chainPrior(parent,candidate);
        }

        // just to resolve ties
        private static float chainPrior(LipidCandidate parent, LipidChain candidate) {
            if (candidate==null) return 0f;
            float prior = 0f;
            prior -= candidate.numberOfDoubleBonds/1000f;
            double balancedChainLength = parent.chainFormula.numberOfCarbons()/((double)parent.acylChains+parent.alkylChains);
            prior -= Math.abs(candidate.chainLength - balancedChainLength)/1000f;
            if (candidate.getType()== LipidChain.Type.ACYL) prior += 1e-3;
            return prior;
        }

        public List<LipidTreeNode> collectAllChainNodes(LipidChain chain) {
            final ArrayList<LipidTreeNode> nodes = new ArrayList<>();
            collect(nodes, chain);
            return nodes;
        }


        private void collect(List<LipidTreeNode> nodes, LipidChain chain) {
            if (candidate!=null && this.candidate.equals(chain)) {
                nodes.add(this);
            }
            for (LipidTreeNode child : childNodes) child.collect(nodes,chain);
        }

        public void attach(HashMap<LipidChain, ArrayList<IndexedPeak>> fragmentAnnotations) {
            this.peaks.addAll(fragmentAnnotations.getOrDefault(this.candidate, new ArrayList<>()));
            for (LipidTreeNode node : childNodes) {
                node.attach(fragmentAnnotations);
            }
        }

        public void putAllIn(HashMap<LipidChain, ArrayList<IndexedPeak>> remainingAnnotations) {
            if (candidate!=null) {
                remainingAnnotations.computeIfAbsent(candidate, (l)->new ArrayList<>()).addAll(peaks);
            }
            for (LipidTreeNode child : childNodes) {
                child.putAllIn(remainingAnnotations);
            }
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
                                    k, spectrum.getMzAt(k), spectrum.getIntensityAt(k),f, new ChainAnnotation(LipidAnnotation.Target.FRAGMENT, f,f, map.ionTypesPerPeak[k][j], modification, lipidChain)));
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
                                    k, spectrum.getMzAt(k), spectrum.getIntensityAt(k),f, new ChainAnnotation(LipidAnnotation.Target.LOSS, finalFormula.add(modification),f, map.ionTypesPerPeak[k][j], modification, lipidChain)));
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
        }).max(Comparator.comparingDouble(x->x.score(candidate)));
        return best.map(x->new ArrayList<>(Arrays.asList(x,x.childNodes.get(0)))).orElseGet(ArrayList::new);
    }
















}
