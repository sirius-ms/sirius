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

import de.unijena.bioinf.ms.gui.table.SiriusTableFormat;
import de.unijena.bioinf.ms.nightsky.sdk.model.BasicSpectrum;

import java.util.Optional;
import java.util.function.Function;

public class SpectralMatchTableFormat extends SiriusTableFormat<SpectralMatchBean> {

    private static final int COL_COUNT = 10;

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
            case 0 -> "Name";
            case 1 -> "Molecular Formula";
            case 2 -> "SMILES";
            case 3 -> "Precursor m/z";
            case 4 -> "Similarity";
            case 5 -> "Shared Peaks";
            case 6 -> "Ionization";
            case 7 -> "Collision Energy";
//            case 8 -> "Instrument";
            case 8 -> "Database";
            case 9 -> "ID";
            case 10 -> "Best";
            default -> throw new IllegalStateException();
        };
    }

    @Override
    public Object getColumnValue(SpectralMatchBean baseObject, int column) {
        return switch (column) {
            case 0 -> baseObject.getReference().map(BasicSpectrum::getName).orElse("");
            case 1 -> baseObject.getMatch().getMolecularFormula();
            case 2 -> baseObject.getMatch().getSmiles();
            case 3 -> baseObject.getReference().map(BasicSpectrum::getPrecursorMz).map(d -> Double.toString(d)).orElse("N/A");
            case 4 -> baseObject.getMatch().getSimilarity();
            case 5 -> baseObject.getMatch().getSharedPeaks();
            case 6 -> Optional.ofNullable(baseObject.getMatch().getAdduct()).orElse("N/A");
            case 7 -> baseObject.getReference().map(BasicSpectrum::getCollisionEnergy).orElse("N/A");
//            case 8 -> "N/A"; //todo nightsky -> do we want to add this info to the api model?
            case 8 -> baseObject.getMatch().getDbName();
            case 9 -> baseObject.getDBLink();
            case 10 -> isBest.apply(baseObject);
            default -> throw new IllegalStateException();
        };
    }
}
