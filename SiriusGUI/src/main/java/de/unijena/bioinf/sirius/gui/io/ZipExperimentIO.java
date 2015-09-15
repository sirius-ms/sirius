package de.unijena.bioinf.sirius.gui.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

public class ZipExperimentIO implements ExperimentIO{

	public ZipExperimentIO() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void save(ExperimentContainer ec, File file) {
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))){
			List<SiriusResultElement> sres = ec.getResults();
			for(SiriusResultElement sre : sres){
				ZipEntry ze = new ZipEntry("Rank_"+sre.getRank()+".json");
				zos.putNextEntry(ze);
				IdentificationResult ir = new IdentificationResult(sre.getRawTree(), sre.getRank());
				String json = ir.getJSONTree();
				byte[] bytes = json.getBytes();
				zos.write(bytes);
			}
			List<CompactSpectrum> ms1 = ec.getMs1Spectra();
			if(ms1!=null&&!ms1.isEmpty()){
				ZipEntry ze = new ZipEntry("ms1.txt");
				zos.putNextEntry(ze);
				String ms1S = getSpectrumString(ms1.get(0));
				byte[] ms1B = ms1S.getBytes();
				zos.write(ms1B);
			}
			List<CompactSpectrum> ms2 = ec.getMs2Spectra();
			if(ms2!=null){
				int counter=0;
				for(CompactSpectrum sp : ms2){
					ZipEntry ze = new ZipEntry("ms2_"+counter+".txt");
					zos.putNextEntry(ze);
					String ms2S = getSpectrumString(sp);
					byte[] ms2B = ms2S.getBytes();
					zos.write(ms2B);
					counter++;
				}
			}
			ZipEntry ze = new ZipEntry("info.txt");
			zos.putNextEntry(ze);
			String infoS = getInformations(ec);
			byte[] infoB = infoS.getBytes();
			zos.write(infoB);
			zos.close();
		}catch(IOException e){
			throw new RuntimeException(e);
		}
		
		
	}
	
	private static String getInformations(ExperimentContainer ec){
		StringBuilder sb = new StringBuilder();
		String name = ec.getName();
		Ionization ion = ec.getIonization();
		double dataFM = ec.getDataFocusedMass();
		double selFM  = ec.getSelectedFocusedMass();
		if(name!=null){
			sb.append(name+"\n");
		}else{
			sb.append("null\n");
		}
		if(ion==Ionization.M){
			sb.append("M\n");
		}else if(ion==Ionization.MPlusH){
			sb.append("MPlusH\n");
		}else if(ion==Ionization.MPlusNa){
			sb.append("MPlusNa\n");
		}else if(ion==Ionization.MMinusH){
			sb.append("MMinusH\n");
		}else if(ion==Ionization.Unknown){
			sb.append("Unknown\n");
		}
		sb.append(String.valueOf(dataFM)+"\n");
		sb.append(String.valueOf(selFM)+"\n");
		return sb.toString();
	}
	
	private static String getSpectrumString(CompactSpectrum sp){
		StringBuilder sb = new StringBuilder();
		CollisionEnergy ce = sp.getCollisionEnergy();
		if(ce!=null){
			sb.append(String.valueOf(ce.getMinEnergy())+"\n");
			sb.append(String.valueOf(ce.getMaxEnergy())+"\n");
		}else{
			sb.append("-1\n");
			sb.append("-1\n");
		}
		sb.append(String.valueOf(sp.getMSLevel())+"\n");
		sb.append(String.valueOf(sp.getSize())+"\n");
		for(int i=0;i<sp.getSize();i++){
			sb.append(sp.getMass(i)+" "+sp.getAbsoluteIntensity(i)+" "+sp.getRelativeIntensity(i)+" "+
		              sp.getResolution(i)+" "+sp.getSignalToNoise(i)+"\n");
		}
		return sb.toString();
	}

	@Override
	public void load(File file) {
		try(ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))){
			
			List<CompactSpectrum> ms1List = new ArrayList<>();
			List<CompactSpectrum> ms2List = new ArrayList<>();
			TreeMap<Integer, CompactSpectrum> ms2Set = new TreeMap<>();
			List<SiriusResultElement> results = new ArrayList<>();
			TreeMap<Integer, SiriusResultElement> resultsSet = new TreeMap<>();
			double dataFM = -1;
			double selFM = -1;
			String name = null;
			Ionization ion = null;

			ZipEntry entry;
	        while((entry = zis.getNextEntry()) != null) {
	        	String zName = entry.getName();
	        	ByteArrayOutputStream temp = new ByteArrayOutputStream();
	        	byte[] data = new byte[2048];
	        	int count;
	        	while((count = zis.read(data,0,2048)) != -1){
	        		temp.write(data,0,count);
	        	}
	        	
	        	if(zName.equals("info.txt")){
	        		String[] splits = temp.toString().split("\n");
	        		name = splits[0] == "null" ? null : splits[0];
	        		if(splits[1].equals("M")){
	        			ion = Ionization.M;
	        		}else if(splits[1].equals("MPlusH")){
	        			ion = Ionization.MPlusH;
	        		}else if(splits[1].equals("MPlusNa")){
	        			ion = Ionization.MPlusNa;
	        		}else if(splits[1].equals("MMinusH")){
	        			ion = Ionization.MMinusH;
	        		}else if(splits[1].equals("Unknown")){
	        			ion = Ionization.Unknown;
	        		}
	        		dataFM = Double.parseDouble(splits[2]);
	        		selFM = Double.parseDouble(splits[3]);
	        	}else if(zName.startsWith("Rank")){
	        		int rank = Integer.parseInt(zName.substring(5,zName.length()-5));
	        		//TODO parsen und in Tree einfuegen
	        	}
	        	
	        	//TODO MS1, MS2...
	        	
	        	
	        	System.out.println(name);
	        	System.out.println("--------------------");
	        	System.out.println(temp.toString());
	        }
	        	 
	    }catch(IOException e){
	         throw new RuntimeException(e);
	    }
	}

}
