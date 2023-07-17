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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.IonTrace;
import de.unijena.bioinf.ChemistryBase.ms.lcms.Trace;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.lcms.ionidentity.AdductResolver;
import de.unijena.bioinf.lcms.quality.LCMSQualityCheck;
import de.unijena.bioinf.lcms.quality.LCMSQualityCheckResult;

import java.util.*;
import java.util.stream.Collectors;

public class LCMSCompoundSummary {

    public CoelutingTraceSet traceSet;
    public IonTrace compoundTrace;
    public Ms2Experiment ms2Experiment;

    public LCMSQualityCheckResult peakQualityResult, isotopeQualityResult, adductQualityResult, ms2QualityResult;

    public LCMSCompoundSummary(CoelutingTraceSet traceSet, IonTrace compoundTrace, Ms2Experiment ms2Experiment) {
        this.traceSet = traceSet;
        this.compoundTrace = compoundTrace;
        this.ms2Experiment = ms2Experiment;
        this.peakQualityResult = checkPeak();
        this.isotopeQualityResult = checkIsotopes();
        this.adductQualityResult = checkAdducts();
        this.ms2QualityResult = checkMs2();
    }

    public int points() {
        return peakQualityResult.getQuality().ordinal() + isotopeQualityResult.getQuality().ordinal() + adductQualityResult.getQuality().ordinal() + ms2QualityResult.getQuality().ordinal();
    }

    public LCMSQualityCheck.Quality getPeakQuality() {
        return peakQualityResult.getQuality();
    }

    public LCMSQualityCheck.Quality getIsotopeQuality() {
        return isotopeQualityResult.getQuality();
    }

    public LCMSQualityCheck.Quality getAdductQuality() {
        return adductQualityResult.getQuality();
    }

    public LCMSQualityCheck.Quality getMs2Quality() {
        return ms2QualityResult.getQuality();
    }

    private LCMSQualityCheckResult checkAdducts() {
        List<LCMSQualityCheck> adductCheck = new ArrayList<>();
        LCMSQualityCheck.Quality adductQuality;
        HashSet<PrecursorIonType> possibleIonTypes;
        possibleIonTypes = new HashSet<>(ms2Experiment.getAnnotationOrDefault(AdductSettings.class).getDetectable(ms2Experiment.getPrecursorIonType().getCharge()));
        possibleIonTypes = new HashSet<>(Arrays.asList(
                PrecursorIonType.fromString("[M+Na]+"),
                PrecursorIonType.fromString("[M+K]+"),
                PrecursorIonType.fromString("[M+H]+"),
                PrecursorIonType.fromString("[M-H2O+H]+"),
                PrecursorIonType.fromString("[M-H4O2+H]+"),
                PrecursorIonType.fromString("[M-H2O+Na]+"),
                PrecursorIonType.fromString("[M + FA + H]+"),
                PrecursorIonType.fromString("[M + ACN + H]+"),
                PrecursorIonType.fromString("[M + ACN + Na]+"),
                //PrecursorIonType.fromString("[M-H+Na2]+"),
                PrecursorIonType.fromString("[M+NH3+H]+"),
                PrecursorIonType.fromString("[M-H]-"),
                PrecursorIonType.fromString("[M+Cl]-"),
                PrecursorIonType.fromString("[M+Br]-"),
                PrecursorIonType.fromString("[M-H2O-H]-"),
                PrecursorIonType.fromString("[M-H]-"),
                PrecursorIonType.fromString("[M+Cl]-"),
                PrecursorIonType.fromString("[M+Br]-"),
                PrecursorIonType.fromString("[M-H2O-H]-"),

                PrecursorIonType.fromString("[M + CH2O2 - H]-"),
                PrecursorIonType.fromString("[M + C2H4O2 - H]-"),
                PrecursorIonType.fromString("[M + H2O - H]-"),
                PrecursorIonType.fromString("[M - H3N - H]-"),
                PrecursorIonType.fromString("[M - CO2 - H]-"),
                PrecursorIonType.fromString("[M - CH2O3 - H]-"),
                PrecursorIonType.fromString("[M - CH3 - H]-"),
                PrecursorIonType.fromString("[M+Na-2H]-")
        ));
        possibleIonTypes.removeIf(x->x.getCharge() != ms2Experiment.getPrecursorIonType().getCharge());


        LCMSQualityCheck.QualityCategory category = LCMSQualityCheck.QualityCategory.ADDUCTS;
        String identifier;

        final double ionmz = compoundTrace.getMonoisotopicPeak().getApexMass();
        final AdductResolver resolver = new AdductResolver(ionmz, possibleIonTypes);
//        System.out.println(possibleIonTypes);
        final IonTrace[] adducts = traceSet.getIonTrace().getAdducts();
        for (int i = 0; i < adducts.length; ++i) {
            resolver.addAdduct(adducts[i].getMonoisotopicPeak().getApexMass(), adducts[i].getCorrelationScores().length > 0 ? adducts[i].getCorrelationScores()[0] : 1e-3);
        }
        final List<AdductResolver.AdductAssignment> assignments = resolver.getAssignments();


        identifier = "adductAssignment";
        LCMSQualityCheck.ParameterValue numberOfAdductsParameter = new LCMSQualityCheck.ParameterValue(assignments.size(), "numberOfAdducts", "Number of possible adducts detected based on correlating features.");
        if (assignments.size()==0) {
            adductCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                    "No adducts found.",
                    //additional parameters not used for string formatting
                    numberOfAdductsParameter
                    ));
            adductQuality = LCMSQualityCheck.Quality.MEDIUM;
            return new LCMSQualityCheckResult(adductCheck, adductQuality);
        }
        final AdductResolver.AdductAssignment bestAss = assignments.get(0);
        LCMSQualityCheck.ParameterValue bestAssParameter = new LCMSQualityCheck.ParameterValue(bestAss.ionType.toString(), "bestAdduct", "Best adduct assignement.");
        LCMSQualityCheck.ParameterValue supportedIonsParameter = new LCMSQualityCheck.ParameterValue(bestAss.supportedIons, "supportedIons", "Number of correlated ions that support this adduct.");
        LCMSQualityCheck.ParameterValue probabilityParameter = new LCMSQualityCheck.ParameterValue(bestAss.probability, "probability", "Probability of adduct assignemnt.");

        if (assignments.size()==1) {
            boolean strongSupport = (bestAss.probability>=0.9 || bestAss.supportedIons>=3);
            adductCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                    "Adduct assignment is unambigous " + (strongSupport ? ":": ", but support is not strong.") + " Ion is annotated as %s with %s correlated ions and probability %.2f",
                    //for string format
                    bestAssParameter, supportedIonsParameter, probabilityParameter,
                    //additional parameters not used for string formatting
                    numberOfAdductsParameter
            ));
        } else if (assignments.size()>1) {
            StringBuilder alternativeAssignments = new StringBuilder();
            alternativeAssignments.append(". Alternative assignments are ");
            for (int j=1; j < assignments.size(); ++j) {
                alternativeAssignments.append(String.format(Locale.US, "%s with probability %.2f", assignments.get(j).ionType, assignments.get(j).probability));
                if (j < assignments.size()-1) {
                    alternativeAssignments.append(", ");
                } else {
                    alternativeAssignments.append(".");
                }
            }
            LCMSQualityCheck.ParameterValue alternativeAdductsParameter = new LCMSQualityCheck.ParameterValue(alternativeAssignments, "alternativeAdducts", "Alternative adduct assignments and their probabilities");

            String formatTextPrefix;
            LCMSQualityCheck.Quality quality;
            if (bestAss.probability>=0.9) {
                formatTextPrefix = "Adduct assignment is ambigous, but %s has strong support";
                quality = LCMSQualityCheck.Quality.GOOD;
            } else if (bestAss.probability>0.5) {
                formatTextPrefix = "Adduct assignment is ambigous, but %s has good support";
                quality = LCMSQualityCheck.Quality.MEDIUM;
            } else {
                formatTextPrefix = "Adduct assignment is ambigous. Best assignment is %s";
                quality = LCMSQualityCheck.Quality.LOW;
            }
            String formatTextDetails = " with %s correlated ions and probability %.2f. Alternative assignments are %s";
            adductCheck.add(new LCMSQualityCheck(quality, category, identifier,
                    formatTextPrefix + formatTextDetails,
                    //for string format
                    bestAssParameter, supportedIonsParameter, probabilityParameter, alternativeAdductsParameter,
                    //additional parameters not used for string formatting
                    numberOfAdductsParameter
            ));
        }


        identifier = "adductAssignmentMassDeltaInterpretationDetails";
        LCMSQualityCheck.ParameterValue massDeltaParameter = new LCMSQualityCheck.ParameterValue(null, "massDelta", "Mass difference between the ion feature and the supporting ion.");
        LCMSQualityCheck.ParameterValue adductRelationParameter = new LCMSQualityCheck.ParameterValue(null, "adductRelation", "The adduct explanations of the ion feature and the supporting ion.");
        for (int j=0; j < bestAss.supportedIonTypes.length; ++j) {
            adductCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                    "Mass delta %f can be interpreted as %s",
                    //for string format
                    massDeltaParameter.withValue(bestAss.supportedIonMzs[j]-ionmz), adductRelationParameter.withValue(bestAss.ionType + " -> " + bestAss.supportedIonTypes[j])
            ));
        }

        for (int i=1; i < assignments.size(); ++i) {
            for (int j = 0; j < assignments.get(i).supportedIonTypes.length; ++j) {
                adductCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.LOW, category, identifier,
                        "Mass delta %f can be interpreted as %s",
                        //for string format
                        massDeltaParameter.withValue(assignments.get(i).supportedIonMzs[j] - ionmz),
                        adductRelationParameter.withValue(assignments.get(i).ionType + " -> " + assignments.get(i).supportedIonTypes[j])
                ));
            }
        }

        identifier = "adductMassDeltas";
        double[] massDeltas = Arrays.stream(traceSet.getIonTrace().getAdducts()).mapToDouble(x -> x.getMonoisotopicPeak().getApexMass()-ionmz).toArray();
        String massDeltaString = Arrays.stream(massDeltas).mapToObj(x->String.format(Locale.US, "%.3f", x)).collect(Collectors.joining(", "));
        adductCheck.add(new LCMSQualityCheck(
                traceSet.getIonTrace().getAdducts().length>2 ? LCMSQualityCheck.Quality.GOOD : LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                "Adduct mass deltas are: " + massDeltaString, //preformatted string
                //additional parameters not used for string formatting
                new LCMSQualityCheck.ParameterValue(massDeltas, "massDeltas", "mass differences of correlating ions")
        ));
        adductQuality = adductCheck.get(0).getQuality();

        return new LCMSQualityCheckResult(adductCheck, adductQuality);
    }

    private LCMSQualityCheckResult checkMs2() {
        return new Ms2Checker(ms2Experiment).performCheck();
    }

    private LCMSQualityCheckResult checkIsotopes() {
        return new IsotopesChecker(traceSet, compoundTrace).performCheck();
    }

    private LCMSQualityCheckResult checkPeak() {
        return new PeakChecker(traceSet, compoundTrace).performCheck();
    }

    public static LCMSQualityCheck.Quality checkPeakQuality(CoelutingTraceSet traceSet, IonTrace compoundTrace){
        LCMSQualityCheckResult result = new PeakChecker(traceSet, compoundTrace).performCheck();
        return  result.getQuality();
    }


    protected class Ms2Checker implements Checker {
        private final Ms2Experiment ms2Experiment;

        public Ms2Checker(Ms2Experiment ms2Experiment) {
            this.ms2Experiment = ms2Experiment;
        }

        @Override
        public LCMSQualityCheckResult performCheck() {
            ArrayList<LCMSQualityCheck> ms2Check=new ArrayList<>();
            if (ms2Experiment!=null) {
                final MassToFormulaDecomposer mfd = new MassToFormulaDecomposer(
                        new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNOPS").elementArray())
                );
                final FormulaConstraints fcr = new FormulaConstraints("CHNOPS");
                final Deviation dev = new Deviation(5);
                final PrecursorIonType ionType = PrecursorIonType.getPrecursorIonType("[M+H]+");
                // check number of peaks and number of peaks with intensity above 3%
                int npeaks=0, nIntensivePeaks=0, total = 0;
                final HashSet<CollisionEnergy> ces = new HashSet<>();
                final Optional<NoiseInformation> noiseModel = ms2Experiment.getAnnotation(NoiseInformation.class);
                for (Ms2Spectrum<Peak> ms2 : ms2Experiment.getMs2Spectra()) {
                    ces.add(ms2.getCollisionEnergy());
                    final double mx = Spectrums.getMaximalIntensity(ms2);
                    final double threshold = noiseModel.map(NoiseInformation::getSignalLevel).orElse(mx*0.03);
                    for (int k=0; k < ms2.size(); ++k) {
                        final double mass = ionType.precursorMassToNeutralMass(ms2.getMzAt(k));
                        if (mass >= ms2Experiment.getIonMass()-10)
                            continue;
                        final Iterator<MolecularFormula> molecularFormulaIterator = mfd.neutralMassFormulaIterator(mass, dev, fcr);
                        boolean found=false;
                        while (molecularFormulaIterator.hasNext()) {
                            final MolecularFormula F = molecularFormulaIterator.next();
                            if (F.rdbe()>=0) {
                                found=true;
                                break;
                            }
                        }

                        if (found) {
                            if (ms2.getIntensityAt(k) >= threshold) {
                                ++nIntensivePeaks;
                            }
                            ++npeaks;
                        }
                        ++total;
                    }
                }

                int score = 0;

                LCMSQualityCheck.QualityCategory category = LCMSQualityCheck.QualityCategory.MSMS;
                String identifier;

                identifier = "msMsNumDecomposablePeaks";
                LCMSQualityCheck.ParameterValue npeaksParameter = new LCMSQualityCheck.ParameterValue<>(npeaks, "numberOfPeaks", "Number of decomposable peaks.");
                LCMSQualityCheck.Quality decompQuality;
                if (npeaks >= 20) {
                    decompQuality = LCMSQualityCheck.Quality.GOOD;
                    score += 5;
                } else if (npeaks >= 10) {
                    decompQuality = LCMSQualityCheck.Quality.MEDIUM;
                    score += 3;
                } else {
                    decompQuality = LCMSQualityCheck.Quality.LOW;
                    score += 1;
                }
                ms2Check.add(new LCMSQualityCheck(
                        decompQuality, category, identifier,
                        "Ms/Ms has " + (decompQuality== LCMSQualityCheck.Quality.LOW?"only ":"") + "%d decomposable peaks.",
                        //for string format
                        npeaksParameter
                        //additional parameters not used for string formatting
                ));

                identifier = "msMsNumIntensiveDecomposablePeaks";
                LCMSQualityCheck.ParameterValue nIntensivePeaksParameter = new LCMSQualityCheck.ParameterValue<>(nIntensivePeaks, "numberOfIntensivePeaks", "Number of decomposable peaks above noise threshold.");
                LCMSQualityCheck.Quality numPeaksQuality;
                if (nIntensivePeaks >= 6) {
                    numPeaksQuality = LCMSQualityCheck.Quality.GOOD;
                    score += 3;
                } else if (nIntensivePeaks >= 3) {
                    numPeaksQuality = LCMSQualityCheck.Quality.MEDIUM;
                    score += 2;
                } else {
                    numPeaksQuality = LCMSQualityCheck.Quality.LOW;
                    score += 1;
                }

                ms2Check.add(new LCMSQualityCheck(
                        numPeaksQuality, category, identifier,
                        "Ms/Ms has " + (nIntensivePeaks == 0 ? "no" : (nIntensivePeaks==1) ? "only one" : (nIntensivePeaks<3) ? "only %d" : "%d") +
                                (noiseModel.isPresent() ? " peaks above the noise level." : " peaks with relative intensity above 3%%."),
                        //for string format
                        nIntensivePeaksParameter
                        //additional parameters not used for string formatting
                ));

                identifier = "msMsNumCollisionEnergies";
                LCMSQualityCheck.ParameterValue numberOfCEsParameter = new LCMSQualityCheck.ParameterValue<>(ces.size(), "numberOfCEs", "Number of different collision energies the compound was measured at.");
                if (ces.size()>1) {
                    ms2Check.add(new LCMSQualityCheck(
                            LCMSQualityCheck.Quality.GOOD, category, identifier,
                            "Ms/Ms has %d different collision energies.",
                            //for string format
                            numberOfCEsParameter
                            //additional parameters not used for string formatting
                    ));
                    score += 3;
                } else {
                    ms2Check.add(new LCMSQualityCheck(
                            LCMSQualityCheck.Quality.LOW, category, identifier,
                            "Ms/Ms was only recorded at a single collision energy.",
                            //for string format
                            numberOfCEsParameter
                            //additional parameters not used for string formatting
                    ));
                }

                LCMSQualityCheck.Quality ms2Quality = (score >= 6) ? LCMSQualityCheck.Quality.GOOD : (score >= 3 ? LCMSQualityCheck.Quality.MEDIUM : LCMSQualityCheck.Quality.LOW);
                return new LCMSQualityCheckResult(ms2Check, ms2Quality);
            } else {
                return new LCMSQualityCheckResult(ms2Check, null);
            }
        }
    }

    protected class IsotopesChecker implements Checker {
        private final CoelutingTraceSet traceSet;
        private final IonTrace compoundTrace;

        public IsotopesChecker(CoelutingTraceSet traceSet, IonTrace compoundTrace) {
            this.traceSet = traceSet;
            this.compoundTrace = compoundTrace;
        }

        @Override
        public LCMSQualityCheckResult performCheck() {
            ArrayList<LCMSQualityCheck> isotopeCheck = new ArrayList<>();
            LCMSQualityCheck.Quality isotopeQuality = null;

            LCMSQualityCheck.QualityCategory category = LCMSQualityCheck.QualityCategory.ISOTOPE;
            String identifier;

            // check number if isotope peaks
            identifier = "isotopeNumIsotopes";
            LCMSQualityCheck.ParameterValue numberOfIsotopesParameter = new LCMSQualityCheck.ParameterValue<>(compoundTrace.getIsotopes().length, "numberOfIsotopes", "Number of isotope peaks including monoisotopic.");
            if (compoundTrace.getIsotopes().length>=3) {
                isotopeQuality= LCMSQualityCheck.Quality.GOOD;
                isotopeCheck.add(new LCMSQualityCheck(
                        LCMSQualityCheck.Quality.GOOD, category, identifier,
                        "Has %d isotope peaks.",
                        //for string format
                        numberOfIsotopesParameter
                        //additional parameters not used for string formatting
                ));

            } else if (compoundTrace.getIsotopes().length>=2) {
                isotopeQuality= LCMSQualityCheck.Quality.MEDIUM;
                isotopeCheck.add(new LCMSQualityCheck(
                        LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                        "Has two isotope peaks.",
                        //for string format
                        //additional parameters not used for string formatting
                        numberOfIsotopesParameter
                ));
            } else {
                isotopeCheck.add(new LCMSQualityCheck(
                        LCMSQualityCheck.Quality.LOW, category, identifier,
                        "Has no isotope peaks besides the monoisotopic peak.",
                        //for string format
                        //additional parameters not used for string formatting
                        numberOfIsotopesParameter
                ));
                isotopeQuality = LCMSQualityCheck.Quality.LOW;
                return new LCMSQualityCheckResult(isotopeCheck, isotopeQuality);
            }

            int goodIsotopePeaks = 0;
            final Trace m = compoundTrace.getIsotopes()[0];
            checkIsotopes:
            for (int k=1; k < compoundTrace.getIsotopes().length; ++k) {
                final Trace t = compoundTrace.getIsotopes()[k];
                double correlation = 0d;
                double[] iso = new double[t.getDetectedFeatureLength()];
                double[] main = iso.clone();
                int j=0;
                eachPeak:
                for (int i=t.getDetectedFeatureOffset(),n=t.getDetectedFeatureOffset()+ t.getDetectedFeatureLength(); i<n; ++i) {
                    final int mainIndex = i+t.getIndexOffset()-m.getIndexOffset();
                    if (mainIndex < 0 || mainIndex >= m.getIntensities().length) {
                        if (t.getIntensities()[i] <  traceSet.getNoiseLevels()[i+t.getIndexOffset()]) {
                            continue eachPeak;
                        } else {
                            isotopeCheck.add(new LCMSQualityCheck(
                                    LCMSQualityCheck.Quality.LOW, category, "isotopeRetentionTimeShift",
                                    "The isotope peak is found at retention times outside of the monoisotopic peak"
                            ));
                            break checkIsotopes;
                        }
                    }
                    main[j] = m.getIntensities()[mainIndex];
                    iso[j++] = t.getIntensities()[i];
                }
                if (j < iso.length) {
                    main = Arrays.copyOf(main, j);
                    iso = Arrays.copyOf(iso,j);
                }
                correlation = Statistics.pearson(main, iso);
                // we also use our isotope scoring
                final double intensityScore = scoreIso(main, iso);
                LCMSQualityCheck.Quality quality;
                if (correlation>=0.99 || intensityScore>=5) quality= LCMSQualityCheck.Quality.GOOD;
                else if (correlation>=0.95 || intensityScore>=0) quality= LCMSQualityCheck.Quality.MEDIUM;
                else quality = LCMSQualityCheck.Quality.LOW;

                LCMSQualityCheck.ParameterValue isotopeIndexParameter = new LCMSQualityCheck.ParameterValue<>(k, "isotopeIndex", "Index of the isotope peak.");
                LCMSQualityCheck.ParameterValue correlationParameter = new LCMSQualityCheck.ParameterValue<>(correlation, "correlation", "Correlation to monoisotopic peak.");
                LCMSQualityCheck.ParameterValue intensityScoreParameter = new LCMSQualityCheck.ParameterValue<>(intensityScore, "isotopeScore", "Isotope intensity score.");
                isotopeCheck.add(new LCMSQualityCheck(
                        quality, LCMSQualityCheck.QualityCategory.ISOTOPE, "isotopeCorrelation",
                        getNumWord(k)+" isotope peak has a correlation of %.2f. The isotope score is %.3f.",
                        //for string format
                        correlationParameter, intensityScoreParameter,
                        //additional parameters not used for string formatting
                        isotopeIndexParameter
                ));

                if (quality== LCMSQualityCheck.Quality.GOOD) ++goodIsotopePeaks;
            }

            if (goodIsotopePeaks>=2) {
                isotopeQuality = LCMSQualityCheck.Quality.GOOD;
            }

            return new LCMSQualityCheckResult(isotopeCheck, isotopeQuality);
        }

        private String[] words = new String[]{
                "First","Second","Third","Fourth","Fifth"
        };

        private String getNumWord(int i) {
            if (i-1 < words.length) return words[i-1];
            else return i + "th";
        }

        private double scoreIso(double[] a, double[] b) {
            double mxLeft = a[0];
            for (int i=1; i < a.length; ++i) {
                mxLeft = Math.max(a[i],mxLeft);
            }
            double mxRight = b[0];
            for (int i=1; i < b.length; ++i) {
                mxRight = Math.max(b[i],mxRight);
            }

            double sigmaA = Math.max(0.02, traceSet.getNoiseLevels()[compoundTrace.getMonoisotopicPeak().getAbsoluteIndexApex()]/Math.max(mxLeft,mxRight));
            double sigmaR = 0.05;

            double score = 0d;
            for (int i=0; i < a.length; ++i) {
                final double ai = a[i]/mxLeft;
                final double bi = b[i]/mxRight;
                final double delta = bi-ai;

                double peakPropbability = Math.log(Math.exp(-(delta*delta)/(2*(sigmaA*sigmaA + bi*bi*sigmaR*sigmaR)))/(2*Math.PI*bi*sigmaR*sigmaR));
                final double sigma = 2*bi*sigmaR + 2*sigmaA;
                score += peakPropbability-Math.log(Math.exp(-(sigma*sigma)/(2*(sigmaA*sigmaA + bi*bi*sigmaR*sigmaR)))/(2*Math.PI*bi*sigmaR*sigmaR));
            }

            return score;
        }
    }


    protected static class PeakChecker implements Checker {
        private final CoelutingTraceSet traceSet;
        private final IonTrace compoundTrace;

        public PeakChecker(CoelutingTraceSet traceSet, IonTrace compoundTrace) {
            this.traceSet = traceSet;
            this.compoundTrace = compoundTrace;
        }


        @Override
        public LCMSQualityCheckResult performCheck() {
            ArrayList<LCMSQualityCheck> peakCheck = new ArrayList<>();
            final Trace t = compoundTrace.getMonoisotopicPeak();

            // has a clearly defined apex
            // 1. apex is larger than variance
            double variance = 0d;
            int l=0;
            int apexIndex = t.getAbsoluteIndexApex()-t.getIndexOffset();
            for (int i=t.getDetectedFeatureOffset(), n = t.getDetectedFeatureOffset()+t.getDetectedFeatureLength(); i < n; ++i) {
                if (i>0 && i+1 < n) {
                    if (i != apexIndex && t.getIntensities()[i-1] < t.getIntensities()[i] && t.getIntensities()[i+1] < t.getIntensities()[i]) {
                        variance += Math.min(Math.pow(t.getIntensities()[i] - t.getIntensities()[i - 1], 2),Math.pow(t.getIntensities()[i] - t.getIntensities()[i + 1], 2));
                        l++;
                    }
                }
            }
            double v2 = 0d;
            if (l!=0) {
                v2 = Math.sqrt(variance/Math.sqrt(l));
                variance /= l;
                variance = Math.sqrt(variance);
            }
            double stp = Math.max(t.getLeftEdgeIntensity(),t.getRightEdgeIntensity());
            double peak = t.getApexIntensity() - stp;

            LCMSQualityCheck.QualityCategory category = LCMSQualityCheck.QualityCategory.ISOTOPE;
            String identifier;

            identifier = "chromPeakApex";
            //todo don't know if/how to include @{ParameterValue}s
            if (l==0) {
                double m = peak/stp;
                if (m > 8) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                            "The chromatographic peak has clearly defined apex."));
                } else if (m > 4) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The apex of the chromatographic peak has a low slope."));
                } else peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.LOW, category, identifier,
                        "The chromatographic peak has no clearly defined apex."));

            } else {
                if (peak > 10*v2) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                            "The chromatographic peak has clearly defined apex."));
                } else if (peak > 5*v2) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The apex of the chromatographic peak has a low intensity compared to the variance of the surrounding peaks."));
                } else peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.LOW, category, identifier,
                        "The chromatographic peak has no clearly defined apex."));
            }

            // has a clearly defined start point
            float apex = t.getApexIntensity();
            float begin = t.getLeftEdgeIntensity();
            float ende = t.getRightEdgeIntensity();

            // check if there is another peak on the left side
            boolean leftNeighbour = false;
            if (t.getDetectedFeatureOffset()>1) {
                float[] xs = t.getIntensities();
                int j = t.getDetectedFeatureOffset();
                if (xs[j] < xs[j-1] && xs[j-1] < xs[j-2] && (xs[j-2]-xs[j]) > traceSet.getNoiseLevels()[j] ) {
                    leftNeighbour = true;
                }
            }

            // check if there is another peak on the right side
            boolean rightNeighbour = false;
            if (t.getDetectedFeatureOffset()+t.getDetectedFeatureLength()+1 <t.getIntensities().length ) {
                float[] xs = t.getIntensities();
                int j = t.getDetectedFeatureOffset()+t.getDetectedFeatureLength()-1;
                if (xs[j] < xs[j+1] && xs[j+1] < xs[j+2] && (xs[j+2]-xs[j]) > traceSet.getNoiseLevels()[j] ) {
                    rightNeighbour = true;
                }
            }

            boolean relativeBegin = begin/apex <= 0.2;
            boolean absoluteBegin = begin <= traceSet.getNoiseLevels()[t.absoluteIndexLeft()]*20;

            boolean relativeEnd = ende/apex <= 0.2;
            boolean clearlyRelativeEnd = ende/apex < 0.05;
            boolean clearlyRelativeBegin = begin/apex < 0.05;
            boolean absoluteEnd = ende <= traceSet.getNoiseLevels()[t.absoluteIndexRight()]*20;

            absoluteBegin = absoluteBegin || clearlyRelativeBegin;
            absoluteEnd = absoluteEnd || clearlyRelativeEnd;

            boolean slopeLeft = (apex-begin)/apex >= 0.2;
            boolean slopeRight = (apex-ende)/apex >= 0.2;

            final boolean reallyBadLeft = (begin/apex > 0.3) && ((apex-begin)/apex > 0.3);
            final boolean reallyBadRight = (ende/apex > 0.3) && ((apex-ende)/apex > 0.3);

            leftNeighbour &= slopeLeft;
            rightNeighbour &= slopeRight;

            identifier = "chromPeakStartEnd";
            if (relativeBegin&&absoluteBegin && (relativeEnd&&absoluteEnd || clearlyRelativeEnd)) {
                peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                        "The chromatographic peak has clearly defined start and end points."));
            } else {
                if (relativeBegin&&(absoluteBegin||leftNeighbour)) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                            "The chromatographic peak has clearly defined start points."));
                } else if (relativeBegin) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The chromatographic peak starts way above the noise level."));
                } else if (absoluteBegin) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The left side of the chromatographic peak is close to noise level"));
                } else if (leftNeighbour) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The left edge of the chromatographic peak is clearly separated from its left neighbour peak"));
                } else if (!reallyBadLeft) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The left edge of the chromatographic peak is not clearly defined."));
                } else {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.LOW, category, identifier,
                            "The left edge of the chromatographic peak is not well defined."));
                }
                if (relativeEnd&&(absoluteEnd||rightNeighbour)) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                            "The chromatographic peak has clearly defined end points."));
                } else if (relativeEnd) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The chromatographic peak ends way above the noise level."));
                } else if (absoluteEnd) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The right side of the chromatographic peak is close to noise level"));
                } else if (rightNeighbour) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The right edge of the chromatographic peak is clearly separated from its right neighbour peak"));
                } else if (!reallyBadRight) {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.MEDIUM, category, identifier,
                            "The right edge of the chromatographic peak is not clearly defined."));
                }  else {
                    peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.LOW, category, identifier,
                            "The right edge of the chromatographic peak is not well defined."));
                }


            }

            // check number of data points
            identifier = "chromPeakNumDataPoints";
            LCMSQualityCheck.ParameterValue numDataPointParameter = new LCMSQualityCheck.ParameterValue(t.getDetectedFeatureLength(), "numberOfDataPoints", "Number of data points available for the chromatographic peak.");
            LCMSQualityCheck.Quality datapointQualitiy;
            if (t.getDetectedFeatureLength() >= 8) {
                datapointQualitiy = LCMSQualityCheck.Quality.GOOD;
            } else if (t.getDetectedFeatureLength() >= 4) {
                datapointQualitiy = LCMSQualityCheck.Quality.MEDIUM;
            } else{
                datapointQualitiy = LCMSQualityCheck.Quality.LOW;
            }
            peakCheck.add(new LCMSQualityCheck(LCMSQualityCheck.Quality.GOOD, category, identifier,
                    "The chromatographic peak consists of" + (datapointQualitiy== LCMSQualityCheck.Quality.LOW ? " only" : " ") + " %d data points",
                    numDataPointParameter
            ));

            // check peak slope

            LCMSQualityCheck.Quality quality = LCMSQualityCheck.Quality.GOOD;
            for (LCMSQualityCheck c : peakCheck) {
                if (c.getQuality().ordinal() < quality.ordinal()) {
                    quality = c.getQuality();
                }
            }

            return new LCMSQualityCheckResult(peakCheck, quality);
        }
    }


    protected interface Checker {
        public LCMSQualityCheckResult performCheck();
    }
}
