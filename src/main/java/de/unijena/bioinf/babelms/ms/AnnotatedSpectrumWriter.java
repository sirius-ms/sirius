/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
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
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        final List<Fragment> fragments = new ArrayList<Fragment>(tree.getFragments());
        Collections.sort(fragments);
        bw.write("mz\tintensity\trel.intensity\texactmass\texplanation\n");
        for (Fragment f : fragments) {
            final AnnotatedPeak p = peakAno.get(f);
            if (p==null) continue;
            bw.write(String.format(Locale.US, "%.6f", p.getMass()));
            bw.write('\t');
            bw.write(String.format(Locale.US, "%.2f", p.getMaximalIntensity()));
            bw.write('\t');
            bw.write(String.format(Locale.US, "%.2f", 100d*p.getRelativeIntensity()));
            bw.write('\t');
            bw.write(String.format(Locale.US, "%.6f", ion.addToMass(f.getFormula().getMass())));
            bw.write('\t');
            bw.write(f.getFormula().toString());
            bw.write('\n');
        }
        bw.close();
    }

}
