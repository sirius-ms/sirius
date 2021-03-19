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

package de.unijena.bioinf.ms.gui.io.spectrum.csv;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.gui.io.CSVDialogReturnContainer;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.List;

public class CSVToSpectrumConverter {

    public Spectrum<?> convertCSVToSpectrum(List<TDoubleArrayList> data, CSVDialogReturnContainer cont) {
        return convertCSVToSpectrum(data, cont.getMassIndex(), cont.getIntIndex(), cont.getMinEnergy(), cont.getMaxEnergy(), cont.getMsLevel());
    }

    public Spectrum<?> convertCSVToSpectrum(List<TDoubleArrayList> data, int massIndex, int absIntIndex, double minEnergy, double maxEnergy, int msLevel) {
        int rowNumber = data.size();
        double[] masses = new double[rowNumber];
        double[] ints = new double[rowNumber];

        for (int i = 0; i < rowNumber; i++) {
            masses[i] = data.get(i).get(massIndex);
            ints[i] = data.get(i).get(absIntIndex);
        }
        
        Spectrum<?> sp = new SimpleSpectrum(masses, ints);

        if (msLevel > 1) {
            sp = new MutableMs2Spectrum(sp, 0d, (minEnergy > 0 && maxEnergy > 0 && minEnergy <= maxEnergy) ? new CollisionEnergy(minEnergy, maxEnergy) : CollisionEnergy.none(), msLevel);
        }

        return sp;
    }

}
