package hc.server.ui.design;

import javax.swing.text.Highlighter;
import javax.swing.tree.DefaultMutableTreeNode;

public class SearchResult {
	public final DefaultMutableTreeNode treeNode;
	public final int type;
	public final String itemName;
	public int offset;
	public int length;
	public final int lineNo;//从0开始
	public final String lineText;
	public Highlighter.Highlight highLight;
	public Highlighter highlighter;
	
	public SearchResult(final DefaultMutableTreeNode treeNode, final int type, final String itemName, final int offset, final int len, final int lineNo, final String lineText){
		this.treeNode = treeNode;
		this.type = type;
		this.itemName = itemName;
		this.offset = offset;
		this.length = len;
		this.lineNo = lineNo;
		this.lineText = lineText.trim();
	}
	
	@Override
	public final String toString(){
		return itemName + ", " + (lineNo + 1) + ": " + lineText;
	}
}
