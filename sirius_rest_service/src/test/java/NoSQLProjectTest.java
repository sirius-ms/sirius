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

import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

public class NoSQLProjectTest {

    @Test
    public void testCompounds() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm);

            List<CompoundImport> imports = List.of(CompoundImport.builder().name("foo").features(
                    List.of(FeatureImport.builder().name("foo").ionMass(42d).build())
                    // TODO add more properties and msdata
            ).build());

            List<Compound> compounds = project.addCompounds(imports, EnumSet.of(Compound.OptField.none), EnumSet.of(AlignedFeature.OptField.msData));

            Page<Compound> compounds2 = project.findCompounds(Pageable.unpaged(), EnumSet.of(Compound.OptField.none), EnumSet.of(AlignedFeature.OptField.msData));

            System.out.println(compounds);
            System.out.println(compounds2);

        }
    }

}
