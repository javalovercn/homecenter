package hc.server.ui.design.hpj;

import hc.core.util.ThreadPriorityManager;
import hc.server.ui.design.code.TabHelper;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.tree.MutableTreeNode;

public class JRubyNodeEditPanel extends ScriptEditPanel {
	public static final String JRUBY_SCRIPT = "JRuby Script";
	public static final String JRUBY_SCRIPT_BORDER = JRUBY_SCRIPT + " :";
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
		namePanel.add(scriptBtn);
		
		final JPanel jtascriptPanel = new JPanel();
		jtascriptPanel.setBorder(new TitledBorder(JRUBY_SCRIPT_BORDER));
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
		
		setInitText(scripts);
		TabHelper.initScriptPanel(jtaScript, this);
		
		initColor(true, false, 0);
		
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
	}
}
