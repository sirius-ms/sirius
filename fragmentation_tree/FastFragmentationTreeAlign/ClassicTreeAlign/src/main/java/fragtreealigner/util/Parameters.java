
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package fragtreealigner.util;

import fragtreealigner.FragmentationTreeAligner.ExecutionMode;
import fragtreealigner.algorithm.ScoringFunctionNeutralLosses.ScoreWeightingType;
import fragtreealigner.algorithm.TreeAligner.NormalizationType;
import fragtreealigner.domainobjects.chem.basics.ChemInfo;
import fragtreealigner.domainobjects.chem.basics.Element;
import fragtreealigner.domainobjects.chem.basics.ElementTable;
import fragtreealigner.domainobjects.db.FragmentationTreeDatabase.DecoyType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;

@SuppressWarnings("serial")
public class Parameters implements Serializable {
	public ChemInfo chemInfo;
	public ElementTable elementTable;
	
	public ExecutionMode executionMode;
	public boolean makeGraphicalOutput;
	public boolean makeMatrixOutput;
	public boolean makeMatrixExtOutput;
	public boolean makeTopOutput;
	public boolean makeVerboseOutput;
	
	public DecoyType decoyType;

	public boolean makeLocalAlignment;
	public boolean isNodeUnionAllowed;
	public boolean useCnl;
	public boolean testRDiff;
	public boolean testRDiffH2;
	public boolean testH2;
	public boolean computePlikeValue;
	public int runsPlikeValue;
	public boolean considerPullUps;
	public int calcStatistics;
	public String statOutDir;
	public NormalizationType normalizationType;
	public ScoreWeightingType scoreWeightingType; 
	
	public float scoreEquality;
    public float scoreNodeEquality;
    public float scoreNodeInequality;
    public float scoreNodeEqualityPerAtom;
	public float scoreNodeInequalityPerAtom;
    public float scoreEqualityPerAtom;
	public float scoreInequality;
	public float scoreInequalityPerAtom;
	public float scoreGap;
	public float scoreGapCnl;
	public float scoreCnlCnl;
	public float scoreNullNull;
	public float scoreUnion;
	public float scoreDiffCommonFirstOrder;
	public float scoreDiffCommonSecondOrder;
	public float scoreDiffCommonPerAtom;
	public float scoreDiffH2;
	public float scoreDiffH2PerAtom;
	public float scoreDiffCommonPlusH2;
	public float scoreDiffCommonPlusH2PerAtom;
	public float scoreSimilarNLMass;
	public double ppm_error;
	public double manipStrength;
	public boolean DmatchesH;
	public boolean useNodeLabels;
	public boolean cnlSizeDependent;
	public boolean useNLandNodes;
    public boolean useOnlyNodeBonus;
	public boolean oneNodePenalty;
	public boolean makeGlobalAlignment;
	public boolean makeEndGapFreeAlignment;
	public boolean testSimilarNLMass;
	public boolean scoreRoot;
	
	public Parameters(Session session) {
		session.setParameters(this);
		
		elementTable = new ElementTable();
		elementTable.addCHNOPS();
		elementTable.addElement(new Element("Cl", "Chlorine", 34.968852, 1));
		elementTable.addElement(new Element("F", "Flourine", 18.998403, 1));
		chemInfo = new ChemInfo(session);
		
		executionMode = ExecutionMode.NORMAL;
		makeGraphicalOutput = false;
		makeMatrixOutput = false;
		makeMatrixExtOutput = false;
		makeTopOutput = false;
		makeVerboseOutput = false;
		
		decoyType = DecoyType.NONE;

		makeLocalAlignment = true;
		makeGlobalAlignment = false;
		makeEndGapFreeAlignment = false;
		isNodeUnionAllowed = true;
		useCnl = true;
		testRDiff = true;
		testRDiffH2 = true;
		testH2 = true;
		testSimilarNLMass = false;
		computePlikeValue = false;
		runsPlikeValue = 10;
		considerPullUps = false;
		normalizationType = NormalizationType.NONE;
		scoreWeightingType = ScoreWeightingType.NONE;
		ppm_error = 10;
		manipStrength = 5;
		DmatchesH = true;
		useNodeLabels = false;
		useNLandNodes = false;
        useOnlyNodeBonus=false;
		cnlSizeDependent = false;
		oneNodePenalty = false;
		scoreRoot = false;


        scoreNodeEquality=5;
        scoreNodeInequality=-5;
        scoreNodeEqualityPerAtom=1;
        scoreNodeInequalityPerAtom=-1;

        scoreEquality = 5;
        scoreEqualityPerAtom = 1;
		scoreInequality = -5;
		scoreInequalityPerAtom = -1;
        //scoreGap = -5;
        scoreGap = -8;
		scoreGapCnl = -2;
		scoreCnlCnl = +3;
		scoreNullNull = 0;
		scoreUnion = 0;
		scoreDiffCommonFirstOrder = 2;
		scoreDiffCommonSecondOrder = 0;
		scoreDiffCommonPerAtom = 0.5f;
		scoreDiffH2 = 3;
		scoreDiffH2PerAtom = 1;
		scoreDiffCommonPlusH2 = 0;
		scoreDiffCommonPlusH2PerAtom = 0.1f;
        scoreSimilarNLMass = 3f;

//		scoreEquality = 3;
//		scoreEqualityPerAtom = 3;
//		scoreInequality = -11;
//		scoreInequalityPerAtom = -2;
//		scoreGap = -10;
//		scoreGapCnl = -5;
//		scoreCnlCnl = -2;
//		scoreNullNull = 0;
//		scoreUnion = -6;
//		scoreDiffCommon = -3;
//		scoreDiffCommonPerAtom = -2;
//		scoreDiffH2 = -3;
//		scoreDiffH2PerAtom = -1;
//		scoreDiffCommonPlusH2 = -6;
//		scoreDiffCommonPlusH2PerAtom = -2;

//		scoreEquality = 3.0f;
//		scoreEqualityPerAtom = 4.8f;
//		scoreInequality = -11.2f;
//		scoreInequalityPerAtom = -2.3f;
//		scoreGap = -10.1f;
//		scoreGapCnl = -5.5f;
//		scoreCnlCnl = -2.2f;
//		scoreDiffCommon = -3.7f;
//		scoreDiffCommonPerAtom = -2.6f;
//		scoreDiffH2 = -4.1f;
//		scoreDiffH2PerAtom = 0.5f;
//		scoreDiffCommonPlusH2 = 4.0f;
//		scoreDiffCommonPlusH2PerAtom = 2.4f;
//		scoreUnion = -5.8f;
	}
	
	public void setScores(BufferedReader reader) throws IOException {
		while (reader.ready()) {
			String line = reader.readLine();
			String[] scoreParams = line.trim().split("\\s");
			if (scoreParams[0].equalsIgnoreCase("EqualityInit")) scoreEquality = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("EqualityPerAtom")) scoreEqualityPerAtom = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("CnlCnl")) scoreCnlCnl = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("DiffH2Init")) scoreDiffH2 = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("DiffH2PerAtom")) scoreDiffH2PerAtom = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("DiffFirstReasonableInit")) scoreDiffCommonFirstOrder = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("DiffSecondReasonableInit")) scoreDiffCommonSecondOrder = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("DiffReasonablePerAtom")) scoreDiffCommonPerAtom = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("DiffReasonablePlusH2Init")) scoreDiffCommonPlusH2 = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("DiffReasonablePlusH2PerAtom")) scoreDiffCommonPlusH2PerAtom = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("InequalityInit")) scoreInequality = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("InequalityPerAtom")) scoreInequalityPerAtom = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("GapCnl")) scoreGapCnl = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("Gap")) scoreGap = Float.parseFloat(scoreParams[1]);
			else if (scoreParams[0].equalsIgnoreCase("Union")) scoreUnion = Float.parseFloat(scoreParams[1]);
			else System.err.println("Unrecognized scoring parameter " + scoreParams[0]);
		}
	}
}
