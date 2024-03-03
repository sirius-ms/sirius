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

package de.unijena.bioinf.ms.middleware.model;

import de.unijena.bioinf.ms.frontend.subtools.InputResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class MultipartInputResource implements InputResource<MultipartFile> {
    private final MultipartFile file;

    public MultipartInputResource(MultipartFile file) {
        this.file = file;
    }

    @Override
    public MultipartFile getResource() {
        return file;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return getResource().getBytes();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return getResource().getInputStream();
    }

    @Override
    public String getFilename() {
        return getResource().getOriginalFilename();
    }

    @Override
    public URI toUri() {
        try {
            return getResource().getResource().getURI();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return getResource().isEmpty();
    }

    @Override
    public long getSize() {
        return getResource().getSize();
    }
}
