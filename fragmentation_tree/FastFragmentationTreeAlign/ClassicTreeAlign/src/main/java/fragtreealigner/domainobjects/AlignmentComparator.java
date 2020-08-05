
package fragtreealigner.domainobjects;

import java.io.Serializable;
import java.util.Comparator;

@SuppressWarnings("serial")
public class AlignmentComparator implements Comparator<Alignment>, Serializable {
	private int scoreNum;

	public AlignmentComparator() {
		this.scoreNum = 0;
	}
	
	public AlignmentComparator(int scoreNum) {
		this.scoreNum = scoreNum;
	}
	
	public int compare(Alignment alignment1, Alignment alignment2) {
		if (scoreNum == 0) {
			if (alignment1.getScore() < alignment2.getScore()) return -1;
			if (alignment1.getScore() > alignment2.getScore()) return 1;		
			return 0;
		} else {
			if (alignment1.getScoreList().get(scoreNum - 1) < alignment2.getScoreList().get(scoreNum - 1)) return -1;
			if (alignment1.getScoreList().get(scoreNum - 1) > alignment2.getScoreList().get(scoreNum - 1)) return 1;					
			return 0;
		}
	}
}
