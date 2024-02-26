/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.middleware.service.dbs;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.webapi.WebAPI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChemDbServiceImpl implements ChemDbService {
    private final WebAPI<?> webAPI;
    public ChemDbServiceImpl(WebAPI<?> webAPI) {
        this.webAPI = webAPI;
        System.out.println("INIT Databases");
        log.info("INIT Databases");
        try {
            //request fingerprint version to init db and check compatibility
            final CdkFingerprintVersion version = webAPI.getCDKChemDBFingerprintVersion();
            //loads all current available dbs
            SearchableDatabases.getCustomDatabases(version);
        } catch (Exception e) {
            log.error("Error when loading Custom databases",e);
        }
    }

    @Override
    public WebWithCustomDatabase db() {
        return webAPI.getChemDB();
    }
}
