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

import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.chemdb.ChemDBs;
import de.unijena.bioinf.spectraldb.nitrite.SpectralNitriteDatabase;

import java.nio.file.Path;

public class NoSQLSpectralDBs extends ChemDBs {

    SpectralLibrary getLocalSpectralLibrary(Path file) {
        return new SpectralNitriteDatabase(file);
    }

    SpectralLibrary getLocalSpectralLibrary(Path file, FingerprintVersion version) {
        return new SpectralNitriteDatabase(file, version);
    }

}
