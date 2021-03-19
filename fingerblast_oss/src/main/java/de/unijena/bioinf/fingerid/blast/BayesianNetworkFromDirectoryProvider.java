/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid.blast;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.exceptions.InsufficientDataException;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BayesianNetworkFromDirectoryProvider implements BayesianNetworkScoringProvider, BayesianNetworkScoringStorage {
    private final Logger Log = LoggerFactory.getLogger(BayesianNetworkFromDirectoryProvider.class);

    private final Path scoringDirectory;
    private final BayesianScoringUtils bayesianScoringUtils;

    private final static String filePrefix = "bayesianScoring_";
    private final static String fileSuffix = "";

    private BayesnetScoring defaultScoring = null;

    private final boolean autoSaveComputedScorings;

    public BayesianNetworkFromDirectoryProvider(Path scoringDirectory, BayesianScoringUtils bayesianScoringUtils, boolean autoSaveComputedScorings) {
        if (!Files.exists(scoringDirectory)) throw new IllegalArgumentException("Directory does not exist, please create first: "+scoringDirectory);
        if (!Files.isDirectory(scoringDirectory)) throw new IllegalArgumentException(scoringDirectory+" is not a directory");
        this.scoringDirectory = scoringDirectory;
        this.bayesianScoringUtils = bayesianScoringUtils;
        this.autoSaveComputedScorings = autoSaveComputedScorings;
    }

    public BayesianNetworkFromDirectoryProvider(Path scoringDirectory, BayesianScoringUtils bayesianScoringUtils) {
        this(scoringDirectory, bayesianScoringUtils, true);
    }

    @Override
    public BayesnetScoring getScoringOrDefault(MolecularFormula formula) throws IOException {
        Path scoringPath = getScoringPath(formula);
        if (Files.exists(scoringPath)) {
            return readScoringFromFile(scoringPath);
        } else {
            return computeOrGetDefaultScoring(formula);
        }
    }

    @Override
    public BayesnetScoring getScoringOrNull(MolecularFormula formula) throws IOException {
        Path scoringPath = getScoringPath(formula);
        if (Files.exists(scoringPath)) {
            return readScoringFromFile(scoringPath);
        } else {
            return computeOrGetNull(formula);
        }
    }

    private BayesnetScoring computeOrGetDefaultScoring(MolecularFormula formula) throws IOException {
        return computeScoring(formula, true);
    }

    private BayesnetScoring computeOrGetNull(MolecularFormula formula) throws IOException {
        return computeScoring(formula, false);
    }

    private BayesnetScoring computeScoring(MolecularFormula formula, boolean getDefaultIfFails) throws IOException {
        BayesnetScoring scoring = null;
        try {
            //todo always store scoring after being computed?
            scoring = bayesianScoringUtils.computeScoring(formula);
            if (autoSaveComputedScorings && (scoring != null)) {
                storeScoring(formula, scoring, true);
            }
        } catch (ChemicalDatabaseException e) {
            e.printStackTrace();
            Log.error("Cannot compute Bayesian scoring tree topolology. Error retrieving data", e);
        } catch (InsufficientDataException e) {
            Log.info("Cannot compute Bayesian scoring tree topolology for "+formula+". Insufficient data");
        }
        if (scoring != null) return scoring;
        return getDefaultIfFails ? getDefaultScoring() : null;
    }



    @Override
    public BayesnetScoring getDefaultScoring() throws IOException {
        if (defaultScoring != null) return defaultScoring;
        else {
            Path scoringPath = getDefaultScoringPath();
            if (Files.exists(scoringPath)) {
                defaultScoring = readScoringFromFile(scoringPath);
            } else {
                defaultScoring = bayesianScoringUtils.computeDefaultScoring();
                if (autoSaveComputedScorings && (defaultScoring != null)) {
                    storeDefaultScoring(defaultScoring, true);
                }
            }
            return defaultScoring;
        }
    }

    public boolean hasPrecomputedDefaultScoring() {
        if (defaultScoring != null) return true;
        Path scoringPath = getDefaultScoringPath();
        if (Files.exists(scoringPath)) {
            try {
                defaultScoring = readScoringFromFile(scoringPath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean hasPrecomputedScoring(MolecularFormula formula) {
        //todo also keep track somewhere, if there was no data to compute the scoring
        Path scoringPath = getScoringPath(formula);
        if (Files.exists(scoringPath)) {
            try {
                defaultScoring = readScoringFromFile(scoringPath);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    private BayesnetScoring readScoringFromFile(Path scoringPath) throws IOException {
        //todo maybe move to utils/factory
        final double alpha = bayesianScoringUtils.getPseudoCount();
        final MaskedFingerprintVersion fpVersion = bayesianScoringUtils.getMaskedFingerprintVersion();
        return BayesnetScoringBuilder.readScoringFromFile(scoringPath, fpVersion, alpha, bayesianScoringUtils.allowNegativeScoresForBayesianNetScoringOnly());
    }

    @Override
    public synchronized void storeScoring(MolecularFormula formula, BayesnetScoring scoring, boolean override) throws IOException {
        if (!override & hasPrecomputedScoring(formula)) return;
        Path scoringPath = getScoringPath(formula);
        scoring.writeTreeWithCovToFile(scoringPath);
    }

    @Override
    public synchronized void storeDefaultScoring(BayesnetScoring scoring, boolean override) throws IOException {
        if (!override & hasPrecomputedDefaultScoring()) return;
        Path scoringPath = getDefaultScoringPath();
        scoring.writeTreeWithCovToFile(scoringPath);
    }

    private Path getDefaultScoringPath() {
        return scoringDirectory.resolve(filePrefix+"default"+fileSuffix);
    }

    private Path getScoringPath(MolecularFormula formula) {
        return scoringDirectory.resolve(filePrefix+formula.formatByHill()+fileSuffix);
    }


}
