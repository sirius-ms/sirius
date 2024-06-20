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

package de.unijena.bioinf.ms.middleware.controller;

import de.unijena.bioinf.FragmentationTreeConstruction.computation.tree.TreeBuilderFactory;
import de.unijena.bioinf.chemdb.ChemicalDatabase;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.middleware.SiriusContext;
import de.unijena.bioinf.ms.middleware.model.info.Info;
import de.unijena.bioinf.ms.middleware.service.info.ConnectionChecker;
import de.unijena.bioinf.ms.rest.model.info.VersionsInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@Tag(name = "Info", description = "Status und Information")
public class InfoController {
    private final SiriusContext siriusContext;
    private final ConnectionChecker connectionChecker;

    public InfoController(SiriusContext siriusContext, ConnectionChecker connectionChecker) {
        this.siriusContext = siriusContext;
        this.connectionChecker = connectionChecker;
    }

    @RequestMapping(value = "/api/info", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Info getInfo() {

        Info.InfoBuilder b = Info.builder()
                .chemDbVersion("N/A")
                .nightSkyApiVersion(siriusContext.getApiVersion())
                .siriusVersion(ApplicationCore.VERSION())
                .siriusLibVersion(FingerIDProperties.siriusVersion())
                .fingerIdLibVersion(FingerIDProperties.fingeridFullVersion())
                .availableILPSolvers(TreeBuilderFactory.getInstance().getAvailableBuilders())
                .fingerIdModelVersion("N/A") //todo add model version in a performant way.
                .fingerprintId(ChemicalDatabase.FINGERPRINT_ID);

        try {
            VersionsInfo info = siriusContext.webAPI().getVersionInfo(true);
            if (info != null){
                b.chemDbVersion(info.databaseDate);
                b.latestSiriusVersion(info.getLatestSiriusVersion().toString());
                b.latestSiriusLink(info.getLatestSiriusLink());
                b.updateAvailable(ApplicationCore.VERSION_OBJ().compareTo(info.getLatestSiriusVersion()) < 0);
            } else {
                try {
                    String dbDate = Optional.ofNullable(siriusContext.webAPI().getChemDbDate()).orElse("N/A");
                    b.chemDbVersion(dbDate);
                } catch (Exception e) {
                    log.warn("Could not get chemDbDate because of: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not get VersionsInfo because of: {}", e.getMessage());
        }

        return b.build();
    }

    @RequestMapping(value = "/api/connection-status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ConnectionChecker.ConnectionCheck getConnectionCheck() {
        return connectionChecker.checkConnection();
    }

}