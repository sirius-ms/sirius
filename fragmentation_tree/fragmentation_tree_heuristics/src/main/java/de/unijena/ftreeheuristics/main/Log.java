package de.unijena.ftreeheuristics.main;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;

@XStreamAlias("log")
public class Log {

	@XStreamAsAttribute
	private String heuristic;
	@XStreamAsAttribute
	private String date;
	@XStreamAsAttribute
	private long ns;
	
	@XStreamImplicit(itemFieldName="tree")
	private ArrayList<LogTree> treeList;

	public Log(String method, String date, long ns){
		this.heuristic = method;
		this.date = date;
		this.ns = ns;
		this.treeList = new ArrayList<LogTree>();
	}

	public void addLogTree(LogTree lt){
		this.treeList.add(lt);
	}
	
	public String getMHeuristic() {
		return heuristic;
	}

	public String getDate() {
		return date;
	}

	public long getMs() {
		return ns;
	}
	
}
