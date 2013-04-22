package de.unijena.bioinf.MassDecomposer.cli;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.*;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class Main {

    public static String VERSION = "DECOMP 1.1";

    public static void main(String[] args) {
        final Cli<Options> cli = CliFactory.createCli(Options.class);
        final Options options;
        try {
            options = cli.parseArguments(args);
        } catch (ArgumentValidationException e) {
            final PrintStream out;
            if (e instanceof HelpRequestedException) {
                out = System.out;
            } else {
                out = System.err;
                out.println("Error while parsing command line arguments: " + e.getMessage());
            }
            out.println(cli.getHelpMessage());
            out.println("usage: decomp <mass>\ndecomp -p <ppm> -a <absolute error> -e \"CH[<min>-<max>]N[<min>-]O[-<max>]P[<num>]\" <mass>");
            return;
        }
        if (options.getVersion()) {
            System.out.println(VERSION);
            return;
        }
        final String filter = options.getFilter().toUpperCase();
        final FilterLevel level = FilterLevel.valueOf(filter);
        DecompositionValidator<Element> validator = null;
        if (level == null) {
            System.err.println("Unknown filter '" + options.getFilter() + "'. Allowed are strict, common, permissive, rdbe, none.\n");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        } else if (level != FilterLevel.NONE && options.getDontUseRDBE()) {
            System.err.println("Conflicting options: --nofilter and --filter='" + options.getFilter() + "'. Only one of both must be set.\n");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        } else {
            switch (level) {
                case STRICT: validator = ChemicalValidator.getStrictThreshold(); break;
                case COMMON: validator = ChemicalValidator.getCommonThreshold(); break;
                case PERMISSIVE: validator = ChemicalValidator.getPermissiveThreshold(); break;
                case RDBE: validator = new ValenceValidator<Element>(); break;
                case NONE: validator = null;
            }
        }
        final double mass;
        if (options.getIonization() != null) {
            final double mz = options.getMass();
            final String ion = options.getIonization();
            final Ionization ionization = PeriodicTable.getInstance().ionByName(ion);
            if (ionization == null) {
                System.err.println("Unknown ion '" + ion + "'");
                return;
            }
            mass = ionization.subtractFromMass(mz);
            if (validator == null) {
                // do nothing
            } else if (validator instanceof ChemicalValidator) {
                final ChemicalValidator c = (ChemicalValidator)validator;
                validator = new ChemicalValidator(c.getRdbeThreshold(), c.getRdbeLowerbound()+0.5d, c.getHeteroToCarbonThreshold(), c.getHydrogenToCarbonThreshold() );
            } else if (validator instanceof ValenceValidator) {
                validator = new ValenceValidator<Element>(0d);
            }
        } else {
            mass = options.getMass();
        }
        final Deviation dev = new Deviation(options.getPPM(), options.getAbsoluteDeviation(), Math.pow(10, -options.getPrecision()));
        final ChemicalAlphabet alphabet = options.getAlphabet().getAlphabet();
        final MassDecomposer<Element> decomposer = new MassDecomposer<Element>(dev.getPrecision(), dev.getPpm(),
                dev.getAbsolute(),alphabet);

        Map<Element, Interval> boundary = options.getAlphabet().getBoundary();
        final String parentFormula = options.getParentFormula();
        if (parentFormula != null) {
            final MolecularFormula formula = MolecularFormula.parse(parentFormula);
            for (Element e : alphabet.getElements()) {
                Interval i = boundary.get(e);
                if (i==null) {
                    i = new Interval(0, formula.numberOf(e));
                    boundary.put(e, i);
                } else {
                    boundary.put(e, new Interval(i.getMin(), Math.min(i.getMax(), formula.numberOf(e))));
                }
            }
        }
        /*
        if (validator != null && (validator instanceof ValenceValidator)) {
            boundary = new ValenceBoundary<Element>(options.getAlphabet().getAlphabet()).getMapFor(mass +
                    dev.absoluteFor(mass), options.getAlphabet().getBoundary());
        }
        */
        decomposer.setValidator(validator);

        final List<int[]> compomers = decomposer.decompose(mass, boundary);
        final List<MolecularFormula> formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            formulas.add(alphabet.decompositionToFormula(c));
        }
        Collections.sort(formulas, new Comparator<MolecularFormula>() {
            @Override
            public int compare(MolecularFormula o1, MolecularFormula o2) {
                return Double.compare(Math.abs(o1.getMass() - mass), Math.abs(o2.getMass() - mass));
            }
        });
        final boolean printErrors = options.getMassErrors();
        final DecimalFormat formater = (DecimalFormat)NumberFormat.getInstance(Locale.ENGLISH);
        formater.applyPattern("#.##");
        for (MolecularFormula f : formulas) {
            System.out.print(f);
            if (printErrors) {
                System.out.print("\t");
                System.out.print(mass - f.getMass());
                System.out.print("\t");
                System.out.println(formater.format(((mass - f.getMass()) / mass) * 1e6));
            } else {
                System.out.println("");
            }
        }
    }

}