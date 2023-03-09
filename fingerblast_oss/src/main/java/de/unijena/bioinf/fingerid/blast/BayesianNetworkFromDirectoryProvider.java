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
import de.unijena.bioinf.chemdb.AbstractChemicalDatabase;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BayesianNetworkFromDirectoryProvider implements BayesianNetworkScoringProvider, BayesianNetworkScoringStorage {
    private final Logger Log = LoggerFactory.getLogger(BayesianNetworkFromDirectoryProvider.class);

    private final Path scoringDirectory;
    private final BayesianScoringUtils bayesianScoringUtils;

    private final static String filePrefix = "bayesianScoring_";
    private final static String fileSuffix = "";
    private final AbstractChemicalDatabase chemDB;

    private final ReadWriteLock lock;

    private BayesnetScoring defaultScoring = null;

    private final boolean autoSaveComputedScorings;

    public BayesianNetworkFromDirectoryProvider(Path scoringDirectory, BayesianScoringUtils bayesianScoringUtils, AbstractChemicalDatabase chemDB, boolean autoSaveComputedScorings) {
        this.lock = new ReentrantReadWriteLock();
        if (!Files.exists(scoringDirectory))
            throw new IllegalArgumentException("Directory does not exist, please create first: " + scoringDirectory);
        if (!Files.isDirectory(scoringDirectory))
            throw new IllegalArgumentException(scoringDirectory + " is not a directory");
        this.scoringDirectory = scoringDirectory;
        this.bayesianScoringUtils = bayesianScoringUtils;
        this.autoSaveComputedScorings = autoSaveComputedScorings;
        this.chemDB = chemDB;
    }

    public BayesianNetworkFromDirectoryProvider(Path scoringDirectory, BayesianScoringUtils bayesianScoringUtils, AbstractChemicalDatabase chemDB) {
        this(scoringDirectory, bayesianScoringUtils, chemDB, true);
    }

    public boolean isDefaultScoring(BayesnetScoring scoring) {
        if (this.defaultScoring==null) {
            try {
                getDefaultScoring();
            } catch (IOException e) {
                return false;
            }
        }
        return this.defaultScoring==scoring;
    }

    @Override
    public BayesnetScoring getScoringOrDefault(MolecularFormula formula) throws IOException {
        Path scoringPath = getScoringPath(formula);
        lock.readLock().lock();
        final boolean exists;
        try {
             exists = Files.exists(scoringPath);
        } finally {
            lock.readLock().unlock();
        }
        if (exists) {
            return readScoringFromFile(scoringPath);
        } else {
            try {
                final BayesnetScoring scoring = bayesianScoringUtils.computeScoring(formula, chemDB);
                if (autoSaveComputedScorings && (scoring != null)) {
                    storeScoring(formula, scoring, true);
                }
                return scoring;
            } catch (InsufficientDataException e) {
                Log.info("Cannot compute Bayesian scoring tree topolology for "+formula+". Insufficient data");
                return getDefaultScoring();
            }
        }
    }

    @Override
    public BayesnetScoring getScoringOrNull(MolecularFormula formula) throws IOException {
        Path scoringPath = getScoringPath(formula);
        lock.readLock().lock();
        final boolean exists;
        try {
            exists = Files.exists(scoringPath);
        } finally {
            lock.readLock().unlock();
        }
        if (exists) {
            return readScoringFromFile(scoringPath);
        } else {
            try {
                final BayesnetScoring scoring = bayesianScoringUtils.computeScoring(formula, chemDB);
                if (autoSaveComputedScorings && (scoring != null)) {
                    storeScoring(formula, scoring, true);
                }
                return scoring;
            } catch (InsufficientDataException e) {
                Log.info("Cannot compute Bayesian scoring tree topolology for "+formula+". Insufficient data");
                return null;
            }
        }
    }

    @Override
    public BayesnetScoring getDefaultScoring() throws IOException {
        if (defaultScoring!=null) return defaultScoring;
        Path scoringPath = getDefaultScoringPath();
        lock.readLock().lock();
        final boolean ex = Files.exists(scoringPath);
        lock.readLock().unlock();
        if (ex) {
            BayesnetScoring scoring = readScoringFromFile(scoringPath);
            lock.writeLock().lock();
            if (defaultScoring==null) defaultScoring=scoring;
            lock.writeLock().unlock();
            return defaultScoring;
        } else {
            if (! (chemDB instanceof ChemicalDatabase))
                throw new UnsupportedOperationException("Default scoring can only be computed with SQL based chemDB");
            BayesnetScoring scoring = bayesianScoringUtils.computeDefaultScoring((ChemicalDatabase) chemDB);
            lock.writeLock().lock();
            if (defaultScoring!=null) {
                lock.writeLock().unlock();
                return defaultScoring;
            }
            storeDefaultScoring(scoring, true);
            this.defaultScoring = scoring;
            lock.writeLock().unlock();
            return defaultScoring;
        }
    }

    private BayesnetScoring readScoringFromFile(Path scoringPath) throws IOException {
        //todo maybe move to utils/factory
        final double alpha = bayesianScoringUtils.getPseudoCount();
        final MaskedFingerprintVersion fpVersion = bayesianScoringUtils.getMaskedFingerprintVersion();
        return BayesnetScoringBuilder.readScoringFromFile(scoringPath, fpVersion, alpha, bayesianScoringUtils.allowNegativeScoresForBayesianNetScoringOnly());
    }

    @Override
    public void storeScoring(MolecularFormula formula, BayesnetScoring scoring, boolean override) throws IOException {
        Path scoringPath = getScoringPath(formula);
        lock.writeLock().lock();
        scoring.writeTreeWithCovToFile(scoringPath);
        lock.writeLock().unlock();
    }

    @Override
    public void storeDefaultScoring(BayesnetScoring scoring, boolean override) throws IOException {
        Path scoringPath = getDefaultScoringPath();
        lock.writeLock().lock();
        scoring.writeTreeWithCovToFile(scoringPath);
        lock.writeLock().unlock();
    }

    private Path getDefaultScoringPath() {
        return scoringDirectory.resolve(filePrefix+"default"+fileSuffix);
    }

    private Path getScoringPath(MolecularFormula formula) {
        return scoringDirectory.resolve(filePrefix+formula.formatByHill()+fileSuffix);
    }


}
