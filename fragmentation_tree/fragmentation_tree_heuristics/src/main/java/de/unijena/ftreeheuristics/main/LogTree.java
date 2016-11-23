package de.unijena.ftreeheuristics.main;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

public class LogTree {
	@XStreamAsAttribute
	private Double score;

	@XStreamAsAttribute
	private String formular;
	
	@XStreamAsAttribute
	private boolean isCorrect;
	
	public LogTree(Double score,String formular, boolean correct){
		this.score = score;
		this.formular = formular;
		this.isCorrect = correct;
	}
}
