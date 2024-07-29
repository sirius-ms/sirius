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

package de.unijena.bioinf.lcms.io;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.persistence.model.core.Run;
import de.unijena.bioinf.ms.persistence.model.core.Scan;
import de.unijena.bioinf.ms.persistence.model.core.*;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.jmzml.model.mzml.*;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MzMLParser implements LCMSParser {

    @Override
    public void parse(
            File file,
            IOThrowingConsumer<Run> runConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            Run.RunBuilder defaultRun
    ) throws IOException {
        parse(file, new MzMLUnmarshaller(file), runConsumer, scanConsumer, msmsScanConsumer, defaultRun);
    }

    private void parse(
            File file,
            MzMLUnmarshaller um,
            IOThrowingConsumer<Run> runConsumer,
            IOThrowingConsumer<Scan> scanConsumer,
            IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            Run.RunBuilder runBuilder
    ) throws IOException {
        try {
            Map<String, String> runAtts = um.getSingleElementAttributes("/run");
            runBuilder = runBuilder.runId(-1L).name(runAtts.get("id")).sourcePath(file.getPath());

            // Read available instument information
            IonizationType ionization = null;
            FragmentationType fragmentation = null;
            List<MassAnalyzerType> massAnalyzers = new ArrayList<>();
            
            InstrumentConfigurationList instrumentList = um.unmarshalFromXpath("/instrumentConfigurationList", InstrumentConfigurationList.class);
            if (instrumentList != null && instrumentList.getCount() > 0) {
                for (InstrumentConfiguration ic : instrumentList.getInstrumentConfiguration()) {

                    // get ion source
                    for (SourceComponent sc : ic.getComponentList().getSource()) {
                        for (CVParam cvParam : sc.getCvParam()) {
                            ionization = switch (cvParam.getAccession()) {
                                case "MS:1000073" -> IonizationType.ESI;
                                case "MS:1000071" -> IonizationType.CI;
                                case "MS:1000075" -> IonizationType.MALDI;
                                case "MS:1000074" -> IonizationType.FAB;
                                case "MS:1000257" -> IonizationType.FD;
                                case "MS:1000134" -> IonizationType.PD;
                                case "MS:1000266" -> IonizationType.LD;
                                case "MS:1000072" -> IonizationType.EI;
                                default -> ionization;
                            };
                        }
                        if (ionization == null) {
                            for (UserParam userParam : sc.getUserParam()) {
                                Optional<IonizationType> optType = matchIonizationType(userParam.getValue());
                                if (optType.isPresent()) {
                                    ionization = optType.get();
                                }
                            }
                        }
                    }

                    // get instruments
                    analyzerLoop:
                    for (AnalyzerComponent ac : ic.getComponentList().getAnalyzer()) {
                        for (CVParam cvParam : ac.getCvParam()) {
                            switch (cvParam.getAccession()) {
                                case "MS:1000084":
                                    massAnalyzers.add(MassAnalyzerType.TOF);
                                    break analyzerLoop;
                                case "MS:1000264":
                                    massAnalyzers.add(MassAnalyzerType.IONTRAP);
                                    break analyzerLoop;
                                case "MS:1000082":
                                    massAnalyzers.add(MassAnalyzerType.QUAD);
                                    break analyzerLoop;
                                case "MS:1000484":
                                    massAnalyzers.add(MassAnalyzerType.ORBITRAP);
                                    break analyzerLoop;
                                case "MS:1000079":
                                    massAnalyzers.add(MassAnalyzerType.FTICR);
                                    break analyzerLoop;
                            }
                        }
                        for (UserParam userParam : ac.getUserParam()) {
                            Optional<MassAnalyzerType> optType = matchMassAnalyzerType(userParam.getValue());
                            optType.ifPresent(massAnalyzers::add);
                        }
                    }

                }
            }

            // get fragmentation
            ReferenceableParamGroup refParams = um.unmarshalFromXpath("/run/referenceableParamGroup", ReferenceableParamGroup.class);
            if (refParams != null) {
                for (CVParam cvParam : refParams.getCvParam()) {
                    fragmentation = switch (cvParam.getAccession()) {
                        case "MS:1000133" -> FragmentationType.CID;
                        case "MS:1000136" -> FragmentationType.SID;
                        case "MS:1000250" -> FragmentationType.ECD;
                        case "MS:1000598" -> FragmentationType.ETD;
                        case "MS:1001880" -> FragmentationType.ION_SOURCE;
                        case "MS:1003247" -> FragmentationType.NETD;
                        default -> fragmentation;
                    };
                }
                if (fragmentation == null) {
                    for (UserParam userParam : refParams.getUserParam()) {
                        Optional<FragmentationType> optType = matchFragmentationType(userParam.getValue());
                        if (optType.isPresent()) {
                            fragmentation = optType.get();
                        }
                    }
                }
            }

            Run run = runBuilder
                    .ionization(ionization)
                    .fragmentation(fragmentation)
                    .massAnalyzers(!massAnalyzers.isEmpty() ? massAnalyzers : null)
                    .build();
            runConsumer.consume(run);

            // read spectra
            Map<String, Long> ms1Ids = new HashMap<>();
            for (String sid : um.getSpectrumIDs()) {
                final Spectrum spectrum = um.getSpectrumById(sid);
                String polarity = null;
                byte msLevel = 0;
                double ccs = Double.NaN;
                boolean centroided = true;
                final Set<CVParam> skipList = new HashSet<>();
                for (CVParam cvParam : spectrum.getCvParam()) {
                    switch (cvParam.getAccession()) {
                        case "MS:1000129":
                        case "MS:1000076":
                            polarity = "negative";
                            break;
                        case "MS:1000130":
                        case "MS:1000077":
                            polarity = "positive";
                            break;
                        case "MS:1000511":
                            msLevel = Byte.parseByte(cvParam.getValue());
                            break;
                        case "MS:1002954":
                            ccs = Double.parseDouble(cvParam.getValue());
                            break;
                        case "MS:1000128":
                            centroided = false;
                            break;
                        // skip this scan
                        case "MS:1000804":
                            skipList.add(cvParam);
                            break;
                    }
                }

                if (!centroided) {
                    LoggerFactory.getLogger(getClass()).debug("Spectrum with ID '" + sid  + "' is not centroided. Skipping!");
                    continue;
                }
                if (!skipList.isEmpty()){
                    LoggerFactory.getLogger(getClass()).debug("Spectrum with ID '" + sid  + "' contains parameters that indicate non Mass Spectrometry data (e.g. EMR spectra). Skipping! Parameters: " + skipList.stream().map(CVParam::getAccession).collect(Collectors.joining(", ")) );
                    continue;
                }
                if (msLevel < 1 && polarity == null){
                    LoggerFactory.getLogger(getClass()).warn("Spectrum with ID '" + sid  + "' does neither contain mslevel nor polarity information. Spectrum is likely to not be an Mass Spectrum. Skipping this entry." + System.lineSeparator() + "Spectrum information: " + spectrum.getCvParam().stream().map(CVParam::toString).collect(Collectors.joining(System.lineSeparator())));
                    continue;
                }

                msLevel = (byte) Math.max(1, msLevel);

                double rt = 0d;
                if (!spectrum.getScanList().getScan().isEmpty()) {
                    for (CVParam cvParam : spectrum.getScanList().getScan().get(0).getCvParam()) {
                        switch (cvParam.getAccession()) {
                            case "MS:1000016":
                                rt = CVUtils.getTimeInSeconds(cvParam);
                                break;
                            // not sure if CCS is here or in spectrum.getCVParam()
                            case "MS:1002954":
                                ccs = Double.parseDouble(cvParam.getValue());
                                break;
                        }
                    }
                }


                double[] mzArray = null;
                double[] intArray = null;
                if (spectrum.getBinaryDataArrayList()!=null) {
                    for (BinaryDataArray array : spectrum.getBinaryDataArrayList().getBinaryDataArray()) {
                        switch (array.getDataType()) {
                            case INTENSITY:
                                intArray = Arrays.stream(array.getBinaryDataAsNumberArray()).mapToDouble(Number::doubleValue).toArray();
                                break;
                            case MZ_VALUES:
                                mzArray = Arrays.stream(array.getBinaryDataAsNumberArray()).mapToDouble(Number::doubleValue).toArray();
                                break;
                            case UNKNOWN:
                                break;
                        }

                    }
                }

                if (mzArray == null || intArray == null || mzArray.length != intArray.length || mzArray.length == 0) {
                    LoggerFactory.getLogger(MzMLParser.class).warn("No spectrum data found in Spectrum with id: " + spectrum.getId() + " Skipping!");
                    continue;
                }

                final SimpleSpectrum peaks = Spectrums.getBaselined(Spectrums.wrap(mzArray, intArray), 0);

                if (peaks.isEmpty()) {
                    LoggerFactory.getLogger(MzMLParser.class).warn("No valid spectrum data found Spectrum with id: " + spectrum.getId() + " Skipping!");
                    continue;
                }

                if (msLevel == 1) {
                    Scan scan = Scan.builder()
                            .scanId(-1L)
                            .runId(run.getRunId())
                            .scanNumber(sid)
                            .scanTime(rt)
                            .peaks(peaks)
                            .ccs(ccs)
                            .build();

                    scanConsumer.consume(scan);
                    ms1Ids.put(spectrum.getId(), scan.getScanId());
                } else {
                    Precursor precursor;
                    double collisionEnergy = Double.NaN;
                    if (spectrum.getPrecursorList()==null || spectrum.getPrecursorList().getPrecursor()==null || spectrum.getPrecursorList().getPrecursor().isEmpty()) {
                        precursor = null;
                        LoggerFactory.getLogger(de.unijena.bioinf.io.lcms.MzMLParser.class).warn("No precursor given for MS/MS spectra");
                    } else {
                        precursor = spectrum.getPrecursorList().getPrecursor().get(0);
                        collisionEnergy = precursor.getActivation().getCvParam().stream().filter(cv -> cv.getAccession().equals("MS:1000045"))
                                .findFirst().map(cv -> Double.parseDouble(cv.getValue())).orElse(0d);
                    }

                    MSMSScan.MSMSScanBuilder scanBuilder = MSMSScan.builder()
                            .scanId(-1L)
                            .runId(run.getRunId())
                            .scanNumber(sid)
                            .scanTime(rt)
                            .peaks(peaks)
                            .msLevel(msLevel)
                            .ccs(ccs)
                            .collisionEnergy(Double.isFinite(collisionEnergy) ? new CollisionEnergy(collisionEnergy) : CollisionEnergy.none());

                    if (precursor != null) {
                        Triple<Double, IsolationWindow, Long> targetIsolationId = makePrecursor(precursor, ms1Ids);
                        scanBuilder = scanBuilder
                                .mzOfInterest(targetIsolationId.getLeft())
                                .isolationWindow(targetIsolationId.getMiddle())
                                .precursorScanId(targetIsolationId.getRight());
                    }

                    msmsScanConsumer.consume(scanBuilder.build());
                }

            }

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Triple<Double, IsolationWindow, Long> makePrecursor(Precursor precursor, Map<String, Long> ms1Ids) {
        IsolationWindow w = new IsolationWindow(0, Double.NaN);
        double target_mz = Double.NaN;
        if (precursor.getIsolationWindow() != null) {
            double lower = 0;
            double higher = 0;
            for (CVParam cvParam : precursor.getIsolationWindow().getCvParam()) {
                switch (cvParam.getAccession()) {
                    case "MS:1000827": // isolation window target m/z
                        target_mz = (Double.parseDouble(cvParam.getValue()));
                        break;
                    case "MS:1000828":
                        lower = (Double.parseDouble(cvParam.getValue()));
                        break;
                    case "MS:1000829":
                        higher = (Double.parseDouble(cvParam.getValue()));
                        break;
                }
            }
            w = IsolationWindow.fromOffsets(lower, higher);
        }

        double selectedIon_mz = Double.NaN;
        if (precursor.getSelectedIonList() != null && precursor.getSelectedIonList().getCount() > 0) {
            for (CVParam cvParam : precursor.getSelectedIonList().getSelectedIon().get(0).getCvParam()) {
                if (cvParam.getAccession().equals("MS:1000744")) { // selected ion m/z
                    selectedIon_mz = (Double.parseDouble(cvParam.getValue()));
                }
            }
        }
        //use isolation target m/z if available
        //(it happens that the instrument targets the +2 isotope peak but the selected ion m/z is the monoisotopic m/z)
        double mz = !Double.isNaN(target_mz) ? target_mz : selectedIon_mz;

        return Triple.of(mz, w, ms1Ids.getOrDefault(precursor.getSpectrumRef(), -1L));
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/home/mel/lcms-data/polluted_citrus/G87532_1x_RD6_01_26277.mzML");
        AtomicInteger scans = new AtomicInteger(0);
        AtomicInteger ms2scans = new AtomicInteger(0);
        new MzMLParser().parse(f, System.out::println, ms -> scans.addAndGet(1), msms -> ms2scans.addAndGet(1), Run.builder().runType(Run.Type.SAMPLE).chromatography(ChromatographyType.LC));
        System.out.println(scans);
        System.out.println(ms2scans);
    }

}