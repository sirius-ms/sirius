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

package de.unijena.bioinf.lcms;

public class SpectralMerger {
/*
    protected final LCMSProccessingInstance lcms;

    public SpectralMerger(LCMSProccessingInstance lcms) {
        this.lcms = lcms;
    }

    public MergedSpectrum mergeConsecutiveScans(ChromatographicPeak peak, List<Scan> ms2Scans) {
        ArrayList<ArrayList<FragmentScan>> segments = segmentScans(peak,ms2Scans);
        final List<MergedSpectrum> specPerSegment = new ArrayList<>();
        for (ArrayList<FragmentScan> s : segments) {
            specPerSegment.add(merge(s));
        }
        return null;
    }

    private MergedSpectrum merge(ArrayList<FragmentScan> scans) {
        // find best one
        final double noiseLevel = lcms.getNoiseModelMs2().getNoiseLevel(scans.get(0).getMsms().getScanNumber(),scans.get(0).getMsms().getPrecursor().getMass());
        scans.sort(Comparator.comparingDouble(FragmentScan::getQualityscore));
        Collections.reverse(scans);
        MergedSpectrum init = new MergedSpectrum(scans.get(0).getMsms(), lcms.getStorage().getScan(scans.get(0).getMsms()));
        for (int k=1; k < scans.size(); ++k) {
            merge(init, scans.get(k), lcms.getStorage().getScan(scans.get(k).getMsms()), noiseLevel);
        }

    }

    private boolean merge(MergedSpectrum init, FragmentScan fragmentScan, SimpleSpectrum scan, double noiseLevel) {
        final CosineQuerySpectrum a = prepareForCosine(init, init.getPrecursor(), noiseLevel);
        final CosineQuerySpectrum b =prepareForCosine(scan, init.getPrecursor(), noiseLevel);
        if (new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(10))).cosineProduct(a,b).similarity > 0.7) {

        } else {
            return false;
        }

    }

    private CosineQuerySpectrum prepareForCosine(Spectrum<? extends Peak> spec, Precursor precursor, double noiseLevel) {
        CosineQueryUtils cosineQueryUtils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(15)));
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum(spec);
        Spectrums.applyBaseline(buf, noiseLevel);
        Spectrums.cutByMassThreshold(buf, precursor.getMass()-20);
        SimpleSpectrum spectrum = Spectrums.extractMostIntensivePeaks(buf, 6, 100d);
        return cosineQueryUtils.createQuery(spectrum, precursor.getMass());
    }

    @NotNull
    private ArrayList<ArrayList<FragmentScan>> segmentScans(ChromatographicPeak peak, List<Scan> ms2Scans) {
        // segment MS/MS
        final ArrayList<ArrayList<FragmentScan>> scanSegments = new ArrayList<>();
        final List<FragmentScan> ms2FragmentScans = ms2Scans.stream().map(x->FragmentScan.createFragmentScanFor(lcms,x)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        scanSegments.add(new ArrayList<>(Arrays.asList(ms2FragmentScans.get(0))));
        int apexBefore = ms2FragmentScans.get(0).getExtremumScanIndex();
        for (int i=1; i < ms2Scans.size(); ++i) {
            final int apx = ms2FragmentScans.get(i).getExtremumScanIndex();
            if (apx != apexBefore) {
                scanSegments.add(new ArrayList<>());
                apexBefore = apx;
            }
            scanSegments.get(scanSegments.size()-1).add(ms2FragmentScans.get(i));
        }
        return scanSegments;
    }

*/
}
