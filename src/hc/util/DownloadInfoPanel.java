package hc.util;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class DownloadInfoPanel extends JPanel {
	public final JLabel totalDown = new JLabel("", null, SwingConstants.CENTER);
	
	final JLabel src = new JLabel(getItem(9295), null, SwingConstants.LEFT);
	final JLabel md5 = new JLabel(buildItem("MD5"), null, SwingConstants.LEFT);
	final JLabel avg = new JLabel(getItem(9296), null, SwingConstants.LEFT);
	final JLabel cost = new JLabel(getItem(9297), null, SwingConstants.LEFT);
	final JLabel left = new JLabel(getItem(9298), null, SwingConstants.LEFT);

	final JLabel srcValue;
	final JLabel md5Value;
	public final JLabel avgValue = new JLabel("", null, SwingConstants.RIGHT);
	public final JLabel costValue = new JLabel("", null, SwingConstants.RIGHT);
	public final JLabel leftValue = new JLabel("", null, SwingConstants.RIGHT);
	
	public DownloadInfoPanel(final String fromURL, final String md5Str) {
		super(new BorderLayout());
		
		srcValue = new JLabel(fromURL, null, SwingConstants.RIGHT);
		md5Value = new JLabel(md5Str, null, SwingConstants.RIGHT);

		add(totalDown, BorderLayout.NORTH);
		
		final JPanel leftPanel = new JPanel(new GridLayout(0, 1));
		final JPanel rightPanel = new JPanel(new GridLayout(0, 1));
		final JPanel center = new JPanel(new BorderLayout());
		center.add(leftPanel, BorderLayout.WEST);
		center.add(rightPanel, BorderLayout.CENTER);
		
		add(center, BorderLayout.CENTER);
		
		leftPanel.add(src);
		leftPanel.add(md5);
		leftPanel.add(avg);
		leftPanel.add(cost);
		leftPanel.add(left);
		
		rightPanel.add(srcValue);
		rightPanel.add(md5Value);
		rightPanel.add(avgValue);
		rightPanel.add(costValue);
		rightPanel.add(leftValue);
	}
	
	final String getItem(final int resID) {
		return buildItem(ResourceUtil.get(resID));
	}
	
	final String buildItem(final String res) {
		return res + ResourceUtil.get(1041);//1041 = : 
	}
}
