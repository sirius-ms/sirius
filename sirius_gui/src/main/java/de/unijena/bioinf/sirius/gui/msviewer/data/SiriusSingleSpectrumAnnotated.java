package de.unijena.bioinf.sirius.gui.msviewer.data;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms2IsotopePattern;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

import java.util.BitSet;

public class SiriusSingleSpectrumAnnotated extends SiriusSingleSpectrumModel {

    protected Fragment[] annotatedFormulas;
    protected BitSet isIsotopicPeak;

    static Range<Double> getVisibleRange(Spectrum<? extends Peak> spec) {
        final double maxIntensity = Spectrums.getMaximalIntensity(spec);
        double minMz = Double.POSITIVE_INFINITY, maxMz = 0d;
        for (int i=0; i < spec.size(); ++i) {
            if (spec.getIntensityAt(i)/maxIntensity>=0.005) {
                minMz = Math.min(spec.getMzAt(i), minMz);
                maxMz = Math.max(spec.getMzAt(i), maxMz);
            }
        }
        return Range.closed(minMz, maxMz);
    }

    public SiriusSingleSpectrumAnnotated(FTree tree, Spectrum<? extends Peak> spectrum, double minMz, double maxMz) {
        super(spectrum, minMz, maxMz);
        this.annotatedFormulas = new Fragment[spectrum.size()];
        this.isIsotopicPeak = new BitSet(spectrum.size());
        annotate(tree);
    }

    public SiriusSingleSpectrumAnnotated(FTree tree, Spectrum<? extends Peak> spectrum) {
        super(spectrum);
        this.annotatedFormulas = new Fragment[spectrum.size()];
        this.isIsotopicPeak = new BitSet(spectrum.size());
        annotate(tree);
    }

    @Override
    public String getMolecularFormula(int index) {
        final Fragment f = annotatedFormulas[index];
        if (f==null) return null;
        else return f.getFormula().toString();
    }

    @Override
    public boolean isImportantPeak(int index) {
        return annotatedFormulas[index]!=null;
    }

    @Override
    public boolean isIsotope(int index) {
        return isIsotopicPeak.get(index);
    }

    private void annotate(FTree tree) {
        final FragmentAnnotation<AnnotatedPeak> annotatedPeak = tree.getFragmentAnnotationOrNull(AnnotatedPeak.class);
        if (annotatedPeak==null) return;
        final FragmentAnnotation<Ms2IsotopePattern> isoAno = tree.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        final Deviation dev = new Deviation(1,0.01);
        double scale = 0d;
        for (Fragment f : tree) {
            AnnotatedPeak peak = annotatedPeak.get(f);
            if (peak == null) {
                continue;
            }
            Ms2IsotopePattern isoPat = isoAno==null ? null : isoAno.get(f);
            if (isoPat!=null) {
                for (Peak p : isoPat.getPeaks()) {
                    if (p.getMass() - peak.getMass() > 0.25) {
                        int i = Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, p.getMass()-1e-6);
                        for (int j=i; j < spectrum.size(); ++j) {
                            if (dev.inErrorWindow(p.getMass(), spectrum.getMzAt(j))) {
                                annotatedFormulas[j] = f;
                                isIsotopicPeak.set(j);
                            } else break;
                        }
                    }
                }
            }
            for (Peak p : peak.getOriginalPeaks()) {
                int i = Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, p.getMass()-1e-6);
                for (int j=i; j < spectrum.size(); ++j) {
                    if (dev.inErrorWindow(p.getMass(), spectrum.getMzAt(j))) {
                        annotatedFormulas[j] = f;
                    } else break;
                }
            }
        }
    }
}
