
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

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;


public class Sim2Dist {


	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		String compoundFile = "/home/m3rafl/data/tandem_ms_data/riken/negModeInfo.csv";
		//String compoundFile = "/home/m3rafl/data/tandem_ms_data/HalleQStarNew/info.csv";
		String input = "/home/m3rafl/data/tandem_ms_data/10x10compounds/alignScores/OrbiNewAAOldNorm.csv";
		//String output = "/home/m3rafl/data/tandem_ms_data/10x10compounds/alignScores/GreedyDynCutOffNewNormLipidsDist3+.csv";
		String output2 = "/home/m3rafl/data/tandem_ms_data/10x10compounds/alignScores/OrbiNewAAOldNorm2+.csv";
		int minimalSize = 2;
		
		BufferedReader br = FileUtils.ensureBuffering(new FileReader(input));
		BufferedReader cpdReader = FileUtils.ensureBuffering(new FileReader(compoundFile));
		//BufferedWriter out = new BufferedWriter(new FileWriter(output));
		BufferedWriter out2 = new BufferedWriter(new FileWriter(output2));
		
		Map<Integer, String> nameMap = new HashMap<Integer, String>();
		Map<String, String> classMap = new HashMap<String, String>();
		cpdReader.readLine();
		for (String line = cpdReader.readLine(); line != null; line = cpdReader.readLine()){
			String[] values = line.split(",");
			int cid = Integer.parseInt(values[4].trim());
			nameMap.put(cid, values[0].substring(1, values[0].length()-1).trim());
			classMap.put(values[0].substring(1, values[0].length()-1).trim(), values[1].substring(1, values[1].length()-1).trim());
//			nameMap.put(cid, values[0].trim());
//			classMap.put(values[0].trim(), values[1].trim());
		}
		//System.out.println(classMap);
		
		cpdReader.close(); 
		
		List<List<Double>> matrix = new ArrayList<List<Double>>(100);
		List<String> names = new ArrayList<String>(100);
		
		br.readLine();
		for (String line = br.readLine(); line != null; line = br.readLine()){
			String[] values = line.split(",");
			List<Double> row = new ArrayList<Double>(100);
			/*try {
				Double.parseDouble(values[1].trim());
			} catch (NumberFormatException e) {
				continue;
			}*/
			boolean name = false;
			for (String string : values) {
				string = string.trim();
				if (!name){
					name = true;
					names.add(string.substring(1, string.length()-5));
				} else {
					String[] dists=string.split(" ");
					// Filter for tree size
					if (Integer.parseInt(dists[2]) > minimalSize && Integer.parseInt(dists[3]) > minimalSize && !dists[0].equals("\"NaN")){
						row.add(Double.parseDouble(dists[0].substring(1)));						
					}
				}
			}
			if (!row.isEmpty()){
				matrix.add(row);
			} else {
				names.remove(names.size()-1);
			}
		}		
		br.close();
		
		for (int i = 0; i < matrix.size(); i++) {
			if (matrix.get(i).size() != matrix.size()){
				System.out.println(i+" "+matrix.get(i).size());
				System.out.println(names.get(i));
			}
		}
			System.out.println("Size: "+matrix.size());
		
		List<List<Double>> result = new ArrayList<List<Double>>(100);
		List<List<Double>> resultEuc = new ArrayList<List<Double>>(100);
		for (int i = 0; i < matrix.size(); i++) {
			List<Double> row = new ArrayList<Double>(matrix.get(i).size());
			List<Double> rowEuc = new ArrayList<Double>(matrix.get(i).size());
			for (int j = 0; j < matrix.get(i).size(); j++) {
				double dist = subtractionFromSelf(matrix, i, j);
				double eucDist = euclidian(matrix, i, j);
				row.add(dist);
				rowEuc.add(eucDist);
			}
			result.add(row);
			resultEuc.add(rowEuc);
		}
		
		Iterator<String> namesIter = names.iterator();
/*		for (List<Double> row : result){
			String name = namesIter.next();
			String group = classMap.get(name);
			if (group != null){
				out.write(group+"_");
			}
			out.write(name);
			for (Double value : row){
				out.write(", ");
				out.write(Double.toString(value));
			}
			out.newLine();
		}
		out.close();
*/
		namesIter = names.iterator();
		for (List<Double> row : resultEuc){
			String name = namesIter.next();
			String group = classMap.get(name);
			if (group != null){
				out2.write(group+"_");
			}
			out2.write(name);
			for (Double value : row){
				out2.write(", ");
				out2.write(Double.toString(value));
			}
			out2.newLine();
		}
		out2.close();
	}

	public static double subtractionFromSelf(List<List<Double>> matrix, int i, int j){
		if (matrix.get(i).get(i).isInfinite()){
			if (matrix.get(j).get(j).isInfinite()){
				return Double.POSITIVE_INFINITY;
			}
			return matrix.get(j).get(j)-matrix.get(i).get(j);
		}
		if (matrix.get(j).get(j).isInfinite()){
			return matrix.get(i).get(i)-matrix.get(i).get(j);
		}
		return (matrix.get(i).get(i) + matrix.get(j).get(j) - 2*matrix.get(i).get(j))/2;
	}
	
	public static double euclidian(List<List<Double>> matrix, int i, int j){
		double result = 0.0;
		for (int k=0; k<matrix.size(); ++k){
			result += Math.pow(matrix.get(i).get(k) - matrix.get(j).get(k), 2);
		}
		
		return Math.sqrt(result);
	}
	
	/**
	 * @param args
	 */
	public static void mainOld(String[] args) throws Exception{
		String input = "/home/m3rafl/data/tandem_ms_data/AdditionalInfos/AgilentGreedyScores.csv";
		String output = "/home/m3rafl/data/tandem_ms_data/AdditionalInfos/AgilentGreedyDist.csv";
		
		BufferedReader br = FileUtils.ensureBuffering(new FileReader(input));
		BufferedWriter out = new BufferedWriter(new FileWriter(output));
		
		List<List<Double>> matrix = new ArrayList<List<Double>>(100);
		List<String> names = new ArrayList<String>(100);
		
		for (String line = br.readLine(); line != null; line = br.readLine()){
			String[] values = line.split(",");
			List<Double> row = new ArrayList<Double>(100);
			try {
				Double.parseDouble(values[1].trim());
			} catch (NumberFormatException e) {
				continue;
			}
			boolean name = false;
			for (String string : values) {
				string = string.trim();
				if (!name){
					name = true;
					names.add(string);
				} else {
					row.add(Double.parseDouble(string));
				}
			}
			matrix.add(row);
		}
		
		br.close();
		
		List<List<Double>> result = new ArrayList<List<Double>>(100);
		for (int i = 0; i < matrix.size(); i++) {
			List<Double> row = new ArrayList<Double>(matrix.get(i).size());
			for (int j = 0; j < matrix.get(i).size(); j++) {
				double dist = matrix.get(i).get(i) + matrix.get(j).get(j) - 2*matrix.get(i).get(j);
				row.add(dist);
			}
			result.add(row);
		}
		
		Iterator<String> namesIter = names.iterator();
		for (List<Double> row : result){
			out.write(namesIter.next());
			for (Double value : row){
				out.write(", ");
				out.write(value.toString());
			}
			out.newLine();
		}
		out.close();

	}

}
