package de.unijena.bioinf.sirius.gui.io;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.DefaultTreeNode;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeEdge;
import de.unijena.bioinf.myxo.gui.tree.structure.TreeNode;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactSpectrum;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.json.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipExperimentIO implements ExperimentIO{

	public ZipExperimentIO() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void save(ExperimentContainer ec, File file){
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))){
			List<SiriusResultElement> sres = ec.getResults();
			for(SiriusResultElement sre : sres){
				ZipEntry ze = new ZipEntry("Rank_"+sre.getRank()+".json");
				zos.putNextEntry(ze);
				zos.write(getSREString(sre).getBytes());
			}
			List<CompactSpectrum> ms1 = ec.getMs1Spectra();
			if(ms1!=null&&!ms1.isEmpty()){
				ZipEntry ze = new ZipEntry("ms1.json");
				zos.putNextEntry(ze);
				String ms1S = getSpectrumString(ms1.get(0));
				byte[] ms1B = ms1S.getBytes();
				zos.write(ms1B);
			}
			List<CompactSpectrum> ms2 = ec.getMs2Spectra();
			if(ms2!=null){
				int counter=0;
				for(CompactSpectrum sp : ms2){
					ZipEntry ze = new ZipEntry("ms2_"+counter+".json");
					zos.putNextEntry(ze);
					String ms2S = getSpectrumString(sp);
					byte[] ms2B = ms2S.getBytes();
					zos.write(ms2B);
					counter++;
				}
			}
			ZipEntry ze = new ZipEntry("info.json");
			zos.putNextEntry(ze);
			String infoS = getInformations(ec);
			byte[] infoB = infoS.getBytes();
			zos.write(infoB);
			zos.close();
		}catch(IOException e){
			throw new RuntimeException(e);
		}
		
		
	}
	
	private static String getSREString(SiriusResultElement sre){
		ArrayDeque<TreeNode> nDeque = new ArrayDeque<>();
//		StringBuilder sb = new StringBuilder();
		TreeNode root = sre.getTree();
		List<TreeEdge> edges = new ArrayList<>();
		nDeque.add(root);
		
		HashMap<Integer,TreeNode> idToNode = new HashMap<>();
		HashMap<TreeNode, Integer> nodeToID = new HashMap<>();
		int counter = 0;
		idToNode.put(counter, root);
		nodeToID.put(root, counter);
		counter++;
		
		StringWriter sw = new StringWriter();
		JSONWriter writer = new JSONWriter(sw);
		
		try{
			
			//schreibe allgemein
			
			writer.object();
//			writer.key("type");
//			writer.value("nodes");
			writer.key("rootID");
			writer.value("0");
			writer.key("rank");
			writer.value(String.valueOf(sre.getRank()));
			writer.key("mf");
			writer.value(sre.getMolecularFormula());
			writer.key("score");
			writer.value(sre.getScore());
			
			//schreibe Nodes
			
			writer.key("nodes");
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
					idToNode.put(counter, target);
					nodeToID.put(target, counter);
					counter++;
					nDeque.add(target);
					
					edges.add(edge);
				}
			}
			writer.endArray();
			
			// schreibe edges
			
			writer.key("edges");
			writer.array();	
			
			for(TreeEdge edge : edges){
				writer.object();
				
				writer.key("sourceID");
				writer.value(String.valueOf(nodeToID.get(edge.getSource())));
				writer.key("targetID");
				writer.value(String.valueOf(nodeToID.get(edge.getTarget())));
				writer.key("mf");
				writer.value(edge.getLossFormula());
				writer.key("lossMass");
				writer.value(edge.getLossMass());
				writer.key("score");
				writer.value(edge.getScore());
				writer.endObject();
			}
			
			writer.endArray();
			writer.endObject();
			
			//TODO Edges + ids funktionieren nichts
			
			
		}catch(JSONException e){
			System.err.println("getSREString: ");
			e.printStackTrace();
		}
		
		
		return sw.toString();
	}
	
	private static String getInformations(ExperimentContainer ec){
		
		StringWriter sw = new StringWriter();
		JSONWriter writer = new JSONWriter(sw);
		
		Ionization ion = ec.getIonization();
		double dataFM = ec.getDataFocusedMass();
		double selFM  = ec.getSelectedFocusedMass();
		String ionS = null;
		if(ion==Ionization.M){
			ionS = "M";
		}else if(ion==Ionization.MPlusH){
			ionS = "MPlusH";
		}else if(ion==Ionization.MPlusNa){
			ionS = "MPlusNa";
		}else if(ion==Ionization.MMinusH){
			ionS = "MMinusH";
		}else if(ion==Ionization.Unknown){
			ionS = "Unknown";
		}
		
		try{
			writer.object();
			writer.key("name");
			writer.value(ec.getName()==null? "null" : ec.getName());
			writer.key("dataFM");
			writer.value(dataFM);
			writer.key("selFM");
			writer.value(selFM);
			writer.key("ionization");
			writer.value(ionS);
			writer.endObject();
		}catch(JSONException e){
			throw new RuntimeException(e);
		}
		
		return sw.toString();
		
	}
	
	private static String getSpectrumString(CompactSpectrum sp){
		
		StringWriter sWriter = new StringWriter();
		JSONWriter writer = new JSONWriter(sWriter);
		
		CollisionEnergy ce = sp.getCollisionEnergy();
		
		boolean resolutionPresent = false;
		for(int i=0;i<sp.getSize();i++){
			if(sp.getResolution(i)>0){
				resolutionPresent = true;
				break;
			}
		}
		
		boolean snPresent = false;
		for(int i=0;i<sp.getSize();i++){
			if(sp.getSignalToNoise(i)>0){
				snPresent = true;
				break;
			}
		}
		
		try{
			writer.object();
			writer.key("minCE");
			writer.value(ce==null?-1:ce.getMinEnergy());
			writer.key("maxCE");
			writer.value(ce==null?-1:ce.getMaxEnergy());
			writer.key("msLevel");
			writer.value(String.valueOf(sp.getMSLevel()));
			int size = sp.getSize();
			
			writer.key("masses");
			writer.array();
			for(int i=0;i<size;i++){
				writer.value(sp.getMass(i));
			}
			writer.endArray();
			
			writer.key("absInts");
			writer.array();
			for(int i=0;i<size;i++){
				writer.value(sp.getAbsoluteIntensity(i));
			}
			writer.endArray();
			
			if(resolutionPresent){
				writer.key("resolutions");
				writer.array();
				for(int i=0;i<size;i++){
					writer.value(sp.getResolution(i));
				}
				writer.endArray();
			}
			
			if(snPresent){
				writer.key("s/n");
				writer.array();
				for(int i=0;i<size;i++){
					writer.value(sp.getSignalToNoise(i));
				}
				writer.endArray();
			}
			
			writer.endObject();
		}catch(JSONException e){
			throw new RuntimeException(e);
		}
		
		return sWriter.toString();
		
	}

	@Override
	public ExperimentContainer load(File file) {
		
		List<CompactSpectrum> ms1List = new ArrayList<>();
		TreeMap<Integer, CompactSpectrum> ms2Map = new TreeMap<>();
		TreeMap<Integer, SiriusResultElement> rankToSRE = new TreeMap<>();
		
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
	        	
	        	if(zName.equals("info.json")){
	        		StringReader stringReader = new StringReader(temp.toString());
	        		JSONTokener tok = new JSONTokener(stringReader);
	        		try{
	        			JSONObject obj = new JSONObject(tok);
	        			name = obj.getString("name");
	        			if(name.equals("null")) name = null;
	        			dataFM = obj.getDouble("dataFM");
	        			selFM = obj.getDouble("selFM");
	        			String ionS = obj.getString("ionization");
	        			if(ionS.equals("M")){
		        			ion = Ionization.M;
		        		}else if(ionS.equals("MPlusH")){
		        			ion = Ionization.MPlusH;
		        		}else if(ionS.equals("MPlusNa")){
		        			ion = Ionization.MPlusNa;
		        		}else if(ionS.equals("MMinusH")){
		        			ion = Ionization.MMinusH;
		        		}else if(ionS.equals("Unknown")){
		        			ion = Ionization.Unknown;
		        		}
	        			
	        		}catch(JSONException e){
	        			throw new RuntimeException(e);
	        		}
	        	}else if(zName.startsWith("Rank")){
	        		int rank = Integer.parseInt(zName.substring(5,zName.length()-5));
	        		StringReader stringReader = new StringReader(temp.toString());
	        		SiriusResultElement sre = readSRE(stringReader);
	        		rankToSRE.put(rank,sre);
	        	}else if(zName.equals("ms1.json")){
	        		BufferedReader re = new BufferedReader(new StringReader(temp.toString()));
	        		ms1List.add(readSpectrum(re));
	        	}else if(zName.startsWith("ms2")){
	        		BufferedReader re = new BufferedReader(new StringReader(temp.toString()));
	        		int index = Integer.parseInt(zName.substring(4,zName.length()-5));
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
		for(Integer rank : rankToSRE.keySet()){
			results.add(rankToSRE.get(rank));
		}
		
		ec.setResults(results);
		ec.setMs1Spectra(ms1List);
		ec.setMs2Spectra(ms2List);
		return ec;
	}
	
	private SiriusResultElement readSRE(Reader re){
		
		SiriusResultElement sre = null;
		
		try{
			JSONTokener tok = new JSONTokener(re);
			JSONObject obj = new JSONObject(tok);
			
			HashMap<Integer, TreeNode> idToNode = new HashMap<>();
			int rank = Integer.parseInt(obj.getString("rank"));
			String mf = obj.getString("mf");
			double score = obj.getDouble("score");
			int rootID = Integer.parseInt(obj.getString("rootID"));
			
			JSONArray nodes = obj.getJSONArray("nodes");
			int noNodes = nodes.length();
			for(int i=0;i<noNodes;i++){
				JSONObject jNode = nodes.getJSONObject(i);
				String nodeMF = jNode.getString("mf");
				double nodeMFMass = jNode.getDouble("mfMass");
				double nodeMass = jNode.getDouble("peakMass");
				double nodeInt = jNode.getDouble("peakInt");
				double nodeRelInt = jNode.getDouble("peakRelInt");
				double nodeScore = jNode.getDouble("score");
				Integer nodeId = Integer.valueOf(jNode.getString("id"));
				DefaultTreeNode tn = new DefaultTreeNode();
				tn.setMolecularFormula(nodeMF);
				tn.setMolecularFormulaMass(nodeMFMass);
				tn.setPeakMass(nodeMass);
				tn.setPeakAbsoluteIntenstiy(nodeInt);
				tn.setPeakRelativeIntensity(nodeRelInt);
				tn.setScore(nodeScore);
				idToNode.put(nodeId,tn);
			}
			
			//Edges
			JSONArray edges = obj.getJSONArray("edges");
			int noEdges = edges.length();
			for(int i=0;i<noEdges;i++){
				JSONObject jEdge = edges.getJSONObject(i);
				Integer sourceID = Integer.valueOf(jEdge.getString("sourceID"));
				Integer targetID = Integer.valueOf(jEdge.getString("targetID"));
				String edgeMf = jEdge.getString("mf");
				double lossMass = jEdge.getDouble("lossMass");
				double edgeScore = jEdge.getDouble("score");
				DefaultTreeEdge te = new DefaultTreeEdge();
				TreeNode source = idToNode.get(sourceID);
				TreeNode target = idToNode.get(targetID);
				if(source==null) throw new RuntimeException("source null "+sourceID);
				if(target==null) throw new RuntimeException("target null "+targetID);
				te.setSource(source);
				te.setTarget(target);
				te.setLossFormula(edgeMf);
				te.setLossMass(lossMass);
				te.setScore(edgeScore);
				source.addOutEdge(te);
				target.setInEdge(te);
				
			}
			
			sre = new SiriusResultElement();
			sre.setRank(rank);
			sre.setMolecularFormula(MolecularFormula.parse(mf));
			sre.setScore(score);
			sre.setTree(idToNode.get(rootID));
			
		}catch(JSONException e){
			throw new RuntimeException(e);
		}
		
		return sre;
		
	}
	
	private CompactSpectrum readSpectrum(BufferedReader re) throws IOException{
		
		JSONTokener tok = new JSONTokener(re);
		
		DefaultCompactSpectrum sp = null;
		
		try{
			JSONObject obj = new JSONObject(tok);
			double minCE = obj.getDouble("minCE");
			double maxCE = obj.getDouble("maxCE");
			int msLevel = Integer.parseInt(obj.getString("msLevel"));
			
			boolean resPresent = obj.has("resolutions");
			boolean snPresent = obj.has("s/n");
			
			JSONArray jMasses = obj.getJSONArray("masses");
			JSONArray jAbsInts = obj.getJSONArray("absInts");
			
			int size = jMasses.length();
			
			double[] masses = new double[size];
			double[] absInts = new double[size];
			
			for(int i=0;i<size;i++){
				masses[i] = jMasses.getDouble(i);
				absInts[i] = jAbsInts.getDouble(i);
			}
			
			sp = new DefaultCompactSpectrum(masses, absInts);
			sp.setMSLevel(msLevel);
			
			if(resPresent){
				JSONArray jResolutions = obj.getJSONArray("resolutions");
				double[] resolutions = new double[size];
				for(int i=0;i<size;i++){
					resolutions[i] = jResolutions.getDouble(i);
				}
				sp.setResolutions(resolutions);
			}
			
			if(snPresent){
				JSONArray jSignalNoise = obj.getJSONArray("s/n");
				double[] signalNoise = new double[size];
				for(int i=0;i<size;i++){
					signalNoise[i] = jSignalNoise.getDouble(i);
				}
				sp.setSnRatios(signalNoise);
			}
			
			if(minCE>=0&&maxCE>=0&&minCE<=maxCE){
				sp.setCollisionEnergy(new CollisionEnergy(minCE, maxCE));
			}
			
		}catch(JSONException e){
			throw new RuntimeException(e);
		}
		
		return sp;
		
	}

}
