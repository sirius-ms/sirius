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
package de.unijena.bioinf.MassDecomposer;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class Main {

    public final static String VERSION = "1.21";

    public final static String CITATION =
            "DECOMP---from interpreting Mass Spectrometry peaks to solving the Money Changing Problem\n" +
                    "Sebastian Böcker, Zsuzsanna Lipták, Marcel Martin, Anton Pervukhin and Henner Sudek\n" +
                    "Bioinformatics, 24(4):591-593, 2008\n\n" +
                    "Faster mass decomposition\n" +
                    "Kai Dührkop, Marcus Ludwig, Marvin Meusel and Sebastian Böcker\n" +
                    "Proc. of Workshop on Algorithms in Bioinformatics (WABI 2013), of Lect Notes Comput Sci, Springer, Berlin, 2013";

    public final static String VERSION_STRING = "DECOMP " + VERSION + "\n\n" + CITATION;

    public final static String USAGE = "usage:\ndecomp <mass>\ndecomp -p <ppm> -a <absolute error> -e \"CH[<min>-<max>]N[<min>-]O[-<max>]P[<num>]\" <mass>";


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
            out.println(USAGE);
            return;
        }
        if (options.getVersion() || options.getCite()) {
            System.out.println(VERSION);
            return;
        }
        if (args.length == 0) {
            System.out.println(VERSION);
            System.out.println(USAGE);
            System.out.println("write 'decomp --help' for further information");
            return;
        }
        final String filter = options.getFilter().toUpperCase();
        final FilterLevel level = FilterLevel.valueOf(filter);
        DecompositionValidator<Element> validator = null;
        if (level == null) {
            System.err.println("Unknown filter '" + options.getFilter() + "'. Allowed are strict, common, permissive, rdbe, none.\n");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        } else if ((level != FilterLevel.COMMON && level != FilterLevel.NONE) && options.getDontUseRDBE()) {
            System.err.println("Conflicting options: --nofilter and --filter='" + options.getFilter() + "'. Only one of both must be set.\n");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        } else {
            switch (level) {
                case STRICT:
                    validator = ChemicalValidator.getStrictThreshold();
                    break;
                case COMMON:
                    validator = options.getDontUseRDBE() ? null : ChemicalValidator.getCommonThreshold();
                    break;
                case PERMISSIVE:
                    validator = ChemicalValidator.getPermissiveThreshold();
                    break;
                case RDBE:
                    validator = new ValenceValidator<Element>();
                    break;
                case NONE:
                    validator = null;
            }
        }
        final double mass;
        final double mz = options.getMass();
        final String ion = options.getIonization();
        final PrecursorIonType ionization = ion == null ? null/*PeriodicTable.getInstance().ionByName("[M+H]+") */ : PeriodicTable.getInstance().ionByNameOrNull(ion);
        /*
        if (ionization == null) {
            System.err.println("Unknown ion '" + ion + "'");
            return;
        }
        */
        mass = ionization==null ? mz : ionization.precursorMassToNeutralMass(mz);
        if (validator == null) {
            // do nothing
        } else if (validator instanceof ChemicalValidator) {
            final ChemicalValidator c = (ChemicalValidator) validator;
            validator = new ChemicalValidator(c.getRdbeThreshold(), c.getRdbeLowerbound() + 0.5d, c.getHeteroToCarbonThreshold(), c.getHydrogenToCarbonThreshold());
        } else if (validator instanceof ValenceValidator) {
            validator = new ValenceValidator<Element>(-0.5d);
        }
        final Deviation dev = new Deviation(options.getPPM(), options.getAbsoluteDeviation());
        final ChemicalAlphabet alphabet = options.getAlphabet().getAlphabet();
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(alphabet);

        Map<Element, Interval> boundary = options.getAlphabet().getBoundary();
        final String parentFormula = options.getParentFormula();
        if (parentFormula != null) {
            final MolecularFormula formula = MolecularFormula.parseOrThrow(parentFormula);
            for (Element e : alphabet.getElements()) {
                Interval i = boundary.get(e);
                if (i == null) {
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

        final List<int[]> compomers = decomposer.decompose(mass, dev, boundary);
        final List<MolecularFormula> formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            if (validator==null || validator.validate(c, decomposer.orderedCharacterIds, decomposer.getAlphabet()))
                formulas.add(alphabet.decompositionToFormula(c));
        }
        Collections.sort(formulas, new Comparator<MolecularFormula>() {
            @Override
            public int compare(MolecularFormula o1, MolecularFormula o2) {
                return Double.compare(Math.abs(o1.getMass() - mass), Math.abs(o2.getMass() - mass));
            }
        });
        final boolean printErrors = options.getMassErrors();
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
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
