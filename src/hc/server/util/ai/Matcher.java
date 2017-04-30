package hc.server.util.ai;


public class Matcher {
	public static int matchRecord(final byte[] recordRS, int startIdx, final Query query, final MatchScore score, 
			int matchSequenceNum, final int queryIdx){
		final int recordSizeidx = startIdx + 1;
		final byte recordSize = recordRS[startIdx];
		final int itemNum = query.itemNum;
		for (int i = queryIdx; i < itemNum; i++) {
			boolean isMatch = true;
			final byte itemSize = query.itemSize[i];
			if(recordSize == itemSize){
				final byte[] itemBS = query.itemBS[i];
				
				for (int k = 0; k < itemBS.length; k++) {
					if(itemBS[k] != recordRS[recordSizeidx + k]){
						isMatch = false;
						if(matchSequenceNum > 0){
							return 0;
						}
						break;
					}
				}
				
				if(isMatch){
					score.matchKeyNum++;
					score.setMaxMatchSequenceNum(++matchSequenceNum);
					startIdx += (1 + recordSize);
					if(startIdx == recordRS.length){
						return startIdx;
					}
					final int outIdx = matchRecord(recordRS, startIdx, query, score, matchSequenceNum, i + 1);
					if(outIdx == 0){
						return startIdx;
					}else{
						return outIdx;
					}
				}
			}else{
				if(matchSequenceNum > 0){
					return 0;
				}
			}
		}
		return 0;
	}
	
	public static void matchRecord(final byte[] recordRS, final Query query, final MatchScore score){
		final int length = recordRS.length;
		for (int j = 0; j < length; ) {
			final int shiftIdx = matchRecord(recordRS, j, query, score, 0, 0);
			if(shiftIdx > 0){
				j = shiftIdx;
			}else{
				j += (1 + recordRS[j]);
			}
		}
	}
}
