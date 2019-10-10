package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Score;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.sirius.SiriusLocations.SCORES;

public class FormulaScoringSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FormulaScoring> {
    @Override
    public FormulaScoring read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final Map<String,String> kv = reader.keyValues(SCORES.relFilePath(id));
        final FormulaScoring scoring = new FormulaScoring();
        for (String key : kv.keySet()) {
            final Class<? extends FormulaScore> s = (Class<? extends FormulaScore>) Score.resolve(key);
            final double value = Double.parseDouble(kv.get(key));
            scoring.addAnnotation(s, value);
        }
        return scoring;
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
        writer.delete(SCORES.relFilePath(id));
    }
}
