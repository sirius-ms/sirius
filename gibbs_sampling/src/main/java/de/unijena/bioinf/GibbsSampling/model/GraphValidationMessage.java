/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.GibbsSampling.model;

/**
 * Created by ge28quv on 01/06/17.
 */
public class GraphValidationMessage {

    private final String message;
    private final boolean isError;
    private final boolean isWarning;

    public GraphValidationMessage(String message, boolean isError, boolean isWarning) {
        this.message = message;
        this.isError = isError;

        if (isError) this.isWarning = false;
        else {
            this.isWarning = isWarning;
        }
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return isError;
    }

    public boolean isWarning() {
        return isWarning;
    }
}
