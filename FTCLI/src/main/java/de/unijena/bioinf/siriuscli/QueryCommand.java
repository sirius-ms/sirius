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
package de.unijena.bioinf.siriuscli;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.ElementMap;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTree.Main;
import de.unijena.bioinf.FragmentationTree.QueryOptions;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.babelms.chemdb.DBMolecularFormulaCache;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by kaidu on 10.04.14.
 */
public class QueryCommand implements Command {
    @Override
    public String getDescription() {
        return "search a molecular formula or mass in a structure or molecular formula database";
    }

    @Override
    public String getName() {
        return "query";
    }

    private final static Pattern NUM_PATTERN = Pattern.compile("^\\s*\\d+");
    @Override
    public void run(String[] args) {
        final Cli<QueryOptions> cli = CliFactory.createCli(QueryOptions.class);
        try {
            final QueryOptions options = cli.parseArguments(args);
            final File cacheFile = Main.getFormulaCacheFile(options.getCachingDirectory(), options.getDatabase());
            final DBMolecularFormulaCache cache = Main.initializeFormulaCache(cacheFile, options.getDatabase());
            final Deviation dev = new Deviation(options.getPPM(), options.getAbsoluteDeviation());
            final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(ChemicalAlphabet.alphabetFor(MolecularFormula.parse("CHNOPSClBrIF")));
            for (String s : options.getQueries()) {
                if (NUM_PATTERN.matcher(s).find()) {
                    final double value = Double.parseDouble(s);
                    cache.checkMassRange(value, dev);
                    final List<MolecularFormula> formulas = decomposer.decomposeToFormulas(value, dev);
                    for (MolecularFormula f : formulas)
                        if (cache.isFormulaExist(f)) System.out.println(f + "\t" + f.getMass());
                } else {
                    final MolecularFormula f = MolecularFormula.parse(s);
                    if (cache.isFormulaExist(f)) System.out.println(f + "\t" + f.getMass());
                }
            }
        } catch (ArgumentValidationException e) {
            final PrintStream out;
            if (e instanceof HelpRequestedException) {
                out = System.out;
            } else {
                out = System.err;
                out.println("Error while parsing command line arguments: " + e.getMessage());
            }
            out.println(cli.getHelpMessage());
        }
    }

    @Override
    public String getVersion() {
        return null;
    }
}
