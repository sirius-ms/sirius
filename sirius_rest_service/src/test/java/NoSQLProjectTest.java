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

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.middleware.model.compounds.Compound;
import de.unijena.bioinf.ms.middleware.model.compounds.CompoundImport;
import de.unijena.bioinf.ms.middleware.model.features.AlignedFeature;
import de.unijena.bioinf.ms.middleware.model.features.FeatureImport;
import de.unijena.bioinf.ms.middleware.model.features.MsData;
import de.unijena.bioinf.ms.middleware.model.spectra.BasicSpectrum;
import de.unijena.bioinf.ms.middleware.service.projects.NoSQLProjectImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.nitrite.NitriteSirirusProject;
import de.unijena.bioinf.projectspace.NoSQLProjectSpaceManager;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class NoSQLProjectTest {

    @Test
    public void testCompounds() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);
            BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);

            ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
            ms2.setMsLevel(2);
            ms2.setPrecursorMz(42d);
            ms2.setScanNumber(5);

            List<CompoundImport> imports = List.of(CompoundImport.builder().name("foo").features(
                    List.of(FeatureImport.builder()
//                            .name("foo")
                            .externalFeatureId("testFID")
                            .ionMass(42d)
                            .charge(1)
                            .detectedAdducts(Set.of("M+H+"))
                            .rtStartSeconds(6d)
                            .rtEndSeconds(12d)
                            .mergedMs1(ms1)
                            .ms1Spectra(List.of(ms1))
                            .ms2Spectra(List.of(ms2, ms2))
                            .build())
            ).build());

            List<Compound> compounds = project.addCompounds(imports, null, EnumSet.of(Compound.OptField.none), EnumSet.of(AlignedFeature.OptField.msData));
            List<Compound> compounds2 = project.findCompounds(Pageable.unpaged(), EnumSet.of(Compound.OptField.none), EnumSet.of(AlignedFeature.OptField.msData)).getContent();

            Assert.assertEquals(1, compounds.size());
            Assert.assertEquals(1, compounds2.size());

            Compound c1 = compounds.get(0);
            Compound c2 = compounds2.get(0);

            Assert.assertEquals(1, c1.getFeatures().size());
            Assert.assertEquals(1, c2.getFeatures().size());

            AlignedFeature f1 = c1.getFeatures().get(0);
            AlignedFeature f2 = c2.getFeatures().get(0);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(c1, c2, "features"));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(f1, f2, "msData"));

            MsData d1 = f1.getMsData();
            MsData d2 = f2.getMsData();

            Assert.assertNotNull(d1);
            Assert.assertNotNull(d2);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs1(), d2.getMergedMs1()));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs2(), d2.getMergedMs2()));
            Assert.assertEquals(2, d1.getMs2Spectra().size());
            Assert.assertEquals(2, d2.getMs2Spectra().size());

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(0), d2.getMs2Spectra().get(0)));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(1), d2.getMs2Spectra().get(1)));

        }
    }

    @Test
    public void testFeatures() throws IOException {

        Path location = FileUtils.createTmpProjectSpaceLocation(SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX);
        try (NitriteSirirusProject ps = new NitriteSirirusProject(location)) {
            NoSQLProjectSpaceManager psm = new NoSQLProjectSpaceManager(ps);
            NoSQLProjectImpl project = new NoSQLProjectImpl("test", psm, (a, b) -> false);

            BasicSpectrum ms1 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);
            BasicSpectrum ms2 = new BasicSpectrum(new double[]{1, 2, 42}, new double[]{1, 2, 3}, 1d);

            ms2.setCollisionEnergy(CollisionEnergy.fromString("20eV"));
            ms2.setMsLevel(2);
            ms2.setPrecursorMz(42d);
            ms2.setScanNumber(5);

            List<FeatureImport> imports = List.of(FeatureImport.builder()
                            .name("foo")
                            .externalFeatureId("testFID")
                            .ionMass(42d)
                            .charge(1)
                            .detectedAdducts(Set.of("M+H+"))
                            .rtStartSeconds(6d)
                            .rtEndSeconds(12d)
                            .mergedMs1(ms1)
                            .ms1Spectra(List.of(ms1))
                            .ms2Spectra(List.of(ms2, ms2))
                            .build());

            List<AlignedFeature> features = project.addAlignedFeatures(imports, null, EnumSet.of(AlignedFeature.OptField.msData));
            List<AlignedFeature> features2 = project.findAlignedFeatures(Pageable.unpaged(), EnumSet.of(AlignedFeature.OptField.msData)).getContent();


            Assert.assertEquals(1, features.size());
            Assert.assertEquals(1, features2.size());

            AlignedFeature f1 = features.get(0);
            AlignedFeature f2 = features2.get(0);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(f1, f2, "msData"));

            MsData d1 = f1.getMsData();
            MsData d2 = f2.getMsData();

            Assert.assertNotNull(d1);
            Assert.assertNotNull(d2);

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs1(), d2.getMergedMs1()));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMergedMs2(), d2.getMergedMs2()));
            Assert.assertEquals(2, d1.getMs2Spectra().size());
            Assert.assertEquals(2, d2.getMs2Spectra().size());

            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(0), d2.getMs2Spectra().get(0)));
            Assert.assertTrue(EqualsBuilder.reflectionEquals(d1.getMs2Spectra().get(1), d2.getMs2Spectra().get(1)));

        }
    }

}
