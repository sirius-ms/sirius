package de.unijena.bioinf.sirius.gui.io;

import java.io.*;
import java.util.*;

import de.unijena.bioinf.myxo.io.spectrum.CSVNumberReader;
import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;

public class DataFormatIdentifier {

	private MS2FormatSpectraReader ms;
	private CSVNumberReader csv;
	
	public DataFormatIdentifier() {
		ms = new MS2FormatSpectraReader();
		csv = new CSVNumberReader();
	}
	
	public DataFormat identifyFormat(File f){
		if(ms.isCompatible(f)) return DataFormat.JenaMS;
		else if(csv.isCompatible(f)) return DataFormat.CSV;
		else if(f.getName().toLowerCase().endsWith(".mgf")) return DataFormat.MGF;
		else return DataFormat.NotSupported;
	}

}
