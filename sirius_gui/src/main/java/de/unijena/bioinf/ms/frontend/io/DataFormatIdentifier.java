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

package de.unijena.bioinf.ms.frontend.io;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.gui.io.spectrum.csv.CSVFormatReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DataFormatIdentifier {

	private CSVFormatReader csv;
	
	public DataFormatIdentifier() {
		csv = new CSVFormatReader();
	}
	
	public DataFormat identifyFormat(File f){
		if(f.getName().toLowerCase().endsWith(".ms")) return DataFormat.JenaMS;
		else if(csv.isCompatible(f)) return DataFormat.CSV;
		else if(f.getName().toLowerCase().endsWith(".mgf")) return DataFormat.MGF;
		else if (f.getName().toLowerCase().endsWith(".txt")) return DataFormat.CSV;
		else return DataFormat.NotSupported;
	}

}

class MGFCompatibilityValidator{
	public boolean isCompatible(File f){
		try(BufferedReader reader = FileUtils.ensureBuffering(new FileReader(f))){
			String temp = null;
			while((temp = reader.readLine()) != null){
				temp = temp.trim();
				if(temp.isEmpty()) continue;
                return temp.toUpperCase().equals("BEGIN IONS");
			}
		}catch(IOException e){
			return false;
		}
		return false;
	}
}
