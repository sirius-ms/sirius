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


import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.io.IOException;
import java.util.logging.*;

public class TextAreaHandler extends StreamHandler {
    private static final  int MAX_DOC_LENGTH = 100000;

    private void configure() {
        setOutputStream(new TextAreaOutputStream(area));
        setFormatter(new SimpleFormatter());
        try {
            setEncoding("UTF-8");
        } catch (IOException ex) {
            try {
                setEncoding(null);
            } catch (IOException ex2) {
                // doing a setEncoding with null should always work.
                // assert false;
                ex2.printStackTrace();
            }
        }
    }

    private void shrinkToSize() {
        int docLength = area.getDocument().getLength();

        if (docLength > MAX_DOC_LENGTH) {
            area.setCaretPosition(docLength);

            try {
                area.getDocument().remove(0, docLength - MAX_DOC_LENGTH
                        + MAX_DOC_LENGTH / 10);
            } catch (BadLocationException e) {
                LoggerFactory.getLogger(getClass()).warn("Error when shrinking log JTextArea", e);
            }
        }
    }

    private final JTextArea area;

    public TextAreaHandler(JTextArea area, Level level) {
        this(area, level, null);
    }

    public TextAreaHandler(JTextArea area, Level level, Filter logFilter) {
        super();
        this.area = area;
        configure();
        setLevel(level);
        setFilter(logFilter);
    }

    public JTextArea getArea() {
        return area;
    }

    // [UnsynchronizedOverridesSynchronized] Unsynchronized method publish overrides synchronized method in StreamHandler
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
        shrinkToSize();
    }

    // [UnsynchronizedOverridesSynchronized] Unsynchronized method close overrides synchronized method in StreamHandler
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    @Override
    public synchronized void close() {
        flush();
    }
}