
package fragtreealigner.domainobjects.db;

import fragtreealigner.domainobjects.chem.components.NeutralLoss;
import fragtreealigner.domainobjects.graphs.FragmentationTree;
import fragtreealigner.domainobjects.graphs.FragmentationTreeEdge;
import fragtreealigner.util.Session;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class DatabaseStatistics implements Serializable {
	private Map<String, Integer> neutralLossCount;
	private List<String> neutralLossList;
	private int neutralLossTotalCount = 0;
	private int maxNeutralLossCount = 0;
	private int numberOfEntries;
	private Random randGen;
	private Session session;
	
	public DatabaseStatistics(Session session) {
		randGen = new Random();
		neutralLossCount = new HashMap<String, Integer>();
		neutralLossList = new ArrayList<String>();
		this.session = session;
	}
	
	public void addFragmentationTree(FragmentationTree fragTree) {
		NeutralLoss neutralLoss;
		String neutralLossStr;
		for (FragmentationTreeEdge fragTreeEdge : fragTree.getEdges()) {
			neutralLoss = fragTreeEdge.getNeutralLoss();
			neutralLossStr = neutralLoss.getMolecularFormula().toString();
			if (neutralLossCount.get(neutralLossStr) == null) neutralLossCount.put(neutralLossStr, new Integer(0));
			neutralLossCount.put(neutralLossStr, neutralLossCount.get(neutralLossStr) + 1);
			if (neutralLossCount.get(neutralLossStr) > maxNeutralLossCount) {
				maxNeutralLossCount = neutralLossCount.get(neutralLossStr);
			}
			neutralLossList.add(neutralLossStr);
			neutralLossTotalCount++;
		}
		numberOfEntries++;
	}

	public int getNeutralLossTotalCount() {
		return neutralLossTotalCount;
	}

	public int getNumberOfEntries() {
		return numberOfEntries;
	}
	
	public NeutralLoss getRandomNeutralLoss() {
		String neutralLossStr = neutralLossList.get(randGen.nextInt(neutralLossTotalCount));
		return new NeutralLoss(neutralLossStr, 0, neutralLossStr, session);
	}
	
	public List<NeutralLoss> getRandomNeutralLossList() {
		List<NeutralLoss> result=new ArrayList<NeutralLoss>();
		List<NeutralLoss> tmp=new ArrayList<NeutralLoss>();
		for(String neutralLossStr:neutralLossList){
			tmp.add(new NeutralLoss(neutralLossStr, 0, neutralLossStr, session));
		}
		for(int i=tmp.size()-1;i>=0;i--){
			int randomInt=randGen.nextInt(tmp.size());
			result.add(tmp.get(randomInt));
			tmp.remove(randomInt);
		}
		return result;
	}
	
	public int getNeutralLossCount(NeutralLoss neutralLoss) {
		if (neutralLoss == null) return 0;
		if (neutralLossCount.get(neutralLoss.getMolecularFormula().toString()) == null) return 0;
		else return (neutralLossCount.get(neutralLoss.getMolecularFormula().toString()) / 5);
	}
	
	public int getMaxNeutralLossCount() {
		return maxNeutralLossCount;
	}
	
	public void printStatistics() {
		TreeSet<String> neutralLossSet = new TreeSet<String>();
		neutralLossSet.addAll(neutralLossCount.keySet());
		
		for (String key : neutralLossSet) {
			System.out.println(key + "\t" + neutralLossCount.get(key) + "\t" + ((float)neutralLossCount.get(key) / neutralLossTotalCount));
		}
	}
}
