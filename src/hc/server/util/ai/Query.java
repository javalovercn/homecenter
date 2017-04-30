package hc.server.util.ai;

import hc.core.util.StringUtil;

import java.util.List;
import java.util.Vector;

public class Query {
	final List<String> search;
	final byte[] itemSize;
	final byte[][] itemBS;
	final int itemNum;
	Vector<MatchScore> listScore;
	
	public Query(final List<String> search){
		this.search = search;
		itemNum = search.size();
		itemSize = new byte[itemNum];
		itemBS = new byte[itemNum][];
		
		for (int i = 0; i < itemNum; i++) {
			final String item = search.get(i);
			final byte[] bs = StringUtil.getBytes(item);
			itemBS[i] = bs;
			itemSize[i] = new Integer(bs.length).byteValue();
		}
	}
	
	public final void addScore(final MatchScore score){
		if(listScore == null){
			listScore = new Vector<MatchScore>(5);
		}
		
		listScore.add(score);
	}
	
	public final Vector<MatchScore> getScoreList(){
		return listScore;
	}
	
	public final void reset(){
		listScore = null;
	}
}
