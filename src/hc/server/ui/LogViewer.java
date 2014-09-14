package hc.server.ui;

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.res.ImageSrc;
import hc.server.data.screen.ScreenCapturer;
import hc.server.util.ServerCUtil;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;

public class LogViewer extends JFrame {
	static Highlighter.HighlightPainter painterQuery = new DefaultHighlighter.DefaultHighlightPainter(
			Color.YELLOW);
	static Highlighter.HighlightPainter ERROR_LIGHTER = new DefaultHighlighter.DefaultHighlightPainter(Color.RED);
	static Highlighter.HighlightPainter OP_LIGHTER = new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);

	private void highlightErrAndOpration(JTextArea jta){
		buildHighlight(jta, ILog.ERR, ERROR_LIGHTER);
		buildHighlight(jta, ScreenCapturer.OP_STR, OP_LIGHTER);
	}
	private void buildHighlight(JTextArea jta, String patternStr, HighlightPainter lighter) {
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(jta.getText());
		boolean matchFound = matcher.matches(); // false
		if (!matchFound) {
			while (matcher.find()) {
				matchFound = true;
				int start = matcher.start();
				int end = matcher.end();
				try {
//					Font font = new Font("Verdana", Font.BOLD, 40);
					jta.getHighlighter().addHighlight(start, end, lighter);
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	public static LogViewer loadFile(String fileName, byte[] pwdBS, String title) throws Exception{
        File file = new File(fileName);
        if(file.exists() == false){
        	ContextManager.getContextInstance().displayMessage((String) ResourceUtil.get(IContext.ERROR), 
					(String) ResourceUtil.get(9004), IContext.ERROR, null, 0);
        	return null;
        }
		return new LogViewer(file, pwdBS, title);
	}

	JToolBar toolbar;
	JPanel pnlText, pnlBody;
	JButton btnSearch, btnNext, btnPre, btnRefresh;
	public JTextArea jta;
	Container contentpane;
	private ArrayList<Integer> searchIdx = new ArrayList<Integer>();
	int currSearchIdx;
	int searchLen;
	final File file;
	final byte[] pwdBS;
	
	public LogViewer(File file, byte[] pwdBS, String title) {
		this.file = file;
		this.pwdBS = pwdBS;
		
        setTitle(title);

		setIconImage(App.SYS_LOGO);
		
		setName("logView");
		ComponentListener cl = new LocationComponentListener();
		addComponentListener(cl);
		
		final JFrame self = this;
//		setModal(true);
		ActionListener exitActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				self.dispose();
			}
		};
		this.getRootPane().registerKeyboardAction(exitActionListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		

		JPanel contentpane = new JPanel();
		getContentPane().add(contentpane);
		
		contentpane.setLayout(new BorderLayout());
		pnlText = new JPanel();
		pnlBody = new JPanel();
		toolbar = new JToolBar();
		jta = new JTextArea(20, 500);
		
		jta.addMouseListener(new MouseAdapter() {
        	final Cursor TXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);
        	final Cursor D_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
            public void mouseEntered(MouseEvent mouseEvent)   { 
				jta.setCursor(TXT_CURSOR);   //鼠标进入Text区后变为文本输入指针
            } 
            public void mouseExited(MouseEvent mouseEvent)   { 
				jta.setCursor(D_CURSOR);   //鼠标离开Text区后恢复默认形态 
            } 
        }); 
		jta.getCaret().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e)   { 
            	jta.getCaret().setVisible(true);   //使Text区的文本光标显示 
            } 
        }); 
        
	    jta.setEditable(false);//makes the field editable

	    
		// Set lineWrap and wrapStyleWord true for the text area
		jta.setLineWrap(true);
		jta.setWrapStyleWord(true);

		loadStream();

		// Create a scroll pane to hold the text area
		JScrollPane jsp = new JScrollPane(jta);
		// Set BorderLayout for the panel, add label and scrollpane
		pnlBody.setLayout(new BorderLayout());
		pnlBody.add(jsp, BorderLayout.CENTER);
		pnlBody.setBorder(new TitledBorder("log :"));
		
		btnSearch = new JButton("", new ImageIcon(ImageSrc.SEARCH_ICON));
		btnNext = new JButton("", new ImageIcon(ImageSrc.DOWN_ICON));
		btnPre = new JButton("", new ImageIcon(ImageSrc.UP_ICON));
		btnRefresh = new JButton("", new ImageIcon(ResourceUtil.getResource("hc/res/refres_22.png")));
		
		btnNext.setEnabled(false);
		btnPre.setEnabled(false);
		
		final Action nextAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jta.setCaretPosition(searchIdx.get(currSearchIdx++));
				
				btnPre.setEnabled(true);

				if(searchIdx.size() > currSearchIdx){
					btnNext.setEnabled(true);
				}else{
					btnNext.setEnabled(false);
				}
			}
		};
		{
			nextAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, ResourceUtil.getAbstractCtrlInputEvent()));
			btnNext.addActionListener(nextAction);
			btnNext.getActionMap().put("nextAction", nextAction);
			btnNext.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) nextAction.getValue(Action.ACCELERATOR_KEY), "nextAction");
		}
		
		final Action preAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jta.setCaretPosition(searchIdx.get((--currSearchIdx) - 1));
				
				btnNext.setEnabled(true);

				if(currSearchIdx == 1){
					btnPre.setEnabled(false);
				}
			}
		};
		{
			preAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, ResourceUtil.getAbstractCtrlInputEvent() | InputEvent.SHIFT_MASK));
			btnPre.addActionListener(preAction);
			btnPre.getActionMap().put("preAction", preAction);
			btnPre.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) preAction.getValue(Action.ACCELERATOR_KEY), "preAction");
		}
		
		final Action findAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JTextField field = new JTextField(20);
				final JPanel panel = new JPanel(){
		            public void addNotify()
		            {
		            	super.addNotify();
						new Thread(){
							public void run(){
								try{
									Thread.sleep(500);
								}catch (Exception e) {
								}
								field.requestFocusInWindow();								
							}
						}.start();
		            }
		        };
				panel.setLayout(new FlowLayout(FlowLayout.LEFT));
				panel.add(new JLabel(btnSearch.getIcon()));
				panel.add(field);
				int result = JOptionPane.showConfirmDialog(self, panel, "Find...", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				final String searchStr = field.getText();
				if(result == JOptionPane.OK_OPTION && searchStr.length() > 0){
				}else{
					return;
				}

				searchLen = searchStr.length();
				
				btnNext.setEnabled(false);
				btnPre.setEnabled(false);
				
				searchIdx.clear();
				
				String myWord = jta.getText();

				Highlighter h = jta.getHighlighter();
				
//				h.removeHighlight(LogViewer.painter);
				Highlighter.Highlight[] hs = h.getHighlights();
				for (int i = 0; i < hs.length; i++) {
					Highlight highlight = hs[i];
					if(highlight.getPainter() == LogViewer.painterQuery){
						h.removeHighlight(highlight);
					}
				}
				
				//Highlight(LogViewer.painter);
				
				//Pattern pattern = Pattern.compile("\\b" + searchStr + "\\b");
				Pattern pattern = Pattern.compile(searchStr);
				Matcher matcher = pattern.matcher(myWord);
				boolean matchFound = matcher.matches(); // false
				if (!matchFound) {
					while (matcher.find()) {
						matchFound = true;
						int start = matcher.start();
						searchIdx.add(start);
						int end = matcher.end();
						try {
//							Font font = new Font("Verdana", Font.BOLD, 40);
							h.addHighlight(start, end, LogViewer.painterQuery);
						} catch (BadLocationException e1) {
							e1.printStackTrace();
						}
					}
					
					if(matchFound){
						currSearchIdx = 0;
						jta.setCaretPosition(searchIdx.get(currSearchIdx++));
						if(searchIdx.size() > currSearchIdx){
							btnNext.setEnabled(true);
							btnNext.setFocusable(true);
						}
					}
//				}else {
//					JOptionPane.showMessageDialog(null, "No Match Found");
				}
			}
		};
		{
			findAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, ResourceUtil.getAbstractCtrlInputEvent()));
			btnSearch.addActionListener(findAction);
			btnSearch.getActionMap().put("findAction", findAction);
			btnSearch.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) findAction.getValue(Action.ACCELERATOR_KEY), "findAction");
		}
		{
			final Action refreshAction = new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					loadStream();
				}
			};
			refreshAction.putValue(Action.ACCELERATOR_KEY, ResourceUtil.getRefreshKeyStroke());
			btnRefresh.addActionListener(refreshAction);
			btnRefresh.getActionMap().put("refreshAction", refreshAction);
			btnRefresh.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) refreshAction.getValue(Action.ACCELERATOR_KEY), "refreshAction");

		}
		btnSearch.setToolTipText("Find ("+ResourceUtil.getAbstractCtrlInputEventText()+" + F)");
		btnNext.setToolTipText("Find Next ("+ResourceUtil.getAbstractCtrlInputEventText()+" + K)");
		btnPre.setToolTipText("Find Previous ("+ResourceUtil.getAbstractCtrlInputEventText()+" + Shift + K)");
		btnRefresh.setToolTipText("Refresh (" + ResourceUtil.getRefreshKeyText() + ")");
		
		toolbar.add(btnSearch);
		toolbar.add(btnNext);
		toolbar.add(btnPre);
		toolbar.add(btnRefresh);
		
		toolbar.addSeparator();
		
		contentpane.add(toolbar, BorderLayout.NORTH);
		contentpane.add(pnlBody, BorderLayout.CENTER);
		setSize(1024, 700);
		
		if(LocationComponentListener.hasLocation(this) && LocationComponentListener.loadLocation(this)){
			setVisible(true);
		}else{
			App.showCenter(this);
		}

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.getRootPane().setDefaultButton(btnSearch);
	}
	
	int posBeforeRefresh = 0;
	private void loadStream() {
		LogManager.flush();
		posBeforeRefresh = jta.getCaretPosition();
		
		try {
	        FileInputStream ins = new FileInputStream(file);
	        
	        InputStreamReader reader = new InputStreamReader(
	        		ServerCUtil.decodeStream((InputStream)ins, pwdBS), IConstant.UTF_8);  
			jta.read(reader, null);
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		
		jta.setCaretPosition(posBeforeRefresh);
		highlightErrAndOpration(jta);
	}
}