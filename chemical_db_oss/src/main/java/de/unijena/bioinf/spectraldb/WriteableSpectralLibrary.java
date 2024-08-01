/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.spectraldb;

import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public interface WriteableSpectralLibrary {

    int upsertSpectra(List<Ms2ReferenceSpectrum> data) throws IOException;

    void updateSpectraMatchingSmiles(Consumer<Ms2ReferenceSpectrum> updater, String smiles) throws IOException;

}
