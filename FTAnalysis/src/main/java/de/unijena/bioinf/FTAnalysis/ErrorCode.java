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
package de.unijena.bioinf.FTAnalysis;

public enum ErrorCode {

    NOERROR(0, "no errors"),
    UNINITIALIZED(1, "instance is not properly initialized"),
    IO(2, "IO exception"),
    DECOMPNOTFOUND(3, "cannot find correct decomposition"),
    REALTREENOTFOUND(4, "cannot find correct tree"),
    OUTOFTIME(6, "computation needs to much time"),
    TOMUCHTIME(7, "computation needs to much time to complete"),
    UNKNOWN(5, "unknown exception");

    private int code;
    private String message;

    private ErrorCode(int code, String message) {
        this.code=code;
        this.message=message;
    }

}
