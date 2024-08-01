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

package de.unijena.bioinf.babelms.inputresource;

import de.unijena.bioinf.babelms.ReportingInputStream;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public interface InputResource<Resource> {

    Resource getResource();

    /**
     * Return the contents of the resource as an array of bytes.
     * @return the contents of the file as bytes, or an empty byte array if empty
     * @throws IOException in case of access errors (if the temporary store fails)
     */
    byte[] getBytes() throws IOException;

    /**
     * Return an InputStream to read the contents of the resource.
     * <p>The user is responsible for closing the returned stream.
     * @return the contents of the file as stream, or an empty stream if empty
     * @throws IOException in case of access errors (if the temporary store fails)
     */
    InputStream getInputStream() throws IOException;

    default ReportingInputStream getReportingInputStream() throws IOException{
        return new ReportingInputStream(getInputStream());
    }

    default ReportingInputStream getReportingInputStream(int reportingChunkInBytes) throws IOException{
        return new ReportingInputStream(getInputStream(), reportingChunkInBytes);
    }


    default BufferedReader getBufferedReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    String getFilename();
    default String getFileExt(){
        String f = getFilename();
        return f.substring(f.lastIndexOf(".") + 1);
    }

    @Nullable
    default URI toUri() {
        if (getFilename() != null && !getFilename().isBlank()) {
            try {
                return new URI(URLEncoder.encode(getFilename(), Charset.defaultCharset()));
            } catch (URISyntaxException e) {
                LoggerFactory.getLogger(getClass()).warn("Error when parsing filename to URI of resource", e);
                return null;
            }
        }
        return null;
    }
    /**
     * Return whether the resource is empty, that is, either no file has
     * been chosen in the multipart form or the chosen file has no content.
     */
    boolean isEmpty();

    /**
     * Return the size of the resource in bytes.
     * @return the size of the file, or 0 if empty
     */
    long getSize();

    default boolean isDeleteAfterImport() {
        return false;
    }
}
