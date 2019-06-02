import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.io.lcms.MzXMLParser;
import de.unijena.bioinf.lcms.ChromatogramBuilder;
import de.unijena.bioinf.lcms.CorrelatedPeakDetector;
import de.unijena.bioinf.lcms.IonIdentityNetwork;
import de.unijena.bioinf.lcms.Ms2CosineSegmenter;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bionf.spectral_alignment.CosineQuerySpectrum;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestMZXMLParser {

    @Test
    public void shortTest() {
        final MgfParser parser = new MgfParser();
        try (final BufferedReader r = FileUtils.getReader(new File("/home/kaidu/test.mgf"))) {
            final ArrayList<Ms2Spectrum> spectra = new ArrayList<>();
            parser.parseSpectra(r).forEachRemaining(spectra::add);
            spectra.removeIf(x->x.size()<=6);
            double precursor = 707.1682;
            final double[][] matrix = new double[spectra.size()][spectra.size()];
            for (int i=0; i < spectra.size(); ++i) {
                for (int j=0; j < spectra.size(); ++j) {
                    final SimpleMutableSpectrum A = new SimpleMutableSpectrum(spectra.get(i));
                    Spectrums.cutByMassThreshold(A, precursor-20);
                    Spectrums.applyBaseline(A, 0);
                    final SimpleMutableSpectrum B = new SimpleMutableSpectrum(spectra.get(j));
                    Spectrums.cutByMassThreshold(B, precursor-20);
                    Spectrums.applyBaseline(B, 0);
                    final CosineQueryUtils utils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(20)));
                    CosineQuerySpectrum query = utils.createQuery(new SimpleSpectrum(A), precursor);
                    CosineQuerySpectrum query2 = utils.createQuery(new SimpleSpectrum(B), precursor);
                    matrix[i][j] = matrix[j][i] = utils.cosineProduct(query,query2).similarity;
                }
            }
            try (final BufferedWriter w = FileUtils.getWriter(new File("/home/kaidu/matrix.txt"))) {
                w.write('0');
                for (int k=1; k < matrix.length; ++k) {
                    w.write(' ');
                    w.write(String.valueOf(k));
                }
                w.newLine();
                for (int i = 0; i < matrix.length; ++i) {
                    w.write(String.valueOf(i));
                    for (int j = 0; j < matrix.length; ++j) {
                        w.write(' ');
                        w.write(String.valueOf(matrix[i][j]));
                    }
                    w.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testParser() {
        final SpectrumStorage spectrumStorage = new InMemoryStorage();
        final File file = new File("/home/kaidu/analysis/test.mzXML");
        try {
            final LCMSRun run = new MzXMLParser().parse(file,spectrumStorage);
            System.out.println(new GlobalNoiseModel(run, spectrumStorage,0.5, (x)->true));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Test
    public void testMs2CosineDetector() {
        final SpectrumStorage spectrumStorage = new InMemoryStorage();
        final File file = new File("/home/kaidu/analysis/large.mzXML");
        try {
            final LCMSRun run = new MzXMLParser().parse(file, spectrumStorage);
            int ms2=0; for (Scan s : run) {
                if (s.isMsMs()) ++ms2;
            }
            System.out.println(ms2 + " MS/MS scans in total");
            LCMSProccessingInstance instance = new LCMSProccessingInstance(run, spectrumStorage);
            List<FragmentedIon> process = new Ms2CosineSegmenter(instance).process();
            CorrelatedPeakDetector correlatedPeakDetector = new CorrelatedPeakDetector(instance);
            ListIterator<FragmentedIon> liter = process.listIterator();
            while (liter.hasNext()) {
                boolean isMetabolite = correlatedPeakDetector.detectCorrelatedPeaks(liter.next());
                if (!isMetabolite) liter.remove();
            }
            System.out.println("----------------------------------");
            new IonIdentityNetwork(instance).filterByIonIdentity(process);
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSegmentDetector() {
        final SpectrumStorage spectrumStorage = new InMemoryStorage();
        final File file = new File("/home/kaidu/analysis/test.mzXML");
        try {
            final LCMSRun run = new MzXMLParser().parse(file,spectrumStorage);
            final ChromatogramCache cache = new ChromatogramCache();
            GlobalNoiseModel noiseModel = new GlobalNoiseModel(run, spectrumStorage,0.5d,(x)->true);
            Scan lastMs1 = null;
            TLongArrayList retentionTimeSpans = new TLongArrayList();
            TIntArrayList nsegments = new TIntArrayList();
            int count = 0;
            for (Scan s : run) {
                if (s.isMsMs()) {
                    if (s.getPrecursor().getScanNumber()>0) {
                        lastMs1 = run.getScanByNumber(s.getPrecursor().getScanNumber()).filter(x -> !x.isMsMs()).orElse(lastMs1);
                    }

                    ChromatogramBuilder builder = new ChromatogramBuilder(run, noiseModel, spectrumStorage);
                    Optional<ChromatographicPeak> detect = builder.detect(lastMs1, s.getPrecursor().getMass());
                    if (detect.isPresent() && detect.get().getSegments().size()>0) {
                        ChromatographicPeak chromatographicPeak = detect.get();
                        final ArrayList<ChromatographicPeak.Segment> segments = new ArrayList<>(chromatographicPeak.getSegments());
                        nsegments.add(segments.size());
                        ChromatographicPeak.Segment segment = segments.stream().max(Comparator.comparingLong(ChromatographicPeak.Segment::retentionTimeWidth)).get();
                        retentionTimeSpans.add(segment.retentionTimeWidth());
                    }

                } else lastMs1 = s;
            }

            retentionTimeSpans.sort();
            nsegments.sort();
            System.out.println("Retention time span median = " + retentionTimeSpans.get(retentionTimeSpans.size()/2));
            System.out.println("Number of segments median = " + nsegments.get(nsegments.size()/2));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInput() {
        final MzXMLParser parser = new MzXMLParser();
        try {
            InMemoryStorage storage = new InMemoryStorage();
            LCMSRun parse = parser.parse(new File("/home/kaidu/temp/leptochelin_lock622_895_fullscan.mzXML"), storage);
            final MutableMs2Experiment ms2 = new MutableMs2Experiment();
            ms2.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M+H]+"));
            ms2.setName("leptochelin");
            ms2.setMergedMs1Spectrum(storage.getScan(parse.iterator().next()));
            try (final BufferedWriter bw = FileUtils.getWriter(new File("/home/kaidu/temp/leptochelin.ms"))) {
                new JenaMsWriter().write(bw, ms2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
