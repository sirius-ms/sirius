/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.treeviewer;


import java.util.regex.Pattern;

public class Property {

    private final String name;
    private final int lineNumber;
    private final int offset, length;
    private final Pattern regexp;
    private boolean enabled;

    public Property(String name, int lineNumber, int offset, int length, Pattern regexp) {
        this.name = name;
        this.lineNumber = lineNumber;
        this.offset = offset;
        this.length = length;
        this.regexp = regexp;
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public Pattern getRegexp() {
        return regexp;
    }
}
