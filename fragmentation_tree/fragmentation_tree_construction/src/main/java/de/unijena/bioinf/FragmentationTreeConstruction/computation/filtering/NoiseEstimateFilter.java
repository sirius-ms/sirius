package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.MassDecomposer.Chemistry.DecomposerCache;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntObjectProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Estimate noise level from
 * - peaks with m/z over parent mass
 * - peaks with no explanation
 */
public class NoiseEstimateFilter implements Preprocessor, Initializable {

    final static Logger logger = LoggerFactory.getLogger(NoiseEstimateFilter.class);

    protected DecomposerCache cache;

    protected int minNumberOfNoisePeaks=40;
    protected boolean checkOnlyNonRadicals=false;

    public DecomposerCache getCache() {
        if (cache == null) cache = new DecomposerCache(10);
        return cache;
    }

    public void setCache(DecomposerCache cache) {
        this.cache = cache;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.minNumberOfNoisePeaks = (int)document.getIntFromDictionary(dictionary, "minNumberOfNoisePeaks");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "minNumberOfNoisePeaks", minNumberOfNoisePeaks);
    }

    @Override
    public MutableMs2Experiment process(MutableMs2Experiment experiment, MeasurementProfile profile) {
        int npeaks = 0;
        for (Ms2Spectrum spec : experiment.getMs2Spectra()) npeaks += spec.size();
        if (npeaks <= minNumberOfNoisePeaks) return experiment;

        final MassToFormulaDecomposer decomposer = getCache().getDecomposer(profile.getFormulaConstraints().getChemicalAlphabet());
        final Deviation dev = profile.getAllowedMassDeviation();
        final FormulaConstraints constraints = profile.getFormulaConstraints();
        final boolean intrinsicalCharged = experiment.getPrecursorIonType().isIntrinsicalCharged();
        final PrecursorIonType ion = experiment.getPrecursorIonType();
        final boolean ionIsKnown = !ion.isIonizationUnknown();

        final double parentmass = Double.isNaN(experiment.getIonMass()) || experiment.getIonMass()<=0 ? 0 : experiment.getIonMass();

        final List<MutableMs2Spectrum> spectra = experiment.getMs2Spectra();
        final TIntObjectHashMap<TDoubleArrayList> noiseLevels = new TIntObjectHashMap<>();
        final TIntArrayList levels = new TIntArrayList();
        for (MutableMs2Spectrum spec : spectra) {
            if (spec.size() < 10) continue;
            final int intensityLevel = (int) Math.round(Math.log10(Spectrums.getMedianIntensity(spec)));
            levels.add(intensityLevel);
            if (!noiseLevels.containsKey(intensityLevel))
                noiseLevels.put(intensityLevel, new TDoubleArrayList(minNumberOfNoisePeaks));

            // print median intensity
            final TDoubleArrayList noise = noiseLevels.get(intensityLevel);

            eachPeak:
            for (int k = 0; k < spec.size(); ++k) {

                if (parentmass > 0 && spec.getMzAt(k) >= (parentmass - 1)) {
                    // avoid adding isotope pattern into noise profile
                    if (spec.getMzAt(k) > (parentmass + 5)) {
                        noise.add(spec.getIntensityAt(k));
                    } else continue;
                }

                final Iterator<MolecularFormula> finder = decomposer.formulaIterator(ion.subtractIonAndAdduct(spec.getMzAt(k)), dev, constraints);
                while (finder.hasNext()) {
                    final MolecularFormula f = finder.next();
                    if (ionIsKnown && checkOnlyNonRadicals) {
                        if (f.maybeCharged() != intrinsicalCharged) continue;
                    }
                    continue eachPeak;
                }
                noise.add(spec.getIntensityAt(k));
            }
        }

        // learn noise thresholds
        final TIntDoubleHashMap noiseThresholds = new TIntDoubleHashMap(10, 0.75f, Integer.MIN_VALUE, 0);
        noiseLevels.forEachEntry(new TIntObjectProcedure<TDoubleArrayList>() {
            @Override
            public boolean execute(int a, TDoubleArrayList b) {
                final double noise = b.size() >= minNumberOfNoisePeaks ? b.getQuick(__quickselect(b, 0, b.size(), b.size()/2)) : 0d;

                noiseThresholds.put(a, noise);
                return true;
            }
        });

        int index=0;
        for (MutableMs2Spectrum spec : spectra) {
            if (spec.size() < 10) continue;
            final int level = levels.get(index++);
            double noiseThreshold = 0d;
            for (int k=level; k >= 0; --k) {
                noiseThreshold = noiseThresholds.get(k);
                if (noiseThreshold>0) break;
            }
            if (noiseThreshold>0)  {
                // apply baseline!
                int n=spec.size();
                for (int k=spec.size()-1; k >= 0; --k) {
                    if (spec.getIntensityAt(k) <= noiseThreshold) {
                        // never delete parent peak
                        if (Math.abs(spec.getMzAt(k)-parentmass) > 0.5) {
                            if (k!=n-1) spec.swap(k, n-1);
                            --n;
                        }
                    }
                }
                int deleted=0;
                for (int k=spec.size()-1; k >= n; --k) {
                    spec.removePeakAt(k);
                    ++deleted;
                }
                if (logger.isDebugEnabled()) logger.debug("Apply baseline at " + noiseThreshold + ", delete " + deleted + " peaks");
                System.err.println("Apply baseline at " + noiseThreshold + ", delete " + deleted + " peaks, for " + String.valueOf(experiment.getSource()) );
            }
        }
        return experiment;
    }

    @Override
    public void initialize(FragmentationPatternAnalysis analysis) {
        this.cache = analysis.getDecomposerCache();
    }

    private static final short[] ALMOST_RANDOM = new short[]{9205, 23823, 4568, 17548, 15556, 31788, 3, 580, 17648, 22647, 17439, 24971, 10767, 9388, 6174, 21774, 4527, 19015, 22379, 12727, 23433, 11160, 15808, 27189, 17833, 7758, 32619, 12980, 31234, 31103, 5140, 571, 4439};

    private static int __quickselect(TDoubleArrayList list, int lo, int hi, int k) {
        int n = hi - lo;
        if (n < 2)
            return lo;

        double pivot = list.getQuick(lo + (ALMOST_RANDOM[k%ALMOST_RANDOM.length]) % n); // Pick a random pivot

        // Triage list to [<pivot][=pivot][>pivot]
        int nLess = 0, nSame = 0, nMore = 0;
        int lo3 = lo;
        int hi3 = hi;
        while (lo3 < hi3) {
            double e = list.getQuick(lo3);
            int cmp = Double.compare(e, pivot);
            if (cmp < 0) {
                nLess++;
                lo3++;
            } else if (cmp > 0) {
                __swap(list, lo3, --hi3);
                if (nSame > 0)
                    __swap(list, hi3, hi3 + nSame);
                nMore++;
            } else {
                nSame++;
                __swap(list, lo3, --hi3);
            }
        }
        assert (nSame > 0);
        assert (nLess + nSame + nMore == n);
        assert (list.getQuick(lo + nLess) == pivot);
        assert (list.getQuick(hi - nMore - 1) == pivot);
        if (k >= n - nMore)
            return __quickselect(list, hi - nMore, hi, k - nLess - nSame);
        else if (k < nLess)
            return __quickselect(list, lo, lo + nLess, k);
        return lo + k;
    }
    private static void __swap(TDoubleArrayList list, int a, int b) {
        final double z = list.getQuick(a);
        list.setQuick(a, list.getQuick(b));
        list.setQuick(b, z);
    }

}
