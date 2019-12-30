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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class AnnotatedSpectrumWriter {

    public enum Fields {
        MZ("mz"),
        INTENSITY("intensity"),
        REL_INTENSITY("rel.intensity"),
        EXACTMASS("exactmass"),
        FORMULA("formula"),
        ION("ionization"),
        //ISOTOPE("isotope")
        ;

        public final String name;

        private Fields(String name) {
            this.name=name;
        }
    };

    private EnumSet<Fields> enabledFields;

    public AnnotatedSpectrumWriter() {
        this.enabledFields = EnumSet.allOf(Fields.class);
    }

    public AnnotatedSpectrumWriter(Fields enabled, Fields... enabledFields) {
        this.enabledFields = EnumSet.of(enabled, enabledFields);
    }

    public void writeFile(File f, FTree tree) throws IOException {
        final FileWriter fw = new FileWriter(f);
        write(fw, tree);
    }

    public void write(Writer writer, FTree tree) throws IOException {
        final BufferedWriter bw = (writer instanceof BufferedWriter) ? (BufferedWriter)writer : new BufferedWriter(writer);

        final PrecursorIonType ion = tree.getAnnotationOrThrow(PrecursorIonType.class);
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        final List<Fragment> fragments = new ArrayList<Fragment>(tree.getFragments());
        Collections.sort(fragments);
        bw.write(Arrays.stream(Fields.values()).filter(enabledFields::contains).map(x->x.name).collect(Collectors.joining("\t")));
        bw.newLine();
        final List<String> values = new ArrayList<>();
        for (Fragment f : fragments) {
            values.clear();
            final AnnotatedPeak p = peakAno.get(f);
            if (p==null) continue;
            if (enabledFields.contains(Fields.MZ)) {
                values.add(String.format(Locale.US, "%.6f", p.getMass()));
            }
            if (enabledFields.contains(Fields.INTENSITY)) {
                values.add(String.format(Locale.US, "%.2f", p.getMaximalIntensity()));
            }
            if (enabledFields.contains(Fields.REL_INTENSITY)) {
                values.add(String.format(Locale.US, "%.2f", 100d*p.getRelativeIntensity()));
            }
            if (enabledFields.contains(Fields.EXACTMASS)) {
                values.add(String.format(Locale.US, "%.6f", ion.getIonization().addToMass(f.getFormula().getMass())));
            }
            if (enabledFields.contains(Fields.FORMULA)) {
                values.add(f.getFormula().toString());
            }
            if (enabledFields.contains(Fields.ION)) {
                values.add(f.getIonization().toString());
            }
            bw.write(values.stream().collect(Collectors.joining("\t")));
            bw.newLine();
        }
        bw.close();
    }

}
