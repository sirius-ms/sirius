package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.MsInstrumentation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.InMemoryStorage;
import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.Polarity;
import de.unijena.bioinf.model.lcms.Scan;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.jmzml.model.mzml.*;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class MzMLParser implements LCMSParser {

    @Override
    public LCMSRun parse(File file, SpectrumStorage storage) throws IOException {
        return parse(new DataSource(file), new MzMLUnmarshaller(file), storage);
    }

    public LCMSRun parse(URL source, SpectrumStorage storage) throws IOException {
        return parse(new DataSource(source), new MzMLUnmarshaller(source), storage);
    }

    public LCMSRun parse(@NotNull DataSource source, @NotNull MzMLUnmarshaller um, @NotNull SpectrumStorage storage) throws IOException {
        try {
            final LCMSRun run = new LCMSRun(source);
            InstrumentConfigurationList instrumentList = um.unmarshalFromXpath("/instrumentConfigurationList", InstrumentConfigurationList.class);
            Map<String, String> runAtts = um.getSingleElementAttributes("/run");
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
                int msLevel = 1;
                for (CVParam cvParam : spectrum.getCvParam()) {
                    switch (cvParam.getAccession()) {
                        case "MS:1000129":
                            polarity = Polarity.NEGATIVE;
                            break;
                        case "MS:1000130":
                            polarity = Polarity.POSITIVE;
                            break;
                        case "MS:1000511":
                            msLevel = Integer.parseInt(cvParam.getValue());
                            break;
                    }
                }

                long retentionTimeMillis = 0L;
                if (!spectrum.getScanList().getScan().isEmpty()) {
                    for (CVParam cvParam : spectrum.getScanList().getScan().get(0).getCvParam()) {
                        switch (cvParam.getAccession()) {
                            case "MS:1000016":
                                retentionTimeMillis = (long) (Double.parseDouble(cvParam.getValue()) * 60L * 1000L);
                                break;
                        }
                    }
                }

                Precursor precursor = null;
                double collisionEnergy = 0d;
                if (msLevel > 1) {
                    precursor = spectrum.getPrecursorList().getPrecursor().get(0);
                    collisionEnergy = precursor.getActivation().getCvParam().stream().filter(cv -> cv.getAccession().equals("MS:1000045"))
                            .findFirst().map(cv -> Double.parseDouble(cv.getValue())).orElse(0d);
                }

                double[] mzArray = null;
                double[] intArray = null;
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

                if (mzArray == null || intArray == null)
                    throw new IllegalArgumentException("No spectrum data found in Spectrum with id: " + spectrum.getId());
                final SimpleSpectrum spec = new SimpleSpectrum(mzArray, intArray);

                final Scan scan = new Scan(
                        spectrum.getIndex(),
                        polarity,
                        retentionTimeMillis, //retention time
                        collisionEnergy, //collision energy
                        spec.size(),
                        Spectrums.calculateTIC(spec),
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
        if (precursor.getIsolationWindow() != null) {
            double lower = 0;
            double higher = 0;
            for (CVParam cvParam : precursor.getIsolationWindow().getCvParam()) {
                switch (cvParam.getAccession()) {
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
        double mz = Double.NaN;
        int chargeState = 0;
        if (precursor.getSelectedIonList() != null && precursor.getSelectedIonList().getCount() > 0) {
            for (CVParam cvParam : precursor.getSelectedIonList().getSelectedIon().get(0).getCvParam()) {
                switch (cvParam.getAccession()) {
                    case "MS:1000744": // m/z
                        mz = (Double.parseDouble(cvParam.getValue()));
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
