/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.decomp;

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.*;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class DecompWorkflow implements Workflow {
    DecompOptions options;
    InputFilesOptions input;

    public DecompWorkflow(DecompOptions options, InputFilesOptions input) {
        this.options = options;
        this.input = input;
    }

    @Override
    public void run() {
        DecompositionValidator<Element> validator;
        if ((options.level != FilterLevel.COMMON && options.level != FilterLevel.NONE) && options.noRDBE) {
            LoggerFactory.getLogger(getClass()).error("Conflicting options: --nofilter and --filter='" + options.level + "'. Only one of both must be set.");
            return;
        } else {
            switch (options.level) {
                case STRICT:
                    validator = ChemicalValidator.getStrictThreshold();
                    break;
                case COMMON:
                    validator = options.noRDBE ? null : ChemicalValidator.getCommonThreshold();
                    break;
                case PERMISSIVE:
                    validator = ChemicalValidator.getPermissiveThreshold();
                    break;
                case RDBE:
                    validator = new ValenceValidator<>();
                    break;
                default:
                    validator = null;
            }
        }

        if (validator == null) {
            // do nothing
        } else if (validator instanceof ChemicalValidator) {
            final ChemicalValidator c = (ChemicalValidator) validator;
            validator = new ChemicalValidator(c.getRdbeThreshold(), c.getRdbeLowerbound() + 0.5d, c.getHeteroToCarbonThreshold(), c.getHydrogenToCarbonThreshold());
        } else {
            validator = new ValenceValidator<>(-0.5d);
        }
        final Deviation dev = new Deviation(options.ppm, options.absDeviation);
        final ChemicalAlphabet alphabet = options.alphabet.getAlphabet();
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(alphabet);

        Map<Element, Interval> boundary = options.alphabet.getBoundary();
        final String parentFormula = options.parentFormula;
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

        // parse input massed
        List<Double> masses = options.masses != null ? Arrays.stream(options.masses).boxed().collect(Collectors.toList()) : new ArrayList<>();
        if (input != null && input.msInput != null && input.msInput.unknownFiles != null) {
            for (Path path : input.msInput.unknownFiles) {
                try {
                    masses.addAll(Files.readAllLines(path).stream().map(Double::valueOf).collect(Collectors.toList()));
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when parsing masses from input file: '" + path.toString() + "'. Skipping this file!", e);
                }
            }
        }

        final boolean printErrors = options.massErrors;
        try (Writer ow = options.out != null ? Files.newBufferedWriter(options.out) : new OutputStreamWriter(System.out)) {
            //write header
            ow.write("m/z\tdecompositions");
            if (printErrors)
                ow.write("\tabsMassDev\trelMassDev");
            ow.write(System.lineSeparator());

            for (double mz : masses) {
                final double mass;
                final String ion = options.ionization;
                final PrecursorIonType ionization = ion == null ? null : PeriodicTable.getInstance().ionByNameOrNull(ion);
                mass = ionization == null ? mz : ionization.precursorMassToNeutralMass(mz);

                final List<int[]> compomers;
                if (options.maxDecomps == null || options.maxDecomps <= 0) {
                    compomers = decomposer.decompose(mass, dev, boundary);
                } else {
                    compomers = new ArrayList<>(options.maxDecomps);
                    int count = options.maxDecomps;
                    DecompIterator<Element> it = decomposer.decomposeIterator(mass, dev, boundary);
                    while (it.next() && count > 0) {
                        compomers.add(it.getCurrentCompomere().clone());
                        count--;
                    }
                }

                final List<MolecularFormula> formulas = new ArrayList<>(compomers.size());
                for (int[] c : compomers) {
                    if (validator == null || validator.validate(c, decomposer.getOrderedCharacterIds(), decomposer.getAlphabet()))
                        formulas.add(alphabet.decompositionToFormula(c));
                }
                formulas.sort(Comparator.comparingDouble(o -> Math.abs(o.getMass() - mass)));


                final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
                formater.applyPattern("#.####");
                ow.write(formater.format(mz));
                ow.write("\t");
                ow.write(formulas.stream().map(MolecularFormula::toString).collect(Collectors.joining(",")));
                if (printErrors) {
                    ow.write("\t");
                    ow.write(formulas.stream().map(f -> formater.format(mass - f.getMass())).collect(Collectors.joining(",")));
                    ow.write("\t");
                    ow.write(formulas.stream().map(f -> formater.format(((mass - f.getMass()) / mass) * 1e6)).collect(Collectors.joining(",")));
                }
                ow.write(System.lineSeparator());
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass()).error("could not write output! Canceling...", e);
        }
    }
}
