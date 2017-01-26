package de.unijena.bioinf.sirius.gui.msviewer.data;

import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

public class SiriusSingleSpectrumAnnotated extends SiriusSingleSpectrumModel {

    protected Fragment[] annotatedFormulas;

    public SiriusSingleSpectrumAnnotated(FTree tree, Spectrum<? extends Peak> spectrum) {
        super(spectrum);
        this.annotatedFormulas = new Fragment[spectrum.size()];
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
        return false;
    }

    private void annotate(FTree tree) {
        final FragmentAnnotation<AnnotatedPeak> annotatedPeak = tree.getFragmentAnnotationOrNull(AnnotatedPeak.class);
        if (annotatedPeak==null) return;
        final Deviation dev = new Deviation(1,0.01);
        double scale = 0d;
        for (Fragment f : tree) {
            AnnotatedPeak peak = annotatedPeak.get(f);
            for (Peak p : peak.getOriginalPeaks()) {
                int i = Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, peak.getMass()-1e-6);
                for (int j=i; j < spectrum.size(); ++j) {
                    if (dev.inErrorWindow(p.getMass(), spectrum.getMzAt(j))) {
                        annotatedFormulas[j] = f;
                    } else break;
                }
            }
        }
    }
}
