
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

package fragtreealigner.domainobjects.chem.basics;

import fragtreealigner.domainobjects.chem.components.FunctionalGroup;
import fragtreealigner.domainobjects.chem.components.FunctionalGroupSubstitution;
import fragtreealigner.domainobjects.chem.components.NeutralLoss;
import fragtreealigner.util.Session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class ChemInfo implements Serializable {
	private NeutralLoss[] commonNeutralLosses;
	private FunctionalGroup[] commonFunctionalGroups;
	private int[][] fgSubstitutionMatrix;
	private HashMap<String, Integer> commonNeutralLossesHash;
	private HashMap<String, List<FunctionalGroupSubstitution>> commonMolFormulaDiffsHashSecondOrder;
	private HashMap<String, List<FunctionalGroupSubstitution>> commonMolFormulaDiffsHashFirstOrder;
//	private MolecularFormula[] commonMolFormulaDiffs;
//	private MolecularFormula emptyMolFormula;
//	private HashMap<String, Integer> commonMolFormulaDiffsHash2;
	private Session session;
	
	public enum neutralLossDiff { FUNCTIONAL_GROUP_PLUS_H2, H2, NOT_COMMON, SIMILAR_NEUTRALLOSS_MASS, FUNCTIONAL_GROUP_FIRST_ORDER, FUNCTIONAL_GROUP_SECOND_ORDER };

	public ChemInfo(Session session) {
		this.session = session;

		commonNeutralLosses = new NeutralLoss[44];
		
		/*commonNeutralLosses[0] = new NeutralLoss("CH3", -1, "CH3", false, session);
		commonNeutralLosses[1] = new NeutralLoss("CH4", -1, "CH4", false, session);
		commonNeutralLosses[2] = new NeutralLoss("O", -1, "H2O", false, session);
		commonNeutralLosses[3] = new NeutralLoss("H2O", -1, "H2O", false, session);
		commonNeutralLosses[4] = new NeutralLoss("CO", -1, "CO", false, session);
		commonNeutralLosses[5] = new NeutralLoss("N2", -1, "N2", false, session);
		commonNeutralLosses[6] = new NeutralLoss("C2H4", -1, "C2H4", false, session);
		commonNeutralLosses[7] = new NeutralLoss("CH2O", -1, "CH2O", false, session);
		commonNeutralLosses[8] = new NeutralLoss("C4H8", -1, "C4H8", false, session);
		commonNeutralLosses[9] = new NeutralLoss("C5H8", -1, "C5H8", false, session);
		commonNeutralLosses[10] = new NeutralLoss("CH2O2", -1, "CH2O2", false, session);
		commonNeutralLosses[11] = new NeutralLoss("C3H2O3", -1, "C3H2O3", false, session);
		commonNeutralLosses[12] = new NeutralLoss("C3H9N", -1, "C3H9N", false, session);
		commonNeutralLosses[13] = new NeutralLoss("C5H8O4", -1, "C5H8O4", false, session);
		commonNeutralLosses[14] = new NeutralLoss("C6H10O4", -1, "C6H10O4", false, session);
		commonNeutralLosses[15] = new NeutralLoss("C6H10O5", -1, "C6H10O5", false, session);
		commonNeutralLosses[16] = new NeutralLoss("C6H8O6", -1, "C6H8O6", false, session);*/
		commonNeutralLosses[0] = new NeutralLoss("O", -1, session);
		commonNeutralLosses[1] = new NeutralLoss("C2H2", -1, session);
		commonNeutralLosses[2] = new NeutralLoss("C2H4", -1, session);
		commonNeutralLosses[3] = new NeutralLoss("C2H4O2", -1, session);
		commonNeutralLosses[4] = new NeutralLoss("C2H5O4P", -1, session);
		commonNeutralLosses[5] = new NeutralLoss("C3H2O3", -1, session);
		commonNeutralLosses[6] = new NeutralLoss("C3H4O4", -1, session);
		commonNeutralLosses[7] = new NeutralLoss("C3H6O2", -1, session);
		commonNeutralLosses[8] = new NeutralLoss("C3H9N", -1, session);
		commonNeutralLosses[9] = new NeutralLoss("C4H8", -1, session);
		commonNeutralLosses[10] = new NeutralLoss("C5H8", -1, session);
		commonNeutralLosses[11] = new NeutralLoss("C5H8O4", -1, session);
		commonNeutralLosses[12] = new NeutralLoss("C6H10O4", -1, session);
		commonNeutralLosses[13] = new NeutralLoss("C6H10O5", -1, session);
		commonNeutralLosses[14] = new NeutralLoss("C6H6", -1, session);
		commonNeutralLosses[15] = new NeutralLoss("C6H8O6", -1, session);
		commonNeutralLosses[16] = new NeutralLoss("CH2O", -1, session);
		commonNeutralLosses[17] = new NeutralLoss("CH2O2", -1, session);
		commonNeutralLosses[18] = new NeutralLoss("CH3", -1, session);
		commonNeutralLosses[19] = new NeutralLoss("CH3N", -1, session);
		commonNeutralLosses[20] = new NeutralLoss("CH4", -1, session);
		commonNeutralLosses[21] = new NeutralLoss("CH4N2O", -1, session);
		commonNeutralLosses[22] = new NeutralLoss("CH4O", -1, session);
		commonNeutralLosses[23] = new NeutralLoss("CH5N", -1, session);
		commonNeutralLosses[24] = new NeutralLoss("CHNO", -1, session);
		commonNeutralLosses[25] = new NeutralLoss("CO", -1, session);
		commonNeutralLosses[26] = new NeutralLoss("CO2", -1, session);
		commonNeutralLosses[27] = new NeutralLoss("H2", -1, session);
		commonNeutralLosses[28] = new NeutralLoss("H2O", -1, session);
		commonNeutralLosses[29] = new NeutralLoss("H2S", -1, session);
		commonNeutralLosses[30] = new NeutralLoss("H2SO4", -1, session);
		commonNeutralLosses[31] = new NeutralLoss("HPO3", -1, session);
		commonNeutralLosses[32] = new NeutralLoss("N2", -1, session);
		commonNeutralLosses[33] = new NeutralLoss("NH3", -1, session);
		commonNeutralLosses[34] = new NeutralLoss("S", -1, session);
		commonNeutralLosses[35] = new NeutralLoss("SO2", -1, session);
		commonNeutralLosses[36] = new NeutralLoss("SO3", -1, session);
		commonNeutralLosses[37] = new NeutralLoss("H", -1, session);
		commonNeutralLosses[38] = new NeutralLoss("OH", -1, session);
		commonNeutralLosses[39] = new NeutralLoss("CH3", -1, session);
		commonNeutralLosses[40] = new NeutralLoss("CH3O", -1, session);
		commonNeutralLosses[41] = new NeutralLoss("C3H7", -1, session);
		commonNeutralLosses[42] = new NeutralLoss("C4H9", -1, session);
		commonNeutralLosses[43] = new NeutralLoss("C6H5O", -1, session);

//		emptyMolFormula = new MolecularFormula(new int[]{0,0,0,0,0,0}, session);
//		commonMolFormulaDiffs = new MolecularFormula[5];
//		commonMolFormulaDiffs[0] = new MolecularFormula(new int[]{0,0,0,1,0,0}, session);	// O
//		commonMolFormulaDiffs[1] = new MolecularFormula(new int[]{0,1,1,0,0,0}, session);	// NH
//		commonMolFormulaDiffs[2] = new MolecularFormula(new int[]{1,2,0,0,0,0}, session);	// CH2 ???
//		commonMolFormulaDiffs[3] = new MolecularFormula(new int[]{0,-2,0,1,0,0}, session);	// -H2+O
//		commonMolFormulaDiffs[4] = new MolecularFormula(new int[]{0,-2,0,2,0,0}, session);	// -H2+O2
		
//		commonFunctionalGroups = new FunctionalGroup[8];
//		commonFunctionalGroups[0] = new FunctionalGroup("", -1, "H", 1, session);		// -H
//		commonFunctionalGroups[1] = new FunctionalGroup("", -1, "HO", 1, session);		// -OH
//		commonFunctionalGroups[2] = new FunctionalGroup("", -1, "H2N", 1, session);		// -NH2
//		commonFunctionalGroups[3] = new FunctionalGroup("", -1, "CH3", 1, session);		// -CH3
//		commonFunctionalGroups[4] = new FunctionalGroup("", -1, "CH3O", 2, session);	// -CH2-OH
//		commonFunctionalGroups[5] = new FunctionalGroup("", -1, "CH4N", 2, session);	// -CH2-NH2
//		commonFunctionalGroups[6] = new FunctionalGroup("", -1, "CHO", 2, session);		// -CHO
//		commonFunctionalGroups[7] = new FunctionalGroup("", -1, "CHO2", 2, session);	// -COOH
//
//		fgSubstitutionMatrix = new int[commonFunctionalGroups.length][];
//		fgSubstitutionMatrix[0] = new int[]{0, 1, 1, 0, 0, 0, 0, 0}; 
//		fgSubstitutionMatrix[1] = new int[]{1, 0, 1, 0, 0, 0, 0, 0}; 
//		fgSubstitutionMatrix[2] = new int[]{1, 1, 0, 0, 0, 0, 0, 0}; 
//		fgSubstitutionMatrix[3] = new int[]{0, 0, 0, 0, 1, 1, 1, 1}; 
//		fgSubstitutionMatrix[4] = new int[]{0, 0, 0, 1, 0, 1, 1, 1}; 
//		fgSubstitutionMatrix[5] = new int[]{0, 0, 0, 1, 1, 0, 1, 1}; 
//		fgSubstitutionMatrix[6] = new int[]{0, 0, 0, 1, 1, 1, 0, 1}; 
//		fgSubstitutionMatrix[7] = new int[]{0, 0, 0, 1, 1, 1, 1, 0}; 	
		
		commonFunctionalGroups = new FunctionalGroup[13];
		commonFunctionalGroups[0] = new FunctionalGroup("", -1, "H2", 1, session);
		commonFunctionalGroups[0] = new FunctionalGroup("", -1, "H4", 1, session);
		commonFunctionalGroups[1] = new FunctionalGroup("", -1, "CH2", 1, session);
		commonFunctionalGroups[1] = new FunctionalGroup("", -1, "C2H4", 1, session);
		commonFunctionalGroups[1] = new FunctionalGroup("", -1, "C2H2", 1, session);
		commonFunctionalGroups[2] = new FunctionalGroup("", -1, "O", 1, session);
		commonFunctionalGroups[3] = new FunctionalGroup("", -1, "H2O", 1, session);
		commonFunctionalGroups[4] = new FunctionalGroup("", -1, "NH", 1, session);
		commonFunctionalGroups[5] = new FunctionalGroup("", -1, "S", 1, session);
		commonFunctionalGroups[6] = new FunctionalGroup("", -1, "ClH", 1, session);
		commonFunctionalGroups[7] = new FunctionalGroup("", -1, "BrH", 1, session);
		commonFunctionalGroups[7] = new FunctionalGroup("", -1, "IH", 1, session);
		commonFunctionalGroups[8] = new FunctionalGroup("", -1, "FH", 1, session);
		commonFunctionalGroups[9] = new FunctionalGroup("", -1, "CO2", 1, session);
		commonFunctionalGroups[10] = new FunctionalGroup("", -1, "SO2", 1, session);
		commonFunctionalGroups[11] = new FunctionalGroup("", -1, "SO3", 1, session);
		commonFunctionalGroups[12] = new FunctionalGroup("", -1, "HPO3", 1, session);
		
//		fgSubstitutionMatrix = new int[commonFunctionalGroups.length][];
//		fgSubstitutionMatrix[0] = new int[]{0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[1] = new int[]{1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[2] = new int[]{1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[3] = new int[]{1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[4] = new int[]{1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[5] = new int[]{1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[6] = new int[]{1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[7] = new int[]{1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1};
//		fgSubstitutionMatrix[8] = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1};
//		fgSubstitutionMatrix[9] = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1};
//		fgSubstitutionMatrix[10] = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1};
//		fgSubstitutionMatrix[11] = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1};
//		fgSubstitutionMatrix[12] = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0};
		
		buildCommonNeutralLossesHash();
		buildCommonMolFormulaDiffsHash();
		return;
	}
	
	public void setCommonNeutralLosses(NeutralLoss[] commonNeutralLosses) {
		this.commonNeutralLosses = commonNeutralLosses;
	}

	public NeutralLoss[] getCommonNeutralLosses() {
		return commonNeutralLosses;
	}
	
	public void buildCommonNeutralLossesHash() {
		commonNeutralLossesHash = new HashMap<String, Integer>();
		for (NeutralLoss commonNeutralLoss: commonNeutralLosses) {
			commonNeutralLossesHash.put(commonNeutralLoss.getMolecularFormula().toString(), 1);
		}
	}

	public void buildCommonMolFormulaDiffsHash() {
//		commonMolFormulaDiffsHash2 = new HashMap<String, Integer>();
//		for (MolecularFormula commonMolFormulaDiff: commonMolFormulaDiffs) {
//			commonMolFormulaDiffsHash2.put(commonMolFormulaDiff.toString(), 1) ;
//			commonMolFormulaDiffsHash2.put(emptyMolFormula.diff(commonMolFormulaDiff).toString(), 1);
//		}
//		for (MolecularFormula commonMolFormulaDiff1: commonMolFormulaDiffs) {
//			for (MolecularFormula commonMolFormulaDiff2: commonMolFormulaDiffs) {
//				if (commonMolFormulaDiff1.equals(commonMolFormulaDiff2)) continue;
//				commonMolFormulaDiffsHash2.put(commonMolFormulaDiff1.diff(commonMolFormulaDiff2).toString(), 1);
//			}
//		}
		
		MolecularFormula molFormulaDiff;
		String molFormulaDiffStr;
		FunctionalGroupSubstitution fgSubstitution;
		commonMolFormulaDiffsHashSecondOrder = new HashMap<String, List<FunctionalGroupSubstitution>>();
		commonMolFormulaDiffsHashFirstOrder = new HashMap<String, List<FunctionalGroupSubstitution>>();
		int i = -1, j = -1;
		for (FunctionalGroup commonFunctionalGroup1: commonFunctionalGroups) {
			FunctionalGroup emptyFunctionalGroup=new FunctionalGroup("", -1, "", 1, session);
			String molFormulaDiff1 = commonFunctionalGroup1.getMolecularFormula().diff(emptyFunctionalGroup.getMolecularFormula()).toString();
			String molFormulaDiff2 = emptyFunctionalGroup.getMolecularFormula().diff(commonFunctionalGroup1.getMolecularFormula()).toString();
			addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashFirstOrder, molFormulaDiff1, new FunctionalGroupSubstitution(commonFunctionalGroup1, emptyFunctionalGroup, false, false));			
			addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashFirstOrder, molFormulaDiff2, new FunctionalGroupSubstitution(emptyFunctionalGroup, commonFunctionalGroup1, false, false));
			addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashFirstOrder, molFormulaDiff1, new FunctionalGroupSubstitution(commonFunctionalGroup1, emptyFunctionalGroup, true, true));			
			addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashFirstOrder, molFormulaDiff2, new FunctionalGroupSubstitution(emptyFunctionalGroup, commonFunctionalGroup1, true, true));
			i++;
			j = -1;
			for (FunctionalGroup commonFunctionalGroup2: commonFunctionalGroups) {
				j++;
				//if (fgSubstitutionMatrix[i][j] == 0) continue;
				molFormulaDiff = commonFunctionalGroup1.getMolecularFormula().diff(commonFunctionalGroup2.getMolecularFormula());
				molFormulaDiffStr = molFormulaDiff.toString();

				fgSubstitution = new FunctionalGroupSubstitution(commonFunctionalGroup1, commonFunctionalGroup2, false, false);
				addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashSecondOrder, molFormulaDiffStr, fgSubstitution);
				fgSubstitution = new FunctionalGroupSubstitution(commonFunctionalGroup1, commonFunctionalGroup2, true, true);
				addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashSecondOrder, molFormulaDiffStr, fgSubstitution);

				if(session.getParameters().testRDiffH2){
					molFormulaDiff.setNumberOfAtom("H", molFormulaDiff.getNumberOfAtom("H") - 2);
					molFormulaDiffStr = molFormulaDiff.toString();
					fgSubstitution = new FunctionalGroupSubstitution(commonFunctionalGroup1, commonFunctionalGroup2, false, true);
					addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashSecondOrder, molFormulaDiffStr, fgSubstitution);
					
					molFormulaDiff.setNumberOfAtom("H", molFormulaDiff.getNumberOfAtom("H") + 4);
					molFormulaDiffStr = molFormulaDiff.toString();
					fgSubstitution = new FunctionalGroupSubstitution(commonFunctionalGroup1, commonFunctionalGroup2, true, false);
					addFunctionalGroupSubstitutionToHash(commonMolFormulaDiffsHashSecondOrder, molFormulaDiffStr, fgSubstitution);
				}
			}
		}
		
		
	}
	
	private void addFunctionalGroupSubstitutionToHash(HashMap<String, List<FunctionalGroupSubstitution>> commonMolFormulaDiffsHash, String molFormulaDiffStr, FunctionalGroupSubstitution fgSubstitution) {
		if (!commonMolFormulaDiffsHash.containsKey(molFormulaDiffStr)) {
			commonMolFormulaDiffsHash.put(molFormulaDiffStr, new LinkedList<FunctionalGroupSubstitution>());
		}
		commonMolFormulaDiffsHash.get(molFormulaDiffStr).add(fgSubstitution);
	}

	public boolean lossIsCommonNeutralLoss(NeutralLoss neutralLoss) {
		if (session.getParameters().useCnl) return commonNeutralLossesHash.containsKey(neutralLoss.getMolecularFormula().toString());
		else return false;
	}
	
	public neutralLossDiff determineNeutralLossDiff(NeutralLoss neutralLoss1, NeutralLoss neutralLoss2) {
		MolecularFormula molFormula = neutralLoss1.diff(neutralLoss2);
		String molFormulaStr = molFormula.toString();
		int res = 0;
		boolean doubleBondDiff = false;
		int fgState1, fgState2;
		if(session.getParameters().testSimilarNLMass){
			double delta=Math.abs((neutralLoss1.getMass()+neutralLoss2.getMass())*1e-6*session.getParameters().ppm_error);
			double sigma=Math.abs(neutralLoss1.getMass()-neutralLoss2.getMass());
			if (sigma<delta){
				return neutralLossDiff.SIMILAR_NEUTRALLOSS_MASS;
			}
		}		
		if (molFormulaStr.equals("H2") || molFormulaStr.equals("H-2")){
			if (session.getParameters().testH2) {
				return neutralLossDiff.H2;
			}
//			else {
//				return neutralLossDiff.NOT_COMMON;
//			}
		}
		if (commonMolFormulaDiffsHashFirstOrder.containsKey(molFormulaStr)) {
			for (FunctionalGroupSubstitution fgSubstitution: commonMolFormulaDiffsHashFirstOrder.get(molFormulaStr)) {
				fgState1 = neutralLoss1.hasFunctionalGroup(fgSubstitution.getFunctionalGroupBeforeAsString());
				fgState2 = neutralLoss2.hasFunctionalGroup(fgSubstitution.getFunctionalGroupAfterAsString());
				if(fgState1>0&&fgState2>0) {
					res=1;
					break;
				}
			}			
		}else if (commonMolFormulaDiffsHashSecondOrder.containsKey(molFormulaStr)) {
			for (FunctionalGroupSubstitution fgSubstitution: commonMolFormulaDiffsHashSecondOrder.get(molFormulaStr)) {
				fgState1 = neutralLoss1.hasFunctionalGroup(fgSubstitution.getFunctionalGroupBeforeAsString());
				fgState2 = neutralLoss2.hasFunctionalGroup(fgSubstitution.getFunctionalGroupAfterAsString());
				if(fgState1>0&&fgState2>0) {
					res=2;
					break;
				}				
//				if (fgState1 == 0 || fgState2 == 0) continue;
//				if ((((fgState1 & 1) == 1 && !fgSubstitution.isDoubleBondBefore()) || ((fgState1 & 2) == 2 && fgSubstitution.isDoubleBondBefore())) &&
//						(((fgState2 & 1) == 1 && !fgSubstitution.isDoubleBondAfter()) || ((fgState2 & 2) == 2 && fgSubstitution.isDoubleBondAfter()))) {
//					res = true;
//					if (fgSubstitution.isDoubleBondBefore() ^ fgSubstitution.isDoubleBondAfter()) doubleBondDiff = true;
//				}
//				if (res) break;
			}
		}
		else res = 0;
		if (res==0) return neutralLossDiff.NOT_COMMON;
		if (doubleBondDiff) {
			if (session.getParameters().testRDiffH2) return neutralLossDiff.FUNCTIONAL_GROUP_PLUS_H2;
			else return neutralLossDiff.NOT_COMMON;
		} else {
			if (session.getParameters().testRDiff){
				if(res==1)return neutralLossDiff.FUNCTIONAL_GROUP_FIRST_ORDER;
				if(res==2)return neutralLossDiff.FUNCTIONAL_GROUP_SECOND_ORDER;
			}
		}
		return neutralLossDiff.NOT_COMMON;
	}

	public HashMap<String, Integer> determineFunctionalGroups(NeutralLoss neutralLoss) {
		int res = 0;
		HashMap<String, Integer> functionalGroups = new HashMap<String, Integer>();
		for (FunctionalGroup commonFunctionalGroup : commonFunctionalGroups) {
			res = this.checkMainPartOfNeutralLoss(neutralLoss.getMolecularFormula().diff(commonFunctionalGroup.getMolecularFormula()));
			functionalGroups.put(commonFunctionalGroup.getMolecularFormula().toString(), res);
		}
		return functionalGroups;
	}
	
	private int checkMainPartOfNeutralLoss(MolecularFormula molFormula) {
		HashMap<String, Integer> numberOfAtomsHash = molFormula.getNumberOfAtomsAsHash();
		for (String elementSymbol: numberOfAtomsHash.keySet()) {
			int quantity = numberOfAtomsHash.get(elementSymbol);
			if(quantity<0) return 0;
		}
		return 1;
		
//		boolean emptyFormula = true;
//		
//		ElementTable elementTable = session.getParameters().elementTable;
//		Integer quantity;
//		HashMap<String, Integer> numberOfAtomsHash = molFormula.getNumberOfAtomsAsHash();
//		for (String elementSymbol: numberOfAtomsHash.keySet()) {
//			quantity = numberOfAtomsHash.get(elementSymbol);
//			if (elementSymbol.equals("H")) {
//				if (quantity < -1) return 0;
//			} 
//			else if (quantity < 0) return 0;
//			else if (quantity > 0) emptyFormula = false;
//		}
//		
//		if (emptyFormula && numberOfAtomsHash.containsKey("H")) {
//			if (numberOfAtomsHash.get("H") == -1) return 1;
//			if (numberOfAtomsHash.get("H") == 1) return 2;
//		}
//		
//		if (molFormula.size() > 4) {
//			if (numberOfAtomsHash.containsKey("H") && numberOfAtomsHash.get("H") > 0) return 3;
//			else return 1;
//		}
//
//		int[] numberOfBondingElectrons = elementTable.getNumberOfBondingElectrons();		
//		int[] numberOfAtomsList = molFormula.getNumberOfAtomsAsList();
//		int indexOfH = elementTable.getElementList().indexOf("H");
//		int k, numberOfFreeBondingElectrons;
//		numberOfAtomsList[indexOfH]++;	
//
//		int indexOfHydrogenDeficiency = 0, res = 0;
//		for (k = 0; k < elementTable.getNumberOfElements(); k++) {
//			numberOfFreeBondingElectrons = numberOfBondingElectrons[k] - 2;
//			indexOfHydrogenDeficiency += numberOfAtomsList[k] * numberOfFreeBondingElectrons;
//		}
//		if (indexOfHydrogenDeficiency % 2 == 0) {
//			if (indexOfHydrogenDeficiency == 0) res += 1;
//			if (indexOfHydrogenDeficiency > 0 && molFormula.size() > 1) res += 1;
//			if ((numberOfAtomsList[indexOfH] >= 2 ) && (indexOfHydrogenDeficiency) == -2) res += 2;
//			if ((numberOfAtomsList[indexOfH] >= 2 ) && (indexOfHydrogenDeficiency) > -2 && molFormula.size() > 1) res += 2;			
//		}
//		return res;
	}
}
