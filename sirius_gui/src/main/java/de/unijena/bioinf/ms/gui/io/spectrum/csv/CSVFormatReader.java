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

package de.unijena.bioinf.ms.gui.io.spectrum.csv;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.gui.io.spectrum.SpectraReader;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CSVFormatReader implements SpectraReader {
	
	private Pattern pattern;

	@SuppressWarnings("unused")
	public CSVFormatReader() {
		pattern = Pattern.compile("[-+]?(([0-9]*\\.?[0-9]+)|([0-9]+\\.{1}))([eE][-+]?[0-9]+)?");
	}

	@Override
	public boolean isCompatible(File f) {
		int columnNumber = -1;
		try(BufferedReader reader = FileUtils.ensureBuffering(new FileReader(f))){
			String temp = null;
			int rowCounter=0; //lese max 5 Zeilen ein
			while((temp = reader.readLine()) != null){
				temp = temp.trim();
				if(temp.isEmpty()) continue;
				String[] splits = temp.split("(\\s|[;,])");
				int counter=0;
				for(String s : splits){
					if(s.isEmpty()){
						continue;
					}else{
						
						if(!pattern.matcher(s).matches()){
							return false;
						}
						counter++;
					}
				}
				if(counter<2){
					return false;
				}
				if(columnNumber==-1){
					columnNumber = counter;
				}else{
					if(columnNumber!=counter){
						return false;
					}
				}
				rowCounter++;
				if(rowCounter==5) return true;
			}
		}catch(IOException e){
			throw new RuntimeException(e);
		}
		return true;
	}
	
	@SuppressWarnings("unused")
	public List<TDoubleArrayList> readCSV(File f){
		List<TDoubleArrayList> data = new ArrayList<TDoubleArrayList>();
		try(BufferedReader reader = FileUtils.ensureBuffering(new FileReader(f))){
			String temp = null;
			while((temp = reader.readLine()) != null){
				temp = temp.trim();
				if(temp.isEmpty()) continue;
				String[] splits = temp.split("(\\s|[;,])");
				TDoubleArrayList row = new TDoubleArrayList();
				for(String s : splits){
					if(s.isEmpty()) continue;
					
				}
				for(String s : splits){
					if(s.isEmpty()){
						continue;
					}else{
						row.add(Double.parseDouble(s));
					}
				}
				data.add(row);
			}
		}catch(IOException e){
			throw new RuntimeException(e);
		}catch(NumberFormatException e2){
			throw new RuntimeException(e2);
		}
		int size = data.get(0).size();
		for(int i=1;i<data.size();i++){
			if(data.get(i).size()!=size){
				throw new RuntimeException("unequal row length");
			}
		}
		return data;
	}
	

}
