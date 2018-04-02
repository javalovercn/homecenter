package hc.server.ui.design.hpj;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import hc.App;
import hc.UIActionListener;
import hc.core.util.ExceptionReporter;
import hc.res.ImageSrc;
import hc.server.ConfigPane;
import hc.server.FileSelector;
import hc.server.HCActionListener;
import hc.server.ui.design.Designer;
import hc.util.ResourceUtil;

public class MenuManager {
	private final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	private static int newNodeIdx = 1;

	public static void setNextNodeIdx(final int idx) {
		// LogManager.log("set Next Node Idx:" + idx);
		newNodeIdx = idx;
	}

	public static int getCurrNodeIdx() {
		return newNodeIdx;
	}

	public static int getNextNodeIdx() {
		return newNodeIdx++;
	}

	public void updateSkinUI() {
		final Iterator<JPopupMenu> it = map.values().iterator();
		while (it.hasNext()) {
			ConfigPane.updateComponentUI(it.next());
		}
	}

	public static final String ADD_ITEM = "add item";

	private final Map<Integer, JPopupMenu> map = new HashMap<Integer, JPopupMenu>();

	public void popUpMenu(final int type, final TreeNode treeNode, final JTree tree, final MouseEvent e, final Designer designer) {
		final Integer integer_type = Integer.valueOf(type);
		JPopupMenu pMenu = map.get(integer_type);

		// Menu关联菜单尚未创建
		if (pMenu == null) {
			// if(HPNode.isNodeType(type, HPNode.MASK_ROOT)){
			// pMenu = new JPopupMenu();
			// JMenuItem addItem = new JMenuItem("new menu");
			// addItem.setIcon(Designer.iconMenu);
			// addItem.addActionListener(new HCActionListener(new Runnable() {
			// public void actionPerformed(ActionEvent event) {
			// DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new
			// HPMenu(
			// HPNode.MASK_MENU,
			// "new node " + getNextNodeIdx(), 0));
			//
			// HCjar.buildMenuEventNodes(new HashMap<String, Object>(), newNode,
			// 0);
			//
			// designer.addNode(null, newNode);
			// designer.setMainMenuNode(newNode);
			// }
			// });
			// pMenu.add(addItem);
			//
			// map.put(integer_type, pMenu);
			if (HPNode.isNodeType(type, HPNode.MASK_MENU)) {
				pMenu = new JPopupMenu();
				final JMenuItem addItem = new JMenuItem(ADD_ITEM);
				addItem.setIcon(Designer.iconMenuItem);
				addItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						addMenuItem(designer, null, designer.getMainMenuNode(), designer.getMSBFolderNode());
					}
				}, threadPoolToken));
				pMenu.add(addItem);

				// 暂停删除menu，限制一个menu
				// {
				// JMenuItem delItem = new JMenuItem("del menu");
				// delItem.setIcon(Designer.iconDel);
				// delItem.addActionListener(new HCActionListener(new Runnable()
				// {
				// public void actionPerformed(ActionEvent event) {
				// designer.delNode();
				// }
				// });
				// pMenu.add(delItem);
				// }

				map.put(integer_type, pMenu);
			} else if (HPNode.isNodeType(type, HPNode.MASK_MSB_FOLDER)) {
				pMenu = new JPopupMenu();
				final JMenuItem addItem = new JMenuItem(ADD_ITEM);
				addItem.setIcon(Designer.iconMenuItem);
				addItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						TypeWizard.selectIOT(designer, null);
						final HPNode node = TypeWizard.getWizardEnd();
						if (node == null) {
						} else {
							final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(node);
							final int menuType = node.type;
							if (menuType == HPNode.MASK_MSB_ROBOT || menuType == HPNode.MASK_MSB_CONVERTER
									|| menuType == HPNode.MASK_MSB_DEVICE) {
								designer.addNode(designer.getMSBFolderNode(), newNode);
							}
						}
					}
				}, threadPoolToken));
				pMenu.add(addItem);

				map.put(integer_type, pMenu);
			} else if (HPNode.isNodeType(type, HPNode.MASK_MENU_ITEM) || HPNode.isNodeType(type, HPNode.MASK_MSB_ITEM)) {
				pMenu = new JPopupMenu();

				final JMenuItem delItem = new JMenuItem("del item");
				delItem.setIcon(Designer.iconDel);
				delItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						designer.delNode();
					}
				}, threadPoolToken));
				pMenu.add(delItem);

				map.put(integer_type, pMenu);
			} else if (HPNode.isNodeType(type, HPNode.MASK_SHARE_RB)) {
				pMenu = new JPopupMenu();

				final JMenuItem delItem = new JMenuItem("del JRuby file");
				delItem.setIcon(Designer.iconDel);
				delItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						designer.delNode();
					}
				}, threadPoolToken));
				pMenu.add(delItem);

				map.put(integer_type, pMenu);
			} else if (HPNode.isNodeType(type, HPNode.MASK_SHARE_RB_FOLDER)) {
				pMenu = new JPopupMenu();
				final JMenuItem addItem = new JMenuItem("add JRuby share file");
				addItem.setIcon(Designer.iconShareRB);
				addItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
								new HPShareJRuby(HPNode.MASK_SHARE_RB, "share" + getNextNodeIdx() + ".rb"));
						designer.addNode(null, newNode);
					}
				}, threadPoolToken));
				pMenu.add(addItem);

				map.put(integer_type, pMenu);
			} else if (HPNode.isNodeType(type, HPNode.MASK_SHARE_NATIVE_FOLDER)) {
				pMenu = new JPopupMenu();
				final JMenuItem addItem = new JMenuItem("add native file");
				addItem.setIcon(Designer.iconShareNative);
				addItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final File addNativeFile = FileSelector.selectImageFile(designer, FileSelector.NATIVE_FILTER, true);
						if (addNativeFile != null) {
							DefaultMutableTreeNode newNode;
							try {
								newNode = new DefaultMutableTreeNode(
										new HPShareNative(HPNode.MASK_SHARE_NATIVE, addNativeFile.getName(), addNativeFile));
								designer.addNode(null, newNode);
								// designer.setNeedRebuildTestJRuby(true);

								if (designer.getRootUserObject().csc.isLoadLib() == false) {
									final JButton ok = App.buildDefaultOKButton();
									final JButton no = App.buildDefaultCloseButton();
									final String addPerm = "click [" + ok.getText() + "] to add [" + ResourceUtil.LOAD_NATIVE_LIB
											+ "] permission to project!";
									final JPanel panel = new JPanel(new BorderLayout());
									final JLabel msg = new JLabel(addPerm, App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);
									panel.add(msg, BorderLayout.CENTER);
									final UIActionListener jbOKAction = new UIActionListener() {
										@Override
										public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
											window.dispose();
											designer.getRootUserObject().csc.setLoadLib(true);
										}
									};
									final UIActionListener cancelAction = new UIActionListener() {
										@Override
										public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
											window.dispose();
										}
									};
									App.showCenter(panel, 0, 0, "add permission?", true, ok, no, jbOKAction, cancelAction, designer, true,
											false, null, false);
								}

							} catch (final Throwable e) {
								final JPanel ok = new JPanel();
								ok.add(new JLabel("Error add native file, desc : " + e.toString(), new ImageIcon(ImageSrc.CANCEL_ICON),
										SwingConstants.LEFT));
								App.showCenterPanelMain(ok, 0, 0, "Add Error!", false, null, null, null, null, designer, true, false, null,
										false, false);

								ExceptionReporter.printStackTrace(e);
							}
						}
					}
				}, threadPoolToken));
				pMenu.add(addItem);

				map.put(integer_type, pMenu);
			} else if (HPNode.isNodeType(type, HPNode.MASK_RESOURCE_FOLDER)) {
				pMenu = new JPopupMenu();
				final JMenuItem addItem = new JMenuItem("add jar file");
				addItem.setIcon(Designer.iconJar);
				addItem.addActionListener(new HCActionListener(new Runnable() {
					@Override
					public void run() {
						final File addJarFile = FileSelector.selectImageFile(designer, FileSelector.JAR_FILTER, true);
						if (addJarFile != null) {
							// 检查包中不能含有系统保留包名
							if (ResourceUtil.checkSysPackageNameInJar(addJarFile)) {
								final String reservedPackageNameIsInHar = ResourceUtil.RESERVED_PACKAGE_NAME_IS_IN_HAR;
								App.showErrorMessageDialog(designer, reservedPackageNameIsInHar, ResourceUtil.getErrorI18N());
								return;
							}

							// 检查cafeCode是否高于当前运行环境
							final float jreVersion = ResourceUtil.getMaxJREVersionFromCompileJar(addJarFile);// 0表示纯资源包
							final float currJRE = App.getJREVer();
							if (jreVersion > currJRE) {
								final String lowerVersion = "current JRE/JDK version is [" + ResourceUtil.getJavaVersionFromFloat(currJRE) + "], but jar is compiled in version [" + ResourceUtil.getJavaVersionFromFloat(jreVersion) + "]!";
								App.showErrorMessageDialog(designer,
										ResourceUtil.wrapHTMLTag(lowerVersion + "<BR><BR>please shutdown this application, upgrade JRE/JDK and add again."),
										ResourceUtil.getErrorI18N());
								return;
							}

							DefaultMutableTreeNode newNode;
							try {
								newNode = new DefaultMutableTreeNode(
										new HPShareJar(HPNode.MASK_RESOURCE_JAR, addJarFile.getName(), addJarFile));
								designer.addNode(null, newNode);
								designer.setNeedRebuildTestJRuby(true);
							} catch (final Throwable e) {
								final JPanel ok = new JPanel();
								ok.add(new JLabel("Error add jar file, desc : " + e.toString(), new ImageIcon(ImageSrc.CANCEL_ICON),
										SwingConstants.LEFT));
								App.showCenterPanelMain(ok, 0, 0, "Add Error!", false, null, null, null, null, designer, true, false, null,
										false, false);

								ExceptionReporter.printStackTrace(e);
							}
						}
					}
				}, threadPoolToken));
				pMenu.add(addItem);

				map.put(integer_type, pMenu);
			} else if (type == HPNode.MASK_RESOURCE_JAR) {
				pMenu = new JPopupMenu();
				{
					final JMenuItem delItem = new JMenuItem("save as ...");
					delItem.setIcon(Designer.iconSaveAs);
					delItem.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							designer.saveFile(FileSelector.JAR_FILTER, Designer.JAR_EXT);
						}
					}, threadPoolToken));
					pMenu.add(delItem);
				}

				{
					final JMenuItem delItem = new JMenuItem("del jar file");
					delItem.setIcon(Designer.iconDel);
					delItem.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							designer.delNode();
							designer.setNeedRebuildTestJRuby(true);
						}
					}, threadPoolToken));
					pMenu.add(delItem);
				}
				map.put(integer_type, pMenu);
			} else if (type == HPNode.MASK_SHARE_NATIVE) {
				pMenu = new JPopupMenu();
				{
					final JMenuItem delItem = new JMenuItem("save as ...");
					delItem.setIcon(Designer.iconSaveAs);
					delItem.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							designer.saveFile(FileSelector.NATIVE_FILTER, "");
						}
					}, threadPoolToken));
					pMenu.add(delItem);
				}

				{
					final JMenuItem delItem = new JMenuItem("del native file");
					delItem.setIcon(Designer.iconDel);
					delItem.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) designer.getCurrSelectedNode().getParent();
							final boolean oneChildSize = parent.getChildCount() == 1;
							designer.delNode();
							// designer.setNeedRebuildTestJRuby(true);
							if (oneChildSize) {
								if (designer.getRootUserObject().csc.isLoadLib()) {
									final JButton ok = App.buildDefaultOKButton();
									final JButton close = App.buildDefaultCloseButton();
									final String msg = "click [" + ok.getText() + "] to disable [<strong>" + ResourceUtil.LOAD_NATIVE_LIB
											+ "</strong>] permission!";
									final UIActionListener okAction = new UIActionListener() {
										@Override
										public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
											window.dispose();
											designer.getRootUserObject().csc.setLoadLib(false);
										}
									};
									final UIActionListener closeAction = new UIActionListener() {
										@Override
										public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
											window.dispose();
										}
									};
									final JPanel panel = new JPanel(new BorderLayout());
									final JLabel label = new JLabel(msg, App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEADING);
									panel.add(label, BorderLayout.CENTER);
									App.showCenter(panel, 0, 0, "disable permission?", true, ok, close, okAction, closeAction, designer,
											true, false, null, false);
								}
							}
						}
					}, threadPoolToken));
					pMenu.add(delItem);
				}
				map.put(integer_type, pMenu);
			}
		}

		// if(HPNode.isNodeType(type, HPNode.MASK_ROOT)){
		// //限制只能使用一个Menu
		// pMenu.getComponent(0).setEnabled(treeNode.getChildCount() <=
		// Designer.SUB_NODES_OF_ROOT_IN_NEW_PROJ);
		// }
		if (pMenu != null) {
			pMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public static void addMenuItem(final Designer _designer, final Component relativeTo, final DefaultMutableTreeNode mainMenuNode,
			final DefaultMutableTreeNode msbFoulder) {
		// 超过菜单最大数
		if (mainMenuNode == null) {
			return;
		}
		final int currMenuItemNum = mainMenuNode.getChildCount();
		if (currMenuItemNum >= MenuListEditPanel.MAX_MENUITEM) {
			final JPanel panel = new JPanel();
			panel.add(new JLabel("Curr menu item number > " + MenuListEditPanel.MAX_MENUITEM, App.getSysIcon(App.SYS_ERROR_ICON),
					SwingConstants.LEFT));
			App.showCenterPanelMain(panel, 0, 0, "Too much items!", false, null, null, null, null, _designer, true, false, null, false,
					false);
			return;
		}

		final HPNode node = TypeWizard.chooseWizard(_designer, relativeTo);
		if (node == null) {
		} else {
			final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(node);
			final int menuType = node.type;
			if (menuType == HPNode.MASK_MSB_ROBOT || menuType == HPNode.MASK_MSB_CONVERTER || menuType == HPNode.MASK_MSB_DEVICE) {
				_designer.addNode(msbFoulder, newNode);
			} else {
				_designer.addNode(mainMenuNode, newNode);
			}
		}
	}
}
