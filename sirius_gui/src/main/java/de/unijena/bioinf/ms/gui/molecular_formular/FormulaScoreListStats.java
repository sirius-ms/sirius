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

package de.unijena.bioinf.ms.gui.molecular_formular;


import de.unijena.bioinf.ms.gui.table.list_stats.DoubleListStats;

/**
 * Created by fleisch on 12.05.17.
 */
public class FormulaScoreListStats extends DoubleListStats {
    private double expScoreSum;

    public FormulaScoreListStats(double[] values) {
        super(values);
    }

    public FormulaScoreListStats() {
        super();
    }

    public double getExpScoreSum() {
        return expScoreSum;
    }

    public double getExpMaxScore() {
        return Math.exp(getMax() - getMax());
    }

    public double getExpMinScore() {
        return Math.exp(getMin() - getMax());
    }

    @Override
    public FormulaScoreListStats update(double[] values) {
        super.update(values);
        if (values != null)
            for (double value : values)
                expScoreSum += Math.exp(value - getMax());
        return this;
    }

    @Override
    public FormulaScoreListStats reset() {
        super.reset();
        expScoreSum = 0;
        return this;
    }
}
