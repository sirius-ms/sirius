package de.unijena.bioinf.sirius.gui.io;

import java.io.*;
import java.util.*;

import com.sun.tools.xjc.api.util.FilerCodeWriter;

import de.unijena.bioinf.myxo.io.spectrum.CSVFormatReader;
import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;

public class DataFormatIdentifier {

	private MS2FormatSpectraReader ms;
	private CSVFormatReader csv;
	private MGFCompatibilityValidator mgf;
	
	public DataFormatIdentifier() {
		ms = new MS2FormatSpectraReader();
		csv = new CSVFormatReader();
		mgf = new MGFCompatibilityValidator();
	}
	
	public DataFormat identifyFormat(File f){
//		if(f.getName().toLowerCase().endsWith(".ms")) return DataFormat.JenaMS;
		if(ms.isCompatible(f)) return DataFormat.JenaMS;
		else if(csv.isCompatible(f)) return DataFormat.CSV;
		else if(mgf.isCompatible(f)) return DataFormat.MGF;
		else return DataFormat.NotSupported;
	}

}

class MGFCompatibilityValidator{
	public boolean isCompatible(File f){
		try(BufferedReader reader = new BufferedReader(new FileReader(f))){
			String temp = null;
			while((temp = reader.readLine()) != null){
				temp = temp.trim();
				if(temp.isEmpty()) continue;
				if(temp.toUpperCase().equals("BEGIN IONS")){
					return true;
				}else{
					return false;
				}
			}
		}catch(IOException e){
			return false;
		}
		return false;
	}
}
