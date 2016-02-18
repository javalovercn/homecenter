package hc.server.ui.design.hpj;

import hc.core.L;
import hc.server.ui.ClientDesc;
import hc.server.ui.design.Designer;
import hc.server.ui.design.code.CodeHelper;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class MletNodeEditPanel extends DefaultMenuItemNodeEditPanel {
	private static final SimpleAttributeSet CLASS_LIGHTER = build(Color.decode("#088A29"), false);
	private static final SimpleAttributeSet ITEM_LIGHTER = build(Color.decode("#0431B4"), false);
	private static final SimpleAttributeSet SPLITTER_LIGHTER = build(Color.BLACK, false);
	private static final SimpleAttributeSet VALUE_LIGHTER = build(Color.decode("#B18904"), false);
	private static final SimpleAttributeSet VARIABLE_LIGHTER = build(Color.decode("#DF0101"), false);
	
	private static final char[] refreshChar = {'{', ':', ';', '}', '/'};
	private static final Pattern class_pattern = Pattern.compile("(\\s*?.*?\\s*?)\\{");
	private static final Pattern item_pattern = Pattern.compile("(?<=[\\{;])(\\s*?.*?\\s*?):\\s*?.*?\\s*?(?=[\\};])", Pattern.MULTILINE|Pattern.DOTALL);
	private static final Pattern rem_pattern = Pattern.compile("(/\\*.*?\\*/)", Pattern.MULTILINE|Pattern.DOTALL);
	private static final Pattern var_pattern = Pattern.compile("(\\$.*?\\$)");
	private static final Pattern spliter_pattern = Pattern.compile("([\\{\\};:])");
	
	public final void initSytleBlock(final String text, final int offset) {
		final StyledDocument document = (StyledDocument)cssEditPane.getDocument();
		document.setCharacterAttributes(offset, text.length(), VALUE_LIGHTER, true);
		
		buildSytleHighlight(cssEditPane, class_pattern, CLASS_LIGHTER, offset, text);
		buildSytleHighlight(cssEditPane, item_pattern, ITEM_LIGHTER, offset, text);//要置于字符串之前，因为字符串中可能含有数字
		buildSytleHighlight(cssEditPane, var_pattern, VARIABLE_LIGHTER, offset, text);
		buildSytleHighlight(cssEditPane, rem_pattern, REM_LIGHTER, offset, text);//字符串中含有#{}，所以要置于STR_LIGHTER之前
		buildSytleHighlight(cssEditPane, spliter_pattern, SPLITTER_LIGHTER, offset, text);//字符串中含有#{}，所以要置于STR_LIGHTER之前
	}
	
	public static final void buildSytleHighlight(final JTextPane jta, final Pattern pattern, final SimpleAttributeSet attributes, final int offset, final String text) {
		final StyledDocument document = (StyledDocument)jta.getDocument();
		final Matcher matcher = pattern.matcher(text);
		while (matcher.find()) {
			final int start = matcher.start(1) + offset;
			final int end = matcher.end(1) + offset;
			document.setCharacterAttributes(start, end - start, attributes, false);
		}
	}
	
	private final Runnable colorRunnable = new Runnable() {
		@Override
		public void run() {
			initSytleBlock(cssEditPane.getText(), 0);
		}
	};

	private final void colorStyle(){
		SwingUtilities.invokeLater(colorRunnable);
	}
	
	private static final String MLET = "Mlet";
	private final JTabbedPane mainPanel = new JTabbedPane();
	private final JPanel cssPanel = new JPanel();
	private final JLabel tipCssLabel = new JLabel("<html>" +
			"The following styles will be shared for all HTMLMlet(s) in this project." +
			"<BR>if you need special styles for a HTMLMlet, invoke <STRONG>HTMLMlet.loadCSS</STRONG>." +
			"<BR>to set styles for a JComponent, please invoke <STRONG>HTMLMlet.setCSS</STRONG>." +
			"<BR><BR>for variables, input shortcut keys for word completion." +
			"</html>");
	private final JTextPane cssEditPane = new JTextPane(){
		@Override
		public void paste(){
			try{
				synchronized (modifyAndColorAll) {
					super.paste();
					notifyModified();
					colorStyle();
				}
			}catch (final Throwable e) {
				e.printStackTrace();
			}
		}
		@Override
		public void cut(){
			synchronized (modifyAndColorAll) {
				super.cut();
				notifyModified();
				colorStyle();
			}
		}
	};
	private final JScrollPane cssScrollPane = new JScrollPane(cssEditPane);
	
	public MletNodeEditPanel(){
		super();
		
		tipCssLabel.setBorder(new TitledBorder("Note :"));
		cssPanel.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
		cssPanel.add(tipCssLabel, BorderLayout.NORTH);
		{
			final JPanel tmpPanel = new JPanel(new BorderLayout());
			tmpPanel.setBorder(new TitledBorder("Styles Edit Area :"));
			tmpPanel.add(cssScrollPane, BorderLayout.CENTER);
			cssPanel.add(tmpPanel, BorderLayout.CENTER);
		}
		
		mainPanel.addTab(MLET, this);
		
		mainPanel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e){
			    final int selectedIndex = mainPanel.getSelectedIndex();
			    if(selectedIndex==1){
			    	cssEditPane.setText(designer.getProjCSS());
			    	colorStyle();
			    }				   
		}});
		
		cssEditPane.addKeyListener(new KeyListener() {
			final boolean isMacOS = ResourceUtil.isMacOSX();
			final int fontHeight = cssEditPane.getFontMetrics(cssEditPane.getFont()).getHeight();
			
			@Override
			public void keyTyped(final KeyEvent e) {
				final char inputChar = e.getKeyChar();
				final int modifiers = e.getModifiers();

				final CodeHelper codeHelper = designer.codeHelper;
				
				if(isMacOS && (inputChar != 0 && inputChar == codeHelper.wordCompletionChar
						&& ((codeHelper.wordCompletionModifyCode == KeyEvent.VK_ALT && modifiers == 0) 
									|| (codeHelper.wordCompletionModifyMaskCode == modifiers)))){//注意：请同步从ScriptEditPanel
					try {
						codeHelper.inputVariableForCSS(cssEditPane, cssEditPane.getCaret().getMagicCaretPosition(), fontHeight, cssEditPane.getCaretPosition());
					} catch (final Exception ex) {
						if(L.isInWorkshop){
							ex.printStackTrace();
						}
					}
					ScriptEditPanel.consumeEvent(e);
				}
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
				if(Designer.isDirectKeycode(e.getKeyCode()) == false && Designer.isCopyKeyEvent(e, isJ2SEServer) == false){
					MletNodeEditPanel.this.notifyModified();
				}
				
				final char keyChar = e.getKeyChar();
				boolean isRefreshChar = false;
				final int size = refreshChar.length;
				for (int i = 0; i < size; i++) {
					if(refreshChar[i] == keyChar){
						isRefreshChar = true;
						break;
					}
				}
				if(isRefreshChar){
					colorStyle();
				}
			}
			
			@Override
			public void keyPressed(final KeyEvent e) {
				final int keycode = e.getKeyCode();
	            final int modifiers = e.getModifiers();
	            final CodeHelper codeHelper = designer.codeHelper;
				final int wordCompletionModifyMaskCode = codeHelper.wordCompletionModifyMaskCode;
				//无输入字符时的触发提示代码
				if(keycode == codeHelper.wordCompletionCode && (modifiers & wordCompletionModifyMaskCode) == wordCompletionModifyMaskCode){
					//注意：请同步从ScriptEditPanel
					try {
						codeHelper.inputVariableForCSS(cssEditPane, cssEditPane.getCaret().getMagicCaretPosition(), fontHeight, cssEditPane.getCaretPosition());
					} catch (final Exception ex) {
						if(L.isInWorkshop){
							ex.printStackTrace();
						}
					}
					ScriptEditPanel.consumeEvent(e);
				}
			}
		});
		
		cssEditPane.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(final FocusEvent e) {
				designer.setProjCSS(cssEditPane.getText());
			}
			@Override
			public void focusGained(final FocusEvent e) {
			}
		});
	}
	
	@Override
	public void init(final MutableTreeNode data, final JTree tree) {
		super.init(data, tree);
		
		if(NodeEditPanelManager.meetHTMLMletLimit((HPNode)((DefaultMutableTreeNode)data).getUserObject())){
			if(mainPanel.getTabCount() < 2){
				mainPanel.addTab("CSS Styles", cssPanel);
			}
			mainPanel.setTitleAt(0, "HTMLMlet");
		}else{
			if(mainPanel.getTabCount() == 2){
				mainPanel.remove(1);
			}
			mainPanel.setTitleAt(0, MLET);
		}

		mainPanel.setSelectedIndex(0);
	}
	
	@Override
	public JComponent getMainPanel(){
		return mainPanel;
	}
}
