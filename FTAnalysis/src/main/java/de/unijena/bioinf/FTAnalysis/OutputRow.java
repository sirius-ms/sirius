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