/*
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

package de.unijena.bioinf.GibbsSampling;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.ms.CompoundQuality;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS1MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.UnconsideredCandidatesUpperBound;
import de.unijena.bioinf.GibbsSampling.model.*;
import de.unijena.bioinf.sirius.FTreeMetricsHelper;
import de.unijena.bioinf.sirius.Sirius;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TCharSet;
import gnu.trove.set.hash.TCharHashSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ZodiacUtils {
    private static final String[] reactionStringsMyCompoundID = new String[]{"H2", "CH2", "NH", "O", "NH3", "H2O", "CO", "C2H4", "C2H2O", "CO2", "C2H3NO", "SO3", "HPO3", "C4H3N3", "C4H2N2O", "C3H5NOS", "C2H5NO2S", "C5H4N2O", "C3H5NO2S", "C5H8O4", "C5H3N5", "C7H13NO2", "C5H7NO3S", "C6H10O5", "C6H8O6", "C10H12N2O4", "C9H11N3O4", "C9H10N2O5", "C16H30O", "C6H11O8P", "C10H11N5O3", "C10H11N5O4", "C10H15N3O5S", "C10H15N3O6S", "C12H20O10", "C18H30O15"};
    private static final String[] reactionStringsRogers = new String[]{"C10H11N5O3", "C10H11N5O4", "C10H12N2O4", "C10H12N5O6P", "C10H12N5O7P", "C10H13N2O7P", "C10H13N5O10P2", "C10H13N5O9P2", "C10H14N2O10P2", "C10H14N2O2S", "C10H15N2O3S", "C10H15N3O5S", "C11H10N2O", "C12H20O11", "C16H30O", "C18H30O15", "C21H33N7O15P3S", "C21H34N7O16P3S", "C2H2", "C2H2O", "C2H3NO", "C2H3O2", "C2H4", "C2O2", "C3H2O3", "C3H5NO", "C3H5NO2", "C3H5NOS", "C3H5O", "C4H3N2O2", "C4H4N3O", "C4H4O2", "C4H5NO3", "C4H6N2O2", "C4H7NO2", "C5H4N5", "C5H4N5O", "C5H5N2O2", "C5H7", "C5H7NO", "C5H7NO3", "C5H8N2O2", "C5H8O4", "C5H9NO", "C5H9NOS", "C6H10N2O3S2", "C6H10O5", "C6H10O6", "C6H11NO", "C6H11O8P", "C6H12N2O", "C6H12N4O", "C6H7N3O", "C6H8O6", "C8H8NO5P", "C9H10N2O5", "C9H11N2O8P", "C9H12N2O11P2", "C9H12N3O7P", "C9H13N3O10P2", "C9H9NO", "C9H9NO2", "CH2", "CH2ON", "CH3N2O", "CHO2", "CO", "CO2", "H2", "H2O", "H3O6P2", "HPO3", "N", "NH", "NH2", "O", "P", "PP", "SO3"};

    /**
     *
     * @param stepsize up to stepsize many {@link Reaction}s can be combined to a new {@link Reaction}
     * @return
     */
    public static Reaction[] parseReactions(int stepsize){
        Set<Reaction> reactionsSet = new HashSet<>();
        for (int i = 0; i < reactionStringsMyCompoundID.length; i++) {
            reactionsSet.add(parseReactionString(reactionStringsMyCompoundID[i]));
        }
        for (int i = 0; i < reactionStringsRogers.length; i++) {
            reactionsSet.add(parseReactionString(reactionStringsRogers[i]));
        }

        if (stepsize>1){

            //convert to combined reaction for equal testing
            Set<Reaction> reactionsSetCombined = new HashSet<>();
            for (Reaction reaction : reactionsSet) {
                reactionsSetCombined.add(new CombinedReaction(reaction));
            }

            Set<Reaction> reactionsSetAll = new HashSet<>(reactionsSetCombined);
            Set<Reaction> reactionsSet1 = new HashSet<>(reactionsSetCombined);
            Set<Reaction> singleReactionsSet = new HashSet<>(reactionsSetCombined);
            int i = 1;
            while (i++<stepsize){
                Set<Reaction> reactionSet2 = new HashSet<>();
                for (Reaction reaction1 : reactionsSet1) {
                    for (Reaction reaction2 : singleReactionsSet) {
                        CombinedReaction combinedReaction = new CombinedReaction(reaction1, reaction2);

                        //don't use empty or negative reactions (duplicates)
                        if (!combinedReaction.netChange().equals(MolecularFormula.emptyFormula()) && combinedReaction.netChange().getMass()>0){
                            reactionSet2.add(combinedReaction);
                        }
                        combinedReaction = new CombinedReaction(reaction1, reaction2.negate());

                        //don't use empty or negative reactions (duplicates)
                        if (!combinedReaction.netChange().equals(MolecularFormula.emptyFormula()) && combinedReaction.netChange().getMass()>0){
                            reactionSet2.add(combinedReaction);
                        }
                    }
                }

                //todo does not remove dublicates!!
                reactionsSet1 = new HashSet<>();
                int duplicate = 0;
                for (Reaction reaction : reactionSet2) {
                    if (!reactionsSetAll.contains(reaction)) reactionsSet1.add(reaction);
                    else {
                        duplicate++;
                    }
                }
//                System.out.println("duplicate "+duplicate+ " new "+reactionsSet1.size());
                reactionsSetAll.addAll(reactionsSet1);
            }
            reactionsSet = reactionsSetAll;
        }
        Reaction[] reactions = reactionsSet.toArray(new Reaction[0]);
        Set<MolecularFormula> formulas = new HashSet<>();
        for (Reaction reaction : reactions) {
            formulas.add(reaction.netChange());
        }
//        System.out.println("formulas: "+formulas.size());
//        System.out.println("reactions: "+reactions.length);
        return reactions;

    }

    private static Reaction parseReactionString(String string) {
        String[] reactants = string.split("->");
        if(reactants.length == 1) {
            return new SimpleReaction(MolecularFormula.parseOrThrow(reactants[0]));
        } else if(reactants.length == 2) {
            return new Transformation(MolecularFormula.parseOrThrow(reactants[0]), MolecularFormula.parseOrThrow(reactants[1]));
        } else {
            throw new RuntimeException("Error parsing reaction");
        }
    }

    /**
     *
     * cluster spectra based on same MF and retention time information?!
     * //todo removes compounds with no candidate
     * //todo possibly also use library hits?
     * //todo look ate tclusterCompoundsopN identifications?! best hit is not restrictive enough for high mass compounds
     * //todo does not look at CompoundQuality
     * @param candidateMap
     * @return mapping from cluster representative to cluster
     */
    public static Map<String, String[]> clusterCompoundsOld(Map<String, List<FragmentsCandidate>> candidateMap, Logger logger){
        List<String> idList = new ArrayList<>(candidateMap.keySet());

        Map<MolecularFormula, List<String>> bestMFToId = new HashMap<>();
        for (String id : idList) {
            final List<FragmentsCandidate> candidates = candidateMap.get(id);
            if (candidates.size()==0) continue;
            MolecularFormula mf = candidates.get(0).getFormula();
            List<String> ids = bestMFToId.get(mf);
            if (ids==null){
                ids = new ArrayList<>();
                bestMFToId.put(mf, ids);
            } else {
                //todo resolve
                Ms2Experiment experiment1 = candidateMap.get(ids.get(0)).get(0).getExperiment();
                Ms2Experiment experiment2 = candidates.get(0).getExperiment();
                if (experiment1.hasAnnotation(RetentionTime.class) && experiment2.hasAnnotation(RetentionTime.class)){
                    double time1 = experiment1.getAnnotationOrThrow(RetentionTime.class).getRetentionTimeInSeconds();
                    double time2 = experiment2.getAnnotationOrThrow(RetentionTime.class).getRetentionTimeInSeconds();
                    if (Math.abs(time1-time2)>20) logger.warn("merged compounds retention time differs by "+(Math.abs(time1-time2)));
                }
            }

            ids.add(id);
        }

        Map<String, String[]> idToCluster = new HashMap<>();
        for (List<String> ids : bestMFToId.values()) {
            double maxScore = Double.NEGATIVE_INFINITY;
            String bestId = null;
            for (String id : ids) {
                double score = candidateMap.get(id).get(0).getScore();
                if (score>maxScore){
                    maxScore = score;
                    bestId = id;
                }
            }
            assert bestId!=null;

            idToCluster.put(bestId, ids.toArray(new String[0]));
        }
        return idToCluster;
    }

    public static Map<String, List<FragmentsCandidate>> mergeCluster(Map<String, List<FragmentsCandidate>> candidateMap, Map<String, String[]> representativeToCluster){
        Map<String, List<FragmentsCandidate>> candidateMapNew = new HashMap<>();

        for (String repId : representativeToCluster.keySet()) {
            String[] clusterIds = representativeToCluster.get(repId);
            LibraryHit bestHit = null;
            MolecularFormula correct = null;
            for (String id : clusterIds) {
                List<FragmentsCandidate> candidates = candidateMap.get(id);
                if (candidates.get(0).hasLibraryHit()){
                    for (FragmentsCandidate candidate : candidates) {
                        if (candidate.isCorrect()){
                            LibraryHit hit = candidate.getLibraryHit();
                            if (bestHit==null){
                                bestHit = hit;
                                correct = candidate.getFormula();
                            } else {
                                if (hit.getCosine()>bestHit.getCosine()){
                                    bestHit = hit;
                                    correct = candidate.getFormula();
                                }
                            }
                        }
                    }
                }
            }



            List<FragmentsCandidate> repCandidates = candidateMap.get(repId);
            final FragmentsCandidate repC  = repCandidates.get(0);
            if (correct!=null && (!repC.hasLibraryHit() || !repC.getLibraryHit().getQueryExperiment().equals(bestHit.getQueryExperiment()))){
                //update with 'better' library hit
                for (FragmentsCandidate candidate : repCandidates) {
                    candidate.setLibraryHit(bestHit);
                    candidate.setInTrainingSet(true);
                    if (DummyFragmentCandidate.isDummy(candidate)) continue;
                    if (candidate.getFormula().equals(correct)){
                        candidate.setCorrect(true);
                    }
                }
            }

            candidateMapNew.put(repId, repCandidates);
        }

        return candidateMapNew;
    }

    /**
     * add dummy candidate which gains the score of all unconsidered candidates.
     * besides, the list of candidates (without dummy) is truncated to maxCandidates
     * @param candidateMap
     * @param maxCandidates truncate list of candidates to this size, if positive number
     * @param logger
     */
    public static void addNotExplainableDummyAndTruncateCandidateList(Map<String, List<FragmentsCandidate>> candidateMap, int maxCandidates, Logger logger){
        List<String> idList = new ArrayList<>(candidateMap.keySet());

        int missingCounter = 0;
        for (String id : idList) {
            List<FragmentsCandidate> candidates = candidateMap.get(id);
            if (candidateMap.size()==0) continue;
            Ms2Experiment experiment = candidates.get(0).getExperiment();

            UnconsideredCandidatesUpperBound unconsideredCandidatesUpperBound = candidates.get(0).getAnnotationOrNull(UnconsideredCandidatesUpperBound.class);

            if (unconsideredCandidatesUpperBound == null){
                ++missingCounter;
                continue;
            }

            double worstScore = unconsideredCandidatesUpperBound.getLowestConsideredCandidateScore();
            int numberOfIgnored = unconsideredCandidatesUpperBound.getNumberOfUnconsideredCandidates();

            if (maxCandidates>0 && candidates.size()>maxCandidates) {
                numberOfIgnored += candidates.size()-maxCandidates;

                candidates = candidates.subList(0,maxCandidates);
                candidateMap.put(id, candidates);

                worstScore = candidates.get(candidates.size()-1).getScore();
            }

            if (numberOfIgnored>0 && worstScore>0) {
                FragmentsCandidate dummyCandidate = DummyFragmentCandidate.newDummy(worstScore, numberOfIgnored, experiment);
                if (candidates.get(0).hasLibraryHit()) dummyCandidate.setLibraryHit(candidates.get(0).getLibraryHit());
                candidates.add(dummyCandidate);
            }
        }

        if (missingCounter>0){
            logger.warn("Cannot create dummy nodes for "+missingCounter+" compounds. Information missing.");
        }
    }


    //    private final static String IDX_HEADER = "FEATURE_ID";
    private final static String[] KNOWN_IDX_HEADER = new String[]{"FEATURE_ID", "#Scan#", "compoundName", "compoundID", "compound_name", "compound_name"};

    public static List<LibraryHit> parseLibraryHits(Path libraryHitsPath, List<Ms2Experiment> ms2Experiments, Logger logger) throws IOException {
        BufferedReader reader = Files.newBufferedReader(libraryHitsPath);
        String line = reader.readLine();
        reader.close();
        if (line==null) {
            throw new IOException("Spectral library hits file is empty.");
        }
        String[] header = line.split("\t");
        for (String idxHeader : KNOWN_IDX_HEADER) {
            if (arrayFind(header, idxHeader)>=0){
                logger.debug("Parsing spectral library hits file. Use "+idxHeader+" column to match library hits to compounds in the spectrum file.");
                return parseLibraryHitsByFeatureId(libraryHitsPath, ms2Experiments, idxHeader, logger);
            }
        }
        logger.error("Cannot parse spectral library file. Could not find ID column");
        return null;
    }

    private static List<LibraryHit> parseLibraryHitsByFeatureId(Path libraryHitsPath, List<Ms2Experiment> experiments, String idHeader, Logger logger) throws IOException {
        try {
            final Map<String, Ms2Experiment> experimentMap = new HashMap<>();
            for (Ms2Experiment ms2Experiment : experiments) {
                //todo removed clean string
                //                String name = cleanString(experiment.getName());
//               todo  String name = cleanString(experimentResult.getExperimentName()); vs
//                String name = cleanString(experimentResult.getExperiment().getName());
                String name = ms2Experiment.getName();
                if (experimentMap.containsKey(name)) throw new IOException("compound id duplicate: "+name+". Ids must be unambiguous to map library hits");
                experimentMap.put(name, ms2Experiment);
            }


            List<String> lines = Files.readAllLines(libraryHitsPath, Charset.defaultCharset());
            String[] header = lines.remove(0).split("\t");


            LibraryHitInfo idCol = new LibraryHitInfo(idHeader, true, null);
            LibraryHitInfo inchiCol = new LibraryHitInfo("INCHI", false, null);
            LibraryHitInfo smilesCol = new LibraryHitInfo("Smiles", false, null);
            LibraryHitInfo adductCol = new LibraryHitInfo(new String[]{"libraryAdduct", "Adduct"}, false, null);
            LibraryHitInfo cosineCol = new LibraryHitInfo(new String[]{"MQScore", "cosine"}, true, null);
            LibraryHitInfo sharePeaksCol = new LibraryHitInfo("SharedPeaks", false, "Infinity");
            LibraryHitInfo qualityCol = new LibraryHitInfo("Quality", false, "Unknown");
            LibraryHitInfo mfCol = new LibraryHitInfo("molecularFormula", false, null);
            LibraryHitInfo libMzCol = new LibraryHitInfo("libraryMz", false, null);

            LibraryHitInfo[] columnsOfInterest = new LibraryHitInfo[]{
                    idCol, inchiCol, smilesCol, adductCol, cosineCol, sharePeaksCol, qualityCol, mfCol, libMzCol
            };
            for (int i = 0; i < columnsOfInterest.length; i++) {
                LibraryHitInfo libraryHitInfo = columnsOfInterest[i];
                int idx = -1;
                for (String colName : libraryHitInfo.possibleColumnNames) {
                    idx = arrayFind(header, colName);
                    if (idx>=0){
                        break;
                    } else {
                        int[] more = arrayFindSimilar(header, colName);
                        if (more.length>1) throw new IOException("Cannot parse spectral library hits file. Column "+colName+" ambiguous.");
                        else if (more.length==1){
                            idx = more[0];
                            break;
                        }
                    }

                }
                if (idx<0) {
                    if (libraryHitInfo.isMandatory){
                        throw new IOException("Cannot parse spectral library hits file. Column "+Arrays.toString(libraryHitInfo.possibleColumnNames)+" not found.");
                    } else {
                        idx = -1;
                    }
                }

                libraryHitInfo.colIdx = idx;

            }


            List<LibraryHit> libraryHits = new ArrayList<>();
            for (String line : lines) {
                try {
                    String[] cols = line.split("\t",-1);
                    final String featureId = idCol.getInfo(cols);

                    final Ms2Experiment experiment = experimentMap.get(featureId);

                    if (experiment==null){
                        logger.warn("No compound in SIRIUS workspace found that corresponds to spectral library hit " +
                                "(this will occur for multiple charged compounds which are not supported by Sirius or the library file is incorrect). " +
                                idHeader+" "+featureId);
                        continue;
                    }

                    String mfString = mfCol.getInfo(cols);
                    String inchiString = inchiCol.getInfo(cols);
                    if (inchiString!=null) inchiString = inchiString.replace("\"", "");
                    String smilesString = smilesCol.getInfo(cols);
                    if (smilesString!=null) smilesString = smilesString.replace("\"", "");
                    final MolecularFormula formula = getFormulaFromStructure(mfString, inchiString, smilesString);

                    if (formula==null){
                        logger.warn("Cannot parse molecular formula of library hit. "+idHeader+" "+featureId);
                        continue;
                    }

                    String adductString = adductCol.getInfo(cols);
                    if (adductString==null || adductString.replace(" ","").length()==0){
                        logger.warn("Cannot parse adduct information for library hit. "+idHeader+" "+featureId);
                    }

                    String cosineString = cosineCol.getInfo(cols);
                    if (cosineString==null || cosineString.replace(" ","").length()==0){
                        logger.warn("Cannot parse library hit. Reason: cosine score information missing. "+idHeader+" "+featureId);
                        continue;
                    }
                    String sharePeaksString = sharePeaksCol.getInfo(cols);
                    if (sharePeaksString==null || sharePeaksString.replace(" ","").length()==0){
                        logger.warn("Cannot parse library hit. Reason: number of shared peaks missing. "+idHeader+" "+featureId);
                        continue;
                    }

                    String qualityString = qualityCol.getInfo(cols);
                    if (qualityString==null || qualityString.replace(" ","").length()==0){
                        logger.warn("Cannot parse quality information for library hit. Use 'unknown'. "+idHeader+" "+featureId);
                        qualityString = qualityCol.fallBack;
                    }

                    String libMzString = libMzCol.getInfo(cols);
                    if (libMzString==null || qualityString.replace(" ","").length()==0){
                        logger.warn("Cannot parse library mz. "+idHeader+" "+featureId);
                        libMzString = libMzCol.fallBack;
                    }


                    final String structure = (isInchi(inchiString) ? inchiString : smilesString);
                    final PrecursorIonType ionType = adductString==null?null: PeriodicTable.getInstance().ionByName(adductString);
                    final double cosine = Double.parseDouble(cosineString);
                    final int sharedPeaks = parseIntegerOrThrow(sharePeaksString);
                    LibraryHitQuality quality = LibraryHitQuality.valueOf(qualityString); // never null
                    double libMz;
                    if (libMzString != null) libMz = Double.parseDouble(libMzString);
                    else if (ionType!=null) libMz = ionType.neutralMassToPrecursorMass(formula.getMass());
                    else{
//                        libMz = Double.NaN;
//                        logger.warn("Cannot infer library mz. Skip "+idHeader+" "+featureId);
//                        continue;
                        libMz = experiment.getIonMass();
                        logger.warn("Cannot infer library mz. Use precursor m/z instead: "+idHeader+" "+featureId);
                    }

                    LibraryHit libraryHit = new LibraryHit(experiment, formula, structure, ionType, cosine, sharedPeaks, quality, libMz);
                    libraryHits.add(libraryHit);
                } catch (Exception e) {
                    logger.error("Cannot parse library hit. Reason: "+ e.getMessage(),e);
                }

            }

            return libraryHits;
        } catch (Exception e){
            throw new IOException("cannot parse library hits. Reason "+e.getMessage());
        }
    }

    private static class LibraryHitInfo {
        private String[] possibleColumnNames;
        private boolean isMandatory;
        private String fallBack;
        private int colIdx;

        public LibraryHitInfo(String possibleColumnName, boolean isMandatory, String fallBack) {
            this(new String[]{possibleColumnName}, isMandatory, fallBack);
        }

        public LibraryHitInfo(String[] possibleColumnNames, boolean isMandatory, String fallBack) {
            this.possibleColumnNames = possibleColumnNames;
            this.isMandatory = isMandatory;
            this.fallBack = fallBack;
        }

        public String getInfo(String[] row){
            if (colIdx<0){
                if (isMandatory) throw new RuntimeException("Option is mandatory but column unknown");
                return fallBack;
            }
            return row[colIdx];
        }
    }


    private static int parseIntegerOrThrow(String value) {
        double d = Double.parseDouble(value);
        if (d==Double.POSITIVE_INFINITY) return Integer.MAX_VALUE;
        if (d==Double.NEGATIVE_INFINITY) return Integer.MIN_VALUE;
        int i = (int)Math.round(d);
        if (Math.abs(d-i)>0.01) throw new NumberFormatException(value+" in not an integer value");
        return i;
    }

    private static MolecularFormula getFormulaFromStructure(String formulaString, String inchi, String smiles){
        if (formulaString!=null && formulaString.length()>0) return MolecularFormula.parseOrThrow(formulaString);

        MolecularFormula formula = null;
        if (isInchi(inchi)){
            try {
                formula = InChIs.extractFormula(inchi);
            } catch (UnknownElementException e) {
                LoggerFactory.getLogger(ZodiacUtils.class).warn("Cannot parse molecular formula from InChI.: "+inchi);
                formula = null;
            }
        }

        if (formula==null && smiles!=null && smiles.length()>0){
            try {
                final SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
                final IAtomContainer c = parser.parseSmiles(smiles);
                formulaString = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(c));
                formula = MolecularFormula.parseOrThrow(formulaString);
            } catch (CDKException e) {
                return null;
            }
        }
        return formula;
    }

    private static boolean isInchi(String inchi) {
        if (inchi==null) return false;
        if (!inchi.toLowerCase().startsWith("inchi=")) return false;
        int idx1 = inchi.indexOf("/");
        int idx2 = inchi.indexOf("/", idx1+1);
        if (idx1>0 && idx2>0 && (idx2-idx1)>1) return true;
        return false;
    }

    private static <T> int arrayFind(T[] array, T object) {
        for(int i = 0; i < array.length; ++i) {
            Object t = array[i];
            if(t.equals(object)) {
                return i;
            }
        }

        return -1;
    }

    private static int arrayFindIgnoreCase(String[] array, String s) {
        for(int i = 0; i < array.length; ++i) {
            String t = array[i];
            if(t.equalsIgnoreCase(s)) {
                return i;
            }
        }
        return -1;
    }

    private static int[] arrayFindSimilar(String[] array, String object) {
        TIntArrayList positions = new TIntArrayList();
        for(int i = 0; i < array.length; ++i) {
            String t = array[i];
            if(t.toLowerCase().contains(object.toLowerCase())) {
                positions.add(i);
            }
        }

        return positions.toArray();
    }

    private static TCharSet forbidden = new TCharHashSet(new char[]{' ',':','\\','/','[',']', '_'});
    private static String cleanString(String s){
        final StringBuilder builder = new StringBuilder(s.length());
        final char[] chars = s.toCharArray();
        for (char c : chars) {
            if (!forbidden.contains(c)) builder.append(c);
        }
        return builder.toString();
    }

    public static Map<String, String[]> clusterCompounds(Map<String, List<FragmentsCandidate>> candidateMap, Logger logger){
        final HashMap<String, String[]> clusters = new HashMap<>();
        final String[] keys = candidateMap.keySet().toArray(String[]::new);
        Arrays.sort(keys, Comparator.comparingInt(u->candidateMap.get(u).get(0).getFragments().length).reversed());
        Deviation deviation = candidateMap.get(keys[0]).get(0).getExperiment().getAnnotation(MS1MassDeviation.class).map(x -> x.allowedMassDeviation).orElse(new Deviation(20, 0.01)).divide(2);
        final HashSet<MolecularFormula> formulaSet = new HashSet<>();
        final HashSet<String> alreadyClustered = new HashSet<>();
        final ArrayList<String> cluster = new ArrayList<>();
        System.out.println("START CLUSTERING");
        for (int i=0; i < keys.length; ++i) {
            final String left = keys[i];
            if (alreadyClustered.contains(left))
                continue;
            else
                alreadyClustered.add(left);
            final List<FragmentsCandidate> L = candidateMap.get(left);
            Ms2Experiment leftExp = L.get(0).getExperiment();
            cluster.clear();
            outer:
            for (int j = i+1; j < keys.length; ++j) {
                final String right = keys[j];
                if (alreadyClustered.contains(right))
                    continue outer;
                final List<FragmentsCandidate> R = candidateMap.get(right);
                if (deviation.inErrorWindow(leftExp.getIonMass(), R.get(0).getExperiment().getIonMass())) {
                    int bestCount = 0;
                    // compare molecular formulas of top 3 candidates
                    for (int a = 0; a < Math.min(5, L.size()); ++a) {
                        for (int b = 0; b < Math.min(5, R.size()); ++b) {
                            if (L.get(a).getFormula().equals(R.get(b).getFormula())) {
                                formulaSet.clear();
                                // if at least 75% and min 3 of the nodes are the same, merge the compounds
                                for (FragmentWithIndex f : L.get(a).getFragments()) formulaSet.add(f.mf);
                                int count = 0;
                                for (FragmentWithIndex f : R.get(b).getFragments()) {
                                    if (formulaSet.contains(f.mf)) {
                                        ++count;
                                    }
                                }
                                bestCount = Math.max(count,bestCount);
                                if (count >= 3 && count >= Math.floor(0.75 * Math.min(L.get(a).getFragments().length, R.get(b).getFragments().length))) {
                                    // similar enough. Cluster these compounds!
                                    cluster.add(right);
                                    alreadyClustered.add(right); // never cluster a compound twice
                                    System.out.println("Cluster " + left + " with " + right + " because of " + count + " common fragments");
                                    continue outer;
                                }
                            }
                        }
                    }
                    System.out.println("DO NOT cluster " + left + " with " + right + " because of " + bestCount + " common fragments");
                }
            }
            if (cluster.size() > 0) {
                cluster.add(left);
                // find id with best score
                cluster.sort(Comparator.comparingDouble(x -> -candidateMap.get(x).get(0).getScore()));
                clusters.put(cluster.get(0), cluster.toArray(String[]::new));
            } else {
                clusters.put(left, new String[]{left});
            }
        }
        clusters.entrySet().stream().forEach(x->System.out.println(x.getKey() + " -> " + Arrays.toString(x.getValue())));
        return clusters;
    }



    //////////////////////////////////////////////////////////////////////////////////////////
    // write results
    //////////////////////////////////////////////////////////////////////////////////////////



    public static void writeResultSummary(Map<Ms2Experiment, Map<FTree, ZodiacScore>> zodiacScoredTrees, CompoundResult<FragmentsCandidate>[] zodiacResults, Path outputFile) throws IOException {
        Ms2Experiment[] experimentsSortedByMass = zodiacScoredTrees.keySet().stream().sorted(Comparator.comparing(Ms2Experiment::getIonMass)).toArray(size->new Ms2Experiment[size]);

        Scored<FTree>[] bestInitial = bestInitial(experimentsSortedByMass, zodiacScoredTrees);
        writeZodiacOutput(experimentsSortedByMass, bestInitial, zodiacResults, outputFile); // outputPath.resolve("zodiac_summary.csv"));
    }

    private final static int NUMBER_OF_HITS = Integer.MAX_VALUE;
    private final static String SEP = "\t";
    public static void writeZodiacOutput(Ms2Experiment[] sortedExperiments, Scored<FTree>[] initial, CompoundResult<FragmentsCandidate>[] result, Path outputPath) throws IOException {
        Map<Ms2Experiment, CompoundResult<FragmentsCandidate>> experimentToZodiacResult = new HashMap<>();
        for (CompoundResult<FragmentsCandidate> compoundResult : result) {
            if (compoundResult.getCandidates().length>0){
                Ms2Experiment experiment = compoundResult.getCandidates()[0].getCandidate().getExperiment();
                experimentToZodiacResult.put(experiment, compoundResult);

            }
        }

        BufferedWriter writer = Files.newBufferedWriter(outputPath, Charset.defaultCharset());
        writer.write("id" + SEP + "quality" + SEP + "precursorMass" + SEP + "SiriusMF" + SEP + "SiriusScore" + SEP + "numberOfCandidates" + SEP + "hasDummy" + SEP + "connectedCompounds" + SEP + "biggestTreeSize" + SEP + "maxExplainedIntensity" + SEP + "ZodiacMF" + SEP + "ZodiacMFIon"+ SEP + "ZodiacScore" + SEP + "treeSize" + SEP + "explainedIntensity");
        int maxCandidates = maxNumberOfCandidates(result);
        for (int i = 2; i <= maxCandidates; i++) {
            writer.write(SEP + "ZodiacMF" + String.valueOf(i) + SEP + "ZodiacMFIon" + String.valueOf(i) + SEP + "ZodiacScore" + String.valueOf(i) + SEP + "treeSize" + String.valueOf(i) + SEP + "explainedIntensity" + String.valueOf(i));
        }

        for (int i = 0; i < sortedExperiments.length; i++) {
            final Ms2Experiment experiment = sortedExperiments[i];
            CompoundResult<FragmentsCandidate> compoundResult = experimentToZodiacResult.get(experiment);
            if (compoundResult==null) {
                LoggerFactory.getLogger(ZodiacUtils.class).warn("could not find ZODIAC result for compound  "+experiment.getName());
                continue;
//                throw new RuntimeException("could not find ZODIAC result for compound  "+experiment.getName());
            }

            final String siriusMF = initial[i]==null?null:initial[i].getCandidate().getRoot().getFormula().formatByHill();
            final double siriusScore = initial[i]==null?Double.NaN:initial[i].getScore();

            int connections = compoundResult.getAnnotationOrThrow(Connectivity.class).getNumberOfConnectedCompounds();
            String summeryLine = createSummaryLine(experiment.getName(), siriusMF, siriusScore, connections, compoundResult.getCandidates());
            writer.newLine();
            writer.write(summeryLine);
        }

        writer.close();

    }

    /*
    create a matrix of similarities between all compounds based on ZODIAC edge scores of best MF candidates
     */
    public static void writeSimilarityGraphOfBestMF(ZodiacResultsWithClusters clusterResults, Path outputFile) throws IOException {
        Graph<FragmentsCandidate> graph = clusterResults.getGraph();
        CompoundResult<FragmentsCandidate>[] results = clusterResults.getResults();
        String[] ids = clusterResults.getIds();
        String headerWithIds = Arrays.stream(ids).collect(Collectors.joining(SEP));

        int[] indexInZODIACGraphForBestMFs = new int[results.length];
        for (int i = 0; i < results.length; i++) {
            CompoundResult<FragmentsCandidate> result = results[i];
            int index = result.getCandidates()[0].getCandidate().getIndexInGraph();
            if (index<0) {
                LoggerFactory.getLogger(ZodiacUtils.class).error("Index not set for candidate "+result.getId()+". Cannot write ZODIAC graph.");
                return;
            }
            indexInZODIACGraphForBestMFs[i] = index;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, Charset.defaultCharset())){
            writer.write(headerWithIds);

            for (int i = 0; i < indexInZODIACGraphForBestMFs.length; i++) {
                int indexI = indexInZODIACGraphForBestMFs[i];
                StringJoiner joiner = new StringJoiner(SEP);
                for (int j = 0; j < indexInZODIACGraphForBestMFs.length; j++) {
                    int indexJ = indexInZODIACGraphForBestMFs[j];
                    double edgeWeight = graph.getLogWeight(indexI, indexJ);
                    joiner.add(String.valueOf(edgeWeight));
                }

                String line = joiner.toString();
                writer.newLine();
                writer.write(line);
                writer.flush();
            }

        }
    }

    private static int maxNumberOfCandidates(CompoundResult<FragmentsCandidate>[] result) {
        int max = 1;
        for (CompoundResult<FragmentsCandidate> fragmentsCandidateCompoundResult : result) {
            max = Math.max(fragmentsCandidateCompoundResult.getCandidates().length, max);
        }
        return max;
    }

    private static String createSummaryLine(String id, String siriusMF, double siriusScore, int numberConnections, Scored<FragmentsCandidate>[] result){
        String qualityString = "";
        Sirius sirius = new Sirius();
        double precursorMass = Double.NaN;
        if (result.length>0){
            Ms2Experiment experiment = result[0].getCandidate().getExperiment();

            CompoundQuality compoundQuality = experiment.getAnnotationOrNull(CompoundQuality.class);
            precursorMass = experiment.getIonMass();
            if (compoundQuality!=null) qualityString = compoundQuality.toString();
        }

        int biggestTreeSize = -1;
        double maxExplainedIntensity = 0;
        boolean hasDummy = false;
        for (Scored<FragmentsCandidate> scoredCandidate : result) {
            FragmentsCandidate candidate = scoredCandidate.getCandidate();
            if (DummyFragmentCandidate.isDummy(candidate)){
                hasDummy = true;
                continue;
            }
            final int treeSize = candidate.getFragments().length;
            final FTree tree = candidate.getAnnotation(FTree.class);
            final double intensity = (new FTreeMetricsHelper(tree)).getExplainedIntensityRatio();
            biggestTreeSize = Math.max(treeSize,biggestTreeSize);
            maxExplainedIntensity = Math.max(maxExplainedIntensity, intensity);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(id);
        builder.append(SEP);
        builder.append(qualityString);
        builder.append(SEP);
        builder.append(String.valueOf(precursorMass));
        builder.append(SEP);
        builder.append(siriusMF);
        builder.append(SEP);
        builder.append(Double.toString(siriusScore));
        builder.append(SEP);
        builder.append(String.valueOf(result.length)); //numberOfCandidates
        builder.append(SEP);
        builder.append(String.valueOf(hasDummy));
        builder.append(SEP);
        builder.append(numberConnections);
        builder.append(SEP);
        builder.append(biggestTreeSize);
        builder.append(SEP);
        builder.append(maxExplainedIntensity);



        for (int j = 0; j < Math.min(result.length, NUMBER_OF_HITS); j++) {
            Scored<FragmentsCandidate> currentResult = result[j];
            FragmentsCandidate candidate = currentResult.getCandidate();
            final String mf = candidate.getFormula().formatByHill();
            final String ion = candidate.getIonType().toString();
            final double score = currentResult.getScore();
            final double treeSize, explIntensity;
            if (DummyFragmentCandidate.isDummy(candidate)){
                treeSize = -1;
                explIntensity = -1;
            } else {
                treeSize = currentResult.getCandidate().getFragments().length;//number of fragments = treeSize
                final FTree tree = candidate.getAnnotation(FTree.class);
                explIntensity = (new FTreeMetricsHelper(tree)).getExplainedIntensityRatio();
            }

//            if (score <= 0) break; //don't write MF with 0 probability

            builder.append(SEP);
            builder.append(mf);
            builder.append(SEP);
            builder.append(ion);
            builder.append(SEP);
            builder.append(Double.toString(score));
            builder.append(SEP);
            builder.append(Double.toString(treeSize));
            builder.append(SEP);
            builder.append(Double.toString(explIntensity));
        }
        return builder.toString();
    }


    public static Scored<FTree>[] bestInitial(Ms2Experiment[] sortedExperiments, Map<Ms2Experiment, Map<FTree, ZodiacScore>> zodiacScoredTrees){
        Scored<FTree>[] best = new Scored[sortedExperiments.length];
        for (int i = 0; i < sortedExperiments.length; i++) {
            Ms2Experiment experiment = sortedExperiments[i];
            Map<FTree, ZodiacScore> scoredTrees = zodiacScoredTrees.get(experiment);

            if (scoredTrees.size()==0){
                best[i] = null;
                continue;
            }

            TDoubleArrayList siriusScores = new TDoubleArrayList();
            Scored<FTree>[] siriusScoredTrees = new Scored[scoredTrees.keySet().size()];
            int pos = 0;
            for (FTree tree : scoredTrees.keySet()) {
                siriusScoredTrees[pos++] = new Scored<>(tree, (new FTreeMetricsHelper(tree)).getSiriusScore());

            }

            Arrays.sort(siriusScoredTrees, Comparator.reverseOrder());
            double max = siriusScoredTrees[0].getScore();
            double maxExp = Double.NaN;
            double sum = 0.0D;
            //normalize
            for (int j = 0; j < siriusScoredTrees.length; ++j) {
                final Scored<FTree> scoredTree = siriusScoredTrees[j];
                double expS = Math.exp(1d * (scoredTree.getScore() - max));
                sum += expS;
                if (j==0) maxExp = expS;
            }

            //save best with probability
            best[i] = new Scored(siriusScoredTrees[0].getCandidate(), maxExp/sum);


        }
        return best;
    }

}
