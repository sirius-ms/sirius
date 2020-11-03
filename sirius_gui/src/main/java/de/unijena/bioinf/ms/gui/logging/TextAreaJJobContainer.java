/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.logging;

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.jjobs.SwingJJobContainer;

import javax.swing.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

//this is a swing job wrapper that writes log output into a accociated text area instead into the console or the log file
public class TextAreaJJobContainer<R> extends SwingJJobContainer<R> {
    private final JTextArea jobLog;
    private final TextAreaHandler textAreaLogHandler;

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName) {
        super(sourceJob, jobName);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog(sourceJob);
    }

    public TextAreaJJobContainer(ProgressJJob<R> sourceJob, String jobName, String jobCategory) {
        super(sourceJob, jobName, jobCategory);
        jobLog = new JTextArea();
        textAreaLogHandler = connectJobLogToTextArea();
        registerJobLog(sourceJob);
    }

    public JTextArea getJobLog() {
        return jobLog;
    }

    private TextAreaHandler connectJobLogToTextArea() {
        return new TextAreaHandler(new TextAreaOutputStream(jobLog), Level.INFO);
    }

    public void registerJobLogs(JJob... jobs) {
        registerJobLogs(Arrays.asList(jobs));
    }

    public void registerJobLogs(Iterable<JJob> jobs) {
        for (JJob job : jobs) {
            registerJobLog(job);
        }
    }

    public void registerJobLog(JJob job) {
        Logger logger = Logger.getLogger(job.loggerKey());
        logger.addHandler(textAreaLogHandler);
    }
}
