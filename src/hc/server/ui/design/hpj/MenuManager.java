package hc.server.ui.design.hpj;

import hc.App;
import hc.core.L;
import hc.core.util.LogManager;
import hc.res.ImageSrc;
import hc.server.ConfigPane;
import hc.server.FileSelector;
import hc.server.ui.design.Designer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class MenuManager {
	private static int newNodeIdx = 1;
	
	public static void setNextNodeIdx(final int idx){
//		L.V = L.O ? false : LogManager.log("set Next Node Idx:" + idx);
		newNodeIdx = idx;
	}
	
	public static int getCurrNodeIdx(){
		return newNodeIdx;
	}
	
	public static int getNextNodeIdx(){
		return newNodeIdx++;
	}
	
	public void updateSkinUI(){
		Iterator<JPopupMenu> it = map.values().iterator();
		while(it.hasNext()){
			ConfigPane.updateComponentUI(it.next());
		}
	}
	
	public static final String ADD_MENU_ITEM = "add menu item";
	
	private Map<Integer, JPopupMenu> map = new HashMap<Integer, JPopupMenu>(); 
	
	public void popUpMenu(int type, TreeNode treeNode, final JTree tree, MouseEvent e, final Designer designer){
		final Integer integer_type = Integer.valueOf(type);
		JPopupMenu pMenu = map.get(integer_type);
		
		//Menu关联菜单尚未创建
		if(pMenu == null){
//			if(HPNode.isNodeType(type, HPNode.MASK_ROOT)){
//				pMenu = new JPopupMenu();
//				JMenuItem addItem = new JMenuItem("new menu");
//				addItem.setIcon(Designer.iconMenu);
//				addItem.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent event) {
//						DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new HPMenu(
//								HPNode.MASK_MENU,
//								"new node " + getNextNodeIdx(), 0));
//						
//						HCjar.buildMenuEventNodes(new HashMap<String, Object>(), newNode, 0);
//						
//						designer.addNode(null, newNode);
//						designer.setMainMenuNode(newNode);
//					}
//				});
//				pMenu.add(addItem);
//				
//				map.put(integer_type, pMenu);
			if(HPNode.isNodeType(type, HPNode.MASK_MENU)){
				pMenu = new JPopupMenu();
				final JMenuItem addItem = new JMenuItem(ADD_MENU_ITEM);
				addItem.setIcon(Designer.iconMenuItem);
				addItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						addMenuItem(designer, null);
					}
				});
				pMenu.add(addItem);
				
//				暂停删除menu，限制一个menu
//				{
//					JMenuItem delItem = new JMenuItem("del menu");
//					delItem.setIcon(Designer.iconDel);
//					delItem.addActionListener(new ActionListener() {
//						public void actionPerformed(ActionEvent event) {
//							designer.delNode();
//						}
//					});
//					pMenu.add(delItem);
//				}
				
				map.put(integer_type, pMenu);
			}else if(HPNode.isNodeType(type, HPNode.MASK_MENU_ITEM)){
				pMenu = new JPopupMenu();
				
				JMenuItem delItem = new JMenuItem("del menu item");
				delItem.setIcon(Designer.iconDel);
				delItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						designer.delNode();
					}
				});
				pMenu.add(delItem);
				
				map.put(integer_type, pMenu);
			}else if(HPNode.isNodeType(type, HPNode.MASK_SHARE_RB)){
				pMenu = new JPopupMenu();
				
				JMenuItem delItem = new JMenuItem("del JRuby file");
				delItem.setIcon(Designer.iconDel);
				delItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						designer.delNode();
					}
				});
				pMenu.add(delItem);
				
				map.put(integer_type, pMenu);
			}else if(HPNode.isNodeType(type, HPNode.MASK_SHARE_RB_FOLDER)){
				pMenu = new JPopupMenu();
				JMenuItem addItem = new JMenuItem("add JRuby share file");
				addItem.setIcon(Designer.iconShareRB);
				addItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
								new HPShareJRuby(
								HPNode.MASK_SHARE_RB,
								"share" + getNextNodeIdx() + ".rb"));
						designer.addNode(null, newNode);
					}
				});
				pMenu.add(addItem);
				
				map.put(integer_type, pMenu);
			}else if(HPNode.isNodeType(type, HPNode.MASK_RESOURCE_FOLDER)){
				pMenu = new JPopupMenu();
				JMenuItem addItem = new JMenuItem("add jar file");
				addItem.setIcon(Designer.iconJar);
				addItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						File addJarFile = FileSelector.selectImageFile(designer, FileSelector.JAR_FILTER, true);
						if(addJarFile != null){
							DefaultMutableTreeNode newNode;
							try {
								newNode = new DefaultMutableTreeNode(
										new HPShareJar(
										HPNode.MASK_RESOURCE_JAR,
										addJarFile.getName(), addJarFile));
								designer.addNode(null, newNode);
								designer.setNeedRebuildTestJRuby(true);
							} catch (Throwable e) {
								JPanel ok = new JPanel();
								try {
									ok.add(new JLabel("Error add jar file, desc : " + e.toString(), new ImageIcon(ImageIO.read(ImageSrc.CANCEL_ICON)), SwingConstants.LEFT));
								} catch (IOException e2) {
								}
								App.showCenterPanel(ok, 0, 0, "Add Error!", false, null, null, null, null, designer, true, false, null, false, false);
								
								e.printStackTrace();
							}
						}
					}
				});
				pMenu.add(addItem);
				
				map.put(integer_type, pMenu);
			}else if(type == HPNode.MASK_RESOURCE_JAR){
				pMenu = new JPopupMenu();
				{
					JMenuItem delItem = new JMenuItem("save as ...");
					delItem.setIcon(Designer.iconSaveAs);
					delItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							designer.saveJar();
						}
					});
					pMenu.add(delItem);					
				}
				
				{
					JMenuItem delItem = new JMenuItem("del jar file");
					delItem.setIcon(Designer.iconDel);
					delItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent event) {
							designer.delNode();
							designer.setNeedRebuildTestJRuby(true);
						}
					});
					pMenu.add(delItem);
				}
				map.put(integer_type, pMenu);
			}
		}
		
//		if(HPNode.isNodeType(type, HPNode.MASK_ROOT)){
//			//限制只能使用一个Menu
//			pMenu.getComponent(0).setEnabled(treeNode.getChildCount() <= Designer.SUB_NODES_OF_ROOT_IN_NEW_PROJ);
//		}
		if(pMenu != null){
			pMenu.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	public static void addMenuItem(final Designer _designer, final Component relativeTo) {
		//超过菜单最大数
		final DefaultMutableTreeNode mainMenuNode = _designer.getMainMenuNode();
		if(mainMenuNode == null){
			return;
		}
		final int currMenuItemNum = mainMenuNode.getChildCount();
		if(currMenuItemNum >= MenuListEditPanel.MAX_MENUITEM){
			JPanel panel = new JPanel();
			panel.add(new JLabel("Curr menu item number > " + MenuListEditPanel.MAX_MENUITEM, 
					App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
			App.showCenterPanel(panel, 0, 0, "Too much items!", false, null, null, null, null, _designer, true, false, null, false, false);
			return;
		}
		
		HPMenuItem menuItem = TypeWizard.chooseWizard(_designer, relativeTo);
		if(menuItem == null){
			
		}else{
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
					menuItem);
			_designer.addNode(mainMenuNode, newNode);
		}
	}
}
