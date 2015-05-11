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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.io.File;

import static de.unijena.bioinf.FTAnalysis.ErrorCode.TOMUCHTIME;
import static de.unijena.bioinf.FTAnalysis.ErrorCode.UNINITIALIZED;

public class OutputRow {

    File fileName;
    MolecularFormula formula;
    int numberOfDecompositions;
    double optScore, correctScore;
    int correctRank;
    long runtime;
    String name;
    ErrorCode error;

    public OutputRow() {
        fileName=null;
        formula = null;
        optScore=correctScore=Double.NaN;
        correctRank=0;
        error=UNINITIALIZED;
    }

    String header = "name,formula,decompositions,correctScore,optScore,rank,runtime,error";

    public String toCSV() {
        return name + "," + formula.formatByHill() + "," + numberOfDecompositions + "," +
                correctScore + "," + optScore + "," + (error == TOMUCHTIME ? 0 : correctRank) + "," + (runtime/1000000) + "," + error;
    }


}
