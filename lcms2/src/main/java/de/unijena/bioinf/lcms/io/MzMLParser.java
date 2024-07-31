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
import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.ScanPointMapping;
import de.unijena.bioinf.lcms.spectrum.Ms1SpectrumHeader;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.run.Fragmentation;
import de.unijena.bioinf.ms.persistence.model.core.run.Ionization;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MassAnalyzer;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.jmzml.model.mzml.*;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class MzMLParser implements LCMSParser {

    private File createTempFile(@NotNull Path input) throws IOException {
        if (input.getFileSystem().equals(FileSystems.getDefault())) {
            return input.toFile();
        }else {
            Path tmp = FileUtils.newTempFile("mzml_", ".mzml");
            Files.copy(input,tmp);
            File f = tmp.toFile();
            f.deleteOnExit();
            return f;
        }
    }

    @Override
    public ProcessedSample parse(
            URI input,
            LCMSStorageFactory storageFactory,
            LCMSParser.IOThrowingConsumer<LCMSRun> runConsumer,
            LCMSParser.IOThrowingConsumer<LCMSRun> runUpdateConsumer,
            @Nullable LCMSParser.IOThrowingConsumer<Scan> scanConsumer,
            @Nullable LCMSParser.IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            LCMSRun run
    ) throws IOException {
        URI parent = input.getPath().endsWith("/") ? input.resolve("..") : input.resolve(".");
        String fileName = parent.relativize(input).toString();
        return parse(parent, fileName, storageFactory, new MzMLUnmarshaller(input.toURL()), runConsumer, runUpdateConsumer, scanConsumer, msmsScanConsumer, run);
    }

    @Override
    public ProcessedSample parse(
            Path input,
            LCMSStorageFactory storageFactory,
            LCMSParser.IOThrowingConsumer<LCMSRun> runConsumer,
            LCMSParser.IOThrowingConsumer<LCMSRun> runUpdateConsumer,
            @Nullable LCMSParser.IOThrowingConsumer<Scan> scanConsumer,
            @Nullable LCMSParser.IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            LCMSRun run
    ) throws IOException {
        return parse(input.getParent().toUri(), input.getFileName().toString(), storageFactory, new MzMLUnmarshaller(createTempFile(input)), runConsumer, runUpdateConsumer, scanConsumer, msmsScanConsumer, run);
    }

    private ProcessedSample parse(
            URI parent,
            String fileName,
            LCMSStorageFactory storageFactory,
            MzMLUnmarshaller um,
            LCMSParser.IOThrowingConsumer<LCMSRun> runConsumer,
            LCMSParser.IOThrowingConsumer<LCMSRun> runUpdateConsumer,
            @Nullable LCMSParser.IOThrowingConsumer<Scan> scanConsumer,
            @Nullable LCMSParser.IOThrowingConsumer<MSMSScan> msmsScanConsumer,
            LCMSRun run
    ) throws IOException {
        try {
            final LCMSStorage storage = storageFactory.createNewStorage();
            int samplePolarity = 0;

            String mzMlId = um.getMzMLId();
            final MsDataSourceReference reference;
            {
                Map<String, String> runAtts = um.getSingleElementAttributes("/run");
                final String runId = runAtts.get("id");
                // get source location oO
                reference = new MsDataSourceReference(parent, fileName, runId, mzMlId);
            }

            run.setName(mzMlId != null && !mzMlId.isBlank() ? mzMlId : fileName);
            run.setSourceReference(reference);

            final DoubleArrayList retentionTimes = new DoubleArrayList();
            final IntArrayList scanids = new IntArrayList();
            final ArrayList<String> scanIdentifiers = new ArrayList<>();
            final Int2IntMap idmap = new Int2IntOpenHashMap();

            // Read available instument information
            Ionization ionization = null;
            Fragmentation fragmentation = null;
            List<MassAnalyzer> massAnalyzers = new ArrayList<>();

            InstrumentConfigurationList instrumentList = um.unmarshalFromXpath("/instrumentConfigurationList", InstrumentConfigurationList.class);
            if (instrumentList != null) {
                for (InstrumentConfiguration ic : instrumentList.getInstrumentConfiguration()) {
                    ComponentList componentList = ic.getComponentList();
                    if (componentList == null) {
                        continue;
                    }
                    // get ion source
                    sourceLoop:
                    for (SourceComponent sc : componentList.getSource()) {
                        for (CVParam cvParam : sc.getCvParam()) {
                            Optional<Ionization> optType = Ionization.byHupoId(cvParam.getAccession()).or(() -> Ionization.byValue(cvParam.getName()));
                            if (optType.isPresent()) {
                                ionization = optType.get();
                                break sourceLoop;
                            }
                        }
                        if (ionization == null) {
                            for (UserParam userParam : sc.getUserParam()) {
                                Optional<Ionization> optType = Ionization.byValue(userParam.getValue());
                                if (optType.isPresent()) {
                                    ionization = optType.get();
                                    break sourceLoop;
                                }
                            }
                        }
                    }

                    // get instruments
                    analyzerLoop:
                    for (AnalyzerComponent ac : ic.getComponentList().getAnalyzer()) {
                        for (CVParam cvParam : ac.getCvParam()) {
                            Optional<MassAnalyzer> optType = MassAnalyzer.byHupoId(cvParam.getAccession()).or(() -> MassAnalyzer.byValue(cvParam.getName()));
                            if (optType.isPresent()) {
                                massAnalyzers.add(optType.get());
                                continue analyzerLoop;
                            }
                        }

                        for (UserParam userParam : ac.getUserParam()) {
                            Optional<MassAnalyzer> optType = MassAnalyzer.byValue(userParam.getValue());
                            if (optType.isPresent()) {
                                massAnalyzers.add(optType.get());
                                continue analyzerLoop;
                            }
                        }
                    }

                }
            }

            run.setIonization(ionization);
            run.setMassAnalyzers(!massAnalyzers.isEmpty() ? massAnalyzers : null);
            runConsumer.consume(run);

            // read spectra
            Map<String, Long> ms1Ids = new HashMap<>();
            for (String sid : um.getSpectrumIDs()) {
                final Spectrum spectrum = um.getSpectrumById(sid);
                Polarity polarity = Polarity.UNKNOWN;
                byte msLevel = 0;
                double ccs = Double.NaN;
                boolean centroided = true;
                final Set<CVParam> skipList = new HashSet<>();
                for (CVParam cvParam : spectrum.getCvParam()) {
                    switch (cvParam.getAccession()) {
                        case "MS:1000129":
                        case "MS:1000076":
                            polarity = Polarity.NEGATIVE;
                            break;
                        case "MS:1000130":
                        case "MS:1000077":
                            polarity = Polarity.POSITIVE;
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
                    log.error("Spectrum with ID '" + sid  + "' is not centroided. Skipping!");
                    continue;
                }
                if (!skipList.isEmpty()) {
                    log.error("Spectrum with ID '" + sid  + "' contains parameters that indicate non Mass Spectrometry data (e.g. EMR spectra). Skipping! Parameters: " + skipList.stream().map(CVParam::getAccession).collect(Collectors.joining(", ")) );
                    continue;
                }
                if (msLevel < 1 && polarity == Polarity.UNKNOWN) {
                    log.error("Spectrum with ID '" + sid  + "' does neither contain mslevel nor polarity information. Spectrum is likely to not be an Mass Spectrum. Skipping this entry." + System.lineSeparator() + "Spectrum information: " + spectrum.getCvParam().stream().map(CVParam::toString).collect(Collectors.joining(System.lineSeparator())));
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
                    log.error("No spectrum data found in Spectrum with id: " + spectrum.getId() + " Skipping!");
                    continue;
                }

                final SimpleSpectrum peaks = Spectrums.getBaselined(Spectrums.wrap(mzArray, intArray), 0);
                if (samplePolarity == 0)  {
                    samplePolarity = polarity.charge;
                } else if (polarity.charge != 0 && (polarity.charge > 0) != (samplePolarity > 0) ) {
                    throw new RuntimeException("Preprocessing does not support LCMS runs with different polarities.");
                }

                if (peaks.isEmpty()) {
                    log.error("No valid spectrum data found Spectrum with id: " + spectrum.getId() + " Skipping!");
                    continue;
                }
                if (msLevel == 1) {
                    if (scanConsumer != null) {
                        Scan scan = Scan.builder()
                                .runId(run.getRunId())
                                .sourceScanId(sid)
                                .scanTime(rt)
                                .peaks(peaks)
                                .ccs(ccs)
                                .build();

                        scanConsumer.consume(scan);
                        ms1Ids.put(spectrum.getId(), scan.getScanId());
                    }

                    final Ms1SpectrumHeader header = new Ms1SpectrumHeader(scanids.size(), parseScanNumber(sid, spectrum.getIndex()), sid, polarity.charge, true);
                    retentionTimes.add(rt);
                    idmap.put(header.getScanId(), scanids.size());
                    scanids.add(header.getScanId());
                    if (sid.startsWith("scan=")) {
                        scanIdentifiers.add(null);
                    } else scanIdentifiers.add(sid);
                    storage.getSpectrumStorage().addSpectrum(header, peaks);

                } else {
                    if (spectrum.getPrecursorList() == null || spectrum.getPrecursorList().getPrecursor() == null || spectrum.getPrecursorList().getPrecursor().isEmpty()) {
                        log.error("No precursor information given for MS/MS spectrum with id: " + spectrum.getId() + " Skipping!");
                        continue;
                    }
                    uk.ac.ebi.jmzml.model.mzml.Precursor precursor = spectrum.getPrecursorList().getPrecursor().get(0);
                    if (precursor == null) {
                        log.error("No precursor information given for MS/MS spectrum with id: " + spectrum.getId() + " Skipping!");
                        continue;
                    }
                    double collisionEnergy = precursor.getActivation().getCvParam().stream().filter(cv -> cv.getAccession().equals("MS:1000045"))
                                .findFirst().map(cv -> Double.parseDouble(cv.getValue())).orElse(Double.NaN);

                    if (fragmentation == null) {
                        for (CVParam cvParam : precursor.getActivation().getCvParam()) {
                            Optional<Fragmentation> optType = Fragmentation.byHupoId(cvParam.getAccession()).or(() -> Fragmentation.byValue(cvParam.getName()));
                            if (optType.isPresent()) {
                                fragmentation = optType.get();
                                break;
                            }
                        }
                        if (fragmentation == null) {
                            for (UserParam userParam : precursor.getActivation().getUserParam()) {
                                Optional<Fragmentation> optType = Fragmentation.byValue(userParam.getValue());
                                if (optType.isPresent()) {
                                    fragmentation = optType.get();
                                    break;
                                }
                            }
                        }
                    }

                    Precursor prec = makePrecursor(precursor);

                    if (msmsScanConsumer != null) {
                        MSMSScan.MSMSScanBuilder scanBuilder = MSMSScan.builder()
                                .runId(run.getRunId())
                                .scanNumber(sid)
                                .scanTime(rt)
                                .peaks(peaks)
                                .msLevel(msLevel)
                                .ccs(ccs)
                                .collisionEnergy(Double.isFinite(collisionEnergy) ? new CollisionEnergy(collisionEnergy) : CollisionEnergy.none())
                                .mzOfInterest(prec.getMass())
                                .isolationWindow(prec.getIsolationWindow())
                                .precursorScanId(prec.getScanId());
                        msmsScanConsumer.consume(scanBuilder.build());
                    }

                    final Ms2SpectrumHeader header = new Ms2SpectrumHeader(
                            sid,
                            parseScanNumber(sid, spectrum.getIndex()),
                            polarity.charge, msLevel, centroided,
                            Double.isFinite(collisionEnergy) ? new CollisionEnergy(collisionEnergy) : CollisionEnergy.none(),
                            prec.getIsolationWindow(),
                            idmap.getOrDefault(prec.getScanId(), -1), // TODO: potential error
                            prec.getMass(),
                            prec.getMass(), // todo: fix
                            rt
                    );
                    storage.getSpectrumStorage().addMs2Spectrum(header, peaks);
                }

            }

            if (scanids.isEmpty()) {
                throw new RuntimeException("No spectra imported from " + fileName);
            }

            if (fragmentation != null) {
                run.setFragmentation(fragmentation);
                runUpdateConsumer.consume(run);
            }

            String[] sids = scanIdentifiers.stream().allMatch(Objects::isNull) ? null : scanIdentifiers.toArray(String[]::new);

            final ScanPointMapping mapping = new ScanPointMapping(retentionTimes.toDoubleArray(), scanids.toIntArray(), sids, idmap);
            storage.setMapping(mapping);
            ProcessedSample sample = new ProcessedSample(mapping, storage, samplePolarity, -1);
            sample.setRun(run);
            return sample;

        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static Pattern SCAN_PATTERN = Pattern.compile("scan=(\\d+)"), ALT_PATTERN = Pattern.compile("\\S+=(\\d+)");
    private int parseScanNumber(String sid, Integer index) {
        if (sid == null || sid.isEmpty()){
            LoggerFactory.getLogger(MzMLParser.class).warn("Scan ID is null or Empty. Using index instead. This won't effect the preprocessing at all, but might complicate mapping back the processed spectra to their raw datapoints.");
            return index;
        }
        Matcher m = SCAN_PATTERN.matcher(sid);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        } else {
            m = ALT_PATTERN.matcher(sid);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            } else {
                LoggerFactory.getLogger(MzMLParser.class).warn("Spectrum has no valid scan ID. Using index instead. This won't effect the preprocessing at all, but might complicate mapping back the processed spectra to their raw datapoints.");
                return index;
            }
        }
    }

    private Precursor makePrecursor(uk.ac.ebi.jmzml.model.mzml.Precursor precursor) {
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


        double intensity = Double.NaN;
        double selectedIon_mz = Double.NaN;
        int chargeState = 0;
        if (precursor.getSelectedIonList() != null && precursor.getSelectedIonList().getCount() > 0) {
            for (CVParam cvParam : precursor.getSelectedIonList().getSelectedIon().get(0).getCvParam()) {
                switch (cvParam.getAccession()) {
                    case "MS:1000744": // selected ion m/z
                        selectedIon_mz = (Double.parseDouble(cvParam.getValue()));
                        break;
                    case "MS:1000042": // intensity
                        intensity = (Double.parseDouble(cvParam.getValue()));
                        break;
                    case "MS:1000041": // charge state
                        chargeState = Integer.parseInt(cvParam.getValue());
                        break;
                }
            }
        }
        //use isolation target m/z if available
        //(it happens that the instrument targets the +2 isotope peak but the selected ion m/z is the monoisotopic m/z)
        double mz = !Double.isNaN(target_mz) ? target_mz : selectedIon_mz;
        return new Precursor(
                precursor.getSpectrumRef(),
                parseScanNumber(precursor.getSpectrumRef(), Optional.ofNullable(precursor.getSpectrum()).map(Spectrum::getIndex).orElse(-1)),
                mz, intensity, chargeState, w
        );
    }

//    public static void main(String[] args) throws IOException {
//        File f = new File("/home/mel/lcms-data/polluted_citrus/G87532_1x_RD6_01_26277.mzML");
//        List<Scan> scans = new ArrayList<>();
//        List<MSMSScan> ms2scans = new ArrayList<>();
////        AtomicInteger scans = new AtomicInteger(0);
////        AtomicInteger ms2scans = new AtomicInteger(0);
//        ProcessedSample sample = new MzMLParser().parse(
//                f,
//                LCMSStorage.temporaryStorage(),
//                System.out::println,
//                System.out::println,
//                ms -> {
//                    ms.setScanId(new Random().nextLong());
//                    scans.add(ms);
//                },
//                msms -> {
//                    msms.setScanId(new Random().nextLong());
//                    ms2scans.add(msms);
//                },
////                ms -> scans.addAndGet(1),
////                msms -> ms2scans.addAndGet(1),
//                Run.builder().runType(Run.Type.SAMPLE).chromatography(Chromatography.LC).build()
//        );
//        System.out.println(sample.getRtSpan());
//        System.out.println(scans.size());
//        System.out.println(ms2scans.size());
//
////        System.out.println(scans);
////        System.out.println(ms2scans);
//    }

}