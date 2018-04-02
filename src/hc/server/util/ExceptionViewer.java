package hc.server.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import hc.App;
import hc.core.util.CCoreUtil;
import hc.core.util.LogManager;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.DisposeListener;
import hc.server.HCActionListener;
import hc.server.SingleJFrame;
import hc.server.ui.ClientDesc;
import hc.util.ResourceUtil;
import hc.util.StringBuilderCacher;

public class ExceptionViewer {
	private static final ArrayDeque<Object[]> array = new ArrayDeque<Object[]>(32);
	private static final Calendar calendar = Calendar.getInstance();
	private static boolean isPopup = false;

	public static final void notifyPopup(final boolean p) {
		CCoreUtil.checkAccess();

		isPopup = p;
	}

	public static final Vector<String> exception = new Vector<String>();
	public static final Vector<StackTraceElement[]> stacks = new Vector<StackTraceElement[]>();
	private static ExceptionViewer msbViewer;

	private JFrame dialog;
	private final JButton clearBtn = new JButton(ResourceUtil.get(8026), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
	private int currRow;
	private final ScrollTable tableException, tableStacks;
	private JScrollPane scrollPaneException, scrollPaneStacks;
	private final AbstractTableModel modelException, modelStacks;

	final Runnable refreshTable = new Runnable() {
		@Override
		public void run() {
			tableException.updateUI();
		}
	};

	public static void main(final String[] args) {
		ExceptionViewer.notifyPopup(true);
		ExceptionViewer.init();
		new Thread() {
			@Override
			public void run() {
				for (int i = 0; i < 5; i++) {
					ExceptionViewer.pushIn("Hello");
					try {
						Thread.sleep(5000);
					} catch (final Exception e) {
					}
				}
			}
		}.start();
	}

	private final void reset() {
		synchronized (exception) {
			exception.clear();
			stacks.clear();
			currRow = 0;
		}

		tableException.updateUI();
		tableStacks.updateUI();
	}

	public ExceptionViewer() {

		clearBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				reset();
			}
		}));

		modelException = new AbstractTableModel() {
			@Override
			public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
				return;
			}

			@Override
			public void removeTableModelListener(final TableModelListener l) {
			}

			@Override
			public boolean isCellEditable(final int rowIndex, final int columnIndex) {
				return false;
			}

			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				final String rowValue = exception.elementAt(rowIndex);
				return rowValue == null ? "" : rowValue;
			}

			@Override
			public int getRowCount() {
				return exception.size();
			}

			@Override
			public String getColumnName(final int columnIndex) {
				return "Exception Descrition";
			}

			@Override
			public int getColumnCount() {
				return 1;
			}

			@Override
			public Class<?> getColumnClass(final int columnIndex) {
				return String.class;
			}

			@Override
			public void addTableModelListener(final TableModelListener l) {
			}
		};

		modelStacks = new AbstractTableModel() {
			@Override
			public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
				return;
			}

			@Override
			public void removeTableModelListener(final TableModelListener l) {
			}

			@Override
			public boolean isCellEditable(final int rowIndex, final int columnIndex) {
				return false;
			}

			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				if (rowIndex >= getRowCount()) {
					return "";
				}
				final StackTraceElement[] value = stacks.elementAt(currRow);
				if (value == null) {
					return "";
				} else {
					if (rowIndex < value.length) {
						return value[rowIndex];
					} else {
						return "";
					}
				}
			}

			@Override
			public int getRowCount() {
				if (currRow < stacks.size()) {
					return stacks.elementAt(currRow).length;
				}
				return 0;
			}

			@Override
			public String getColumnName(final int columnIndex) {
				return "StackTraceElement";
			}

			@Override
			public int getColumnCount() {
				return 1;
			}

			@Override
			public Class<?> getColumnClass(final int columnIndex) {
				return String.class;
			}

			@Override
			public void addTableModelListener(final TableModelListener l) {
			}
		};

		tableException = new ScrollTable(modelException);
		tableStacks = new ScrollTable(modelStacks);

		tableException.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if (e.getValueIsAdjusting() == false) {
					final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
					if (lsm.isSelectionEmpty()) {
						return;
					} else {
						final int minIndex = lsm.getMinSelectionIndex();
						final int maxIndex = lsm.getMaxSelectionIndex();
						for (int i = minIndex; i <= maxIndex; i++) {
							if (lsm.isSelectedIndex(i)) {
								currRow = i;
								tableStacks.updateUI();
								return;
							}
						}
					}
				}
			}
		});

	}

	private final void show() {
		if (exception.size() == 0) {
			return;
		}

		if (dialog != null) {
			updateUI();
			return;
		}

		final JPanel panel = new JPanel(new GridBagLayout());
		final Insets insets = new Insets(ClientDesc.hgap, ClientDesc.hgap, 0, ClientDesc.vgap);
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = insets;
			c.gridy = 0;
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 0.7;
			c.weightx = 1.0;

			tableException.setRowSelectionAllowed(true);
			// panel.add(tableException, c);
			scrollPaneException = new JScrollPane(tableException, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPaneException.setPreferredSize(new Dimension(600, 200));
			panel.add(scrollPaneException, c);
		}

		{
			final GridBagConstraints c = new GridBagConstraints();
			c.insets = insets;
			c.gridy = 1;
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 0.3;
			c.weightx = 1.0;

			tableStacks.setRowSelectionAllowed(true);
			scrollPaneStacks = new JScrollPane(tableStacks, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPaneStacks.setPreferredSize(new Dimension(600, 300));
			panel.add(scrollPaneStacks, c);
		}

		tableException.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		tableStacks.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

		final JPanel total = new JPanel(new BorderLayout(0, 0));
		{
			final JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
			toolbar.add(clearBtn);
			// toolbar.setRequestFocusEnabled(false);

			total.add(toolbar, BorderLayout.NORTH);
			total.add(panel, BorderLayout.CENTER);
		}

		final ActionListener listener = null;
		final JButton closeBtn = App.buildDefaultCloseButton();
		dialog = (JFrame) App.showCenterPanelMain(total, 0, 0, "Exception List", false, closeBtn, null, listener, null, null, false, true,
				null, true, false);
		App.setDisposeListener(dialog, new DisposeListener() {
			@Override
			public void dispose() {
				dialog = null;
				SingleJFrame.removeJFrame(ExceptionViewer.class.getName());
			}
		});
		SingleJFrame.addJFrame(ExceptionViewer.class.getName(), dialog);
	}

	void updateUI() {
		if (dialog != null) {
			App.invokeLaterUI(refreshTable);
		}
	}

	private static final Thread daemon = new Thread("ExceptionViewWriter") {
		@Override
		public void run() {
			Object[] para;
			while (true) {
				synchronized (array) {
					para = array.pollFirst();
					if (para == null) {
						try {
							array.wait();
						} catch (final Exception e) {
						}
						continue;
					}
				}

				pushIn((String) para[0], (StackTraceElement[]) para[1]);
			}
		}

		private final void pushIn(final String paraMessage, final StackTraceElement[] ste) {
			final StringBuilder sb = StringBuilderCacher.getFree();

			calendar.setTimeInMillis(System.currentTimeMillis());
			sb.append(calendar.get(Calendar.HOUR_OF_DAY));
			sb.append(":");
			sb.append(calendar.get(Calendar.MINUTE));
			sb.append(":");
			sb.append(calendar.get(Calendar.SECOND));
			sb.append(".");
			sb.append(calendar.get(Calendar.MILLISECOND));
			sb.append(" ");

			sb.append(paraMessage);

			final String tmp = sb.toString();
			StringBuilderCacher.cycle(sb);

			synchronized (exception) {
				exception.add(tmp);
				stacks.add(ste);
			}

			if(ResourceUtil.isNonUIServer()) {
				return;
			}
			
			if (msbViewer == null) {
				msbViewer = new ExceptionViewer();
			}

			msbViewer.show();
		}
	};

	public static void pushIn(final String tmpMsg) {
		if (isPopup) {
			final Object[] para = { tmpMsg, Thread.currentThread().getStackTrace() };
			synchronized (array) {
				array.addLast(para);
				array.notify();
			}
		} else {
			if (LogManager.INI_DEBUG_ON) {
				System.err.println("Exception : " + tmpMsg);
				final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
				final int size = ste.length;
				for (int i = 0; i < size; i++) {
					System.err.print("\tat : " + ste[i] + "\n");
				}
			}
		}
	}

	public static void init() {
		CCoreUtil.checkAccess();

		daemon.setPriority(ThreadPriorityManager.LOWEST_PRIORITY);
		daemon.setDaemon(true);
		daemon.start();
	}
}