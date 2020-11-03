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

package de.isas.mztab2.io;

import de.isas.mztab2.model.MzTab;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

public class SiriusWorkspaceMzTabNonValidatingWriter extends MzTabNonValidatingWriter {
    public SiriusWorkspaceMzTabNonValidatingWriter() {
        super();
    }

    public SiriusWorkspaceMzTabNonValidatingWriter(MzTabWriterDefaults writerDefaults) {
        super(writerDefaults);
    }

    /**
     * <p>
     * Write the mzTab object to the provided output stream writer.</p>
     * <p>
     * This method does not close the output stream but will issue a
     * <code>flush</code> on the provided output stream writer!
     *
     * @param writer a {@link java.io.OutputStreamWriter} object.
     * @param mzTab  a {@link de.isas.mztab2.model.MzTab} object.
     * @throws java.io.IOException if any.
     */
    public Optional<Void> write(Writer writer, MzTab mzTab) throws IOException {
        writeMzTab(mzTab, writer);
        return Optional.empty();
    }
}
