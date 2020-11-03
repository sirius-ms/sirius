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
 * 28.09.16.
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public abstract class ErrorUtils {
    private static ErrorReportHandler REPORT_HANDLER;

    public static InputStream getErrorLoggingStream() throws Exception {
        if (REPORT_HANDLER == null) {
            for (Handler h : Logger.getLogger("").getHandlers()) {
                if (h instanceof ErrorReportHandler) {
                    REPORT_HANDLER = (ErrorReportHandler) h;
                    break;
                }
            }
        }
        if (REPORT_HANDLER != null) {
            byte[] arr = REPORT_HANDLER.flushToByteArray();
            if (arr != null)
                return new ByteArrayInputStream(arr);
            else
                throw new NullPointerException("Error logging Stream has no bytes");
        } else {
            throw new NullPointerException("No Error logging Stream available");
        }
    }
}
