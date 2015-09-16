package de.unijena.bioinf.sirius.gui.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.*;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElementConverter;

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
				zos.write(getJSONTree(sre.getTree()).getBytes());
				
//				FTDotWriter dotWriter = new FTDotWriter();
//				StringWriter sWriter = new StringWriter();
//				dotWriter.writeTree(sWriter, sre.getRawTree());
//				zos.write(sWriter.toString().getBytes());
//				sWriter.
				
//				IdentificationResult ir = new IdentificationResult(sre.getRawTree(), sre.getRank());
//				String json = ir.getJSONTree();
//				byte[] bytes = json.getBytes();
//				zos.write(bytes);
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
	
	private static String getJSONTree(TreeNode root){
		ArrayDeque<TreeNode> nDeque = new ArrayDeque<>();
//		StringBuilder sb = new StringBuilder();
		List<TreeEdge> edges = new ArrayList<>();
		nDeque.add(root);
		
		HashMap<Integer,TreeNode> nodeToID = new HashMap<>();
		HashMap<TreeNode, Integer> idToNode = new HashMap<>();
		int counter = 0;
		nodeToID.put(counter, root);
		idToNode.put(root, counter);
		counter++;
		
		StringWriter sw = new StringWriter();
		JSONWriter writer = new JSONWriter(sw);
		
		try{
			writer.object();
			writer.key("type");
			writer.value("nodes");
			writer.key("rootid");
			writer.value("0");
			
			writer.key("data");
			writer.array();	
			
			while(!nDeque.isEmpty()){
				TreeNode node = nDeque.remove();
				writer.object();
				writer.key("mf");
				writer.value(node.getMolecularFormula());
				writer.key("mfMass");
				writer.value(node.getMolecularFormulaMass());
				writer.key("peakMass");
				writer.value(node.getPeakMass());
				writer.key("peakInt");
				writer.value(node.getPeakAbsoluteIntensity());
				writer.key("peakRelInt");
				writer.value(node.getPeakRelativeIntensity());
				writer.key("score");
				writer.value(node.getScore());
				writer.key("id");
				writer.value(String.valueOf(nodeToID.get(node)));
				writer.endObject();
				for(TreeEdge edge : node.getOutEdges()){
					TreeNode target = edge.getTarget();
					nodeToID.put(counter, node);
					idToNode.put(node, counter);
					counter++;
					nDeque.add(target);
					
					edges.add(edge);
				}
			}
			writer.endArray();
			
			writer.endObject();
			
			//TODO Edges + ids funktionieren nichts
			
			
		}catch(JSONException e){
			System.err.println(e);
		}
		
		
		return sw.toString();
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
		for(SiriusResultElement  sre : ec.getResults()){
			sb.append(sre.getRank()+" "+sre.getScore()+" "+sre.getMolecularFormula());
		}
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
			sb.append(sp.getMass(i)+" "+sp.getAbsoluteIntensity(i)+" "+
		              sp.getResolution(i)+" "+sp.getSignalToNoise(i)+"\n");
		}
		return sb.toString();
	}

	@Override
	public ExperimentContainer load(File file) {
		
		List<CompactSpectrum> ms1List = new ArrayList<>();
		TreeMap<Integer, CompactSpectrum> ms2Map = new TreeMap<>();
		TreeMap<Integer, FTree> rankToTree = new TreeMap<>();
		
		HashMap<Integer,String> rankToMF = new HashMap<>();
		HashMap<Integer,Double> rankToScore = new HashMap<>();
		
		double dataFM = -1;
		double selFM = -1;
		String name = null;
		Ionization ion = null;
		
		try(ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(file)))){
			
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
	        		name = splits[0].trim() == "null" ? null : splits[0].trim();
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
	        		for(int i=4;i<splits.length;i++){
	        			String[] row = splits[i].split(" ");
	        			Integer rank = Integer.valueOf(row[0]);
	        			Double score = Double.valueOf(row[1]);
	        			String mf = row[2];
	        			rankToMF.put(rank, mf);
	        			rankToScore.put(rank, score);
	        		}
	        	}else if(zName.startsWith("Rank")){
	        		int rank = Integer.parseInt(zName.substring(5,zName.length()-5));
	        		StringReader stringReader = new StringReader(temp.toString());
	        		FTJsonReader jsonReader = new FTJsonReader();
	        		FTree tree = jsonReader.parse(new BufferedReader(stringReader));
	        		rankToTree.put(rank, tree);
//	        		IdentificationResult res = new IdentificationResult(tree, rank);
//	        		SiriusResultElement sre = SiriusResultElementConverter.convertResult(res);
//	        		resultsMap.put(rank, sre);
	        	}else if(zName.equals("ms1.txt")){
	        		BufferedReader re = new BufferedReader(new StringReader(temp.toString()));
	        		ms1List.add(readSpectrum(re));
	        	}else if(zName.startsWith("ms2")){
	        		BufferedReader re = new BufferedReader(new StringReader(temp.toString()));
	        		int index = Integer.parseInt(zName.substring(4,zName.length()-4));
	        		ms2Map.put(index, readSpectrum(re));
	        	}else{
	        		System.err.println("unknown element: "+zName);
	        	}
	        }
	        	 
	    }catch(IOException e){
	         throw new RuntimeException(e);
	    }
		
		ExperimentContainer ec = new ExperimentContainer();
		ec.setIonization(ion);
		ec.setName(name);
		ec.setDataFocusedMass(dataFM);
		ec.setSelectedFocusedMass(selFM);
		
		List<CompactSpectrum> ms2List = new ArrayList<>();
		List<SiriusResultElement> results = new ArrayList<>();
		for(Integer index : ms2Map.keySet()){
			ms2List.add(ms2Map.get(index));
		}
		for(Integer rank : rankToScore.keySet()){
			SiriusResultElement sre = new SiriusResultElement();
			sre.setMolecularFormula(MolecularFormula.parse(rankToMF.get(rank)));
			sre.setRank(rank);
			sre.setRawTree(rankToTree.get(rank));
			sre.setScore(rankToScore.get(rank));
			sre.setTree(SiriusResultElementConverter.convertTree(sre.getRawTree()));
			results.add(sre);
		}
		
		ec.setResults(results);
		ec.setMs1Spectra(ms1List);
		ec.setMs2Spectra(ms2List);
		return ec;
	}
	
	private CompactSpectrum readSpectrum(BufferedReader re) throws IOException{
		double minCE = Double.parseDouble(re.readLine());
		double maxCE = Double.parseDouble(re.readLine());
		int msLevel = Integer.parseInt(re.readLine());
		int size = Integer.parseInt(re.readLine());
		double[] masses = new double[size];
		double[] absInts = new double[size];
		double[] resolutions = new double[size];
		double[] snRatios = new double[size];
		for(int i=0;i<size;i++){
			String[] splits = re.readLine().split(" ");
			masses[i]  = Double.parseDouble(splits[0]);
			absInts[i] = Double.parseDouble(splits[1]);
			resolutions[i] = Double.parseDouble(splits[2]);
			snRatios[i] = Double.parseDouble(splits[3]);
		}
		DefaultCompactSpectrum sp = new DefaultCompactSpectrum(masses, absInts);
		sp.setMSLevel(msLevel);
		if(minCE>=0&&minCE>=0&&minCE<=maxCE){
			CollisionEnergy ce = new CollisionEnergy(minCE, maxCE);
			sp.setCollisionEnergy(ce);
		}
		sp.setResolutions(resolutions);
		sp.setSnRatios(snRatios);
		return sp;
	}

}
