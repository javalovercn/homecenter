package hc.server.ui.design.hpj;

public class CSSClassIndex implements Comparable<CSSClassIndex> {
	public String fullName;
	public String className;
	public int startIdx;
	public int lineNo;

	public CSSClassIndex(final String fullName, final String className, final int idx,
			final int lineNo) {
		this.fullName = fullName;
		this.className = className;
		this.startIdx = idx;
		this.lineNo = lineNo;
	}

	@Override
	public int compareTo(final CSSClassIndex o) {
		return fullName.compareTo(o.fullName);
	}
}
