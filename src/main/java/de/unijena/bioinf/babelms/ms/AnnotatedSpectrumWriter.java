package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AnnotatedSpectrumWriter {

    public void writeFile(File f, FTree tree) throws IOException {
        final FileWriter fw = new FileWriter(f);
        write(fw, tree);
    }

    public void write(Writer writer, FTree tree) throws IOException {
        final BufferedWriter bw = (writer instanceof BufferedWriter) ? (BufferedWriter)writer : new BufferedWriter(writer);

        final Ionization ion = tree.getAnnotationOrThrow(Ionization.class);
        final FragmentAnnotation<Peak> peakAno = tree.getFragmentAnnotationOrThrow(Peak.class);
        final List<Fragment> fragments = new ArrayList<Fragment>(tree.getFragments());
        Collections.sort(fragments);
        bw.write("mz\tintensity\trel.intensity\texactmass\texplanation\n");
        double scale = 0d;
        for (Fragment f : fragments) {
            scale = Math.max(peakAno.get(f).getIntensity(), scale);
        }
        scale = 100d/scale;
        for (Fragment f : fragments) {
            final Peak p = peakAno.get(f);
            bw.write(String.format(Locale.US, "%.6f", p.getMass()));
            bw.write('\t');
            bw.write(String.format(Locale.US, "%.2f", p.getIntensity()));
            bw.write('\t');
            bw.write(String.format(Locale.US, "%.2f", scale*p.getIntensity()));
            bw.write('\t');
            bw.write(String.format(Locale.US, "%.6f", ion.addToMass(f.getFormula().getMass())));
            bw.write('\t');
            bw.write(f.getFormula().toString());
            bw.write('\n');
        }
        bw.close();
    }

}
