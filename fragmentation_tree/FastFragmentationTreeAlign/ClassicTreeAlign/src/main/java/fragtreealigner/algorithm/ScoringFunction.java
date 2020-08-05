
package fragtreealigner.algorithm;

import fragtreealigner.domainobjects.graphs.AlignmentTreeNode;
import fragtreealigner.util.Parameters;
import fragtreealigner.util.Session;

import java.io.Serializable;

@SuppressWarnings("serial")
abstract public class ScoringFunction implements Serializable {
	protected Session session;
	protected float scoreNullNull;
	protected float scoreUnion;
	
	public ScoringFunction() {
		initialize();
	}
	
	public ScoringFunction(Session session) {
		this.session = session;
		initialize();
	}
	
	public void initialize() {
		if (session == null) {
			scoreNullNull =  0;
			scoreUnion    = -9;
		} else {
			Parameters parameters = session.getParameters();
			scoreNullNull = parameters.scoreNullNull;
			scoreUnion    = parameters.scoreUnion;
		}
	}
	
	abstract public float score(AlignmentTreeNode node1, AlignmentTreeNode node2);
	abstract public float score(AlignmentTreeNode node1p1, AlignmentTreeNode node1p2, AlignmentTreeNode node2);
	
	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public float getScoreNullNull() {
		return scoreNullNull;
	}
	public void setScoreNullNull(float scoreNullNull) {
		this.scoreNullNull = scoreNullNull;
	}
	public float getScoreUnion() {
		return scoreUnion;
	}

	public void setScoreUnion(float scoreUnion) {
		this.scoreUnion = scoreUnion;
	}

}
