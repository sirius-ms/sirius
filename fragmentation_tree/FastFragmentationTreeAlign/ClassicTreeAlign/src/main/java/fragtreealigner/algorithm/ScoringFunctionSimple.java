
package fragtreealigner.algorithm;

import fragtreealigner.domainobjects.graphs.AlignmentTreeNode;
import fragtreealigner.util.Parameters;
import fragtreealigner.util.Session;

@SuppressWarnings("serial")
public class ScoringFunctionSimple extends ScoringFunction {
	private float scoreEquality;
	private float scoreInequality;
	private float scoreGap;
	
	public ScoringFunctionSimple() {
		super();
	}
	
	public ScoringFunctionSimple(Session session) {
		super(session);
	}

	public ScoringFunctionSimple(float scoreEquality, float scoreInequality, float scoreGap, float scoreNullNull, float scoreUnion) {
		super();
		this.scoreEquality = scoreEquality;
		this.scoreInequality = scoreInequality;
		this.scoreGap = scoreGap;
		this.scoreNullNull = scoreNullNull;
		this.scoreUnion = scoreUnion;
	}

	@Override
	public void initialize() {
		super.initialize();
		if (session == null) {
			scoreEquality                = 10;
			scoreInequality              = -7;
			scoreGap                     = -8;
		} else {
			Parameters parameters        = session.getParameters();
			scoreEquality                = parameters.scoreEquality;
			scoreInequality              = parameters.scoreInequality;
			scoreGap                     = parameters.scoreGap;
		}
	}
	
	@Override
	public float score(AlignmentTreeNode node1, AlignmentTreeNode node2) {
		if ((node1 == null) && (node2 == null)) return scoreNullNull;
		if ((node1 == null) || (node2 == null)) return scoreGap;
		if (node1.getNeutralLoss().getName().equalsIgnoreCase(node2.getNeutralLoss().getName())) return scoreEquality;
		else return scoreInequality;
	}

	@Override
	public float score(AlignmentTreeNode node1p1, AlignmentTreeNode node1p2, AlignmentTreeNode node2) {
		if ((node1p1 == null) || (node1p2 == null)) { 
			return scoreGap;
		}
		if (node2 == null) return scoreGap;
		return (((node1p2.getNeutralLoss().getName() + node1p1.getNeutralLoss().getName()).equalsIgnoreCase(node2.getNeutralLoss().getName())) ? scoreEquality : scoreInequality);
	}
}
