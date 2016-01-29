package hc.server.ui.design.hpj;

import hc.core.util.ThreadPriorityManager;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.tree.MutableTreeNode;

public class JRubyNodeEditPanel extends ScriptEditPanel {
	final JLabel nameLabel = new JLabel("Script File Name :");

	public JRubyNodeEditPanel(){
		super();
		
		
		final JPanel namePanel = new JPanel();
		namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		namePanel.add(nameLabel);
		nameField.setColumns(10);
		namePanel.add(nameField);
		namePanel.add(testBtn);
		namePanel.add(formatBtn);
		
		final JPanel jtascriptPanel = new JPanel();
		jtascriptPanel.setBorder(new TitledBorder("JRuby script :"));
		jtascriptPanel.setLayout(new BorderLayout());
		
		jtascriptPanel.add(scrollpane, BorderLayout.CENTER);
		jtascriptPanel.add(errRunInfo, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(namePanel, BorderLayout.NORTH);
		add(jtascriptPanel, BorderLayout.CENTER);

	}
	
	@Override
	public void init(final MutableTreeNode data, final JTree tree) {
		super.init(data, tree);

		final String listener = getListener();
		final String scripts = listener == null ? "" : listener;
		jtaScript.setText(scripts);
		
		//代码很长时，置于首行
		jtaScript.setCaretPosition(0);
		
		initColor(true);
		
		extInit();
		
		try{
			Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);
		}catch (final Exception e) {
		}
		
		super.isInited = true;
	}

	public String getListener() {
		return ((HPShareContent)currItem).content;
	}

	@Override
	public void updateScript(final String script) {
		((HPShareContent)currItem).content = script;
		designer.setNeedRebuildTestJRuby(true);
	}

	@Override
	Map<String, String> buildMapScriptParameter() {
		return null;
	}

	@Override
	public void extInit() {
		updateScriptInInitProcess();
	}
}
