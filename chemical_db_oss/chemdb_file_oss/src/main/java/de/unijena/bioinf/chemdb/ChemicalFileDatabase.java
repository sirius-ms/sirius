/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.storage.blob.file.FileBlobStorage;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/*
    A file based database consists of a directory of files (either .csv or .json), each file contains compounds from the
    same molecular formula. The filenames consists of the molecular formula strings.
 */
@Deprecated
public class ChemicalFileDatabase extends ChemicalBlobDatabase<FileBlobStorage> {

    public ChemicalFileDatabase(FingerprintVersion version, FileBlobStorage storage) throws IOException {
        super(version, storage);
        init();
    }

    @Override
    protected void init() throws IOException {
        final List<String> fileNames = FileUtils.listAndClose(storage.getRoot(), s -> s.map(Path::getFileName).map(Path::toString)
                        .filter(p -> !p.toUpperCase().startsWith("SETTINGS")).collect(Collectors.toList()));

        if (!fileNames.isEmpty()) {
            compression = fileNames.stream().map(Compression::fromName).findFirst().orElse(Compression.NONE);

            format = fileNames.stream().map(s -> s.substring(0, s.length() - compression.ext().length()))
                    .map(Format::fromName).filter(Objects::nonNull)
                    .findFirst().orElseThrow(() -> new IOException("Could not determine Database formatQ"));
        }else {
            compression = Compression.GZIP;
            format = Format.JSON;
            LoggerFactory.getLogger(getClass()).warn("Empty DB '" + storage.getName() + "'. Using default format '" + format.ext + "' with compression '" + compression.ext + "'.");
        }

        this.formulas = fileNames.stream().map(n -> n.substring(0, n.length() - format.ext().length() - compression.ext().length()))
                .map(MolecularFormula::parseOrThrow).sorted().toArray(MolecularFormula[]::new);

        this.reader = format == Format.CSV ? new CSVReader() : new JSONReader();
    }
}
