package hc.server.ui.design.hpj;

import hc.server.ui.design.Designer;

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
		
		
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		namePanel.add(nameLabel);
		nameField.setColumns(10);
		namePanel.add(nameField);
		namePanel.add(testBtn);
		
		
		JPanel jtascriptPanel = new JPanel();
		jtascriptPanel.setBorder(new TitledBorder("JRuby script :"));
		jtascriptPanel.setLayout(new BorderLayout());
		
		jtascriptPanel.add(scrollpane, BorderLayout.CENTER);
		jtascriptPanel.add(errRunInfo, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(namePanel, BorderLayout.NORTH);
		add(jtascriptPanel, BorderLayout.CENTER);

	}
	
	@Override
	public void init(MutableTreeNode data, JTree tree) {
		super.init(data, tree);

		final String listener = ((HPShareJRuby)currItem).content;
		jtaScript.setText(listener == null ? "" : listener);
		
		//代码很长时，置于首行
		jtaScript.setCaretPosition(0);
		
		initColor(true);
		
		super.isInited = true;
	}

	@Override
	public void updateScript(String script) {
		((HPShareJRuby)currItem).content = script;
		Designer.getInstance().setNeedRebuildTestJRuby(true);
	}

	@Override
	Map<String, String> buildMapScriptParameter() {
		return null;
	}
}
