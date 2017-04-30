package hc.server.util.ai;

import java.util.Vector;

public class ScreenScore implements Comparable<ScreenScore>{
	public final String target;
	public final String projectID;
	
	public ScreenScore(final String projectID, final String target){
		this.projectID = projectID;
		this.target = target;
	}
	
	Vector<String> listKeys;
	Vector<MatchScore> labelScore;
	
	@Override
	public int compareTo(final ScreenScore o) {
		final int keyNum = o.listKeys.size() - listKeys.size();
		
		if(keyNum != 0){
			return keyNum;
		}
		
		final int labelScoreDff = o.labelScore.get(0).compareTo(labelScore.get(0));
		if(labelScoreDff != 0){
			return labelScoreDff;
		}
		
		return 0;
	}
	
	public final void addKey(final String key){
		if(listKeys == null){
			listKeys = new Vector<String>(10); 
		}
		
		if(listKeys.contains(key) == false){
			listKeys.add(key);
		}
	}
	
	public final void addMatchScore(final MatchScore ms){
		if(labelScore == null){
			labelScore = new Vector<MatchScore>(10);
		}
		
		labelScore.add(ms);
	}

}
