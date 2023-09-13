/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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

package de.unijena.bioinf.babelms.cef;

import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.ms.persistence.model.MicroStreamProjectDb;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import one.microstream.afs.sql.types.SqlConnector;
import one.microstream.afs.sql.types.SqlFileSystem;
import one.microstream.afs.sql.types.SqlProviderSqlite;
import one.microstream.storage.embedded.types.EmbeddedStorage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;
import org.junit.Test;
import org.sqlite.SQLiteDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgilentCefCompoundParserTest {

    @Test
    public void testReadFMEInput() throws IOException {
        final List<Compound> compounds = new ArrayList<>();
//        try (BufferedReader reader = Files.newBufferedReader(Path.of("/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(1 Cmpd).cef"))){
//        try (BufferedReader reader = Files.newBufferedReader(Path.of("/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(15 cmpds).cef"))){
        try (BufferedReader reader = Files.newBufferedReader(Path.of("/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS_MFE+MS2Extr(15 Cmpds).cef"))){
            GenericParser<Compound> parser = new GenericParser<>(new AgilentCefCompoundParser());
            parser.parseIterator(reader, null).forEachRemaining(compounds::add);
        }

        System.out.println(compounds);
    }

    @Test
    public void testReadFMEStore() throws IOException {
//        NioFileSystem fileSystem = NioFileSystem.New();
        SQLiteDataSource dataSource = new SQLiteDataSource();
        String path = "/tmp/MICRO-" + UUID.randomUUID();
        System.out.println(path);
        dataSource.setUrl("jdbc:sqlite:" + path);

        SqlFileSystem fileSystem = SqlFileSystem.New(
                SqlConnector.Caching(SqlProviderSqlite.New(dataSource))
        );

        try (EmbeddedStorageManager store = EmbeddedStorage.start(fileSystem.ensureDirectoryPath("microstream"))) {
            MicroStreamProjectDb project = new MicroStreamProjectDb(store);
            final List<Compound> compounds = new ArrayList<>();
//        try (Buff)eredReader reader = Files.newBufferedReader(Path.of("/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(1 Cmpd).cef"))){
//        try (BufferedReader reader = Files.newBufferedReader(Path.of("/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(15 cmpds).cef"))){
            String[] locations = new String[]{
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS_MFE+MS2Extr(15 Cmpds).cef",
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(15 cmpds).cef",
//                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_TMSMS(13 cmpds).cef",
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS_MFE+MS2Extr(15 Cmpds).cef",
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(15 cmpds).cef",
//                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_TMSMS(13 cmpds).cef",
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS_MFE+MS2Extr(15 Cmpds).cef",
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(15 cmpds).cef",
//                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_TMSMS(13 cmpds).cef",
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS_MFE+MS2Extr(15 Cmpds).cef",
                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_AMSMS(15 cmpds).cef",
//                    "/home/fleisch/sirius-testing/demo/000_sirius_test/compounds/ForTox_TestMix_TMSMS(13 cmpds).cef",
            };
            for (String location : locations) {
                try (BufferedReader reader = Files.newBufferedReader(Path.of(location))) {
                    GenericParser<Compound> parser = new GenericParser<>(new AgilentCefCompoundParser());
                    parser.parseIterator(reader, null).forEachRemaining(compounds::add);
                }
            }


            System.out.println(compounds);
            project.getProject().setCompounds(compounds);
            project.getStorageManager().storeRoot();
            System.out.println(compounds);
        }
        System.out.println();

    }
}
