package de.unijena.bioinf.GibbsSampling.model;/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Score;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.annotations.NoiseThresholdSettings;
import gnu.trove.map.hash.TIntIntHashMap;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ExamplePreparationUtils {

    public static Map<Ms2Experiment, List<FTree>> getData(String resource, double ppm, boolean disableNoiseIntensityThreshold) throws IOException {
        final Path exampleDir = Paths.get(ExamplePreparationUtils.class.getResource(resource).getFile());

        LoggerFactory.getLogger(GraphBuilderTest.class).warn("test");
        Map<Ms2Experiment, List<FTree>> data = ExamplePreparationUtils.readData(exampleDir);

        setPpmAnnotations(data.keySet(), ppm);
        if (disableNoiseIntensityThreshold) {
            disableNoiseIntensityThreshold(data.keySet());
        }

        return data;
    }

    private static void setPpmAnnotations(Collection<Ms2Experiment> experiments, double ppm) {
        for (Ms2Experiment experiment : experiments) {
            MS1MassDeviation ms1MassDeviation = experiment.computeAnnotationIfAbsent(MS1MassDeviation.class);
            MS2MassDeviation ms2MassDeviation = experiment.computeAnnotationIfAbsent(MS2MassDeviation.class);

            experiment.setAnnotation(MS1MassDeviation.class, new MS1MassDeviation(new Deviation(ppm), ms1MassDeviation.standardMassDeviation, ms1MassDeviation.massDifferenceDeviation));
            experiment.setAnnotation(MS2MassDeviation.class, new MS2MassDeviation(new Deviation(ppm), ms2MassDeviation.standardMassDeviation, ms2MassDeviation.massDifferenceDeviation));
            experiment.setAnnotation(NoiseThresholdSettings.class, new NoiseThresholdSettings(0.0, 1000, NoiseThresholdSettings.BASE_PEAK.NOT_PRECURSOR, 0.0));
        }
    }

    private static void disableNoiseIntensityThreshold(Collection<Ms2Experiment> experiments) {
        for (Ms2Experiment experiment : experiments) {
            experiment.setAnnotation(NoiseThresholdSettings.class, new NoiseThresholdSettings(0.0, Integer.MAX_VALUE, NoiseThresholdSettings.BASE_PEAK.NOT_PRECURSOR, 0.0));
        }
    }

    public static FragmentsCandidate[][] extractCandidates(Map<Ms2Experiment, List<FTree>> data) {
        List<FragmentsCandidate[]> candidateList = new ArrayList<>();
        for (Map.Entry<Ms2Experiment, List<FTree>> entry : data.entrySet()) {
            Ms2Experiment experiment = entry.getKey();
            List<FTree> trees = entry.getValue();
            List<FragmentsCandidate> fc = FragmentsCandidate.createAllCandidateInstances(trees, experiment);
            candidateList.add(fc.toArray(new FragmentsCandidate[0]));
        }
        FragmentsCandidate[][] candidates = candidateList.toArray(new FragmentsCandidate[0][]);

        return candidates;
    }
    public static TIntIntHashMap intIntHashMapFromString(String s) {
        String[] arr = s.split(",");
        int[] keys = new int[arr.length];
        int[] values = new int[arr.length];

        for (int i = 0; i < arr.length; i++) {
            String[] entry = arr[i].replace(" ", "").split("=");
            keys[i] = Integer.parseInt(entry[0]);
            values[i] = Integer.parseInt(entry[1]);
        }
        return new TIntIntHashMap(keys, values);
    }
    public static Map<Ms2Experiment, List<FTree>> readData(Path dir) throws IOException {
        String[] compounds = new String[]{"A", "B", "C"};
        String[][] treeOrder = new String[][]{new String[]{"1", "2", "4", "7"},
                new String[]{"1", "2"},
                new String[]{"1", "2", "4"}
        };

        Map<Ms2Experiment, List<FTree>> map = new LinkedHashMap<>();

        for (int i = 0; i < compounds.length; i++) {
            String prefix = compounds[i];
            String[] order = treeOrder[i];
            Map<Ms2Experiment, List<FTree>> m = readData(dir, prefix, order);
            map.putAll(m);
        }
        return map;
    }

    public static Map<Ms2Experiment, List<FTree>> readData(Path dir, String compoundPrefix, String... treePrefixOrder) throws IOException {
        DescriptorRegistry registry = DescriptorRegistry.getInstance();
        registry.put(FTree.class, Score.class, new ScoreDescriptor());//read old format with beautification penalty

        Path specPath = dir.resolve(compoundPrefix+"_spectrum.ms");

        Ms2Experiment experiment = (new MsExperimentParser()).getParser(specPath.toFile()).parseFromFile(specPath.toFile()).get(0);

        List<FTree> trees = new ArrayList<>();
        for (String treePref : treePrefixOrder) {
            String pref = compoundPrefix+"_"+treePref+"_";
            List<Path> p = Files.find(dir, 1, (path, basicFileAttributes) -> path.getFileName().toString().startsWith(pref)).collect(Collectors.toList());
            if (p.size()!=1) throw new IllegalArgumentException("tree file unambiguously specified: "+treePref);

            final BufferedReader reader = Files.newBufferedReader(p.get(0));
            final FTree tree = (new FTJsonReader()).parse(reader, p.get(0).toUri());
            if (!tree.getAnnotationOrThrow(Score.class).asMap().containsKey("total")) throw new NoSuchElementException("total score");
            tree.setTreeWeight(tree.getAnnotationOrThrow(Score.class).get("total"));
            trees.add(tree);
        }

        Map<Ms2Experiment, List<FTree>> map = new HashMap<>();
        map.put(experiment, trees);

        return map;
    }

    public static class ScoreDescriptor implements Descriptor<Score> {

        @Override
        public String[] getKeywords() {
            return new String[]{"score"};
        }

        @Override
        public Class<Score> getAnnotationClass() {
            return Score.class;
        }

        @Override
        public <G, D, L> Score read(DataDocument<G, D, L> document, D dictionary) {
            final D scoredict = document.getDictionaryFromDictionary(dictionary,"score");
            double totalScore = document.getDoubleFromDictionary(scoredict, "total");
            final Score.HeaderBuilder score = Score.defineScoring();
            score.define("total");
            final Score.ScoreAssigner assign = score.score();
            assign.set("total", totalScore);
            return assign.done();
        }

        @Override
        public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, Score annotation) {

        }
    }

    public static FragmentsCandidate[][] parseExpectedCandidatesFromString(Map<Ms2Experiment, List<FTree>> data) {
        List<Ms2Experiment> experimentsOrdered = data.keySet().stream().collect(Collectors.toList());
        List<FragmentsCandidate> candidatesList;
        String[] fragments, losses;
        FragmentsAndLosses fragmentsAndLosses;
        String infos;
        FragmentsCandidate c, candidate;
        FragmentsCandidate[] candidates;
        FragmentsCandidate[][] allCandidates = new FragmentsCandidate[experimentsOrdered.size()][];

        //experiment _3981384892034070220-1706-unknown1705
        candidatesList = new ArrayList<>();
        //candidate C33H40O10
        fragments = new String[]{
                "C19H20, [M + H]+, 0, 0.1358819281691904",
                "C19H22O, [M + H]+, 1, 0.5652816491117328",
                "C19H24O2, [M + H]+, 3, 0.32185507700583943",
                "C20H20O, [M + H]+, 2, 0.3814764145399541",
                "C20H22O2, [M + H]+, 4, 1.0",
                "C20H24O3, [M + H]+, 5, 0.307465851716522",
                "C21H26O3, [M + H]+, 6, 0.11704742257256917",
                "C22H24O3, [M + H]+, 7, 0.11169770609099047",
                "C22H26O4, [M + H]+, 8, 0.4386607778874459",
                "C22H28O5, [M + H]+, 9, 0.09538023314446617",
                "C23H30O5, [M + H]+, 10, 0.09872973596268007",
                "C24H28O5, [M + H]+, 11, 0.08231866966161533",
                "C24H30O6, [M + H]+, 12, 0.10326015621259517",
                "C25H34O7, [M + H]+, 14, 0.018252110770070874",
                "C26H34O8, [M + H]+, 16, 0.13438539039142303",
                "C27H28O4, [M + H]+, 13, 0.28374667159035355",
                "C29H30O5, [M + H]+, 15, 0.3556319263970664",
                "C29H32O6, [M + H]+, 17, 0.046446345424509934",
                "C31H34O7, [M + H]+, 18, 0.12543763298139302",
                "C31H36O8, [M + H]+, 19, 0.061543017268438714",
                "C33H40O10, [M + H]+, 20, 1.0",
        };
        losses = new String[]{
                "C10H10O5, [M + H]+, 10, 0.09872973596268007",
                "C11H12O5, [M + H]+, 9, 0.09538023314446617",
                "C11H14O6, [M + H]+, 8, 0.4386607778874459",
                "C11H16O7, [M + H]+, 7, 0.11169770609099047",
                "C12H14O7, [M + H]+, 6, 0.11704742257256917",
                "C13H16O7, [M + H]+, 5, 0.307465851716522",
                "C13H18O8, [M + H]+, 4, 1.0",
                "C13H20O9, [M + H]+, 2, 0.3814764145399541",
                "C14H16O8, [M + H]+, 3, 0.32185507700583943",
                "C14H18O9, [M + H]+, 1, 0.5652816491117328",
                "C14H20O10, [M + H]+, 0, 0.1358819281691904",
                "C2H4O2, [M + H]+, 19, 0.061543017268438714",
                "C2H6O3, [M + H]+, 18, 0.12543763298139302",
                "C4H10O5, [M + H]+, 15, 0.3556319263970664",
                "C4H8O4, [M + H]+, 17, 0.046446345424509934",
                "C6H12O6, [M + H]+, 13, 0.28374667159035355",
                "C7H6O2, [M + H]+, 16, 0.13438539039142303",
                "C8H6O3, [M + H]+, 14, 0.018252110770070874",
                "C9H10O4, [M + H]+, 12, 0.10326015621259517",
                "C9H12O5, [M + H]+, 11, 0.08231866966161533",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(0), data.get(experimentsOrdered.get(0))).getMergedPeaks());
        infos = "C33H40O10, [M + H]+, 52.65640143997728";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(0));
        candidatesList.add(candidate);
        //candidate C29H36N6O8
        fragments = new String[]{
                "C19H20, [M + H]+, 0, 0.1358819281691904",
                "C19H22O, [M + H]+, 1, 0.5652816491117328",
                "C19H24O2, [M + H]+, 3, 0.32185507700583943",
                "C20H20O, [M + H]+, 2, 0.3814764145399541",
                "C20H22O2, [M + H]+, 4, 1.0",
                "C20H24O3, [M + H]+, 5, 0.307465851716522",
                "C21H26O3, [M + H]+, 6, 0.11704742257256917",
                "C22H24O3, [M + H]+, 7, 0.11169770609099047",
                "C22H26O4, [M + H]+, 8, 0.4386607778874459",
                "C22H28O5, [M + H]+, 9, 0.09538023314446617",
                "C23H24N6O2, [M + H]+, 13, 0.28374667159035355",
                "C23H30O5, [M + H]+, 10, 0.09872973596268007",
                "C24H28O5, [M + H]+, 11, 0.08231866966161533",
                "C24H30O6, [M + H]+, 12, 0.10326015621259517",
                "C25H26N6O3, [M + H]+, 15, 0.3556319263970664",
                "C25H28N6O4, [M + H]+, 17, 0.046446345424509934",
                "C25H34O7, [M + H]+, 14, 0.018252110770070874",
                "C26H34O8, [M + H]+, 16, 0.13438539039142303",
                "C27H30N6O5, [M + H]+, 18, 0.12543763298139302",
                "C27H32N6O6, [M + H]+, 19, 0.061543017268438714",
                "C29H36N6O8, [M + H]+, 20, 1.0",
        };
        losses = new String[]{
                "C10H12N6O6, [M + H]+, 3, 0.32185507700583943",
                "C10H14N6O7, [M + H]+, 1, 0.5652816491117328",
                "C10H16N6O8, [M + H]+, 0, 0.1358819281691904",
                "C2H4O2, [M + H]+, 19, 0.061543017268438714",
                "C2H6O3, [M + H]+, 18, 0.12543763298139302",
                "C3H2N6, [M + H]+, 16, 0.13438539039142303",
                "C4H10O5, [M + H]+, 15, 0.3556319263970664",
                "C4H2N6O, [M + H]+, 14, 0.018252110770070874",
                "C4H8O4, [M + H]+, 17, 0.046446345424509934",
                "C5H6N6O2, [M + H]+, 12, 0.10326015621259517",
                "C5H8N6O3, [M + H]+, 11, 0.08231866966161533",
                "C6H12O6, [M + H]+, 13, 0.28374667159035355",
                "C6H6N6O3, [M + H]+, 10, 0.09872973596268007",
                "C7H10N6O4, [M + H]+, 8, 0.4386607778874459",
                "C7H12N6O5, [M + H]+, 7, 0.11169770609099047",
                "C7H8N6O3, [M + H]+, 9, 0.09538023314446617",
                "C8H10N6O5, [M + H]+, 6, 0.11704742257256917",
                "C9H12N6O5, [M + H]+, 5, 0.307465851716522",
                "C9H14N6O6, [M + H]+, 4, 1.0",
                "C9H16N6O7, [M + H]+, 2, 0.3814764145399541",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(0), data.get(experimentsOrdered.get(0))).getMergedPeaks());
        infos = "C29H36N6O8, [M + H]+, 50.61324431678162";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(0));
        candidatesList.add(candidate);
        //candidate C32H38N4O6
        fragments = new String[]{
                "C17H22, [M + Na]+, 0, 0.1358819281691904",
                "C17H24O, [M + Na]+, 1, 0.5652816491117328",
                "C17H26O2, [M + Na]+, 3, 0.32185507700583943",
                "C18H22O, [M + Na]+, 2, 0.3814764145399541",
                "C18H24O2, [M + Na]+, 4, 1.0",
                "C18H26O3, [M + Na]+, 5, 0.307465851716522",
                "C19H28O3, [M + Na]+, 6, 0.11704742257256917",
                "C20H26O3, [M + Na]+, 7, 0.11169770609099047",
                "C20H28O4, [M + Na]+, 8, 0.4386607778874459",
                "C20H30O5, [M + Na]+, 9, 0.09538023314446617",
                "C21H32O5, [M + Na]+, 10, 0.09872973596268007",
                "C22H30O5, [M + Na]+, 11, 0.08231866966161533",
                "C22H32O6, [M + Na]+, 12, 0.10326015621259517",
                "C26H26N4, [M + Na]+, 13, 0.28374667159035355",
                "C28H28N4O, [M + Na]+, 15, 0.3556319263970664",
                "C28H30N4O2, [M + Na]+, 17, 0.046446345424509934",
                "C29H32N2O, [M + Na]+, 14, 0.018252110770070874",
                "C30H32N2O2, [M + Na]+, 16, 0.13438539039142303",
                "C30H32N4O3, [M + Na]+, 18, 0.12543763298139302",
                "C30H34N4O4, [M + Na]+, 19, 0.061543017268438714",
                "C32H38N4O6, [M + Na]+, 20, 1.0",
        };
        losses = new String[]{
                "C10H6N4, [M + Na]+, 12, 0.10326015621259517",
                "C10H8N4O, [M + Na]+, 11, 0.08231866966161533",
                "C11H6N4O, [M + Na]+, 10, 0.09872973596268007",
                "C12H10N4O2, [M + Na]+, 8, 0.4386607778874459",
                "C12H12N4O3, [M + Na]+, 7, 0.11169770609099047",
                "C12H8N4O, [M + Na]+, 9, 0.09538023314446617",
                "C13H10N4O3, [M + Na]+, 6, 0.11704742257256917",
                "C14H12N4O3, [M + Na]+, 5, 0.307465851716522",
                "C14H14N4O4, [M + Na]+, 4, 1.0",
                "C14H16N4O5, [M + Na]+, 2, 0.3814764145399541",
                "C15H12N4O4, [M + Na]+, 3, 0.32185507700583943",
                "C15H14N4O5, [M + Na]+, 1, 0.5652816491117328",
                "C15H16N4O6, [M + Na]+, 0, 0.1358819281691904",
                "C2H4O2, [M + Na]+, 19, 0.061543017268438714",
                "C2H6N2O4, [M + Na]+, 16, 0.13438539039142303",
                "C2H6O3, [M + Na]+, 18, 0.12543763298139302",
                "C3H6N2O5, [M + Na]+, 14, 0.018252110770070874",
                "C4H10O5, [M + Na]+, 15, 0.3556319263970664",
                "C4H8O4, [M + Na]+, 17, 0.046446345424509934",
                "C6H12O6, [M + Na]+, 13, 0.28374667159035355",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(0), data.get(experimentsOrdered.get(0))).getMergedPeaks());
        infos = "C32H38N4O6, [M + Na]+, 50.06153573807646";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(0));
        candidatesList.add(candidate);
        //candidate C28H46O11
        fragments = new String[]{
                "C14H26O, [M + K]+, 0, 0.1358819281691904",
                "C14H28O2, [M + K]+, 1, 0.5652816491117328",
                "C14H30O3, [M + K]+, 3, 0.32185507700583943",
                "C15H26O2, [M + K]+, 2, 0.3814764145399541",
                "C15H28O3, [M + K]+, 4, 1.0",
                "C15H30O4, [M + K]+, 5, 0.307465851716522",
                "C16H32O4, [M + K]+, 6, 0.11704742257256917",
                "C17H30O4, [M + K]+, 7, 0.11169770609099047",
                "C17H32O5, [M + K]+, 8, 0.4386607778874459",
                "C17H34O6, [M + K]+, 9, 0.09538023314446617",
                "C18H36O6, [M + K]+, 10, 0.09872973596268007",
                "C19H34O6, [M + K]+, 11, 0.08231866966161533",
                "C19H36O7, [M + K]+, 12, 0.10326015621259517",
                "C20H40O8, [M + K]+, 14, 0.018252110770070874",
                "C21H40O9, [M + K]+, 16, 0.13438539039142303",
                "C22H34O5, [M + K]+, 13, 0.28374667159035355",
                "C24H36O6, [M + K]+, 15, 0.3556319263970664",
                "C24H38O7, [M + K]+, 17, 0.046446345424509934",
                "C26H40O8, [M + K]+, 18, 0.12543763298139302",
                "C26H42O9, [M + K]+, 19, 0.061543017268438714",
                "C28H46O11, [M + K]+, 20, 1.0",
        };
        losses = new String[]{
                "C10H10O5, [M + K]+, 10, 0.09872973596268007",
                "C11H12O5, [M + K]+, 9, 0.09538023314446617",
                "C11H14O6, [M + K]+, 8, 0.4386607778874459",
                "C11H16O7, [M + K]+, 7, 0.11169770609099047",
                "C12H14O7, [M + K]+, 6, 0.11704742257256917",
                "C13H16O7, [M + K]+, 5, 0.307465851716522",
                "C13H18O8, [M + K]+, 4, 1.0",
                "C13H20O9, [M + K]+, 2, 0.3814764145399541",
                "C14H16O8, [M + K]+, 3, 0.32185507700583943",
                "C14H18O9, [M + K]+, 1, 0.5652816491117328",
                "C14H20O10, [M + K]+, 0, 0.1358819281691904",
                "C2H4O2, [M + K]+, 19, 0.061543017268438714",
                "C2H6O3, [M + K]+, 18, 0.12543763298139302",
                "C4H10O5, [M + K]+, 15, 0.3556319263970664",
                "C4H8O4, [M + K]+, 17, 0.046446345424509934",
                "C6H12O6, [M + K]+, 13, 0.28374667159035355",
                "C7H6O2, [M + K]+, 16, 0.13438539039142303",
                "C8H6O3, [M + K]+, 14, 0.018252110770070874",
                "C9H10O4, [M + K]+, 12, 0.10326015621259517",
                "C9H12O5, [M + K]+, 11, 0.08231866966161533",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(0), data.get(experimentsOrdered.get(0))).getMergedPeaks());
        infos = "C28H46O11, [M + K]+, 47.98981404774804";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(0));
        candidatesList.add(candidate);
        candidates = candidatesList.toArray(new FragmentsCandidate[0]);
        allCandidates[0] = candidates;

        //experiment _9944738237342398858-3008-unknown3007
        candidatesList = new ArrayList<>();
        //candidate C35H42N4O4
        fragments = new String[]{
                "C13H16O, [M + Na]+, 0, 2.513634179912902E-4",
                "C14H16O2, [M + Na]+, 2, 6.180061943342101E-4",
                "C14H18O, [M + Na]+, 1, 4.908836567015895E-4",
                "C15H18O2, [M + Na]+, 4, 5.995888724450656E-4",
                "C17H22, [M + Na]+, 3, 0.0016214929451387197",
                "C17H24O, [M + Na]+, 5, 0.005492800753226902",
                "C18H22O, [M + Na]+, 6, 0.03484756646000355",
                "C18H24O2, [M + Na]+, 7, 0.04246489788323715",
                "C18H26O3, [M + Na]+, 9, 0.00810893935507452",
                "C20H20O, [M + Na]+, 8, 0.006709887992134913",
                "C20H22O2, [M + Na]+, 10, 0.22910582626802328",
                "C22H24O3, [M + Na]+, 12, 0.005454692948093376",
                "C22H26O4, [M + Na]+, 14, 0.35979133458459733",
                "C22H28O2, [M + Na]+, 11, 4.719192861987568E-4",
                "C22H30O3, [M + Na]+, 13, 0.006163199325742923",
                "C22H32O4, [M + Na]+, 15, 0.0021805291105332623",
                "C24H30O4, [M + Na]+, 16, 0.24985754877858454",
                "C27H30N4O2, [M + Na]+, 18, 1.0",
                "C28H34O4, [M + Na]+, 17, 0.05058291050138326",
                "C31H34N4O2, [M + Na]+, 19, 0.011926506263372815",
                "C33H38N4O2, [M + Na]+, 20, 0.10386664054716438",
                "C35H42N4O4, [M + Na]+, 21, 1.0",
        };
        losses = new String[]{
                "C11H12N4, [M + Na]+, 16, 0.24985754877858454",
                "C13H10N4, [M + Na]+, 15, 0.0021805291105332623",
                "C13H12N4O, [M + Na]+, 13, 0.006163199325742923",
                "C13H14N4O2, [M + Na]+, 11, 4.719192861987568E-4",
                "C13H16N4, [M + Na]+, 14, 0.35979133458459733",
                "C13H18N4O, [M + Na]+, 12, 0.005454692948093376",
                "C15H20N4O2, [M + Na]+, 10, 0.22910582626802328",
                "C15H22N4O3, [M + Na]+, 8, 0.006709887992134913",
                "C17H16N4O, [M + Na]+, 9, 0.00810893935507452",
                "C17H18N4O2, [M + Na]+, 7, 0.04246489788323715",
                "C17H20N4O3, [M + Na]+, 6, 0.03484756646000355",
                "C18H18N4O3, [M + Na]+, 5, 0.005492800753226902",
                "C18H20N4O4, [M + Na]+, 3, 0.0016214929451387197",
                "C20H24N4O2, [M + Na]+, 4, 5.995888724450656E-4",
                "C21H24N4O3, [M + Na]+, 1, 4.908836567015895E-4",
                "C21H26N4O2, [M + Na]+, 2, 6.180061943342101E-4",
                "C22H26N4O3, [M + Na]+, 0, 2.513634179912902E-4",
                "C2H4O2, [M + Na]+, 20, 0.10386664054716438",
                "C4H8O2, [M + Na]+, 19, 0.011926506263372815",
                "C7H8N4, [M + Na]+, 17, 0.05058291050138326",
                "C8H12O2, [M + Na]+, 18, 1.0",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(1), data.get(experimentsOrdered.get(1))).getMergedPeaks());
        infos = "C35H42N4O4, [M + Na]+, 24.957479793070405";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(1));
        candidatesList.add(candidate);
        //candidate C34H46O8
        fragments = new String[]{
                "C14H18O, [M + Na]+, 1, 4.908836567015895E-4",
                "C15H18O2, [M + Na]+, 4, 5.995888724450656E-4",
                "C18H22O, [M + Na]+, 6, 0.03484756646000355",
                "C18H24O2, [M + Na]+, 7, 0.04246489788323715",
                "C18H26O3, [M + Na]+, 9, 0.00810893935507452",
                "C20H20O, [M + Na]+, 8, 0.006709887992134913",
                "C20H22O2, [M + Na]+, 10, 0.22910582626802328",
                "C22H24O3, [M + Na]+, 12, 0.005454692948093376",
                "C22H26O4, [M + Na]+, 14, 0.35979133458459733",
                "C22H28O2, [M + Na]+, 11, 4.719192861987568E-4",
                "C22H30O3, [M + Na]+, 13, 0.006163199325742923",
                "C22H32O4, [M + Na]+, 15, 0.0021805291105332623",
                "C24H30O4, [M + Na]+, 16, 0.24985754877858454",
                "C26H34O6, [M + Na]+, 18, 1.0",
                "C28H34O4, [M + Na]+, 17, 0.05058291050138326",
                "C30H38O6, [M + Na]+, 19, 0.011926506263372815",
                "C32H42O6, [M + Na]+, 20, 0.10386664054716438",
                "C34H46O8, [M + Na]+, 21, 1.0",
        };
        losses = new String[]{
                "C10H16O4, [M + Na]+, 16, 0.24985754877858454",
                "C12H14O4, [M + Na]+, 15, 0.0021805291105332623",
                "C12H16O5, [M + Na]+, 13, 0.006163199325742923",
                "C12H18O6, [M + Na]+, 11, 4.719192861987568E-4",
                "C12H20O4, [M + Na]+, 14, 0.35979133458459733",
                "C12H22O5, [M + Na]+, 12, 0.005454692948093376",
                "C14H24O6, [M + Na]+, 10, 0.22910582626802328",
                "C14H26O7, [M + Na]+, 8, 0.006709887992134913",
                "C16H20O5, [M + Na]+, 9, 0.00810893935507452",
                "C16H22O6, [M + Na]+, 7, 0.04246489788323715",
                "C16H24O7, [M + Na]+, 6, 0.03484756646000355",
                "C19H28O6, [M + Na]+, 4, 5.995888724450656E-4",
                "C20H28O7, [M + Na]+, 1, 4.908836567015895E-4",
                "C2H4O2, [M + Na]+, 20, 0.10386664054716438",
                "C4H8O2, [M + Na]+, 19, 0.011926506263372815",
                "C6H12O4, [M + Na]+, 17, 0.05058291050138326",
                "C8H12O2, [M + Na]+, 18, 1.0",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(1), data.get(experimentsOrdered.get(1))).getMergedPeaks());
        infos = "C34H46O8, [M + Na]+, 23.804562391716996";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(1));
        candidatesList.add(candidate);
        candidates = candidatesList.toArray(new FragmentsCandidate[0]);
        allCandidates[1] = candidates;
        //experiment _18202298417616308149-930-unknown929
        candidatesList = new ArrayList<>();
        //candidate C31H46O12
        fragments = new String[]{
                "C17H22O, [M + Na]+, 0, 0.27795896230668543",
                "C17H24O2, [M + Na]+, 2, 0.15490183695424192",
                "C18H20O, [M + Na]+, 1, 0.048287275093802075",
                "C18H22O2, [M + Na]+, 3, 1.0",
                "C18H24O3, [M + Na]+, 4, 0.608529115059614",
                "C18H26O4, [M + Na]+, 6, 0.11623119957824768",
                "C19H26O3, [M + Na]+, 5, 0.016733073756886884",
                "C20H24O3, [M + Na]+, 7, 0.06460786875566013",
                "C20H26O4, [M + Na]+, 8, 0.5363894048708605",
                "C20H28O5, [M + Na]+, 9, 0.3047148569148157",
                "C21H30O5, [M + Na]+, 10, 0.0792755947436882",
                "C22H30O6, [M + Na]+, 12, 0.5376515762882368",
                "C22H32O7, [M + Na]+, 13, 0.3240437990720521",
                "C24H34O8, [M + Na]+, 15, 0.20349395899495204",
                "C24H36O9, [M + Na]+, 16, 0.04498330191209444",
                "C25H34O6, [M + Na]+, 14, 0.12779450797511283",
                "C26H38O10, [M + Na]+, 19, 0.2049804507741606",
                "C27H36O7, [M + Na]+, 17, 0.034656375465698275",
                "C27H38O8, [M + Na]+, 18, 0.218883870272458",
                "C31H46O12, [M + Na]+, 20, 1.0",
        };
        losses = new String[]{
                "C10H16O7, [M + Na]+, 10, 0.0792755947436882",
                "C11H18O7, [M + Na]+, 9, 0.3047148569148157",
                "C11H20O8, [M + Na]+, 8, 0.5363894048708605",
                "C11H22O9, [M + Na]+, 7, 0.06460786875566013",
                "C12H20O9, [M + Na]+, 5, 0.016733073756886884",
                "C13H20O8, [M + Na]+, 6, 0.11623119957824768",
                "C13H22O9, [M + Na]+, 4, 0.608529115059614",
                "C13H24O10, [M + Na]+, 3, 1.0",
                "C13H26O11, [M + Na]+, 1, 0.048287275093802075",
                "C14H22O10, [M + Na]+, 2, 0.15490183695424192",
                "C14H24O11, [M + Na]+, 0, 0.27795896230668543",
                "C4H10O5, [M + Na]+, 17, 0.034656375465698275",
                "C4H8O4, [M + Na]+, 18, 0.218883870272458",
                "C5H8O2, [M + Na]+, 19, 0.2049804507741606",
                "C6H12O6, [M + Na]+, 14, 0.12779450797511283",
                "C7H10O3, [M + Na]+, 16, 0.04498330191209444",
                "C7H12O4, [M + Na]+, 15, 0.20349395899495204",
                "C9H14O5, [M + Na]+, 13, 0.3240437990720521",
                "C9H16O6, [M + Na]+, 12, 0.5376515762882368",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(2), data.get(experimentsOrdered.get(2))).getMergedPeaks());
        infos = "C31H46O12, [M + Na]+, 45.14513102259266";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(2));
        candidatesList.add(candidate);
        //candidate C28H50O13
        fragments = new String[]{
                "C14H26O2, [M + K]+, 0, 0.27795896230668543",
                "C14H28O3, [M + K]+, 2, 0.15490183695424192",
                "C15H24O2, [M + K]+, 1, 0.048287275093802075",
                "C15H26O3, [M + K]+, 3, 1.0",
                "C15H28O4, [M + K]+, 4, 0.608529115059614",
                "C15H30O5, [M + K]+, 6, 0.11623119957824768",
                "C16H30O4, [M + K]+, 5, 0.016733073756886884",
                "C17H28O4, [M + K]+, 7, 0.06460786875566013",
                "C17H30O5, [M + K]+, 8, 0.5363894048708605",
                "C17H32O6, [M + K]+, 9, 0.3047148569148157",
                "C18H34O6, [M + K]+, 10, 0.0792755947436882",
                "C19H34O7, [M + K]+, 12, 0.5376515762882368",
                "C19H36O8, [M + K]+, 13, 0.3240437990720521",
                "C21H38O9, [M + K]+, 15, 0.20349395899495204",
                "C21H40O10, [M + K]+, 16, 0.04498330191209444",
                "C22H38O7, [M + K]+, 14, 0.12779450797511283",
                "C23H42O11, [M + K]+, 19, 0.2049804507741606",
                "C24H40O8, [M + K]+, 17, 0.034656375465698275",
                "C24H42O9, [M + K]+, 18, 0.218883870272458",
                "C28H50O13, [M + K]+, 20, 1.0",
        };
        losses = new String[]{
                "C10H16O7, [M + K]+, 10, 0.0792755947436882",
                "C11H18O7, [M + K]+, 9, 0.3047148569148157",
                "C11H20O8, [M + K]+, 8, 0.5363894048708605",
                "C11H22O9, [M + K]+, 7, 0.06460786875566013",
                "C12H20O9, [M + K]+, 5, 0.016733073756886884",
                "C13H20O8, [M + K]+, 6, 0.11623119957824768",
                "C13H22O9, [M + K]+, 4, 0.608529115059614",
                "C13H24O10, [M + K]+, 3, 1.0",
                "C13H26O11, [M + K]+, 1, 0.048287275093802075",
                "C14H22O10, [M + K]+, 2, 0.15490183695424192",
                "C14H24O11, [M + K]+, 0, 0.27795896230668543",
                "C4H10O5, [M + K]+, 17, 0.034656375465698275",
                "C4H8O4, [M + K]+, 18, 0.218883870272458",
                "C5H8O2, [M + K]+, 19, 0.2049804507741606",
                "C6H12O6, [M + K]+, 14, 0.12779450797511283",
                "C7H10O3, [M + K]+, 16, 0.04498330191209444",
                "C7H12O4, [M + K]+, 15, 0.20349395899495204",
                "C9H14O5, [M + K]+, 13, 0.3240437990720521",
                "C9H16O6, [M + K]+, 12, 0.5376515762882368",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(2), data.get(experimentsOrdered.get(2))).getMergedPeaks());
        infos = "C28H50O13, [M + K]+, 44.74924095441745";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(2));
        candidatesList.add(candidate);
        //candidate C33H44O12
        fragments = new String[]{
                "C19H20O, [M + H]+, 0, 0.27795896230668543",
                "C19H22O2, [M + H]+, 2, 0.15490183695424192",
                "C20H18O, [M + H]+, 1, 0.048287275093802075",
                "C20H20O2, [M + H]+, 3, 1.0",
                "C20H22O3, [M + H]+, 4, 0.608529115059614",
                "C20H24O4, [M + H]+, 6, 0.11623119957824768",
                "C21H24O3, [M + H]+, 5, 0.016733073756886884",
                "C22H22O3, [M + H]+, 7, 0.06460786875566013",
                "C22H24O4, [M + H]+, 8, 0.5363894048708605",
                "C22H26O5, [M + H]+, 9, 0.3047148569148157",
                "C22H28O6, [M + H]+, 11, 0.014283104158260366",
                "C23H28O5, [M + H]+, 10, 0.0792755947436882",
                "C24H28O6, [M + H]+, 12, 0.5376515762882368",
                "C24H30O7, [M + H]+, 13, 0.3240437990720521",
                "C26H32O8, [M + H]+, 15, 0.20349395899495204",
                "C26H34O9, [M + H]+, 16, 0.04498330191209444",
                "C27H32O6, [M + H]+, 14, 0.12779450797511283",
                "C28H36O10, [M + H]+, 19, 0.2049804507741606",
                "C29H34O7, [M + H]+, 17, 0.034656375465698275",
                "C29H36O8, [M + H]+, 18, 0.218883870272458",
                "C33H44O12, [M + H]+, 20, 1.0",
        };
        losses = new String[]{
                "C10H16O7, [M + H]+, 10, 0.0792755947436882",
                "C11H16O6, [M + H]+, 11, 0.014283104158260366",
                "C11H18O7, [M + H]+, 9, 0.3047148569148157",
                "C11H20O8, [M + H]+, 8, 0.5363894048708605",
                "C11H22O9, [M + H]+, 7, 0.06460786875566013",
                "C12H20O9, [M + H]+, 5, 0.016733073756886884",
                "C13H20O8, [M + H]+, 6, 0.11623119957824768",
                "C13H22O9, [M + H]+, 4, 0.608529115059614",
                "C13H24O10, [M + H]+, 3, 1.0",
                "C13H26O11, [M + H]+, 1, 0.048287275093802075",
                "C14H22O10, [M + H]+, 2, 0.15490183695424192",
                "C14H24O11, [M + H]+, 0, 0.27795896230668543",
                "C4H10O5, [M + H]+, 17, 0.034656375465698275",
                "C4H8O4, [M + H]+, 18, 0.218883870272458",
                "C5H8O2, [M + H]+, 19, 0.2049804507741606",
                "C6H12O6, [M + H]+, 14, 0.12779450797511283",
                "C7H10O3, [M + H]+, 16, 0.04498330191209444",
                "C7H12O4, [M + H]+, 15, 0.20349395899495204",
                "C9H14O5, [M + H]+, 13, 0.3240437990720521",
                "C9H16O6, [M + H]+, 12, 0.5376515762882368",
        };
        fragmentsAndLosses = fromStrings(fragments, losses, FragmentsCandidate.assignFragmentsToPeaks(experimentsOrdered.get(2), data.get(experimentsOrdered.get(2))).getMergedPeaks());
        infos = "C33H44O12, [M + H]+, 44.40116390013503";
        c = candidateInfoFromString(infos);
        candidate = new FragmentsCandidate(fragmentsAndLosses, c.score, c.formula, c.ionType, experimentsOrdered.get(2));
        candidatesList.add(candidate);
        candidates = candidatesList.toArray(new FragmentsCandidate[0]);
        allCandidates[2] = candidates;

        return allCandidates;
    }

    private static FragmentsAndLosses fromStrings(String[] fragmentStrings, String[] lossStrings, List<ProcessedPeak> processedPeaks) {
        FragmentWithIndex[] fragments = new FragmentWithIndex[fragmentStrings.length];
        for (int i = 0; i < fragmentStrings.length; i++) {
            fragments[i] = fromString(fragmentStrings[i]);
        }

        FragmentWithIndex[] losses = new FragmentWithIndex[lossStrings.length];
        for (int i = 0; i < lossStrings.length; i++) {
            losses[i] = fromString(lossStrings[i]);
        }
        return new FragmentsAndLosses(fragments, losses); //todo add mergedPeaks????
    }

    private static FragmentWithIndex fromString(String s) {
        String[] arr =s.split(", ");
        MolecularFormula mf = MolecularFormula.parseOrThrow(arr[0]);
        Ionization ionization = PrecursorIonType.getPrecursorIonType(arr[1]).getIonization();
        short idx = Short.parseShort(arr[2]);
        double score = Double.parseDouble(arr[3]);
        return new FragmentWithIndex(mf, ionization, idx, score);
    }

    private static FragmentsCandidate candidateInfoFromString(String s) {
        String[] arr =s.split(", ");
        MolecularFormula mf = MolecularFormula.parseOrThrow(arr[0]);
        PrecursorIonType ionType = PrecursorIonType.getPrecursorIonType(arr[1]);
        double score = Double.parseDouble(arr[2]);
        return new FragmentsCandidate(null, score, mf, ionType, null);
    }
}
