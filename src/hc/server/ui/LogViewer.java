package hc.server.ui;

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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

import hc.App;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.util.ExceptionReporter;
import hc.core.util.ILog;
import hc.core.util.LogManager;
import hc.res.ImageSrc;
import hc.server.DisposeListener;
import hc.server.HCActionListener;
import hc.server.SingleJFrame;
import hc.server.util.HCJFrame;
import hc.server.util.ServerCUtil;
import hc.util.ResourceUtil;

public class LogViewer extends HCJFrame {
	private final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	static Highlighter.HighlightPainter painterQuery = new DefaultHighlighter.DefaultHighlightPainter(
			Color.YELLOW);
	static Highlighter.HighlightPainter ERROR_LIGHTER = new DefaultHighlighter.DefaultHighlightPainter(
			Color.RED);
	static Highlighter.HighlightPainter OP_LIGHTER = new DefaultHighlighter.DefaultHighlightPainter(
			Color.GREEN);

	private void highlightErrAndOpration(final JTextArea jta) {
		buildHighlight(jta, ILog.ERR, ERROR_LIGHTER);
		buildHighlight(jta, ILog.OP_STR, OP_LIGHTER);
	}

	private void buildHighlight(final JTextArea jta, final String patternStr,
			final HighlightPainter lighter) {
		final Pattern pattern = Pattern.compile(patternStr);
		final Matcher matcher = pattern.matcher(jta.getText());
		boolean matchFound = matcher.matches(); // false
		if (!matchFound) {
			while (matcher.find()) {
				matchFound = true;
				final int start = matcher.start();
				final int end = matcher.end();
				try {
					// Font font = new Font("Verdana", Font.BOLD, 40);
					jta.getHighlighter().addHighlight(start, end, lighter);
				} catch (final BadLocationException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static LogViewer loadFile(final String fileName, final byte[] pwdBS,
			final String cipherAlgorithm, final String title) throws Exception {
		final File file = new File(ResourceUtil.getBaseDir(), fileName);
		if (file.exists() == false) {
			final JPanel panel = new JPanel(new BorderLayout());
			panel.add(new JLabel(ResourceUtil.get(9004), App.getSysIcon(App.SYS_ERROR_ICON),
					JLabel.LEADING), BorderLayout.CENTER);
			final JPanel descPanel = new JPanel(new BorderLayout());
			descPanel.add(new JLabel("<html><STRONG>" + ResourceUtil.get(9095)
					+ "</STRONG><BR>if <STRONG>debugOn</STRONG> is added to program argument, "
					+ "the log file will NOT be created.</html>"), BorderLayout.CENTER);
			panel.add(descPanel, BorderLayout.SOUTH);
			App.showCenterPanelMain(panel, 0, 0, ResourceUtil.get(IConstant.ERROR), false, null,
					null, new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
						}
					}, null, null, false, true, null, false, false);
			return null;
		}
		return new LogViewer(file, pwdBS, cipherAlgorithm, title);
	}

	JToolBar toolbar;
	JPanel pnlText, pnlBody;
	JButton btnSearch, btnNext, btnPre, btnRefresh;
	JCheckBox reportException = App.buildReportExceptionCheckBox(true);
	public JTextArea jta;
	Container contentpane;
	private final ArrayList<Integer> searchIdx = new ArrayList<Integer>();
	int currSearchIdx;
	int searchLen;
	final File file;
	final byte[] pwdBS;

	public LogViewer(final File file, final byte[] pwdBS, final String cipherAlgorithm,
			final String title) {
		this.file = file;
		this.pwdBS = pwdBS;

		setTitle(title);

		setIconImage(App.SYS_LOGO);

		setName("logView");
		final ComponentListener cl = new LocationComponentListener(threadPoolToken);
		addComponentListener(cl);

		final JFrame self = this;
		// setModal(true);
		final ActionListener exitActionListener = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				self.dispose();
			}
		}, threadPoolToken);
		this.getRootPane().registerKeyboardAction(exitActionListener,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		final JPanel contentpane = new JPanel();
		getContentPane().add(contentpane);

		contentpane.setLayout(new BorderLayout());
		pnlText = new JPanel();
		pnlBody = new JPanel();
		toolbar = new JToolBar();
		jta = new JTextArea(20, 500);

		jta.addMouseListener(new MouseAdapter() {
			final Cursor TXT_CURSOR = new Cursor(Cursor.TEXT_CURSOR);
			final Cursor D_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

			@Override
			public void mouseEntered(final MouseEvent mouseEvent) {
				jta.setCursor(TXT_CURSOR); // 鼠标进入Text区后变为文本输入指针
			}

			@Override
			public void mouseExited(final MouseEvent mouseEvent) {
				jta.setCursor(D_CURSOR); // 鼠标离开Text区后恢复默认形态
			}
		});
		jta.getCaret().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				jta.getCaret().setVisible(true); // 使Text区的文本光标显示
			}
		});

		jta.setEditable(false);// makes the field editable

		// Set lineWrap and wrapStyleWord true for the text area
		jta.setLineWrap(true);
		jta.setWrapStyleWord(true);

		loadStream(cipherAlgorithm);

		// Create a scroll pane to hold the text area
		final JScrollPane jsp = new JScrollPane(jta);
		// Set BorderLayout for the panel, add label and scrollpane
		pnlBody.setLayout(new BorderLayout());
		pnlBody.add(jsp, BorderLayout.CENTER);
		pnlBody.setBorder(new TitledBorder("log :"));

		btnSearch = new JButton("", new ImageIcon(ImageSrc.SEARCH_SMALL_ICON));
		btnNext = new JButton("", new ImageIcon(ImageSrc.DOWN_SMALL_ICON));
		btnPre = new JButton("", new ImageIcon(ImageSrc.UP_SMALL_ICON));
		btnRefresh = new JButton("", new ImageIcon(
				ResourceUtil.getImage(ResourceUtil.getResource("hc/res/refres_22.png"))));

		btnNext.setEnabled(false);
		btnPre.setEnabled(false);

		final Action nextAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						jta.setCaretPosition(searchIdx.get(currSearchIdx++));

						btnPre.setEnabled(true);

						if (searchIdx.size() > currSearchIdx) {
							btnNext.setEnabled(true);
						} else {
							btnNext.setEnabled(false);
						}
					}
				}, threadPoolToken);
			}
		};
		{
			nextAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K,
					ResourceUtil.getAbstractCtrlInputEvent()));
			btnNext.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					nextAction.actionPerformed(null);
				}
			}, threadPoolToken));
			btnNext.getActionMap().put("nextAction", nextAction);
			btnNext.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.put((KeyStroke) nextAction.getValue(Action.ACCELERATOR_KEY), "nextAction");
		}

		final Action preAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						jta.setCaretPosition(searchIdx.get((--currSearchIdx) - 1));

						btnNext.setEnabled(true);

						if (currSearchIdx == 1) {
							btnPre.setEnabled(false);
						}
					}
				}, threadPoolToken);
			}
		};
		{
			preAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K,
					ResourceUtil.getAbstractCtrlInputEvent() | InputEvent.SHIFT_MASK));
			btnPre.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					preAction.actionPerformed(null);
				}
			}, threadPoolToken));
			btnPre.getActionMap().put("preAction", preAction);
			btnPre.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.put((KeyStroke) preAction.getValue(Action.ACCELERATOR_KEY), "preAction");
		}

		final Action findAction = new AbstractAction() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final JTextField field = new JTextField(20);
				final JPanel panel = new JPanel();
				panel.setLayout(new FlowLayout(FlowLayout.LEFT));
				panel.add(new JLabel(btnSearch.getIcon()));
				field.requestFocus();
				panel.add(field);
				App.showCenterPanelMain(panel, 0, 0, "Find...", true, null, null,
						new HCActionListener(new Runnable() {
							@Override
							public void run() {
								final String searchStr = field.getText();
								searchLen = searchStr.length();

								btnNext.setEnabled(false);
								btnPre.setEnabled(false);

								searchIdx.clear();

								final String myWord = jta.getText();

								final Highlighter h = jta.getHighlighter();

								// h.removeHighlight(LogViewer.painter);
								final Highlighter.Highlight[] hs = h.getHighlights();
								for (int i = 0; i < hs.length; i++) {
									final Highlight highlight = hs[i];
									if (highlight.getPainter() == LogViewer.painterQuery) {
										h.removeHighlight(highlight);
									}
								}

								// Highlight(LogViewer.painter);

								// Pattern pattern = Pattern.compile("\\b" +
								// searchStr + "\\b");
								final Pattern pattern = Pattern.compile(searchStr);
								final Matcher matcher = pattern.matcher(myWord);
								boolean matchFound = false;
								while (matcher.find()) {
									matchFound = true;
									final int start = matcher.start();
									searchIdx.add(start);
									final int end = matcher.end();
									try {
										// Font font = new Font("Verdana",
										// Font.BOLD, 40);
										h.addHighlight(start, end, LogViewer.painterQuery);
									} catch (final BadLocationException e) {
										e.printStackTrace();
									}
								}

								if (matchFound) {
									currSearchIdx = 0;
									jta.setCaretPosition(searchIdx.get(currSearchIdx++));
									if (searchIdx.size() > currSearchIdx) {
										btnNext.setEnabled(true);
										btnNext.setFocusable(true);
									}
								} else {
									App.showMessageDialog(LogViewer.this, "No found!");
								}
							}
						}, null), null, LogViewer.this, true, false, btnSearch, false, false);
			}
		};
		{
			findAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F,
					ResourceUtil.getAbstractCtrlInputEvent()));
			btnSearch.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					findAction.actionPerformed(null);
				}
			}, threadPoolToken));
			btnSearch.getActionMap().put("findAction", findAction);
			btnSearch.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.put((KeyStroke) findAction.getValue(Action.ACCELERATOR_KEY), "findAction");
		}
		{
			final Action refreshAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							loadStream(cipherAlgorithm);
						}
					}, threadPoolToken);
				}
			};
			refreshAction.putValue(Action.ACCELERATOR_KEY, ResourceUtil.getRefreshKeyStroke());
			btnRefresh.addActionListener(new HCActionListener(new Runnable() {
				@Override
				public void run() {
					refreshAction.actionPerformed(null);
				}
			}, threadPoolToken));
			btnRefresh.getActionMap().put("refreshAction", refreshAction);
			btnRefresh.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
					(KeyStroke) refreshAction.getValue(Action.ACCELERATOR_KEY), "refreshAction");

		}
		btnSearch.setToolTipText("Find (" + ResourceUtil.getAbstractCtrlInputEventText() + " + F)");
		btnNext.setToolTipText(
				"Find Next (" + ResourceUtil.getAbstractCtrlInputEventText() + " + K)");
		btnPre.setToolTipText(
				"Find Previous (" + ResourceUtil.getAbstractCtrlInputEventText() + " + Shift + K)");
		btnRefresh.setToolTipText("Refresh (" + ResourceUtil.getRefreshKeyText() + ")");

		if (ResourceUtil.isAndroidServerPlatform()) {
			// 由于Android效果较差，关闭
		} else {
			toolbar.add(btnSearch);
			toolbar.add(btnNext);
			toolbar.add(btnPre);
		}
		toolbar.add(btnRefresh);

		toolbar.addSeparator();

		contentpane.add(toolbar, BorderLayout.NORTH);
		contentpane.add(pnlBody, BorderLayout.CENTER);
		contentpane.add(reportException, BorderLayout.SOUTH);

		setSize(1024, 700);

		if (LocationComponentListener.hasLocation(this)
				&& LocationComponentListener.loadLocation(this)) {
			setVisible(true);
		} else {
			App.showCenter(this);
		}
		App.setDisposeListener(this, new DisposeListener() {
			@Override
			public void dispose() {
				SingleJFrame.removeJFrame(LogViewer.class.getName());
			}
		});
		SingleJFrame.addJFrame(LogViewer.class.getName(), this);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.getRootPane().setDefaultButton(btnSearch);
	}

	int posBeforeRefresh = 0;

	private void loadStream(final String cipherAlgorithm) {
		LogManager.flush();
		posBeforeRefresh = jta.getCaretPosition();

		try {
			final FileInputStream ins = new FileInputStream(file);

			final InputStreamReader reader = new InputStreamReader(
					ServerCUtil.decodeStream(ins, pwdBS, cipherAlgorithm), IConstant.UTF_8);
			jta.read(reader, null);
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}

		jta.setCaretPosition(posBeforeRefresh);
		highlightErrAndOpration(jta);
	}
}