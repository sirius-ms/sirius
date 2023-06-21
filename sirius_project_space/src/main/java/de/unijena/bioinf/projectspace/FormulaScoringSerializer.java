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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.SiriusLocations.SCORES;

public class FormulaScoringSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FormulaScoring> {
    @Override
    public FormulaScoring read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        try {
            final Map<String, String> kv = reader.keyValues(SCORES.relFilePath(id));
            final FormulaScoring scoring = new FormulaScoring();
            for (String key : kv.keySet()) {
                final Class<? extends FormulaScore> s = (Class<? extends FormulaScore>) Score.resolve(key);
                double value;
                try {
                    value = Double.parseDouble(kv.get(key));
                } catch (NumberFormatException e) {
                    LoggerFactory.getLogger(getClass()).warn("Could not parse score value '" + key + ":" + kv.get(key) + "'. Setting value to " + FormulaScore.NA());
                    value = FormulaScore.NA(s).score();
                }
                scoring.addAnnotation(s, value);
            }
            return scoring;
        } catch (IOException e) {
            LoggerFactory.getLogger(FormulaScoringSerializer.class).error("Cannot read formula scoring of " + id.getParentId().toString() + " (" + id.toString() + ")");
            throw e;
        }
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FormulaScoring> optScore) throws IOException {
        final FormulaScoring scoring = optScore.orElseThrow(() -> new RuntimeException("Could not find Experiment for FormulaResult with ID: " + id));
        final HashMap<String,String> values = new HashMap<>();
        for (FormulaScore score : scoring) {
            values.put(Score.simplify(score.getClass()), String.valueOf(score.score()));
        }
        writer.keyValues(SCORES.relFilePath(id), values);
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.deleteIfExists(SCORES.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(SCORES.relDir());
    }
}
