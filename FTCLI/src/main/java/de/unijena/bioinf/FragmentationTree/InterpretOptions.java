package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.ValenceFilter;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProfileImpl;
import de.unijena.bioinf.MassDecomposer.Interval;

import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterpretOptions {

    private final static Pattern INTERVAL = Pattern.compile("\\[(\\d+)?(\\s*-\\s*(\\d+)?)?\\]");
    public static FormulaConstraints getFormulaConstraints(Options options) {
        final PeriodicTable PT = PeriodicTable.getInstance();
        final Pattern pattern = PT.getPattern();
        final String string = options.getElements();
        final Matcher matcher = pattern.matcher(string);
        if (!matcher.find()) throw new ArgumentValidationException("Invalid alphabet: " + options.getElements());
        HashMap<Element, Interval> elements = new HashMap<Element, Interval>();
        while(true) {
            final String m = matcher.group(0);
            if (m.charAt(0)=='(' || m.charAt(0) == ')') throw new ArgumentValidationException("Invalid alphabet: " + options.getElements());
            final Element element = PT.getByName(m);
            if (element == null) throw new ArgumentValidationException("Unknown character: " + m);
            final int start = matcher.end();
            final boolean next = matcher.find();
            final int end = next ? matcher.start() : string.length();
            elements.put(element, new Interval(0, Integer.MAX_VALUE));
            if (end-start > 0) {
                final Matcher n = INTERVAL.matcher(string.substring(start, end));
                if (n.find()) {
                    final int a = n.group(0)!=null ? Integer.parseInt(n.group(0)) : 0;
                    // TODO: support lowerbounds
                    if (a != 0) throw new UnsupportedOperationException("Lowerbounds are  currently not supported");
                    final int b = n.group(1)!=null ? (n.group(2)!=null ? Integer.parseInt(n.group(2)) : Integer.MAX_VALUE) : Integer.MAX_VALUE;
                    elements.put(element, new Interval(a, b));
                }
            }
            if (!next) break;
        }
        final FormulaConstraints constraints = new FormulaConstraints(new ChemicalAlphabet(elements.keySet().toArray(new Element[0])));
        for (Map.Entry<Element, Interval> entry : elements.entrySet()) {
            constraints.getUpperbounds()[constraints.getChemicalAlphabet().indexOf(entry.getKey())] = (int)entry.getValue().getMax();
        }
        constraints.addFilter(new ValenceFilter());
        return constraints;
    }

    public static MeasurementProfile getProfile(Options options) {
        final ProfileImpl profile = new ProfileImpl(new Deviation(7), new Deviation(4), new Deviation(8, 0.2d), getFormulaConstraints(options));
        // ...
        return profile;
    }

    public static List<File> getFiles(Options options) {
        final List<File> files = options.getFiles();
        final ArrayList<File> fs = new ArrayList<File>(files);
        for (File f : files) {
            if (f.isDirectory()) {
                fs.addAll(Arrays.asList(f.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isFile() && pathname.canRead();
                    }
                })));
            } else if (f.canRead()) {
                fs.add(f);
            }
        }
        return fs;
    }

}
