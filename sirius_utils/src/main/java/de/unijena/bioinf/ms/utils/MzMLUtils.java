package de.unijena.bioinf.ms.utils;

import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.MzML;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class MzMLUtils {

    private final MzML mz;

    public MzMLUtils(MzML mz) {
        this.mz = mz;
    }


    public static MzMLUtils getInstance(Path mzMLPath) throws MalformedURLException {
        MzMLUnmarshaller um = new MzMLUnmarshaller(mzMLPath.toUri().toURL());
        MzML mz = um.unmarshall();
        return new MzMLUtils(mz);
    }

    public double[] getTooFrequentMasses(double mzBinSize, double minRelIntensity, double minOccurrenceRate) throws InvalidInputData {
        List<Spectrum> spectrumList = mz.getRun().getSpectrumList().getSpectrum();


        //find min and max mz and create bins
        double[] minMax = minMaxMS1Mz(spectrumList);
        if (minMax==null){
            LoggerFactory.getLogger(MzMLUtils.class).error("No spectra available or empty");
        }
        final double min = minMax[0];
        final double max = minMax[1];
        CountingBins countingBins = new CountingBins(min, max, mzBinSize);

        //add peak counts to bins
        int numberOfMs1 = 0;
        for (Spectrum spectrum : spectrumList) {
            if (getMsLevel(spectrum)==1){
                ++numberOfMs1;
                addPeaksToBins(spectrum, countingBins, minRelIntensity);
            }
        }

        TDoubleArrayList frequentMasses = new TDoubleArrayList();
        for (int i = 0; i < countingBins.numberoOfBins(); i++) {
            if (countingBins.getCount(i)>=minOccurrenceRate*numberOfMs1){
                frequentMasses.add(countingBins.getMeanBinValue(i));
            }

        }
        return frequentMasses.toArray();
    }

    /**
     * //todo find better way than relative intensity of current spectrum
     * @param spectrum
     * @param minRelIntensity only count peaks greater equal this value. [0,1]
     */
    private void addPeaksToBins(Spectrum spectrum, CountingBins countingBins, double minRelIntensity){
        double[] masses = getMzValues(spectrum);
        double[] intensities = getIntensityValues(spectrum);
        if (masses==null || intensities==null){
            LoggerFactory.getLogger(MzMLUtils.class).warn("Could not parse peak data for spectrum id "+spectrum.getId());
        }
        SimpleMutableSpectrum spec = new SimpleMutableSpectrum(new SimpleSpectrum(masses, intensities));
        Spectrums.normalizeToMax(spec, 1d);
        for (Peak peak : spec) {
            if (peak.getIntensity()>minRelIntensity){
                countingBins.add(peak.getMass());
            }
        }
    }

    private double[] minMaxMS1Mz(List<Spectrum> spectrumList){
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Spectrum spectrum : spectrumList) {
            if (getMsLevel(spectrum)==1){
                double[] doubles = getMzValues(spectrum);
                if (doubles==null){
                    LoggerFactory.getLogger(MzMLUtils.class).warn("Could not find m/z data for spectrum id "+spectrum.getId());
                } else {
                    for (double d : doubles) {
                        if (d<min){
                            min = d;
                        }
                        if (d>max){
                            max = d;
                        }
                    }
                }
            }
        }
        if (Double.isInfinite(min) || Double.isInfinite(max)) return null;
        return new double[]{min, max};
    }

    private double[] getMzValues(Spectrum spectrum){
        List<BinaryDataArray> dataArrays = spectrum.getBinaryDataArrayList().getBinaryDataArray();
        for (BinaryDataArray dataArray : dataArrays) {
            if (dataArray.getDataType()==BinaryDataArray.DataType.MZ_VALUES){
                Number[] numbers = dataArray.getBinaryDataAsNumberArray();
                double[] doubles = Arrays.stream(numbers).mapToDouble(Number::doubleValue).toArray();
                return doubles;
            }
        }
        return null;
    }

    private double[] getIntensityValues(Spectrum spectrum){
        List<BinaryDataArray> dataArrays = spectrum.getBinaryDataArrayList().getBinaryDataArray();
        for (BinaryDataArray dataArray : dataArrays) {
            if (dataArray.getDataType()==BinaryDataArray.DataType.INTENSITY){
                Number[] numbers = dataArray.getBinaryDataAsNumberArray();
                double[] doubles = Arrays.stream(numbers).mapToDouble(Number::doubleValue).toArray();
                return doubles;
            }
        }
        return null;
    }

    private int getMsLevel(Spectrum s){
        for (CVParam cvParam : s.getCvParam()) {
            if (cvParam.getAccession().equalsIgnoreCase("MS:1000511")){
                return Integer.parseInt(cvParam.getValue());
            }
        }
        return -1;
    }


    protected class CountingBins {
        private double minValue;
        private double maxValue;
        private double binSize;
        private int[] bins;

        public CountingBins(double minValue, double maxValue, double binSize) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.binSize = binSize;
            int numberOfBins = (int)Math.ceil((maxValue-minValue)/binSize);
            this.bins = new int[numberOfBins];
        }

        protected void add(double value){
            if (value>maxValue) throw new RuntimeException("Cannot add to bins. Value higher than maximum allowed value");
            int binPos = (int)Math.floor((value-minValue)/binSize);
            ++bins[binPos];
        }

        public double getMeanBinValue(int binPos) {
            return minValue+binSize*binPos+binSize/2;
        }

        public int getCount(int binPos) {
            return bins[binPos];
        }

        public int getBinPos(double value){
            if (value>maxValue) throw new RuntimeException("Cannot add to bins. Value higher than maximum allowed value");
            int binPos = (int)Math.floor((value-minValue)/binSize);
            return binPos;
        }

        public double numberoOfBins(){
            return bins.length;
        }

        public double getMinValue() {
            return minValue;
        }

        public double getMaxValue() {
            return maxValue;
        }

        public double getBinSize() {
            return binSize;
        }
    }
}
