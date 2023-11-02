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

package de.unijena.bioinf.ms.frontend.subtools.projectspace;

import com.github.f4b6a3.ksuid.KsuidCreator;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumFileSource;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.GenericParser;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.jjobs.JobProgressEvent;
import de.unijena.bioinf.jjobs.JobProgressEventListener;
import de.unijena.bioinf.jjobs.JobProgressMerger;
import de.unijena.bioinf.jjobs.ProgressSupport;
import de.unijena.bioinf.ms.frontend.workflow.Workflow;
import de.unijena.bioinf.projectspace.CompoundContainerId;
import de.unijena.bioinf.projectspace.Instance;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ImportFromMemoryWorkflow implements Workflow, ProgressSupport {
    protected final JobProgressMerger progressSupport = new JobProgressMerger(this);
    private List<CompoundContainerId> importedCompounds = null;

    public List<CompoundContainerId> getImportedCompounds() {
        return importedCompounds;
    }

    private ProjectSpaceManager<?> psm;

    private Supplier<BufferedReader> dataReaderProvide;

    private String sourceName;

    private String ext;

    public ImportFromMemoryWorkflow(ProjectSpaceManager<?> psm, Supplier<BufferedReader> dataReaderProvide, String sourceName, String ext) {
        this.psm = psm;
        this.dataReaderProvide = dataReaderProvide;
        this.sourceName = sourceName;
        this.ext = ext;
    }

    @Override
    public void updateProgress(long min, long max, long progress, String shortInfo) {
        progressSupport.updateConnectedProgress(min, max, progress, shortInfo);
    }

    @Override
    public void addJobProgressListener(JobProgressEventListener listener) {
        progressSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removeJobProgressListener(JobProgressEventListener listener) {
        progressSupport.removeProgress(listener);
    }

    @Override
    public JobProgressEvent currentProgress() {
        return progressSupport.currentConnectedProgress();
    }

    @Override
    public JobProgressEvent currentCombinedProgress() {
        return progressSupport.currentCombinedProgress();
    }

    @Override
    public void run() {
        importedCompounds = new ArrayList<>();
        GenericParser<Ms2Experiment> parser = new MsExperimentParser()
                .getParserByExt(ext);

        int progress = 0;
        try (BufferedReader bodyStream = dataReaderProvide.get()) {
            updateProgress(0, -1, progress, "Data reader opened");
            try (CloseableIterator<Ms2Experiment> it = parser.parseIterator(bodyStream, null)) {
                while (it.hasNext()) {
                    Ms2Experiment next = it.next();
                    if (sourceName == null)  // workaround to fake import file
                        sourceName = "Unknown-" + KsuidCreator.getKsuid().toString();
                    next.setAnnotation(SpectrumFileSource.class,
                            new SpectrumFileSource(
                                    new File((sourceName.endsWith(ext) ? sourceName : sourceName + "." + ext.toLowerCase())).toURI()));

                    @NotNull Instance inst = psm.newCompoundWithUniqueId(next);
                    importedCompounds.add(inst.getID());
                    updateProgress(0, -1, ++progress, "Imported: " + inst.getID().toString());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        updateProgress(0, progress, progress, null);
    }
}
