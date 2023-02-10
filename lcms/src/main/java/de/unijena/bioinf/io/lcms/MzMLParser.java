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

package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.MsDataSourceReference;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.Polarity;
import de.unijena.bioinf.model.lcms.Scan;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.jmzml.model.mzml.*;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class MzMLParser implements LCMSParser {

    @Override
    public LCMSRun parse(File file, SpectrumStorage storage) throws IOException {
        try {
            return parse(new DataSource(file), new MzMLUnmarshaller(file), storage);
        } catch (Throwable w) {
            LoggerFactory.getLogger(MzMLParser.class).error("Error when parsing file: " + file.getName());
            throw w;
        }
    }

    public LCMSRun parse(URI source, SpectrumStorage storage) throws IOException {
        return parse(new DataSource(source), new MzMLUnmarshaller(source.toURL()), storage);
    }

    public LCMSRun parse(@NotNull DataSource source, @NotNull MzMLUnmarshaller um, @NotNull SpectrumStorage storage) throws IOException {
        try {
            final LCMSRun run = new LCMSRun(source);
            final String mzMlId = um.getMzMLId();
            InstrumentConfigurationList instrumentList = um.unmarshalFromXpath("/instrumentConfigurationList", InstrumentConfigurationList.class);
            Map<String, String> runAtts = um.getSingleElementAttributes("/run");
            final String runId = runAtts.get("id");

            {
                // get source location oO
                URI s = source.getURI();
                URI parent = s.getPath().endsWith("/") ? s.resolve("..") : s.resolve(".");
                String fileName = parent.relativize(s).toString();
                run.setReference(new MsDataSourceReference(parent, fileName, runId, mzMlId));
            }

            //todo run wide (default instrumentation) may differ from ms2 analyzer?
            // do we have to handle this

            // Read available instument information
            Map<String, String> instruments = new HashMap<>();
            if (instrumentList != null && instrumentList.getCount() > 0)
                instruments = instrumentList.getInstrumentConfiguration().stream()
                        .collect(Collectors.toMap(
                                InstrumentConfiguration::getId,
                                i -> {
                                    List<String> list = new ArrayList<>();
                                    i.getUserParam().forEach(cv -> list.add(cv.getValue()));
                                    i.getCvParam().forEach(cv -> list.add(cv.getName()));

                                    if (i.getComponentList() != null && i.getComponentList().getAnalyzer() != null) {
                                        i.getComponentList().getAnalyzer().forEach(c -> {
                                            c.getUserParam().forEach(cv -> list.add(cv.getValue()));
                                            c.getCvParam().forEach(cv -> list.add(cv.getName()));
                                        });
                                    }
                                    return String.join(", ", list);
                                })
                        );

            //map id to index  because mzml is id and not index based
            final Map<String, Integer> idToIndex = um.getSpectrumIndexes().stream().collect(Collectors.toMap(um::getSpectrumIDFromSpectrumIndex, idx -> idx));

            //spectra stuff
            for (String sid : um.getSpectrumIDs()) {
                final Spectrum spectrum = um.getSpectrumById(sid);
                Polarity polarity = Polarity.UNKNOWN;
                Integer msLevel = null;
                boolean centroided=true;
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
                            msLevel = Integer.parseInt(cvParam.getValue());
                            break;
                        case "MS:1000127":
                            centroided = true;
                            break;
                        case "MS:1000128":
                            centroided = false;
                            break;
                        // add to skiplist
                        case "MS:1000804":
                            skipList.add(cvParam);
                            break;
                    }
                }

                if (!skipList.isEmpty()){
                    LoggerFactory.getLogger(getClass()).debug("Spectrum with ID '" + sid  + "' contains parameters that indicate non Mass Spectrometry data (e.g. EMR spectra). Skipping! Parameters: " + skipList.stream().map(CVParam::getAccession).collect(Collectors.joining(", ")) );
                    continue;
                }
                if (msLevel == null && polarity == Polarity.UNKNOWN){
                    LoggerFactory.getLogger(getClass()).warn("Spectrum with ID '" + sid  + "' does neither contain mslevel nor polarity information. Spectrum is likely to not be an Mass Spectrum. Skipping this entry." + System.lineSeparator() + "Spectrum information: " + spectrum.getCvParam().stream().map(CVParam::toString).collect(Collectors.joining(System.lineSeparator())));
                    continue;
                }

                if (msLevel == null)
                    msLevel = 1;


                long retentionTimeMillis = 0L;
                if (!spectrum.getScanList().getScan().isEmpty()) {
                    for (CVParam cvParam : spectrum.getScanList().getScan().get(0).getCvParam()) {
                        switch (cvParam.getAccession()) {
                            case "MS:1000016":
                                retentionTimeMillis = CVUtils.getTimeInMilliseconds(cvParam);
                                break;
                        }
                    }
                }

                Precursor precursor = null;
                double collisionEnergy = Double.NaN;
                if (msLevel > 1) {
                    if (spectrum.getPrecursorList()==null || spectrum.getPrecursorList().getPrecursor()==null || spectrum.getPrecursorList().getPrecursor().isEmpty()) {
                        precursor = null;
                        LoggerFactory.getLogger(MzMLParser.class).warn("No precursor given for MS/MS spectra");
                    } else {
                        precursor = spectrum.getPrecursorList().getPrecursor().get(0);
                        collisionEnergy = precursor.getActivation().getCvParam().stream().filter(cv -> cv.getAccession().equals("MS:1000045"))
                                .findFirst().map(cv -> Double.parseDouble(cv.getValue())).orElse(0d);
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

                if (mzArray == null || intArray == null) {
                    LoggerFactory.getLogger(MzMLParser.class).warn("No spectrum data found in Spectrum with id: " + spectrum.getId());
                    mzArray = new double[0];
                    intArray = new double[0];
                }
                final SimpleSpectrum spec = Spectrums.getBaselined(Spectrums.wrap(mzArray, intArray), 0);

                final Scan scan = new Scan(
                        spectrum.getIndex(),
                        polarity,
                        retentionTimeMillis, //retention time
                        Double.isFinite(collisionEnergy) ? new CollisionEnergy(collisionEnergy) : CollisionEnergy.none(), //collision energy
                        spec.size(),
                        Spectrums.calculateTIC(spec),
                        centroided,
                        makePrecursor(precursor, idToIndex)
                );

                storage.add(scan, spec);
                run.addScan(scan);

            }

            //instrumentationstuff
            final String instrumentInfo = instruments.getOrDefault(runAtts.getOrDefault("defaultInstrumentConfigurationRef",""), "");
            run.setInstrument(Arrays.stream(MsInstrumentation.Instrument.values()).map(i -> (MsInstrumentation) i)
                    .filter(i -> i.isInstrument(instrumentInfo)).findFirst().orElse(MsInstrumentation.Unknown)
            );

            return run;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private de.unijena.bioinf.model.lcms.Precursor makePrecursor(Precursor precursor, Map<String, Integer> idToIndex) {
        if (precursor == null)
            return null;

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

        return new de.unijena.bioinf.model.lcms.Precursor(
                idToIndex.getOrDefault(precursor.getSpectrumRef(), -1),
                mz, intensity, chargeState, w
        );
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/home/fleisch/Downloads/demo/I031.mzML");

//        LCMSRun r0 = new MzXMLParser().parse(f, new InMemoryStorage());
        LCMSRun r = new MzMLParser().parse(f, new InMemoryStorage());
    }
}