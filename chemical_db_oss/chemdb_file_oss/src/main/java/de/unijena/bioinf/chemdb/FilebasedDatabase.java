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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/*
    A file based database consists of a directory of files (either .csv or .json), each file contains compounds from the
    same molecular formula. The filenames consists of the molecular formula strings.
 */
public class FilebasedDatabase extends AbstractBlobBasedDatabase {
    private File dir;

    public FilebasedDatabase(FingerprintVersion version, File dir) throws IOException {
        super(version,dir.getName());
        setDir(dir);
    }



    public File getDir() {
        return dir;
    }

    public void setDir(File dir) throws IOException {
        this.dir = dir;
        refresh();
    }

    @Override
    protected void refresh() throws IOException {
        final ArrayList<MolecularFormula> formulas = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) throw new IOException("Database have to be a directory of .csv xor .json files");
        for (File f : dir.listFiles()) {
            final String name = f.getName();
            final String upName = name.toUpperCase(Locale.US);
            if (upName.startsWith("SETTINGS")) continue;
            boolean found = false;

            for (String s : SUPPORTED_FORMATS) {
                if (upName.endsWith(s)) {
                    if (format == null || format.equals(s)) {
                        format = s;
                        found = true;
                        break;
                    } else {
                        throw new IOException("Database contains several formats. Only one format is allowed! Given format is " + String.valueOf(format) + " but " + name + " found.");
                    }
                }
            }

            if (!found) continue;
            final String form = name.substring(0, name.length() - format.length());
            MolecularFormula.parseAndExecute(form, formulas::add);
        }
        if (format == null) throw new IOException("Couldn't find any compounds in given database");
        format = format.toLowerCase();
        this.reader = format.equals(".json") || format.equals(".json.gz") ? new JSONReader() : new CSVReader();
        this.compressed = format.endsWith(".gz");
        this.formulas = formulas.toArray(MolecularFormula[]::new);
        Arrays.sort(this.formulas);
    }

    @Override
    public @NotNull Optional<InputStream> getRawStream(@NotNull String name) throws IOException {
        Path structureFile = getFileFor(name);
        if (!Files.isRegularFile(structureFile))
            return Optional.empty();
        return Optional.of(Files.newInputStream(structureFile));
    }

    protected Path getFileFor(String name) {
        return dir.toPath().resolve(name + format);
    }
}
