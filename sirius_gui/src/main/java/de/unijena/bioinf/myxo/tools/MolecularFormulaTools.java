package de.unijena.bioinf.myxo.tools;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.*;
import java.util.*;

@SuppressWarnings("unused")
public class MolecularFormulaTools {
	
	@SuppressWarnings("unused")
	public static TreeSet<String> readFormulaFile(File inFile){
		TreeSet<String> formulas = new TreeSet<String>();
		try(BufferedReader reader= FileUtils.ensureBuffering(new FileReader(inFile))){
			String temp = null;
			while((temp = reader.readLine()) != null){
				formulas.add(temp.trim());
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return formulas;
	}
	
	@SuppressWarnings("unused")
	public static Map<String,Integer> getElements(String ele){
		TreeMap<String,Integer> formulaElements = new TreeMap<String, Integer>();
		
		List<Integer> upperCasePositions = new ArrayList<Integer>(ele.length());
		for(int i=0;i<ele.length();i++){
			if(Character.isUpperCase(ele.charAt(i)))upperCasePositions.add(i);
		}
		upperCasePositions.add(ele.length());
		
		for(int i=0;i<upperCasePositions.size()-1;i++){
			String temp = ele.substring(upperCasePositions.get(i),upperCasePositions.get(i+1));
			
			if(temp.length()==1){
				formulaElements.put(temp,1);
			}else if(temp.length()==2 && Character.isLowerCase(temp.charAt(1))){
				formulaElements.put(temp,1);
			}else{
				int cutPos = 0;
				for(int j=0;j<temp.length();j++){
					if(Character.isDigit(temp.charAt(j))){
						cutPos=j;
						break;
					}
				}
				String keyVal = temp.substring(0,cutPos);
				String valVal = temp.substring(cutPos);
				
				formulaElements.put(keyVal,Integer.parseInt(valVal));
				
			}
			
		}
		
		return formulaElements;
	}
	
	/**
	 * Falls eines der Elemente in einer Formel vorhanden ist ...
	 * @param formulas
	 * @param elements
	 * @return
	 */
	@SuppressWarnings("unused")
	public List<String> getFormulasWithElements(Collection<String> formulas, String[] elements){
		List<String> backs = new ArrayList<String>();
		
		label1:
		for(String formula : formulas){
			
			Map<String,Integer> formulaElements = MolecularFormulaTools.getElements(formula);
			
			for(String cond : elements){
				
				if(formulaElements.containsKey(cond)){
//					if(formula.equals("C10H10AlF5O")) System.out.println("add...");
					backs.add(formula);
					continue label1;
				}
			}
		}
		
		return backs;
	}
	
	/**
	 * Nur falls diese Elemente in der Formel vorhanden sind ...
	 * @param formulas
	 * @param validElements
	 * @return
	 */
	@SuppressWarnings("unused")
	public static List<String> searchOnlyFor(Collection<String> formulas, String[] validElements){
		List<String> backFormuals = new ArrayList<String>();
		label1 :
		for(String formula : formulas){
			Map<String,Integer> elements = MolecularFormulaTools.getElements(formula);
//			label2 :
			for(String element : elements.keySet()){
				boolean val = false;
				for(String cond : validElements){
					if(element.equals(cond)){
						val = true;
						break;
					}
				}
				if(!val) continue label1;
			}
			backFormuals.add(formula);
		}
		return backFormuals;
	}
	
	/**
	 * Nur falls diese Elemente in der Formel vorhanden sind + obere Grenze
	 * @param formulas
	 * @param names
	 * @param amounts
	 * @return
	 */
	@SuppressWarnings("unused")
	public static List<String> searchOnlyForWithBounds(Collection<String> formulas, String[] names, int[] amounts){
		
		List<String> backs = new ArrayList<String>();
		
		label1:
		for(String formula : formulas){
			
			Map<String,Integer> formulaElements = MolecularFormulaTools.getElements(formula);
			
			label2:
			for(String element : formulaElements.keySet()){
				
				for(int i=0;i<names.length;i++){
					
					String elementId = names[i];
					int elementMaxAmount = amounts[i];
					
					if(element.equals(elementId)){
						int elementAmount = formulaElements.get(element);
						if(elementAmount>elementMaxAmount) continue label1;
						else continue label2;
					}
				}
				
				continue label1;
				
			}
			
			backs.add(formula);
		}
		
		return backs;
		
	}
	
	@SuppressWarnings("unused")
	public static void convertFormulasToMasses(File formulaFile, File massFile){
		
		try(BufferedReader reader = FileUtils.ensureBuffering(new FileReader(formulaFile)); BufferedWriter writer=new BufferedWriter(new FileWriter(massFile))){
			
			List<String> formulaList = new ArrayList<String>(1000);

			
			String temp=null;
			
			while((temp = reader.readLine()) != null){
				formulaList.add(temp.trim());
			}
			
			reader.close();
			
			for(String s : formulaList){
				MolecularFormula mf = MolecularFormula.parse(s);
				writer.write(mf.getMass()+" "+s+"\n");
			}
			
			writer.close();
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}