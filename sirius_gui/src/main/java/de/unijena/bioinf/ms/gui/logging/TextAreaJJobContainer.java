/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.logging;

import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;

import javax.swing.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

//this is a swing job wrapper that writes log output into a associated text area instead into the console or the log file
public class TextAreaJJobContainer<R> extends SwingJJobContainer<R> {
    private JTextArea jobLog;
    private TextAreaHandler textAreaLogHandler;

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName, String projectId) {
        super(sourceJob, jobName, projectId);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog();
    }

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, Supplier<String> jobName, Supplier<String> projectId) {
        super(sourceJob, jobName, projectId);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog();
    }

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName, String jobCategory, String projectId) {
        super(sourceJob, jobName, jobCategory, projectId);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog();
    }

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, Supplier<String> jobName, Supplier<String> projectId, Supplier<String> jobCategory) {
        super(sourceJob, jobName, projectId, jobCategory);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog();
    }


    public JTextArea getJobLog() {
        return jobLog;
    }

    public TextAreaHandler getTextAreaLogHandler() {
        return textAreaLogHandler;
    }

    private TextAreaHandler connectJobLogToTextArea() {
        return new TextAreaHandler(jobLog, Level.INFO, sourceJob.getLogFilter());
    }

    public void registerJobLog() {
        Logger.getLogger(sourceJob.loggerKey()).addHandler(textAreaLogHandler);
    }

    @Override
    public void clean() {
        try {
            Logger.getLogger(sourceJob.loggerKey()).removeHandler(textAreaLogHandler);
            textAreaLogHandler = null;
            jobLog = null;
        } finally {
            super.clean();
        }
    }
}
