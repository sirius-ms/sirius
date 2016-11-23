package de.unijena.ftreeheuristics.main;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;

@XStreamAlias("logfile")
public class LogFile {
	
	@XStreamAsAttribute
	private String filename;
	
	@XStreamAsAttribute
	private String correctFormular;
	
	@XStreamImplicit(itemFieldName="log")
	private ArrayList<Log> logList;

	public LogFile(String filename, String formular){
		this.filename = filename;
		this.correctFormular = formular;
		this.logList = new ArrayList<Log>();
	}
	
	public void addLog(Log log){
		this.logList.add(log);
	}

	public String getFilename() {
		return filename;
	}

	public ArrayList<Log> getLogList() {
		return logList;
	}
	
}
