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

package de.unijena.bioinf.ChemistryBase.data;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.io.File;
import java.net.URI;

public class DataSource implements DataAnnotation, TreeAnnotation, Ms2ExperimentAnnotation {

    private final URI url;

    public static DataSource fromString(String x) {
        return new DataSource(URI.create(x));
    }

    public DataSource(URI url) {
        this.url = url;
    }

    public DataSource(File f) {
        this.url = f.toURI();
    }

    public URI getURI() {
        return url;
    }

    public String toString() {
        return url.toString();
    }

    public String getName(){
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
