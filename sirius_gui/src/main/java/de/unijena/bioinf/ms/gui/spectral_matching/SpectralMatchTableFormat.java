/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.spectral_matching;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import io.sirius.ms.sdk.model.BasicSpectrum;

import java.util.function.Function;

public class SpectralMatchTableFormat extends SiriusTableFormat<SpectralMatchBean> {

    private static final int COL_COUNT = 11;

    public SpectralMatchTableFormat(Function<SpectralMatchBean, Boolean> bestFunc) {
        super(bestFunc);
    }

    @Override
    protected int highlightColumnIndex() {
        return COL_COUNT;
    }

    @Override
    public int getColumnCount() {
        return COL_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "Rank";
            case 1 -> "Name";
            case 2 -> "Molecular Formula";
            case 3 -> "SMILES";
            case 4 -> "Precursor m/z";
            case 5 -> "Similarity";
            case 6 -> "Shared Peaks";
            case 7 -> "Ionization";
            case 8 -> "Collision Energy";
//            case 8 -> "Instrument";
            case 9 -> "Database";
            case 10 -> "ID";
            case 11 -> "Best";
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public Object getColumnValue(SpectralMatchBean baseObject, int column) {
        return switch (column) {
            case 0 -> baseObject.getRank();
            case 1 -> baseObject.getReference().map(BasicSpectrum::getName).orElse(null);
            case 2 -> baseObject.getMatch().getMolecularFormula();
            case 3 -> baseObject.getMatch().getSmiles();
            case 4 -> baseObject.getReference().map(BasicSpectrum::getPrecursorMz).orElse(null);
            case 5 -> baseObject.getMatch().getSimilarity();
            case 6 -> baseObject.getMatch().getSharedPeaks();
            case 7 -> baseObject.getMatch().getAdduct();
            case 8 -> baseObject.getReference()
                    .map(BasicSpectrum::getCollisionEnergy)
                    .map(CollisionEnergy::fromStringOrNull)
                    .orElse(CollisionEnergy.none());
//            case 8 -> "N/A"; //do we want to add this info to the nightsky api model?
            case 9 -> baseObject.getMatch().getDbName();
            case 10 -> baseObject.getDBLink();
            case 11 -> isBest.apply(baseObject);
            default -> throw new IllegalStateException();
        };
    }
}