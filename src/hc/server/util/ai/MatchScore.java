package hc.server.util.ai;

public class MatchScore implements Comparable<MatchScore> {
	String fromKey;

	/**
	 * label记录匹配的关键词数
	 */
	public int matchKeyNum;

	/**
	 * label记录匹配的关键词相邻个数
	 */
	public int matchSequenceNum;

	public LabelData labelData;

	public final void setMaxMatchSequenceNum(final int msn) {
		if (msn > matchSequenceNum) {
			matchSequenceNum = msn;
		}
	}

	@Override
	public int compareTo(final MatchScore o) {
		final int msn = matchSequenceNum - o.matchSequenceNum;
		if (msn != 0) {
			return msn;
		}

		return matchKeyNum - o.matchKeyNum;
	}
}
