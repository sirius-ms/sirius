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

import de.unijena.bioinf.jjobs.SwingJJobContainer;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class JobLogDialog extends JDialog {
    private final SwingJJobContainer jobContainer;
    private TextAreaHandler onDemandHandler;

    public JobLogDialog(Dialog owner, SwingJJobContainer source) {
        super(owner, "Job Log: " + source.getJobName() + " - " + source.getJobCategory());
        jobContainer = source;

        if (source instanceof TextAreaJJobContainer)
            add(new LoggingPanel(((TextAreaJJobContainer) source).getJobLog()));
        else {
            add(createOnDemandLoggingPanel());
        }

        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private JPanel createOnDemandLoggingPanel() {
        JTextArea area = new JTextArea();
        onDemandHandler = new TextAreaHandler(new TextAreaOutputStream(area), Level.INFO);
        Logger logger = LogManager.getLogManager().getLogger(jobContainer.getSourceJob().loggerKey());
        logger.addHandler(onDemandHandler);

        return new LoggingPanel(area);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (onDemandHandler != null) {
            Logger logger = LogManager.getLogManager().getLogger(jobContainer.getSourceJob().loggerKey());
            if (logger != null)
                logger.removeHandler(onDemandHandler);
        }
    }
}
