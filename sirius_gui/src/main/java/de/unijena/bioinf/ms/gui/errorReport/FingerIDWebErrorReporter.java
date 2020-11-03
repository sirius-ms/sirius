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

package de.unijena.bioinf.ms.gui.errorReport;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 04.10.16.
 */

import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.utils.errorReport.ErrorReporter;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FingerIDWebErrorReporter extends ErrorReporter {

    public FingerIDWebErrorReporter() {
    }

    public FingerIDWebErrorReporter(String SOFTWARE_NAME, ErrorReport report) {
        super(SOFTWARE_NAME, report);
    }

    public FingerIDWebErrorReporter(ErrorReport report) {
        super(report);
    }

    @Override
    protected String reportError(ErrorReport report) throws IOException, URISyntaxException {
        String r = ApplicationCore.WEB_API.reportError(report, SOFTWARE_NAME);
        return r;
    }
}
