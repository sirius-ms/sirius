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

package de.unijena.bioinf.ms.middleware.model.compute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.f4b6a3.tsid.TsidCreator;
import com.google.common.jimfs.Jimfs;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.inputresource.InputResource;
import de.unijena.bioinf.babelms.inputresource.PathInputResource;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportMultipartFilesSubmission extends AbstractImportSubmission {
    @Nullable
    protected String tmpDirNameClassifier;
    @NotEmpty
    protected List<MultipartFile> inputFiles;

    @Override
    public List<InputResource<?>> asInputResource() {
        return asPathInputResourceStr().collect(Collectors.toList());
    }

    public List<PathInputResource> asPathInputResource() {
        return asPathInputResourceStr().collect(Collectors.toList());
    }

    public Stream<PathInputResource> asPathInputResourceStr() {
        FileSystem fs = (inputFiles.stream().mapToLong(MultipartFile::getSize).sum() < 4294967296L)
                ? Jimfs.newFileSystem() : FileSystems.getDefault();

        try {
            Path tmpdir = FileUtils.newTempFile("sirius-lcms-import-input_",
                    (tmpDirNameClassifier == null || tmpDirNameClassifier.isBlank() ? "" : "_" + tmpDirNameClassifier),
                    fs);

            Files.createDirectories(tmpdir);
            return inputFiles.stream().map(f -> {
                try {
                    Path nuFile = tmpdir.resolve(Optional.ofNullable(f.getOriginalFilename()).orElse(TsidCreator.getTsid().toString()));
                    f.transferTo(nuFile);
                    return new PathInputResource(nuFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
