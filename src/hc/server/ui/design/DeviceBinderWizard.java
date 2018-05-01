package hc.server.ui.design;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import hc.App;
import hc.UIActionListener;
import hc.core.ContextManager;
import hc.core.util.ExceptionReporter;
import hc.server.HCActionListener;
import hc.server.HCWindowAdapter;
import hc.server.msb.ConverterInfo;
import hc.server.msb.DataDeviceCapDesc;
import hc.server.msb.DeviceBindInfo;
import hc.server.msb.DeviceCompatibleDescription;
import hc.server.msb.IoTSource;
import hc.server.msb.RealDeviceInfo;
import hc.server.ui.ServerUIUtil;
import hc.server.util.HCJDialog;
import hc.server.util.HCJFrame;
import hc.server.util.SafeDataManager;
import hc.util.ClassUtil;
import hc.util.ResourceUtil;

public class DeviceBinderWizard extends JPanel {
	final JFrame selfFrame;
	final ThreadGroup threadPoolToken;

	UIActionListener jbOKAction;
	UIActionListener cancelAction;

	JTree tree;
	boolean isDialogDispose = false;
	DefaultMutableTreeNode root;
	JScrollPane treeScrollPane;
	JPanel editPanel;
	JButton okButton = null;
	JButton cancelButton = null;
	JPanel noEditPanel, refDevDescPanel;
	JPanel bindPanel;
	DefaultMutableTreeNode selectedNode;
	JTextField convertLabel;
	JLabel refDevDescLabel;
	JButton chooseConverterButton, clearConverterButton;
	JTextField realDevLabel;
	JButton chooseRealDevButton;
	JButton searchUnbindButton;

	JTree converterTree, devTree;

	private final void enableSelectDevCon(final boolean enable) {
		chooseConverterButton.setEnabled(enable && converterTree != null);
		clearConverterButton.setEnabled(enable && converterTree != null);
		chooseRealDevButton.setEnabled(enable && devTree != null);
	}

	public void save() {
		devBinderWizSource.save();
	}
	
	public void cancel() {
		devBinderWizSource.cancel();
	}
	
	/**
	 * 
	 * @param devBinderSource
	 * @param owner
	 * @return true means save OK.
	 */
	public static final boolean enterBindUI(final DeviceBinderWizSource devBinderSource, final JFrame owner, final String deployIPMaybeNull) {
		final Boolean[] isDoneBind = { null };
		ContextManager.getThreadPool().run(new Runnable() {
			@Override
			public void run() {
				SafeDataManager.startSafeBackupProcess(true, false);
				DeviceBinderWizard out = null;
				try {
					out = DeviceBinderWizard.getInstance(devBinderSource, false, owner, deployIPMaybeNull);
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
					isDoneBind[0] = false;
					LinkProjectManager.reloadLinkProjects();
					synchronized (isDoneBind) {
						isDoneBind.notify();
					}
					
					return;
				}

				final DeviceBinderWizard binder = out;
				final UIActionListener jbOKAction = new UIActionListener() {
					@Override
					public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
						window.dispose();

						isDoneBind[0] = true;
						binder.save();
						
						synchronized (isDoneBind) {
							isDoneBind.notify();
						}
					}
				};
				final UIActionListener cancelAction = new UIActionListener() {
					@Override
					public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
						window.dispose();

						// 以下代码，请与上行的DeviceBinderWizard.getInstance保持一致
						isDoneBind[0] = false;
						binder.cancel();
						
						synchronized (isDoneBind) {
							isDoneBind.notify();
						}
					}
				};
				binder.setButtonAction(jbOKAction, cancelAction);
				binder.show();
			}
		});
		synchronized (isDoneBind) {
			if(isDoneBind[0] == null) {
				try {
					isDoneBind.wait();
				} catch (final InterruptedException e) {
				}
			}
		}
		
		return isDoneBind[0];
	}

	private void switchPanel(final BindDeviceNode bindNode, final boolean isRefresh) {
		if (isRefresh) {
			refreshPanel(bindNode);
			return;
		}

		editPanel.removeAll();
		if (bindNode.isRefDevType()) {
			editPanel.add(bindPanel, BorderLayout.CENTER);

			refreshPanel(bindNode);
		} else {
			editPanel.add(noEditPanel, BorderLayout.CENTER);
		}

		editPanel.validate();
		ClassUtil.revalidate(editPanel);
		editPanel.repaint();
	}

	private void refreshPanel(final BindDeviceNode bindNode) {
		enableSelectDevCon(true);

		final String buildConverterFieldString = buildConverterFieldString(bindNode.convBind);
		clearConverterButton.setEnabled(buildConverterFieldString.length() != 0);

		convertLabel.setText(buildConverterFieldString);
		realDevLabel.setText(buildRealDeviceFieldString(bindNode.realDevBind));
		refDevDescLabel.setText(bindNode.devCapDescStr);
	}

	private String buildConverterFieldString(final ConverterInfo info) {
		if (info == null) {
			return "";
		}
		return info.name + " : " + info.proj_id;
	}

	private String buildRealDeviceFieldString(final RealDeviceInfo info) {
		if (info == null) {
			return "";
		}
		return info.dev_id + " { " + info.dev_name + " : " + info.proj_id + " }";
	}

	public void setButtonAction(final UIActionListener jbOKAction, final UIActionListener cancelAction) {
		this.jbOKAction = jbOKAction;
		this.cancelAction = cancelAction;

		{
			final JRootPane rootPane = selfFrame.getRootPane();
			final ActionListener anAction = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					cancelAction.actionPerformed(selfFrame, okButton, cancelButton);
				}
			});
			rootPane.registerKeyboardAction(anAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		}
	}

	final DeviceBinderWizSource devBinderWizSource;

	private static DeviceBinderWizard instance;

	public static DeviceBinderWizard getInstance(final DeviceBinderWizSource devBinderSource, final boolean isNoCancelOperation, final Frame owner,
			final String deployIPMaybeNull) throws Exception {
		if (instance != null && instance.isDialogDispose == false) {
		} else {
			// 不能使用App.invokeLater，会产生反射权限不够
			instance = new DeviceBinderWizard(devBinderSource, isNoCancelOperation, owner, deployIPMaybeNull);
		}
		return instance;
	}

	private DeviceBinderWizard(final DeviceBinderWizSource devBindSrc, final boolean isNoCancelOperation, final Frame owner,
			final String deployIPMaybeNull) throws Exception {
		threadPoolToken = App.getThreadPoolToken();
		
		// 由于绑定窗口会可能引发Tip事件，如果是JDialog会导致焦点无法取得，所以改为JFrame
		final String bindManager = ResourceUtil.get(8000);
		final String titleStr;
		if(deployIPMaybeNull == null) {
			titleStr = bindManager;
		}else {
			titleStr = ResourceUtil.buildMaoHaoBetween(bindManager, deployIPMaybeNull);
		}
		selfFrame = new HCJFrame(titleStr) {
			@Override
			public void dispose() {
				super.dispose();
				isDialogDispose = true;
				instance = null;
			}
		};

		this.devBinderWizSource = devBindSrc;

		// 以下两段结果被其它模块使用，所以提前
		buildConverterTree();// 可能抛出异常，需中断本构造化
		buildDevTree();// 可能抛出异常，需中断本构造化

		okButton = App.buildDefaultOKButton();
		okButton.setToolTipText(ResourceUtil.get(8001));
		okButton.setEnabled(false);
		cancelButton = App.buildDefaultCancelButton();
		searchUnbindButton = new JButton(ResourceUtil.get(8002), Designer.iconDeviceGray);
		searchUnbindButton.setToolTipText(ResourceUtil.get(8003));
		searchUnbindButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				jumpToFirstUnbindNode();
			}
		}, threadPoolToken));

		root = new DefaultMutableTreeNode();

		tree = new JTree(root);
		tree.setRootVisible(false);

		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new BindTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
					final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
				final Component compo = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
				final Object o = ((DefaultMutableTreeNode) value).getUserObject();
				if (o instanceof BindDeviceNode) {
					final BindDeviceNode node = (BindDeviceNode) o;
					if (node.type == BindDeviceNode.REAL_DEV_ID_NODE) {
						final ConverterInfo cbi = node.convBind;
						final RealDeviceInfo rdbi = node.realDevBind;

						String text = node.ref_dev_ID;

						if (node.isBindedConverter()) {
							// text += " ~ " + buildConverterFieldString(cbi) +
							// " ~ ";
							text += " ~ " + (cbi.name) + " ~ ";
						}
						if (node.isBindedRealDevice()) {
							if (node.isBindedConverter()) {
								text += rdbi.dev_id;// buildRealDeviceFieldString(rdbi);
							} else {
								// 直联
								text += " <-> " + rdbi.dev_id;// buildRealDeviceFieldString(rdbi);
							}
						}
						((JLabel) compo).setText(text);
					}
				}
				return compo;
			}
		});

		noEditPanel = new JPanel();

		final int columns = 25;

		refDevDescLabel = new JLabel("");
		refDevDescLabel.setMinimumSize(new Dimension(200, 200));

		bindPanel = new JPanel();
		bindPanel.setLayout(new GridBagLayout());

		{
			refDevDescPanel = new JPanel(new BorderLayout());
			refDevDescPanel.setBorder(new TitledBorder(ResourceUtil.get(8013)));
			// refDevDescLabel.setAlignmentY(Component.TOP_ALIGNMENT );
			refDevDescPanel.add(refDevDescLabel, BorderLayout.CENTER);//注：不对lable加JScrollPane，共用外部
		}

		{
			final GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.weighty = 1.0;
			// c.anchor = GridBagConstraints.PAGE_START;
			c.fill = GridBagConstraints.BOTH;
			bindPanel.add(refDevDescPanel, c);
		}

		convertLabel = new JTextField("");
		convertLabel.setEditable(false);
		convertLabel.setColumns(columns);
		final String funcName = ResourceUtil.get(8004);
		chooseConverterButton = new JButton(funcName);
		clearConverterButton = new JButton(ResourceUtil.get(8005));
		{
			final JPanel subPanel = new JPanel();
			subPanel.setLayout(new BorderLayout());
			subPanel.add(new JLabel("", Designer.iconConverter, SwingConstants.LEADING), BorderLayout.LINE_START);
			subPanel.add(convertLabel, BorderLayout.CENTER);
			subPanel.add(clearConverterButton, BorderLayout.LINE_END);

			final JPanel descPanel = new JPanel();
			descPanel.setLayout(new BorderLayout());
			descPanel.add(new JLabel(ResourceUtil.get(8006)), BorderLayout.CENTER);

			final JComponent[] components = { subPanel, chooseConverterButton, descPanel };
			final JPanel buildNorthPanel = ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER);
			buildNorthPanel.setBorder(new TitledBorder(ResourceUtil.get(8010)));

			{
				final GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = 1;
				c.weightx = 1.0;
				c.fill = GridBagConstraints.HORIZONTAL;
				bindPanel.add(buildNorthPanel, c);
			}
		}
		clearConverterButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final BindDeviceNode bdn = (BindDeviceNode) selectedNode.getUserObject();
				bdn.convBind = null;
				switchPanel(bdn, true);

				tree.updateUI();
			}
		}, threadPoolToken));
		chooseConverterButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				// System.out.println("call chooseConverterButton
				// actionPerformed" + e.toString());
				final ActionListener listener = new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) converterTree.getSelectionPath()
								.getLastPathComponent();
						final BindDeviceNode convBdn = (BindDeviceNode) node.getUserObject();

						final ConverterInfo cbi = new ConverterInfo();
						cbi.proj_id = convBdn.projID;
						cbi.name = convBdn.lever2Name;

						final BindDeviceNode bdn = (BindDeviceNode) selectedNode.getUserObject();
						bdn.convBind = cbi;
						switchPanel(bdn, true);

						tree.updateUI();

						updateReferenceDev();// 因为有可能只移动或添加Converter，所以要刷新
					}
				}, threadPoolToken);

				showTreeSelectDialog(selfFrame, chooseConverterButton, funcName, converterTree, BindDeviceNode.CONVERTER_NODE, listener);
			}
		}, threadPoolToken));
		realDevLabel = new JTextField("");
		realDevLabel.setEditable(false);
		realDevLabel.setColumns(columns);
		final String realDevFuncName = ResourceUtil.get(8007);
		chooseRealDevButton = new JButton(realDevFuncName);
		{
			final JPanel subPanel = new JPanel();
			subPanel.setLayout(new BorderLayout());
			subPanel.add(new JLabel("", Designer.iconDevice, SwingConstants.LEADING), BorderLayout.LINE_START);
			subPanel.add(realDevLabel, BorderLayout.CENTER);

			final JPanel descPanel = new JPanel();
			descPanel.setLayout(new BorderLayout());
			descPanel.add(new JLabel(ResourceUtil.get(8008)), BorderLayout.CENTER);

			final JComponent[] components = { subPanel, chooseRealDevButton, descPanel };
			final JPanel buildNorthPanel = ServerUIUtil.buildNorthPanel(components, 0, BorderLayout.CENTER);
			buildNorthPanel.setBorder(new TitledBorder(ResourceUtil.get(8011)));

			{
				final GridBagConstraints c = new GridBagConstraints();
				c.gridy = 2;
				c.gridx = 0;
				c.weightx = 1.0;
				c.fill = GridBagConstraints.HORIZONTAL;
				bindPanel.add(buildNorthPanel, c);
			}
		}
		chooseRealDevButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final ActionListener listener = new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) devTree.getSelectionPath().getLastPathComponent();
						final BindDeviceNode realDevBdn = (BindDeviceNode) node.getUserObject();

						final RealDeviceInfo rdbi = new RealDeviceInfo();
						rdbi.proj_id = realDevBdn.projID;
						rdbi.dev_name = realDevBdn.lever2Name;
						rdbi.dev_id = realDevBdn.ref_dev_ID;

						final BindDeviceNode bdn = (BindDeviceNode) selectedNode.getUserObject();
						bdn.realDevBind = rdbi;
						switchPanel(bdn, true);

						tree.updateUI();

						updateReferenceDev();
					}
				}, threadPoolToken);

				showTreeSelectDialog(selfFrame, chooseRealDevButton, realDevFuncName, devTree, BindDeviceNode.REAL_DEV_ID_NODE, listener);
			}
		}, threadPoolToken));

		// 有可能抛出异常
		buildTree();

		Designer.expandTree(tree);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(final TreeSelectionEvent e) {
				switchTreeNode(e.getPath().getLastPathComponent());
			}
		});
		treeScrollPane = new JScrollPane(tree);

		final JPanel treePanel = new JPanel();
		treePanel.setBorder(new TitledBorder(ResourceUtil.get(8009)));
		treePanel.setLayout(new BorderLayout());
		treePanel.add(searchUnbindButton, BorderLayout.NORTH);
		treePanel.add(treeScrollPane, BorderLayout.CENTER);

		editPanel = new JPanel();
		// editPanel.setBorder(new TitledBorder("Bind Area :"));
		editPanel.setLayout(new BorderLayout());
		editPanel.add(bindPanel, BorderLayout.CENTER);

		JComponent centerPanel = null;
		JSplitPane splitPane = null;
		if(ResourceUtil.isAndroidServerPlatform()) {
			final JPanel cPanel = new JPanel(new BorderLayout());
			cPanel.add(treePanel, BorderLayout.WEST);
			cPanel.add(new JScrollPane(editPanel), BorderLayout.CENTER);
			centerPanel = cPanel;
		}else {
			splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, new JScrollPane(editPanel));// 如Android标准屏时，不足
			centerPanel = splitPane;
		}
		setLayout(new BorderLayout());
		add(centerPanel, BorderLayout.CENTER);

		jumpToFirstUnbindNode();

		okButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				jbOKAction.actionPerformed(selfFrame, okButton, cancelButton);
			}
		}, threadPoolToken));
		cancelButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				cancelAction.actionPerformed(selfFrame, okButton, cancelButton);
			}
		}, threadPoolToken));
		final ActionListener quitAction = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				selfFrame.dispose();
				if (cancelAction != null) {
					cancelAction.actionPerformed(selfFrame, okButton, cancelButton);
				}
			}
		}, threadPoolToken);

		if (isNoCancelOperation) {
			cancelButton.setVisible(false);
		}
		selfFrame.setLayout(new BorderLayout());
		selfFrame.add(this, BorderLayout.CENTER);
		selfFrame.add(buildButtonArea(okButton, cancelButton), BorderLayout.SOUTH);

		if (isNoCancelOperation == false) {
			selfFrame.getRootPane().registerKeyboardAction(quitAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
					JComponent.WHEN_IN_FOCUSED_WINDOW);
		}
		selfFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		if (isNoCancelOperation == false) {
			selfFrame.addWindowListener(new HCWindowAdapter(new Runnable() {
				@Override
				public void run() {
					quitAction.actionPerformed(null);
				}
			}, threadPoolToken));
		}

		selfFrame.setSize(new Dimension(1200, 800));

		if(splitPane != null) {
			splitPane.setDividerLocation(1 - 0.618);
		}
	}

	@Override
	public void show() {
		App.showCenter(selfFrame);

		updateReferenceDev();
	}

	private void updateReferenceDev() {
		// ...
		final DefaultMutableTreeNode node = searchFirstUnbindNode(root);
		if (node == null) {
			final boolean isOldEnable = okButton.isEnabled();
			okButton.setEnabled(true);
			searchUnbindButton.setEnabled(false);

			if (isOldEnable == false) {
				if (ResourceUtil.isJ2SELimitFunction()) {
					// 动画跳动“确定”
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							final String emptyStr = "  ";
							final String text = okButton.getText().trim();
							final int sleepMill = 500;
							while (isDialogDispose == false) {
								okButton.setText(emptyStr + text);
								try {
									Thread.sleep(sleepMill);
								} catch (final Exception e) {
								}
								okButton.setText(text + emptyStr);
								try {
									Thread.sleep(sleepMill);
								} catch (final Exception e) {
								}
							}
						}
					});
				}
			}
		}
	}

	private JPanel buildButtonArea(final JButton okBtn, final JButton cancelBtn) {
		final JPanel base = new JPanel();
		base.setLayout(new GridBagLayout());

		final GridBagConstraints labelConstraints = new GridBagConstraints();
		final GridBagConstraints separatorConstraint = new GridBagConstraints();

		labelConstraints.weightx = 0.0;
		labelConstraints.fill = GridBagConstraints.NONE;
		labelConstraints.anchor = GridBagConstraints.LINE_END;
		labelConstraints.gridwidth = 1;
		labelConstraints.gridwidth = GridBagConstraints.REMAINDER;
		labelConstraints.insets = new Insets(0, 5, 5, 5);

		separatorConstraint.weightx = 1.0;
		separatorConstraint.fill = GridBagConstraints.HORIZONTAL;
		separatorConstraint.gridwidth = GridBagConstraints.REMAINDER;
		separatorConstraint.insets = new Insets(5, 5, 0, 5);

		final JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		base.add(separator, separatorConstraint);

		final JPanel subPanel = new JPanel();
		subPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
		subPanel.add(okBtn);
		subPanel.add(cancelBtn);
		base.add(subPanel, labelConstraints);

		return base;
	}

	private boolean checkTreeBindDone() {
		return checkTreeBindDone(root);
	}

	private boolean checkTreeBindDone(final TreeNode node) {
		final Object userObj = ((DefaultMutableTreeNode) node).getUserObject();
		if (userObj != null && userObj instanceof BindDeviceNode) {
			final BindDeviceNode bdn = (BindDeviceNode) userObj;
			if (bdn.isRefDevType()) {
				return bdn.isFinishBind();
			}
		}

		final int size = node.getChildCount();
		for (int i = 0; i < size; i++) {
			final boolean out = checkTreeBindDone(node.getChildAt(i));
			if (out == false) {
				return false;
			}
		}

		return true;
	}

	private void jumpToFirstUnbindNode() {
		final DefaultMutableTreeNode unbindNode = searchFirstUnbindNode(root);
		if (unbindNode != null) {
			selectedNode = unbindNode;
			switchPanel((BindDeviceNode) selectedNode.getUserObject(), false);
			Designer.jumpToNode(selectedNode, (DefaultTreeModel) tree.getModel(), tree);
			enableSelectDevCon(true);
		} else {
			enableSelectDevCon(false);
		}
	}

	private DefaultMutableTreeNode searchFirstUnbindNode(final DefaultMutableTreeNode node) {
		final int size = node.getChildCount();
		for (int i = 0; i < size; i++) {
			final DefaultMutableTreeNode sub = (DefaultMutableTreeNode) node.getChildAt(i);
			final BindDeviceNode bdn = (BindDeviceNode) sub.getUserObject();
			if (bdn.isRefDevType() && bdn.isFinishBind() == false) {
				return sub;
			}
			final DefaultMutableTreeNode deepNode = searchFirstUnbindNode(sub);
			if (deepNode != null) {
				return deepNode;
			}
		}
		return null;
	}

	private final void buildDevTree() throws Exception {
		final DevTree devTreeSrc = devBinderWizSource.buildDevTree();
		
		final Vector<BindDeviceNode> realDevlist = devTreeSrc.realDevList;
		if (realDevlist == null || realDevlist.size() == 0) {
			return;
		}

		final DefaultMutableTreeNode dev_root = new DefaultMutableTreeNode();
		devTree = new JTree(dev_root);
		devTree.setRootVisible(false);

		devTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		devTree.setCellRenderer(new BindTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
					final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
				final Component compo = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
				final Object o = ((DefaultMutableTreeNode) value).getUserObject();
				if (o instanceof BindDeviceNode) {
					final BindDeviceNode node = (BindDeviceNode) o;
					if (node.type == BindDeviceNode.DEV_NODE) {
						((JLabel) compo).setIcon(Designer.iconDeviceGray);
					} else if (node.type == BindDeviceNode.REAL_DEV_ID_NODE) {
						((JLabel) compo).setIcon(Designer.iconDevice);
					}
				}
				return compo;
			}
		});

		final HashMap<String, DefaultMutableTreeNode> proj_map = new HashMap<String, DefaultMutableTreeNode>();
		final HashMap<String, DefaultMutableTreeNode> dev_name_map = new HashMap<String, DefaultMutableTreeNode>();

		final int projSize = devTreeSrc.projectList.size();
		for (int i = 0; i < projSize; i++) {
			final DefaultMutableTreeNode projNode = new DefaultMutableTreeNode();
			final BindDeviceNode userObject = devTreeSrc.projectList.get(i);
			projNode.setUserObject(userObject);
			dev_root.add(projNode);

			proj_map.put(userObject.projID, projNode);
		}
		
		final int devSize = devTreeSrc.devList.size();
		for (int i = 0; i < devSize; i++) {
			final BindDeviceNode bdn = devTreeSrc.devList.get(i);
			
			final DefaultMutableTreeNode devNameNode = new DefaultMutableTreeNode();
			devNameNode.setUserObject(bdn);
			proj_map.get(bdn.projID).add(devNameNode);
			dev_name_map.put(bdn.projID + bdn.lever2Name, devNameNode);
		}
		
		final int realDevSize = realDevlist.size();
		for (int i = 0; i < realDevSize; i++) {
			final BindDeviceNode bdn = realDevlist.get(i);

			final DefaultMutableTreeNode realDevIDNode = new DefaultMutableTreeNode();
			final BindDeviceNode userObject = bdn;
			realDevIDNode.setUserObject(userObject);
			dev_name_map.get(bdn.projID + bdn.lever2Name).add(realDevIDNode);
		}
	}

	private void buildConverterTree() throws Exception {
		final ConverterTree converterTreeSrc = devBinderWizSource.buildConverterTree();
		if (converterTreeSrc.projectList.size() == 0) {
			return;
		}

		final DefaultMutableTreeNode conv_root = new DefaultMutableTreeNode();
		converterTree = new JTree(conv_root);
		converterTree.setRootVisible(false);

		converterTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		converterTree.setCellRenderer(new BindTreeCellRenderer());

		final HashMap<String, DefaultMutableTreeNode> proj_map = new HashMap<String, DefaultMutableTreeNode>();
		
		final Vector<BindDeviceNode> projectList = converterTreeSrc.projectList;
		for (int i = 0; i < projectList.size(); i++) {//加挂一级节点project
			final DefaultMutableTreeNode projNode = new DefaultMutableTreeNode();
			final BindDeviceNode elementAt = projectList.elementAt(i);
			projNode.setUserObject(elementAt);
			conv_root.add(projNode);

			proj_map.put(elementAt.projID, projNode);
		}

		final Vector<BindDeviceNode> converterList = converterTreeSrc.converterList;
		for (int i = 0; i < converterList.size(); i++) {//加挂二级节点converter
			final BindDeviceNode converterNodeSrc = converterList.get(i);
			final DefaultMutableTreeNode projNode = proj_map.get(converterNodeSrc.projID);

			final DefaultMutableTreeNode converterNode = new DefaultMutableTreeNode();
			converterNode.setUserObject(converterNodeSrc);
			projNode.add(converterNode);
		}
	}

	private void buildTree() throws Exception {
		final BDNTree bdnTree = devBinderWizSource.buildTree();
		
		final Vector<String> list = bdnTree.projectList;
		for (int i = 0; i < list.size(); i++) {
			final String projID = list.get(i);

			final Vector<String> robotList = bdnTree.bdnRobotList.get(i).robotIDList;
			final BDNTreeNode treeNode = bdnTree.treeNodeList.get(i);

			final DefaultMutableTreeNode projNode = new DefaultMutableTreeNode();
			projNode.setUserObject(treeNode.projectNode);
			root.add(projNode);

			for (int j = 0; j < robotList.size(); j++) {
				final String robotID = robotList.get(j);
				
				final DeviceBindInfo[] refList = treeNode.refList.get(j);
				final int refSize = refList.length;

				if (refSize == 0) {
					continue;
				}

				final BindDeviceNode bdnForRobot = treeNode.bdnForRobot.get(j);
				final DefaultMutableTreeNode robotNode = new DefaultMutableTreeNode();
				robotNode.setUserObject(bdnForRobot);
				projNode.add(robotNode);

				final BindDeviceNode[] devBelowRobot = treeNode.devBelowRobotList.get(j);
				for (int k = 0; k < refSize; k++) {
					final DefaultMutableTreeNode devNode = new DefaultMutableTreeNode();
					devNode.setUserObject(devBelowRobot[k]);
					robotNode.add(devNode);
				}
			}
		}
	}

	HashMap<JTree, MouseListener> mapTreeMouseSelectListener = new HashMap<JTree, MouseListener>();
	HashMap<JTree, KeyListener> mapTreeKeySelectListener = new HashMap<JTree, KeyListener>();
	HashMap<JTree, TreeSelectionListener> mapTreeSelectionListener = new HashMap<JTree, TreeSelectionListener>();

	private void showTreeSelectDialog(final JFrame parent, final JComponent relativeTo, final String title, final JTree tree,
			final int leafNodeType, final ActionListener listener) {
		tree.setSelectionRow(0);

		final JButton ok = App.buildDefaultOKButton();
		ok.setEnabled(false);

		Designer.expandTree(tree);

		MouseListener lastTreeMouseSelectListener = mapTreeMouseSelectListener.get(tree);
		if (lastTreeMouseSelectListener != null) {
			tree.removeMouseListener(lastTreeMouseSelectListener);
		}
		KeyListener lastTreeKeySelectListener = mapTreeKeySelectListener.get(tree);
		if (lastTreeKeySelectListener != null) {
			tree.removeKeyListener(lastTreeKeySelectListener);
		}
		TreeSelectionListener lastTreeSelectionListener = mapTreeSelectionListener.get(tree);
		if (lastTreeSelectionListener != null) {
			tree.removeTreeSelectionListener(lastTreeSelectionListener);
		}

		final JLabel desc = new JLabel();

		lastTreeKeySelectListener = new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				final TreePath tp = tree.getSelectionPath();
				selectTreeNodeAction(tree, leafNodeType, ok, desc, tp, e.getKeyCode() == KeyEvent.VK_ENTER);
			}

			@Override
			public void keyPressed(final KeyEvent e) {
			}
		};
		tree.addKeyListener(lastTreeKeySelectListener);
		mapTreeKeySelectListener.put(tree, lastTreeKeySelectListener);

		lastTreeSelectionListener = new TreeSelectionListener() {
			@Override
			public void valueChanged(final TreeSelectionEvent e) {
				final TreePath tp = tree.getSelectionPath();
				selectTreeNodeAction(tree, leafNodeType, ok, desc, tp, false);
			}
		};
		tree.addTreeSelectionListener(lastTreeSelectionListener);
		mapTreeSelectionListener.put(tree, lastTreeSelectionListener);

		lastTreeMouseSelectListener = new MouseListener() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				final TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
				selectTreeNodeAction(tree, leafNodeType, ok, desc, tp, e.getClickCount() == 2);
			}

			@Override
			public void mousePressed(final MouseEvent e) {
			}

			@Override
			public void mouseExited(final MouseEvent e) {
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
			}

			@Override
			public void mouseClicked(final MouseEvent e) {
			}
		};
		tree.addMouseListener(lastTreeMouseSelectListener);
		mapTreeMouseSelectListener.put(tree, lastTreeMouseSelectListener);

		final JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(title));
		panel.setLayout(new GridLayout(2, 1));
		panel.add(new JScrollPane(tree));
		final JPanel descPanel = new JPanel(new BorderLayout());
		descPanel.setBorder(new TitledBorder("Description :"));
		desc.setMinimumSize(new Dimension(200, 200));
		descPanel.add(new JScrollPane(desc), BorderLayout.CENTER);
		panel.add(descPanel);

		App.showCenterPanelMain(panel, 600, 500, title, true, ok, null, listener, null, parent, true, false, null, true, false);// relativeTo会导致dev偏移到左边，故关闭为居中
	}

	public void selectTreeNodeAction(final JTree tree, final int leafNodeType, final JButton ok, final JLabel desc, final TreePath tp,
			final boolean actionOK) {
		if (tp == null) {
			return;
		}

		App.invokeLaterUI(new Runnable() {
			@Override
			public void run() {
				final Object obj = tp.getLastPathComponent();
				if (obj != null && (obj instanceof DefaultMutableTreeNode)) {
					final DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
					final BindDeviceNode bdn = (BindDeviceNode) node.getUserObject();
					if (bdn.type == leafNodeType) {
						ok.setEnabled(true);

						if (ok.isEnabled() && actionOK) {
							ContextManager.getThreadPool().run(new Runnable() {
								@Override
								public void run() {
									ok.doClick();
								}
							});

							return;
						}

						if (leafNodeType == BindDeviceNode.CONVERTER_NODE) {
							try {
								final ConverterInfo convertInfo = bdn.convBind;
								desc.setText(convertInfo.converterInfoDesc);
							} catch (final Throwable e) {
								desc.setText("");
							}
						} else if (leafNodeType == BindDeviceNode.REAL_DEV_ID_NODE) {
							try {
								final RealDeviceInfo realDevInfo = bdn.realDevBind;
								final DataDeviceCapDesc devDesc = realDevInfo.deviceCapDesc;
								desc.setText(BindRobotSource.buildDevCapDescStr(devDesc));
							} catch (final Throwable e) {
								desc.setText("");
							}
						} else {
							desc.setText("");
						}

						return;
					} else {
						desc.setText("");
					}
				}
				ok.setEnabled(false);
			}
		});
	}

	private void switchTreeNode(final Object obj) {
		if (obj != null && (obj instanceof DefaultMutableTreeNode)) {
			selectedNode = (DefaultMutableTreeNode) obj;
		} else {
			selectedNode = null;
		}

		if (selectedNode != null) {
			final Object userObject = selectedNode.getUserObject();
			if (userObject instanceof BindDeviceNode) {
				switchPanel((BindDeviceNode) userObject, false);
			}
		}
	}

}

class BindNode implements Serializable {
	private static final long serialVersionUID = 1L;

	int type;

	public static final int PROJ_NODE = 1;
	public static final int ROBOT_NODE = 2;
	public static final int DEV_NODE = 3;
	public static final int CONVERTER_NODE = 4;
	public static final int REAL_DEV_ID_NODE = 5;
}

class BindDeviceNode extends BindNode {
	private static final long serialVersionUID = 1L;

	// 专用
	BindDeviceNode(final RealDeviceInfo realDevBind, final ConverterInfo convBind) {
		this.realDevBind = realDevBind;
		this.convBind = convBind;
	}

	BindDeviceNode(final MobiUIResponsor mobiResp, final int type, final String projID, final String robotOrConverterID,
			final DeviceBindInfo devBindInfo, final IoTSource rs) {
		this.type = type;
		this.projID = projID;
		this.lever2Name = robotOrConverterID;
		if (devBindInfo != null) {
			this.ref_dev_ID = devBindInfo.ref_dev_id;

			if (rs != null) {
				try {
					final RealDeviceInfo rdbi = rs.getRealDeviceBindInfo(devBindInfo.bind_id);
					if (BindManager.checkSrcOnRealDeviceBindInfo(mobiResp, rdbi)) {
						realDevBind = rdbi;
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}

				try {
					final ConverterInfo cbi = rs.getConverterBindInfo(devBindInfo.bind_id);
					if (BindManager.checkSrcOnConverterBindInfo(mobiResp, cbi)) {
						convBind = cbi;
					}
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}

				final ProjResponser projResponser = mobiResp.getProjResponser(projID);
				
				try {
					final DeviceCompatibleDescription devCompDesc = rs.getDeviceCompatibleDescByRobotName(projID, robotOrConverterID, ref_dev_ID);
					final DataDeviceCapDesc devDesc = rs.getDataForDeviceCompatibleDesc(projResponser, devCompDesc);
					devCapDescStr = BindRobotSource.buildDevCapDescStr(devDesc);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	String devCapDescStr;

	public boolean isBindedRealDevice() {
		return realDevBind != null;
	}

	public boolean isBindedConverter() {
		return convBind != null;
	}

	RealDeviceInfo realDevBind;
	ConverterInfo convBind;
//	DeviceCompatibleDescription devCompDesc;
	String DevCapDescStr;

	String projID;
	String lever2Name;
	String ref_dev_ID = "";

	public boolean isFinishBind() {
		return realDevBind != null;
	}

	public boolean isRefDevType() {
		return type == REAL_DEV_ID_NODE;
	}
}

class BindTreeCellRenderer extends DefaultTreeCellRenderer {
	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected, final boolean expanded,
			final boolean leaf, final int row, final boolean hasFocus) {
		final Component compo = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		final Object o = ((DefaultMutableTreeNode) value).getUserObject();
		if (o instanceof BindDeviceNode) {
			final BindDeviceNode node = (BindDeviceNode) o;
			if (node.type == BindDeviceNode.PROJ_NODE) {
				((JLabel) compo).setText(node.projID);
				((JLabel) compo).setIcon(Designer.iconRoot);
			} else if (node.type == BindDeviceNode.ROBOT_NODE) {
				((JLabel) compo).setText(node.lever2Name);
				((JLabel) compo).setIcon(Designer.iconRobot);
			} else if (node.type == BindDeviceNode.CONVERTER_NODE) {
				((JLabel) compo).setText(node.lever2Name);
				((JLabel) compo).setIcon(Designer.iconConverter);
			} else if (node.type == BindDeviceNode.DEV_NODE) {
				((JLabel) compo).setText(node.lever2Name);
				((JLabel) compo).setIcon(Designer.iconDevice);
			} else if (node.type == BindDeviceNode.REAL_DEV_ID_NODE) {
				((JLabel) compo).setText(node.ref_dev_ID);
				if (node.isFinishBind()) {
					((JLabel) compo).setIcon(Designer.iconDevice);
				} else {
					((JLabel) compo).setIcon(Designer.iconDeviceGray);
				}
			}
		}

		return compo;
	}
}

class DeviceChooser extends HCJDialog {
	DeviceBindInfo di;

	public DeviceChooser(final Window parent) {
		super(parent, "Device Chooser");
	}

	public DeviceBindInfo getChooseDevice() {
		return null;
	}

	public void setDevice(final DeviceBindInfo di) {

	}

	public void setVisible(final boolean b, final Component relativeTo) {
		this.setLocationRelativeTo(relativeTo);
		super.setVisible(b);
	}
}

class RobotTreeModel extends DefaultTreeModel {
	public RobotTreeModel(final TreeNode root, final boolean asksAllowsChildren) {
		super(root, asksAllowsChildren);
	}

	@Override
	public void valueForPathChanged(final TreePath path, final Object newValue) {
		super.valueForPathChanged(path, newValue);
	}

	private static final long serialVersionUID = 1L;
}

class RobotTreeCellEditor extends DefaultTreeCellEditor {

	public RobotTreeCellEditor(final JTree tree, final DefaultTreeCellRenderer renderer) {
		super(tree, renderer);
	}

	@Override
	public Component getTreeCellEditorComponent(final JTree tree, final Object value, final boolean isSelected, final boolean expanded,
			final boolean leaf, final int row) {
		// if (value instanceof MyResourceNode) {
		// value = ((MyResourceNode) value).getName();
		// }
		return super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
	}

	@Override
	public boolean isCellEditable(final EventObject e) {
		return super.isCellEditable(e);
		// && ((TreeNode) lastPath.getLastPathComponent()).isLeaf();
	}
}

class DeviceBinderCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
	DeviceChooser devChooser;
	DeviceBindInfo dinfo;
	Component relativeTo;
	JButton jbutton = new JButton();
	public static final String EDIT = "edit";

	public DeviceBinderCellEditor(final Window window) {
		jbutton.addActionListener(this);
		jbutton.setActionCommand(EDIT);
		devChooser = new DeviceChooser(window);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getActionCommand().equals(EDIT)) {
			jbutton.setText(dinfo.toString());

			devChooser.setDevice(dinfo);
			devChooser.setVisible(true, relativeTo);

			this.fireEditingStopped();
		} else {
			dinfo = devChooser.getChooseDevice();
		}
	}

	@Override
	public Object getCellEditorValue() {
		return dinfo;
	}

	@Override
	public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row,
			final int column) {
		// relativeTo = table.getModel().
		dinfo = (DeviceBindInfo) value;
		return jbutton;
	}
}