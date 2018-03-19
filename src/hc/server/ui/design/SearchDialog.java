package hc.server.ui.design;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.tree.DefaultMutableTreeNode;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.StringUtil;
import hc.server.ui.ClientDesc;
import hc.server.ui.design.hpj.HCTextPane;
import hc.server.ui.design.hpj.HPNode;
import hc.util.ClassUtil;
import hc.util.HCDialog;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

public class SearchDialog extends HCDialog implements DesignScriptNodeIterator {
	static Highlighter.HighlightPainter SEARCH_CODE_LINE_LIGHTER = new DefaultHighlighter.DefaultHighlightPainter(Color.decode("#ADD8E6"));

	private static final String ZERO_RESULT = "0 result.";

	final Designer designer;
	final DefaultComboBoxModel<String> searchModel = new DefaultComboBoxModel<String>();
	final JComboBox<String> searchComboBox = new JComboBox<String>(searchModel);
	final JCheckBox caseSensitive = new JCheckBox("case sensitive");
	final JCheckBox regular = new JCheckBox("regular expression");
	final JButton searchButton = new JButton("Search");
	final JButton closeButton = new JButton("Close");

	final JLabel resultLabel = new JLabel(ZERO_RESULT);
	final JPanel resultTopPanel = new JPanel(new BorderLayout());
	final Vector<SearchResult> resultData = new Vector<SearchResult>();
	final Vector<String> tableColumns = buildTableColumns();
	final JTable resultTable = new JTable(new AbstractTableModel() {
		@Override
		public String getColumnName(final int columnIndex) {
			return tableColumns.get(columnIndex);
		}

		@Override
		public int getRowCount() {
			return resultData.size();
		}

		@Override
		public int getColumnCount() {
			return tableColumns.size();
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			final SearchResult item = resultData.get(rowIndex);
			if (columnIndex == 0) {
				return HPNode.typeToNodeFolderDesc(item.type);
			} else if (columnIndex == 1) {
				return item.itemName;
			} else if (columnIndex == 2) {
				return item.lineText;
			}
			return "";
		}
	});
	final JScrollPane resultScrollPane = new JScrollPane(resultTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

	final JPanel searchPanel = new JPanel(new GridBagLayout());
	public HCTextPane hcTextPane;

	private final int lastSelectionOffset = -1;

	private int lastSelectionEndOffset;
	private Highlighter.Highlight beforeSelection, endSelection;

	public final void splitHighlightWhenSelection(final int offset, final int endOffset, final Highlighter highlighter) {
		// if(lastSelectionOffset != -1){
		// if(beforeSelection != null){
		// highlighter.removeHighlight(beforeSelection);
		// }
		// if(endSelection != null){
		// highlighter.removeHighlight(endSelection);
		// }
		// final int size = sameNodeList.size();
		// for (int i = 0; i < size; i++) {
		// final SearchResult sr = sameNodeList.get(i);
		// if(lastSelectionOffset <= (sr.offset + sr.length) && sr.offset <=
		// lastSelectionEndOffset){
		// try {
		// sr.highlighter.changeHighlight(sr.highLight,
		// sr.highLight.getStartOffset(), sr.highLight.getEndOffset());
		// } catch (final BadLocationException e) {
		// e.printStackTrace();
		// }
		// break;
		// }
		// }
		// beforeSelection = null;
		// endSelection = null;
		// }
		//
		// lastSelectionOffset = -1;
		//
		// if(endOffset == offset){
		// return;
		// }
		//
		// final int size = sameNodeList.size();
		// for (int i = 0; i < size; i++) {
		// final SearchResult sr = sameNodeList.get(i);
		// final int srEndIdx = sr.offset + sr.length;
		// if(offset <= srEndIdx && sr.offset <= endOffset){
		// sr.highlighter.removeHighlight(sr.highLight);
		// sr.highLight = null;
		// lastSelectionOffset = offset;
		// lastSelectionEndOffset = endOffset;
		//
		// try{
		// if(offset > sr.offset){
		// beforeSelection = addSearchHighlight(highlighter, sr.offset, offset);
		// }
		// if(srEndIdx > endOffset){
		// endSelection = addSearchHighlight(highlighter, endOffset, srEndIdx);
		// }
		// }catch (final Exception e) {
		// e.printStackTrace();
		// }
		// break;
		// }
		// }
	}

	public final Highlighter.Highlight addSearchHighlight(final Highlighter highlighter, final int startIdx, final int endIdx)
			throws BadLocationException {
		if (startIdx == endIdx) {
			return null;
		}
		return (Highlighter.Highlight) highlighter.addHighlight(startIdx, endIdx, SEARCH_CODE_LINE_LIGHTER);
	}

	private final Vector<String> buildTableColumns() {
		final Vector<String> out = new Vector<String>(5);
		out.add("Type");
		out.add("Item Name");
		out.add("Line");

		return out;
	}

	String searchText;
	Pattern pattern;
	public final Vector<SearchResult> sameNodeList = new Vector<SearchResult>();

	public SearchDialog(final Designer frame) {
		super(frame, "Search Dialog", false);
		designer = frame;
		setAlwaysOnTop(true);
		searchComboBox.setEditable(true);

		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				clearSelection();

				final String searchText = (String) searchComboBox.getSelectedItem();
				final int oldIdx = searchModel.getIndexOf(searchText);
				if (oldIdx > 0) {
					searchModel.removeElementAt(oldIdx);
					searchModel.insertElementAt(searchText, 0);
				} else if (oldIdx == 0) {
				} else {
					searchModel.insertElementAt(searchText, 0);
				}

				try {
					if (regular.isSelected()) {
						pattern = Pattern.compile(searchText);
					} else {
						final String patternStr = toPatternStr(searchText);
						if (caseSensitive.isSelected() == false) {
							pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
						} else {
							pattern = Pattern.compile(patternStr);
						}
					}
				} catch (final Exception ex) {
					App.showMessageDialog(SearchDialog.this, ex.toString(), ResourceUtil.getErrorI18N(), App.ERROR_MESSAGE,
							App.getSysIcon(App.SYS_ERROR_ICON));
					return;
				}
				doSearch(searchText);
			}

			final char[] reservedPatternChars = { '$', '(', ')', '*', '+', '.', '[', ']', '?', '\\', '^', '{', '}', '|' };

			private final String toPatternStr(final String text) {
				final int reservedSize = reservedPatternChars.length;
				boolean hasReserved = false;
				for (int i = 0; i < reservedSize; i++) {
					if (text.indexOf(reservedPatternChars[i], 0) >= 0) {
						hasReserved = true;
						break;
					}
				}

				if (hasReserved == false) {
					return text;
				}

				final char[] textChars = text.toCharArray();
				final StringBuilder sb = StringBuilderCacher.getFree();
				for (int i = 0; i < textChars.length; i++) {
					boolean isResered = false;
					final char oneChar = textChars[i];
					for (int j = 0; j < reservedSize; j++) {
						if (reservedPatternChars[j] == oneChar) {
							isResered = true;
							break;
						}
					}
					if (isResered) {
						sb.append('\\');
					}
					sb.append(oneChar);
				}
				final String out = sb.toString();
				StringBuilderCacher.cycle(sb);
				return out;
			}
		});

		final JPanel textPanel = new JPanel(new GridLayout(2, 1));
		textPanel.add(new JLabel("search text :"));
		textPanel.add(searchComboBox);

		resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final TableColumnModel columnModel = resultTable.getColumnModel();
		final String columnWidths = PropertiesManager.getValue(PropertiesManager.p_SearchDialogColumnWidths);
		if (columnWidths != null) {
			final String[] widths = StringUtil.splitToArray(columnWidths, StringUtil.SPLIT_LEVEL_1_AT);
			for (int i = 0; i < widths.length; i++) {
				try {
					columnModel.getColumn(i).setPreferredWidth(Integer.parseInt(widths[i]));
				} catch (final Exception e) {
				}
			}

		}
		columnModel.addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnSelectionChanged(final ListSelectionEvent e) {
			}

			@Override
			public void columnRemoved(final TableColumnModelEvent e) {
			}

			@Override
			public void columnMoved(final TableColumnModelEvent e) {
			}

			@Override
			public void columnMarginChanged(final ChangeEvent e) {
				final int columnNum = columnModel.getColumnCount();
				final StringBuilder sb = StringBuilderCacher.getFree();
				for (int i = 0; i < columnNum; i++) {
					if (i > 0) {
						sb.append(StringUtil.SPLIT_LEVEL_1_AT);
					}
					sb.append(columnModel.getColumn(i).getWidth());
				}
				PropertiesManager.setValue(PropertiesManager.p_SearchDialogColumnWidths, sb.toString());
				PropertiesManager.saveFile();
				StringBuilderCacher.cycle(sb);
			}

			@Override
			public void columnAdded(final TableColumnModelEvent e) {
			}
		});
		resultTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2) {
					sameNodeList.clear();
					final SearchResult sr = resultData.get(resultTable.getSelectedRow());
					final int size = resultData.size();
					for (int i = 0; i < size; i++) {
						final SearchResult item = resultData.get(i);
						if (item.treeNode == sr.treeNode) {
							sameNodeList.add(item);
						}
					}

					designer.jumpSearchItem(SearchDialog.this, sr);
				}
			}
		});
		resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		final JPanel optionPanel = new JPanel(new GridLayout(2, 1));
		optionPanel.add(caseSensitive);
		optionPanel.add(regular);

		final JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(textPanel, BorderLayout.CENTER);
		topPanel.add(optionPanel, BorderLayout.LINE_END);
		topPanel.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.SOUTH);

		final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(closeButton);
		buttonPanel.add(searchButton);

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int scrollWidth = screenSize.width / 3;
		resultScrollPane.setPreferredSize(new Dimension(scrollWidth, scrollWidth / 3));

		resultTopPanel.add(resultLabel, BorderLayout.NORTH);
		resultTopPanel.add(resultScrollPane, BorderLayout.CENTER);

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		searchPanel.add(topPanel, c);

		c.anchor = GridBagConstraints.LINE_END;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		searchPanel.add(buttonPanel, c);

		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		searchPanel.add(new JSeparator(SwingConstants.HORIZONTAL), c);

		c.anchor = GridBagConstraints.CENTER;
		c.gridy = 3;
		c.fill = GridBagConstraints.BOTH;
		searchPanel.add(resultTopPanel, c);

		searchPanel.setBorder(new EmptyBorder(ClientDesc.hgap, ClientDesc.vgap, ClientDesc.hgap, ClientDesc.vgap));
		final Container contentPane = getContentPane();
		contentPane.add(searchPanel, BorderLayout.CENTER);

		final ActionListener escListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (resultData.size() > 0) {
					clearSelection();
					resultData.clear();
					ClassUtil.revalidate(resultTable);
					ClassUtil.revalidate(resultScrollPane);
				}
				ContextManager.getThreadPool().run(escRun);
			}

			final Runnable escRun = new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(50);
					} catch (final Exception e) {
					}
					setVisible(false);
				}
			};
		};

		closeButton.addActionListener(escListener);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				escListener.actionPerformed(null);
			}
		});

		rootPane.registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		rootPane.setDefaultButton(searchButton);
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "none");
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released ENTER"), "press");
		setResizable(false);
	}

	private final void clearSelection() {
		if (hcTextPane != null) {
			hcTextPane.searchDialog = null;
			hcTextPane = null;
		}
		final int size = resultData.size();
		if (size > 0) {
			for (int i = 0; i < size; i++) {
				final SearchResult searchResult = resultData.get(i);
				final Highlighter highlighter = searchResult.highlighter;
				if (highlighter != null) {
					// highlighter.removeAllHighlights();
					final Highlight highLight = searchResult.highLight;
					if (highLight != null) {
						highlighter.removeHighlight(highLight);
					}
				}
			}
		}
	}

	boolean isFirstShow = true;

	public final void popUp(final JComponent relativeTo) {
		final String clipText = ResourceUtil.getTxtFromClipboard();
		final int length = clipText.length();
		if (length > 0 && length < 30) {
			searchComboBox.setSelectedItem(clipText);
			searchComboBox.getEditor().selectAll();
		}
		this.setLocationRelativeTo(relativeTo);
		if (isFirstShow) {
			isFirstShow = false;
			pack();
			setVisible(true);
		} else {
			setVisible(true);
		}
	}

	public final void release() {
		dispose();
	}

	private final void doSearch(final String search) {
		resultData.clear();

		final DefaultMutableTreeNode root = designer.root;

		designer.traverseScriptNode(root, this);

		final int size = resultData.size();
		if (size == 0) {
			resultLabel.setText("no found!");
		} else {
			resultLabel.setText(size + " found.");
		}
		ClassUtil.revalidate(resultTable);
		ClassUtil.revalidate(resultScrollPane);
		if (size > 0) {
			resultTable.setRowSelectionInterval(0, 0);
		}
	}

	@Override
	public void next(final DefaultMutableTreeNode treeNode, final HPNode node, final int type, final String name, final String script) {
		char[] scriptChars = null;
		int scriptCharLen = 0;

		int startOff = 0;
		int startLineOff = 0;
		int startLineNo = 0;

		final Matcher matcher = pattern.matcher(script);
		while (matcher.find()) {
			final int offset = matcher.start();
			final int end = matcher.end();

			if (scriptChars == null) {
				scriptChars = script.toCharArray();
				scriptCharLen = scriptChars.length;
			}

			for (; startOff < offset;) {
				if (scriptChars[startOff++] == '\n') {
					startLineOff = startOff;
					startLineNo++;
				}
			}

			int lineEndOff = startOff;
			for (; lineEndOff < scriptCharLen;) {
				if (scriptChars[lineEndOff++] == '\n') {
					lineEndOff--;
					break;
				}
			}

			final SearchResult sr = new SearchResult(treeNode, type, name, offset, end - offset, startLineNo,
					new String(scriptChars, startLineOff, lineEndOff - startLineOff));
			resultData.add(sr);
		}
	}

}
