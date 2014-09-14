package hc.server.ui.design;

import hc.App;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.RootConfig;
import hc.core.util.HCURL;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.FileSelector;
import hc.server.J2SEContext;
import hc.server.JRubyInstaller;
import hc.server.SingleJFrame;
import hc.server.StarterManager;
import hc.server.ThirdlibManager;
import hc.server.ui.ClosableWindow;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.LocationComponentListener;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCShareFileResource;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.HPItemContext;
import hc.server.ui.design.hpj.HPNode;
import hc.server.ui.design.hpj.HPProject;
import hc.server.ui.design.hpj.HPShareJRubyFolder;
import hc.server.ui.design.hpj.HPShareJarFolder;
import hc.server.ui.design.hpj.HPShareRoot;
import hc.server.ui.design.hpj.IModifyStatus;
import hc.server.ui.design.hpj.MenuManager;
import hc.server.ui.design.hpj.NodeEditPanel;
import hc.server.ui.design.hpj.NodeEditPanelManager;
import hc.server.ui.design.hpj.NodeInvalidException;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.server.ui.tip.Wizard;
import hc.util.IBiz;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class Designer extends SingleJFrame implements IModifyStatus{
	public static final String HAR_EXT = ".har";
	public static final String HAD_EXT = ".had";
	public static final String JAR_EXT = ".jar";
	public static final String MY_DEPLOY_PROJ = "myproj.jar";
	public static final String MY_DEPLOY_PROJ_HAR = "myproj.har";
	public static final String OLD_EDIT_JAR = "myedit.jar";
	
	public static final String EXAMPLE = "Demo";
	
	public static final int SUB_NODES_OF_ROOT_IN_NEW_PROJ = 1;
	
	private boolean needRebuildTestJRuby = true;

	public void setNeedRebuildTestJRuby(boolean need){
		needRebuildTestJRuby = need;
	}
	
	public static void startAutoUpgradeBiz() {
		if(JRubyInstaller.checkInstalledJRuby() == false){
			JRubyInstaller.startInstall();
		}else{		
			try{
				LinkProjectManager.appNewLinkedInProjNow(LinkProjectManager.lpsVector, false);
				LinkProjectManager.startLinkedInProjectUpgradeTimer();
			}catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	public static ImageIcon loadImg(String path){
		try {
			return new ImageIcon(ImageIO.read(Designer.class.getClassLoader().getResource("hc/server/ui/design/res/" + path)));
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	public void dispose(){
		LinkProjectStatus.exitStatus();
		
		instance = null;
		super.dispose();
	}
	
	@Override
	public void updateSkinUI(){
		super.updateSkinUI();
		
		mg.updateSkinUI();
		nm.updateSkinUI();
		
		tree.updateUI();
	}
	
	private boolean isModified;
	final MenuManager mg = new MenuManager();

	JTree tree;
	// 上面JTree对象对应的model
	DefaultTreeModel model;
	DefaultMutableTreeNode mainMenuNode;
	
	// 定义几个初始节点
	DefaultMutableTreeNode root = createNewRoot();
	
	public String getCurrProjID(){
		return ((HPProject)root.getUserObject()).id;
	}
	
	public String getCurrProjVer(){
		return ((HPProject)root.getUserObject()).ver;
	}
	
	public static final int ROOT_SUB_FOLDER = 2;
	final DefaultMutableTreeNode[] shareFolders = new DefaultMutableTreeNode[ROOT_SUB_FOLDER];

	private DefaultMutableTreeNode createShareJRubyFolder() {
		HPShareJRubyFolder sj = new HPShareJRubyFolder(HPNode.MASK_SHARE_RB_FOLDER, 
				"Share JRuby Files");
		return new DefaultMutableTreeNode(sj);
	}
	
	private DefaultMutableTreeNode createShareJarFolder() {
		HPShareJarFolder sj = new HPShareJarFolder(HPNode.MASK_RESOURCE_FOLDER_JAR, 
				"Share Jar Files");
		return new DefaultMutableTreeNode(sj);
	}
	
	private DefaultMutableTreeNode createNewRoot() {
		return new DefaultMutableTreeNode(new HPProject(
				HPNode.MASK_ROOT, "Project", LinkProjectManager.buildSysProjID(), HPProject.DEFAULT_VER, "",
				"", "", "", ""));
	}
//	DefaultMutableTreeNode guangdong = new DefaultMutableTreeNode(new HPItem(
//			HPItem.MASK_MENU, "firstMenu"));
//	DefaultMutableTreeNode guangxi = new DefaultMutableTreeNode(new HPItem(
//			HPItem.MASK_MENU, "广西"));
//	DefaultMutableTreeNode foshan = new DefaultMutableTreeNode(
//			new HPMenuItem(HPItem.TYPE_MENU_ITEM_SCREEN, "关机", "screen://home", ""));
//	DefaultMutableTreeNode shantou = new DefaultMutableTreeNode(
//			new HPMenuItem(HPItem.TYPE_MENU_ITEM_CMD, "退出", "cmd://exit", ""));
//	DefaultMutableTreeNode mycommand = new DefaultMutableTreeNode(
//			new HPMenuItem(HPItem.TYPE_MENU_ITEM_CMD, "播放音乐", "cmd://playMusic?loop=true&num=2", ""));

	// 定义需要被拖动的TreePath
	TreePath movePath;
	public final static NodeEditPanel emptyNodeEditPanel = new NodeEditPanel() {
		@Override
		public void init(final MutableTreeNode data, JTree tree) {
		}
	};
	
	private Icon getSampleIcon(){
		return loadImg("gift_24.png");
	}
	
	public static final String ACTIVE = "Activate";
	public static final String DEACTIVE = "Deactivate";
	
	private NodeEditPanel nodeEditPanel = emptyNodeEditPanel;
	JButton sampleButton = new JButton(EXAMPLE, getSampleIcon());
	JButton saveButton = new JButton("Save", loadImg("save_24.png"));
	JButton activeButton = new JButton(ACTIVE, loadImg("deploy_24.png"));
	JButton deactiveButton = new JButton(DEACTIVE, loadImg("undeploy_24.png"));
	final JButton addItemButton = new JButton("Add", loadImg("controller_24.png"));
	JButton newButton = new JButton("New", loadImg("new_24.png"));
	JButton saveAsButton = new JButton("Save as", loadImg("shareout_24.png"));
	JButton loadButton = new JButton("Load", loadImg("sharein_24.png"));
	JButton shiftProjButton = new JButton("Shift Project", new ImageIcon(ResourceUtil.loadImage("menu_22.png")));
	JButton helpButton = new JButton("Help", loadImg("faq_24.png"));
	DefaultMutableTreeNode selectedNode;
	JPanel editPanel = new JPanel();

	private NodeEditPanelManager nm = new NodeEditPanelManager();
	
	public final static ImageIcon iconMenuItem = loadImg("menuitem.png");

	public final static ImageIcon iconMenu = loadImg("menu.png");

	public final static ImageIcon iconRoot = loadImg("root.png");
	
	public final static ImageIcon iconDel = loadImg("del.png");
	
	public final static ImageIcon iconSaveAs = loadImg("save_16.png");
	
	public final static ImageIcon iconShareRoot = loadImg("share.png");
	
	public final static ImageIcon iconShareRBFolder = loadImg("rb_folder.png");
	
	public final static ImageIcon iconShareRB = loadImg("jruby.png");
	
	public final static ImageIcon iconJar = loadImg("jar.png");
	
	public final static ImageIcon iconJarFolder = loadImg("jar_folder.png");
			
	public final static ImageIcon iconEventFolder = loadImg("radar.png");
	
	public final static ImageIcon iconEventItem = loadImg("radar_on.png");
	
	private void changeTreeNodeContext(DefaultMutableTreeNode _node, HPItemContext context){
		((HPNode)_node.getUserObject()).setContext(context);
		
		final int size = _node.getChildCount();
		for (int i = 0; i < size; i++) {
			changeTreeNodeContext((DefaultMutableTreeNode)_node.getChildAt(i), context);
		}
	}
	
	final HPItemContext itemContext;
	
	public static File switchHar(File jar, File har){
		if(jar.exists()){
			ThirdlibManager.copy(jar, har);
			jar.delete();
		}
		return har;
	}
	
	private static Designer instance;
	public static Designer getInstance(){
		return instance;
	}
	
	public Designer() {
		setTitle((String)ResourceUtil.get(9034));
		
		instance = this;
		
		setIconImage(App.SYS_LOGO);
		setName("Designer");
		ComponentListener cl = new LocationComponentListener();
		
		addComponentListener(cl);
		
		shareFolders[0] = createShareJRubyFolder();
		shareFolders[1] = createShareJarFolder();
		
		
		J2SEContext.appendTitleJRubyVer(this);
		
		itemContext = new HPItemContext();
		itemContext.modified = this;
		
		tree = new JTree(root);
		tree.getSelectionModel().setSelectionMode(  
                TreeSelectionModel.SINGLE_TREE_SELECTION); 
		model = (DefaultTreeModel) tree.getModel();		
		
		File har = loadDefaultEdit();
		
		final ActionMap actionMap = tree.getActionMap();
		actionMap.put( "cut", null );
		actionMap.put( "copy", null );
		actionMap.put( "paste", null );
		
//		DragSource dragSource = DragSource.getDefaultDragSource();
//		dragSource.createDefaultDragGestureRecognizer(tree, DnDConstants.ACTION_NONE, null);
		
		tree.setCellRenderer(new NodeTreeCellRenderer());
		
//		自定义树节点，选中颜色
//		DefaultTreeCellRenderer cellRenderer =  
//                (DefaultTreeCellRenderer)tree.getCellRenderer();
//		cellRenderer.setBackgroundNonSelectionColor(Color.white);  
//		cellRenderer.setBackgroundSelectionColor(Color.yellow);  
//		cellRenderer.setBorderSelectionColor(Color.red);  
//        //设置选或不选时，文字的变化颜色  
//		cellRenderer.setTextNonSelectionColor(Color.black);  
//		cellRenderer.setTextSelectionColor(Color.blue);  

        
		// 获取JTree对应的TreeModel对象
        
		tree.setEditable(false);
		
		final Designer self = this;
		
		MouseListener ml = new MouseListener() {
			// 按下鼠标时候获得被拖动的节点
			public void mousePressed(MouseEvent e) {
				// 如果需要唯一确定某个节点，必须通过TreePath来获取。
				TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
				if (tp != null) {
					movePath = tp;
				}
			}

			// 鼠标松开时获得需要拖到哪个父节点
			public void mouseReleased(MouseEvent e) {
				// 根据鼠标松开时的TreePath来获取TreePath
				TreePath tp = tree.getPathForLocation(e.getX(), e.getY());

//				Drag and Drop
//				if (tp != null && movePath != null) {
//					// 阻止向子节点拖动
//					if (movePath.isDescendant(tp) && movePath != tp) {
//						JOptionPane.showMessageDialog(this,
//								"目标节点是被移动节点的子节点，无法移动！", "非法操作",
//								JOptionPane.ERROR_MESSAGE);
//						return;
//					}
//					// 既不是向子节点移动，而且鼠标按下、松开的不是同一个节点
//					else if (movePath != tp) {
//						System.out.println(tp.getLastPathComponent());
//						// add方法可以先将原节点从原父节点删除，再添加到新父节点中
//						((DefaultMutableTreeNode) tp.getLastPathComponent())
//								.add((DefaultMutableTreeNode) movePath
//										.getLastPathComponent());
//						movePath = null;
//						tree.updateUI();
//					}
//				}
				
				if(tp == null){
					return;
				}
				
				Object obj = tp.getLastPathComponent();
				if (obj != null && (obj instanceof DefaultMutableTreeNode)) {
					selectedNode = (DefaultMutableTreeNode) obj;
				}else{
					selectedNode = null;
				}
				
				if (e.getButton() == MouseEvent.BUTTON3) {
					if(selectedNode != null){
						Object o = selectedNode.getUserObject();
						if (o instanceof HPNode) {
							jumpToNode(selectedNode);

							mg.popUpMenu(((HPNode) o).type, selectedNode, tree, e, self);
						}
					}
				}else if (e.getButton() == MouseEvent.BUTTON1) {
					if(selectedNode != null){
						Object o = selectedNode.getUserObject();
						if (o instanceof HPNode) {
							final HPNode nodeData = (HPNode) o;
							notifySelectNode(selectedNode, nodeData);
						}
					}
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		};

		// {
		// public void actionPerformed(ActionEvent event)
		// {
		// //获取选中节点
		// DefaultMutableTreeNode selectedNode
		// = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		// //如果节点为空，直接返回
		// if (selectedNode == null) return;
		// //获取该选中节点的父节点
		// DefaultMutableTreeNode parent
		// = (DefaultMutableTreeNode)selectedNode.getParent();
		// //如果父节点为空，直接返回
		// if (parent == null) return;
		// //创建一个新节点
		// DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("新节点");
		// //获取选中节点的选中索引
		// int selectedIndex = parent.getIndex(selectedNode);
		// //在选中位置插入新节点
		// model.insertNodeInto(newNode, parent, selectedIndex + 1);
		// //--------下面代码实现显示新节点（自动展开父节点）-------
		// //获取从根节点到新节点的所有节点
		// TreeNode[] nodes = model.getPathToRoot(newNode);
		// //使用指定的节点数组来创建TreePath
		// TreePath path = new TreePath(nodes);
		// //显示指定TreePath
		// tree.scrollPathToVisible(path);
		// }
		// });

//		tree.addTreeSelectionListener(new TreeSelectionListener() {
//			@Override
//			public void valueChanged(TreeSelectionEvent e) {
//				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
//
//				if (node == null)
//					return;
//
//				HPItem item = (HPItem)node.getUserObject();
//				notifySelectNode(item);
//			}
//		});
		tree.addMouseListener(ml);
		
		JToolBar toolbar = new JToolBar();
		setModified(false);
		
		{
			final Action saveAction = new AbstractAction() {
				public void actionPerformed(ActionEvent event) {
	//				TreePath selectedPath = tree.getSelectionPath();
	//				if (selectedPath != null) {
	//					// 编辑选中节点
	//					tree.startEditingAtPath(selectedPath);
	//				}
					
					save();
				}
			};
			ResourceUtil.buildAcceleratorKeyOnAction(saveAction, KeyEvent.VK_S);//同时支持Windows下的Ctrl+S和Mac下的Command+S
			saveButton.addActionListener(saveAction);
			saveButton.getActionMap().put("myAction", saveAction);
			saveButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) saveAction.getValue(Action.ACCELERATOR_KEY), "myAction");
		}
		
		//检查是否有新版本
		final String lastSampleVer = PropertiesManager.getValue(PropertiesManager.p_LastSampleVer, "1.0");
		if(StringUtil.higer(RootConfig.getInstance().getProperty(RootConfig.p_Sample_Ver), lastSampleVer)){
			sampleButton.setIcon(loadImg("giftnew_24.png"));
		}
		sampleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(quitToNewProj(self) == false){
					return;
				}
				
				try {
					final String url = "http://homecenter.mobi/download/sample.har";
					final Map<String, Object> map = HCjar.loadJar(url);
					if(map.isEmpty()){
						JPanel panel = new JPanel();
						panel.add(new JLabel("<html>Sorry, Load example har from remote server error." +
								"<BR>please try after a minute</html>", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
						App.showCenterPanel(panel, 0, 0, "Network Error", false, null, null, null, null, self, true, false, null, false, false);
						return;
					}
					final IBiz succLoadBiz = new IBiz() {
						@Override
						public void start() {
							showHCMessage("Success load example from server.", EXAMPLE + " OK", self, true);
							
							sampleButton.setIcon(getSampleIcon());
							
							PropertiesManager.setValue(PropertiesManager.p_LastSampleVer, 
									RootConfig.getInstance().getProperty(RootConfig.p_Sample_Ver));
							PropertiesManager.saveFile();
						}
						
						@Override
						public void setMap(HashMap map) {
						}
					};
					
					final String licenseURL = ((String)map.get(HCjar.PROJ_LICENSE)).trim();
					if(licenseURL.length() > 0){
						IBiz biz = new IBiz(){
							@Override
							public void setMap(HashMap map) {
							}

							@Override
							public void start() {
								if(checkAndLoad(map, succLoadBiz) == false){
									return;
								}
							}
						};
						App.showHARProjectAgreeLicense("License of [" + map.get(HCjar.PROJ_NAME) + "]", licenseURL, biz, true, self);
						return;
					}
					if(checkAndLoad(map, succLoadBiz) == false){
						return;
					}
//					if(PropertiesManager.isTrue(PropertiesManager.p_SampleDeployNotify) == false){
//						PropertiesManager.setValue(PropertiesManager.p_SampleDeployNotify, IConstant.TRUE);
//						PropertiesManager.saveFile();
//
//						JPanel panel = new JPanel();
//						panel.add(new JLabel("Example lib will work for mobile after click 'Deploy' button.", 
//								App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEFT));
//						App.showCenterPanel(panel, 0, 0, "Deploy me later", false, null, null, null, self, true, false);
//					}
				} catch (Throwable e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(self, "Error download example, " +
							"please try after few minutes.", "Error " + EXAMPLE, 
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		sampleButton.setToolTipText("<html>load a example project online.<BR>there are many powerful mobile menu items and source code in it." +
				"<BR><BR>Note : only one project is edited at same time." +
				"<BR>Tip : when the version of online demo project is newer than you had imported, it will display new red star.</html>");
		toolbar.add(sampleButton);
		newButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(quitToNewProj(self) == false){
					return;
				}
				
				HashMap<String, Object> map = new HashMap<String, Object>();
				HCjar.initMap(map);
				map.put(HCjar.MENU_NUM, "1");
				map.put(HCjar.MAIN_MENU_IDX, "0");
				map.put(HCjar.replaceIdxPattern(HCjar.MENU_NAME, 0), "my menu");
				map.put(HCjar.replaceIdxPattern(HCjar.MENU_COL_NUM, 0), "0");
				map.put(HCjar.replaceIdxPattern(HCjar.MENU_ID, 0), HCURL.ROOT_MENU);				
				
				loadNodeFromMap(map);
				
//				final Object userObject = buildInitRoot();
//				MenuManager.setNextNodeIdx(1);
//				mainMenuNode = new DefaultMutableTreeNode(new HPMenu(,
//						HPNode.MASK_MENU,
//						"my menu", 0));
//				HCjar.buildMenuEventNodes(new HashMap<String, Object>(), mainMenuNode, 0);
//				addNode(root, mainMenuNode);
//				setModified(false);
//				
//				notifySelectNode(mainMenuNode, (HPNode)mainMenuNode.getUserObject());
//				
//				addItemButton.setEnabled(true);
//				tree.updateUI();
			}
		});
		newButton.setToolTipText("<html>clear the current project, ready to create new items for new project." +
				"</html>");
		toolbar.add(newButton);
		
		addItemButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MenuManager.addMenuItem(Designer.getInstance(), addItemButton);
			}
		});
		addItemButton.setToolTipText("<html>add a controller, UI Panel(Mlet) or script of cmd." +
				"</html>");
		toolbar.add(addItemButton);
		
		saveButton.setToolTipText("<html>save to disk ("+ResourceUtil.getAbstractCtrlKeyText()+" + S)<BR>current project will be auto loaded when open this designer." +
//				"<BR><BR>Tip : Saving has no effect on a deployed project(which is maybe using by mobile)." +
				"</html>");
		toolbar.add(saveButton);
		
		activeButton.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + D)<BR>after activate, these designed menus will display to mobile when mobile login." +
				"<BR>please re-activate after modifying." +
				"</html>");
		{
			final Action deployAction = new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					Map<String, Object> map = null;
					if(isModified){
						int out = modifyNotify();
						if(out == JOptionPane.YES_OPTION){
							map = save();
						}else{
							return;
						}
					}
					
					if(map == null){
						try{
							map = buildMapFromTree();
						}catch (NodeInvalidException ex) {
							displayError(ex);
							return;
						}
					}
					
					if(deployProcess(map)){
						showHCMessage("Successful activate project, " +
							"mobile can access this resources now.", ACTIVE + " OK", self, true);
					}
				}
			};
			ResourceUtil.buildAcceleratorKeyOnAction(deployAction, KeyEvent.VK_D);
			activeButton.addActionListener(deployAction);
			activeButton.getActionMap().put("myDeloyAction", deployAction);
			activeButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) deployAction.getValue(Action.ACCELERATOR_KEY), "myDeloyAction");

		}
		toolbar.addSeparator();
		toolbar.add(activeButton);
		deactiveButton.setToolTipText("<html>deactivate current project only, not all projects." +
				"<BR>If no project is active, only screen desktop is accessed when mobile login." +
				"</html>");
		deactiveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				
				deactiveButton.setEnabled(false);

				LinkProjectStore lps = LinkProjectManager.getProjByID(getCurrProjID());
				if(lps != null){
					synchronized (ServerUIUtil.LOCK) {
						ServerUIUtil.promptAndStop(true, self);
						
//						仅deactive，而非删除工程
//						LinkProjectManager.removeLinkProjectPhic(lps);
						lps.setActive(false);
						boolean newroot = false;
						final boolean currRoot = lps.isRoot();
						if(currRoot){
							lps.setRoot(false);
							final LinkProjectStore root = LinkProjectManager.searchOtherActive(lps);
							if(root != null){
								//自动升级别的active工程为root
								root.setRoot(true);
								newroot = true;
							}
						}
						LinkProjectManager.updateToLinkProject();
						
						if(currRoot && newroot == false){
							setProjectOff();
	
							//启动远屏或菜单
							ServerUIUtil.restartResponsorServerDelayMode();
							showHCMessage("Successful deactivate current project.", DEACTIVE + " OK", self, true);
						}else{
							//启动远屏或菜单
							ServerUIUtil.restartResponsorServerDelayMode();
							showHCMessage("Successful deactivate current project, " +
									"other project(s) is restart and serving now.", DEACTIVE + " OK", self, true);
						}
					}
				}else{
					JPanel panel = new JPanel(new BorderLayout());
					panel.add(new JLabel("project [" + getCurrProjID() + "] is not active, you may have changed project ID!", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
					App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.ERROR), false, null, null, null, null, instance, true, false, null, false, false);
					return;
				}
//				JOptionPane.showMessageDialog(this, "Successful undeploy project, " +
//						"mobile will access PC screen only.", "Undeploy OK", 
//						JOptionPane.INFORMATION_MESSAGE);
			}
		});
		toolbar.add(deactiveButton);
		saveAsButton.setToolTipText("<html>save current project to a har file on your disk, not all projects.</html>");
		saveAsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				if(self.isModified()){
//					JPanel panel = new JPanel();
//					panel.add(new JLabel("Save this project before share?", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEFT));
//					App.showCenterPanel(panel, 0, 0, "save it before", true, null, new ActionListener() {
//						@Override
//						public void actionPerformed(ActionEvent e) {
//							if(save() == false){
//								return;
//							}
//							shareTo();
//						}
//					}, new ActionListener() {
//						@Override
//						public void actionPerformed(ActionEvent e) {
//							shareTo();
//						}
//					}, self, true, false);
//				}else{
//					shareTo();
//				}
				try{
					Map<String, Object> map = buildMapFromTree();
					saveAs(map);
				}catch (NodeInvalidException ex) {
					displayError(ex);
				}
			}
		});
		toolbar.addSeparator();
		toolbar.add(saveAsButton);
//		if(new File(EDIT_HAR).exists() == false){
//			deployButton.setEnabled(false);
//			saveAsButton.setEnabled(false);
//		}
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = FileSelector.selectImageFile(loadButton, FileSelector.HAR_FILTER, true);
				if(file != null){
					final Map<String, Object> map = HCjar.loadHar(file, true);
					if(map.isEmpty()){
						JOptionPane.showMessageDialog(self, "Error load project from har file.", 
								"Error load", JOptionPane.ERROR_MESSAGE);
					}else{
						final String licenseURL = ((String)map.get(HCjar.PROJ_LICENSE)).trim();
						final IBiz succLoadBiz = new IBiz() {
							@Override
							public void start() {
								showHCMessage("Successful load project from har file.", 
										"Success load", self, true);
							}
							
							@Override
							public void setMap(HashMap map) {
							}
						};
						if(licenseURL.length() > 0){
							IBiz biz = new IBiz(){
								@Override
								public void setMap(HashMap map) {
								}

								@Override
								public void start() {
									if(checkAndLoad(map, succLoadBiz) == false){
										return;
									}
								}
							};
							App.showHARProjectAgreeLicense("License of [" + map.get(HCjar.PROJ_NAME) + "]", licenseURL, biz, true, self);
							return;
						}
						if(checkAndLoad(map, succLoadBiz) == false){
							return;
						}

//						JOptionPane.showMessageDialog(this, "Successful import project from jar file.", 
//									"Success load", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}
		});
		loadButton.setToolTipText("<html>load and edit a project from a har file." +
				"<BR><BR>it will be setted as current project after click '" + saveButton.getText() + "'.</html>");
		toolbar.add(loadButton);
		
		shiftProjButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showLinkPanel(self, false, shiftProjButton);
//				new ShiftProjectPanel(self, false);
			}
		});
		
		toolbar.addSeparator();

		shiftProjButton.setToolTipText("<html>add and delete projects, or choose other project for editing.</html>");

		toolbar.add(shiftProjButton);
		toolbar.addSeparator();
		
		helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showWizard();
			}
		});
		helpButton.setToolTipText("show tutorial steps of using Mobile UI Designer.");
		toolbar.add(helpButton);
		toolbar.addSeparator();
		
		JPanel treePanel = new JPanel();
		treePanel.setBorder(new TitledBorder("Menu Tree :"));
		final JScrollPane scrollPane = new JScrollPane(tree);
		treePanel.setLayout(new BorderLayout());
		treePanel.add(scrollPane, BorderLayout.CENTER);
		
		editPanel.setLayout(new BorderLayout());
		editPanel.add(nodeEditPanel);
		
		final JSplitPane panelSubMRInfo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				treePanel, editPanel);

		setLayout(new BorderLayout());
		add(panelSubMRInfo, BorderLayout.CENTER);
		
		JPanel toolbarLeftPanel = new JPanel();
		toolbarLeftPanel.setLayout(new BorderLayout());
		toolbarLeftPanel.add(toolbar, BorderLayout.WEST);
		add(toolbarLeftPanel, BorderLayout.NORTH);
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				notifyCloseWindow();
			}
		});

		//该行命令不能置于loadMainProject之后，因为导致resize事件不正确
		toVisiableAndLocation(panelSubMRInfo);
		if(har == null || har.exists() == false){
			new Thread(){
				@Override
				public void run() {
					try{
						Thread.sleep(200);
					}catch (Exception e) {
					}
					self.setEnabled(false);
					final JDialog waiting = new JDialog(self, true);
					waiting.setUndecorated(true);
					final Container contentPane = waiting.getContentPane();
					JPanel panel = new JPanel(new BorderLayout());
					panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
					panel.add(new JLabel("loading default project ...", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING),
							BorderLayout.CENTER);
					contentPane.add(panel);
					waiting.pack();
					
					//必须先启thread，因为waiting是modal，会阻塞。
					new Thread(){
						public void run(){
							if(loadMainProject(self)){
								JPanel jpanel = new JPanel(new BorderLayout());
								jpanel.add(new JLabel("<html>" +
										"<IMG src='http://homecenter.mobi/images/ok_16.png' width='16' height='16'/>&nbsp;" +
										"<STRONG>default project had been created successfully!</STRONG>" +
										"</html>", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING));
								waiting.dispose();
								
								ActionListener al = new ActionListener() {
									@Override
									public void actionPerformed(ActionEvent e) {
										showWizard();
									}
								};
								App.showCenterPanel(jpanel, 0, 0, (String)ResourceUtil.get(IContext.INFO), 
										false, null, null, al, al, null, false, true, null, true, false);//jdk 8会出现漂移
							}else{
								waiting.dispose();
							}
							self.setEnabled(true);
						}
					}.start();
					App.showCenter(waiting);
				}
			}.start();
		}
	}
	
	private File loadDefaultEdit() {
		boolean isOverride = false;
		File edit_har = new File(LinkProjectManager.EDIT_HAR);
		if(edit_har.exists() == false){
			return null;
		}
		
		final String last_edit_id = PropertiesManager.getValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID);
		if(last_edit_id != null){
			final LinkProjectStore lps = LinkProjectManager.getProjByID(last_edit_id);
			if(lps != null){
				final String editVer = PropertiesManager.getValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_VER);
				if(StringUtil.higer(lps.getVersion(), editVer)){
					final int out = App.showOptionDialog(instance, "<html>the active version(" + lps.getVersion() + ", auto upgrade) is " +
							"<font style='color:red'>higher</font> than the editing version(" + editVer + ")." +
							"<br><br>override the editing version?</html>", "override the editing version?");
					if(out == JOptionPane.YES_OPTION){
						isOverride = true;
						LinkProjectManager.copyCurrEditFromStorage(lps);
					}
				}
			}
		}
		
		try{
			Map<String, Object> map = HCjar.loadHar(edit_har, true);
			if(map.isEmpty()){
				ThirdlibManager.copy(edit_har, new File(LinkProjectManager.EDIT_BAK_HAR));
				throw new Exception("default har file error!");
			}
			loadNodeFromMap(map);
			if(isOverride 
					|| last_edit_id == null 
					|| (last_edit_id.equals((String)map.get(HCjar.PROJ_ID)) == false)){
				recordEditProjInfo(map);
			}
		}catch (Throwable e) {
			JOptionPane.showMessageDialog(null, "<html>default har project is error, which is copy to <strong>" + LinkProjectManager.EDIT_BAK_HAR + "</strong>!" +
					"<BR>system will create default project.</html>", 
					"Project load error", 
					JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
			edit_har = null;
		}
		return edit_har;
	}

	private void toVisiableAndLocation(final JSplitPane panelSubMRInfo) {
		if(LocationComponentListener.hasLocation(this) && LocationComponentListener.loadLocation(this)){
			setVisible(true);
		}else{
			setPreferredSize(new Dimension(1024, 768));
			pack();
			App.showCenter(this);
		}
		
		//一定要在setVisiable(true)之后
		String dviLoca = PropertiesManager.getValue(PropertiesManager.p_DesignerDividerLocation);
		if(dviLoca == null){
			panelSubMRInfo.setDividerLocation(.3);
		}else{
			panelSubMRInfo.setDividerLocation(Integer.parseInt(dviLoca));
		}
		
		panelSubMRInfo.addPropertyChangeListener(new java.beans.PropertyChangeListener() {  
            public void propertyChange(java.beans.PropertyChangeEvent evt) {  
                if (evt.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {  
                    PropertiesManager.setValue(PropertiesManager.p_DesignerDividerLocation, 
                    		String.valueOf(panelSubMRInfo.getDividerLocation()));
                    PropertiesManager.saveFile();
                }  
            }  
        });
	}
	
	public boolean loadMainProject(final JFrame self){
		try {
			final String url = "http://homecenter.mobi/download/main.har";
			Map<String, Object> map = HCjar.loadJar(url);
			if(map.isEmpty()){
				JPanel panel = new JPanel();
				panel.add(new JLabel("<html>Sorry, Load main har from remote server error." +
						"<BR>please try after a minute</html>", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
				App.showCenterPanel(panel, 0, 0, "Network Error", false, null, null, null, null, this, true, false, null, false, false);
				return false;
			}
			
			loadNodeFromMap(map);
			
			map = save();
			
			if(map == null){
				return false;
			}
			
			//初建工程时，不作active操作
			//deployProcess(map);
			
			return true;
		}catch (Throwable e) {
		}finally{
		}
		return false;
	}
	
	public static void expandTree(JTree tree) { 
		tree.updateUI();
        TreeNode root = (TreeNode) tree.getModel().getRoot(); 
        expand(tree, new TreePath(root)); 
    } 

   
    private static void expand(JTree tree, TreePath parent) { 
        TreeNode node = (TreeNode) parent.getLastPathComponent(); 
        if (node.getChildCount() >= 0) { 
            for (Enumeration e = node.children(); e.hasMoreElements(); ) { 
                TreeNode n = (TreeNode) e.nextElement(); 
                TreePath path = parent.pathByAddingChild(n); 
                expand(tree, path); 
            } 
        } 

        tree.expandPath(parent); 
//      tree.collapsePath(parent); 
    }

	private void loadNodeFromMap(Map<String, Object> map) {
		delAllChildren();
		
		mainMenuNode = HCjar.toNode(map, root, shareFolders);
		appendShareTop();
		
		changeTreeNodeContext(root, itemContext);
		
		final boolean hasMainMenu = (mainMenuNode != null);
		if(hasMainMenu){
			DefaultMutableTreeNode childNode;
			if(mainMenuNode.getChildCount() > HCjar.SKIP_SUB_MENU_ITEM_NUM){
				childNode = (DefaultMutableTreeNode)mainMenuNode.getChildAt(HCjar.SKIP_SUB_MENU_ITEM_NUM);
			}else{
				childNode = (DefaultMutableTreeNode)mainMenuNode.getChildAt(0);
			}
			jumpToNode(childNode);
			notifySelectNode(childNode, (HPNode)childNode.getUserObject());
		}else{
			//没有菜单的空工程情形
		}
		addItemButton.setEnabled(hasMainMenu);
		activeButton.setEnabled(true);
		
		refresh();
		
		needRebuildTestJRuby = true;
		expandTree(tree);
	}
	
	public void refresh(){
		//由于可能切换，所以刷新
		LinkProjectManager.reloadLinkProjects();
		
		LinkProjectStore lps = LinkProjectManager.getProjByID(getCurrProjID());
		deactiveButton.setEnabled(lps != null && lps.isActive());
	}

	public void addNode(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode newNode) {
		changeTreeNodeContext(newNode, itemContext);
		
		if(parentNode == null){
			parentNode = selectedNode;
		}
		
		parentNode.add(newNode);
		// --------下面代码实现显示新节点（自动展开父节点）-------
		TreeNode[] nodes = model.getPathToRoot(newNode);
		TreePath path = new TreePath(nodes);
		tree.setSelectionPath(path);
//				tree.setExpandsSelectedPaths(true); 
		tree.scrollPathToVisible(path);
		tree.updateUI();
		setModified(true);
		
		notifySelectNode(newNode, (HPNode)newNode.getUserObject());
	}
	
	public void saveJar(){
		Object userobj = selectedNode.getUserObject();
		if(userobj instanceof HCShareFileResource){
			File saveTo = FileSelector.selectImageFile(this, FileSelector.JAR_FILTER, false);
			if(saveTo != null){
				if(saveTo.toString().endsWith(JAR_EXT)){
					
				}else{
					saveTo = new File(saveTo.getPath() + JAR_EXT);
				}
				
				FileOutputStream fos = null;
				try{
					fos = new FileOutputStream(saveTo);
					fos.write(((HCShareFileResource)userobj).content);
					fos.flush();
					fos.close();
					
					JPanel ok = new JPanel();
					try {
						ok.add(new JLabel("Successful save jar file!", new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)), SwingConstants.LEFT));
					} catch (IOException e2) {
					}
					App.showCenterPanel(ok, 0, 0, "Save OK!", false, null, null, null, null, this, true, false, null, false, false);
				}catch (Exception e) {
					JPanel ok = new JPanel();
					try {
						ok.add(new JLabel("Error save jar file!", new ImageIcon(ImageIO.read(ImageSrc.CANCEL_ICON)), SwingConstants.LEFT));
					} catch (IOException e2) {
					}
					App.showCenterPanel(ok, 0, 0, "Save Error!", false, null, null, null, null, this, true, false, null, false, false);
					e.printStackTrace();
				}finally{
					try{
						fos.close();
					}catch (Exception e) {
						
					}
				}
			}
		}
	}

	public void delNode() {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selectedNode.getParent();
		model.removeNodeFromParent(selectedNode);
		setModified(true);
		selectedNode = null;
		
		jumpToNode(parent);
		notifySelectNode(parent, (HPNode)parent.getUserObject());
	}
	
	public TreeNode getCurrSelectedNode(){
		return selectedNode;
	}

	public DefaultMutableTreeNode getMainMenuNode(){
		return mainMenuNode;
	}

	public void notifySelectNode(DefaultMutableTreeNode sNode, final HPNode nodeData) {
		selectedNode = sNode;
		
		NodeEditPanel nep = nm.switchNodeEditPanel(nodeData.type);
		editPanel.remove(nodeEditPanel);
		
		if(nep == null){
			nodeEditPanel = Designer.emptyNodeEditPanel;
		}else{
			nodeEditPanel = nep;
		}
		
		nodeEditPanel.init(sNode, tree);
		
		editPanel.add(nodeEditPanel);
		editPanel.validate();
		editPanel.revalidate();
		editPanel.repaint();
	}

	@Override
	public boolean isModified() {
		return isModified;
	}

	@Override
	public void setModified(boolean modified) {
		J2SEContext.refreshActionMS(false);
		this.isModified = modified;
		saveButton.setEnabled(modified);
	}

	/**
	 * 
	 * @return 返回0表示，忽略修改
	 */
	private int modifyNotify() {
		return App.showOptionDialog(instance,
				"project is modified and save now?",
				"save project now?");
	}
	
	private void jumpToNode(DefaultMutableTreeNode node) {
		TreeNode[] nodes = model.getPathToRoot(node);
		TreePath path = new TreePath(nodes);
		tree.setSelectionPath(path);
	}
	
	private static void showHCMessage(final String message, final String title, JFrame frame, boolean model){
		JPanel panel = new JPanel();
		try {
			panel.add(new JLabel(message, new ImageIcon(ImageIO.read(ImageSrc.OK_ICON)), SwingConstants.LEFT));
		} catch (IOException e2) {
		}
		App.showCenterPanel(panel, 0, 0, title, false, null, null, null, null, frame, model, false, null, false, false);
	}
	
	/**
	 * 检查内存变更
	 * @param self
	 * @return
	 */
	private boolean discardModiWhenShift(final Designer self) {
		if(self.isModified()){
			int out = discardQues(self);
			if(out == JOptionPane.YES_OPTION){
				setModified(false);
				return true;
			}
			return false;
		}
		return true;
	}

	private int discardQues(final Designer self) {
		return App.showOptionDialog(self,
				"project is modified, discard all now?",
				"discard now?");
	}

	/**
	 * 
	 * @param self
	 * @return true:继续后续操作；false:中止
	 */
	private boolean quitToNewProj(final Designer self) {
		if(self.isModified()){
			int out = modifyNotify();
			if(out == JOptionPane.YES_OPTION){
				Map<String, Object> map = save();
				if(map == null){
					return false;
				}
			}else if(out == JOptionPane.NO_OPTION){
				self.setModified(false);				
			}else if(out == JOptionPane.CLOSED_OPTION || out == JOptionPane.CANCEL_OPTION){
				return false;
			}
			return true;
		}
		return true;
	}

	private Object buildInitRoot() {
		DefaultMutableTreeNode tnode = createNewRoot();
		
		delAllChildren();
		
		final Object userObject = tnode.getUserObject();
		root.setUserObject(userObject);

		changeTreeNodeContext(root, itemContext);
		
		appendShareTop();
		
		return userObject;
	}

	private void delAllChildren() {
		root.removeAllChildren();
		for (int i = 0; i < shareFolders.length; i++) {
			shareFolders[i].removeAllChildren();
		}
	}

	private void appendShareTop() {
		//加载共享库结点，
		//注意：如果ROOT下增加新结点，请更新参数SUB_NODES_OF_ROOT_IN_NEW_PROJ + 1
		HPShareRoot sr = new HPShareRoot(HPNode.MASK_SHARE_TOP, "resources");
		
		DefaultMutableTreeNode shareRoot = new DefaultMutableTreeNode(sr);
		root.add(shareRoot);
		
		//加挂公用JRuby库
		for (int i = 0; i < shareFolders.length; i++) {
			shareRoot.add(shareFolders[i]);	
		}
	}

	/**
	 * 返回true，表示继续。
	 * @param map
	 * @return
	 */
	private boolean checkLoadVer(Map<String, Object> map) {
		String[] sample_ver = {(String)map.get(HCjar.HOMECENTER_VER), 
				(String)map.get(HCjar.JRE_VER), (String)map.get(HCjar.JRUBY_VER)};
		String[] curr_ver = {StarterManager.getHCVersion(), String.valueOf(App.getJREVer()), 
				PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer, "1.0")};
		boolean[] isSampleHigh = {false, false, false};
		boolean isHigh = false;
		for (int i = 0; i < sample_ver.length; i++) {
			if(sample_ver[i] != null){
				if(StringUtil.higer(sample_ver[i], curr_ver[i])){
					isSampleHigh[i] = true;
					isHigh = true;
				}
			}else{
				sample_ver[i] = "unknow ver";
			}
		}

		
		if(isHigh){
			String sample = "";
			String curr = "";
			
			String[] whatVer = {"HomeCenter Ver", "JRE Ver", "JRuby Ver"};
			for (int i = 0; i < isSampleHigh.length; i++) {
				if(isSampleHigh[i] == false){
					continue;
				}
				boolean isNotFirst = (sample.length() > 0);
				sample += whatVer[i] + ":" + sample_ver[i] + (isNotFirst?",   ":"");
				curr += whatVer[i] + ":" + (isSampleHigh[i]?
						("<font style='color:red'>" + curr_ver[i] + "</font>"):curr_ver[i]) + (isNotFirst?",":"");
				
				if(isNotFirst){
					for (int j = 0, addNum = 3 + (sample_ver[i].length() - curr_ver[i].length()); 
							j < addNum; j++) {
						curr += " ";
					}
				}
			}
			
			sample = "required version : [" + sample + "]";
			curr   = "current version : [" + curr + "]";
			final boolean[] isContinue = {true};
			JPanel panel = new JPanel();
			panel.add(new JLabel("<html><body>" + sample + "<BR>" + curr + "<BR><BR>" +
					"some data may ignore, or run error.<BR><BR>continue any more?" + 
//					(isSampleHigh[0]?("<BR>please 'enable auto upgrade' to upgrade HomeCenter Ver"):"") + 
					"</body></html>", 
					App.getSysIcon(App.SYS_WARN_ICON), SwingConstants.LEFT));
			App.showCenterPanel(panel, 0, 0, "version warning", true, null, null, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
				}
			}, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					isContinue[0] = false;
				}
			}, this, true, false, null, false, false);
			
			if(isContinue[0] == false){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 如果出错，则返回false，以便中止运行脚本。
	 * @return
	 */
	public boolean tryBuildTestJRuby(){
		if(needRebuildTestJRuby){
			try{
				Map<String, Object> map = buildMapFromTree();
				deployRunTest(map);
				needRebuildTestJRuby = false;
			}catch (NodeInvalidException e) {
				displayError(e);
				return false;
			}
		}
		return true;
	}

	private Map<String, Object> save() {
		try{
			Map<String, Object> map = buildMapFromTree();
			
			final File edit_har = new File(LinkProjectManager.EDIT_HAR);
			HCjar.toHar(map, edit_har);
			
			tree.updateUI();
			
			setModified(false);
			
			saveAsButton.setEnabled(true);
			
			LinkProjectStore oldlps = LinkProjectManager.getProjByID(getCurrProjID());
			if(oldlps == null){
				oldlps = LinkProjectManager.getProjLPSWithCreate(getCurrProjID());
				LinkProjectManager.importLinkProject(oldlps, edit_har);

//				final boolean isRoot = LinkProjectManager.getProjectSize()<2?true:oldlps.isRoot();
				//缺省创建时，root=false, active=false
				LinkProjectManager.saveProjConfig(oldlps, false, false);
			}

			recordEditProjInfo(map);
			
			LinkProjectManager.saveToLinkBack(oldlps, edit_har);
			return map;
		}catch (NodeInvalidException e) {
			displayError(e);
			
			return null;
		}
	}

	public static void recordEditProjInfo(Map<String, Object> map) {
		PropertiesManager.setValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID, (String)map.get(HCjar.PROJ_ID));
		PropertiesManager.setValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_VER, (String)map.get(HCjar.PROJ_VER));
		PropertiesManager.saveFile();
	}

	public void displayError(NodeInvalidException e) {
		final DefaultMutableTreeNode node1 = e.node;
		jumpToNode(node1);
		notifySelectNode(node1, (HPNode)node1.getUserObject());
		
		JPanel panel = new JPanel();
		String name1 = ((HPNode)node1.getUserObject()).name;
		panel.add(new JLabel("<html><body>" + e.getDesc() + "</body></html>", 
				App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
		App.showCenterPanel(panel, 0, 0, "Error Node", false, null, null, null, null, this, true, false, null, false, false);
	}

	public static void deployRunTest(Map<String, Object> deployMap) {
		ScriptEditPanel.terminateJRubyEngine();

		//清空runtemp
		File[] subFiles = UpgradeManager.RUN_TEST_DIR.listFiles();
		if(subFiles != null){
			for (int i = 0; i < subFiles.length; i++) {
				subFiles[i].delete();
			}
		}
		
		final boolean forceBuild = (deployMap == null);
		if(forceBuild){
			//toMap不能上述相同代码合并，因前者存储时，对map是一次使用，并删除了二进制数据
			final File jarfile = new File(LinkProjectManager.EDIT_HAR);
			deployMap = HCjar.loadHar(jarfile, true);
		}

		//因为升级到新版本时，可能恰好又是用户新装，所以需要检查文件是否存在
		if(ProjResponser.deloyToWorkingDir(deployMap, UpgradeManager.RUN_TEST_DIR) 
				|| forceBuild //注意：forceBuild必须放后面
				|| (ScriptEditPanel.runTestEngine == null)){//极端情形，即初始加载时，该对象为null，必须进行build
			ScriptEditPanel.rebuildJRubyEngine();
		}
	}

	private void saveAs(final Map<String, Object> map) {
		File file = FileSelector.selectImageFile(saveAsButton, FileSelector.HAR_FILTER, false);
		if(file != null){
			if(file.toString().endsWith(HAR_EXT)){
				
			}else{
				file = new File(file.getPath() + HAR_EXT);
			}
			final File fileExits = file;
			final String absolutePath = fileExits.getAbsolutePath();
			final String upgradeURL = ((String)map.get(HCjar.PROJ_UPGRADE_URL)).trim();
			final File fileHadExits = (upgradeURL.length() > 0 && upgradeURL.endsWith(HAD_EXT))?new File(HCjad.convertToExtHad(absolutePath)):null;
			final Designer self = this;
			if(file.exists()){
				JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				panel.add(new JLabel("Override exit har file?", App.getSysIcon(App.SYS_QUES_ICON), 
						SwingConstants.LEADING));
				App.showCenterPanel(panel, 0, 0, "Override?", true, null, null, new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						doExportBiz(map, fileExits, fileHadExits, self);
					}
				}, null, this, true, false, null, false, false);
			}else{
				doExportBiz(map, fileExits, fileHadExits, self);
			}
		}
	}

	private boolean deployProcess(final Map<String, Object> map) {
		LinkProjectStore oldlps = LinkProjectManager.getProjByID(getCurrProjID());
		if(oldlps != null){
			final String curVer = (String)map.get(HCjar.PROJ_VER);
			if(StringUtil.higer(oldlps.getVersion(), curVer)){
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(new JLabel("<html>the actived version is " + oldlps.getVersion() + ", and current version is " + curVer 
						+ "<br><br>please override the current version!</html>", 
						App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
				App.showCenterPanel(panel, 0, 0, "fail activate current project", false, null, null, null, null, instance, true, false, null, false, false);
				return false;
			}
		}
		
		setProjectOn();
		
		final File newVerHar = new File(LinkProjectManager.EDIT_HAR);

		deactiveButton.setEnabled(true);
		
		synchronized (ServerUIUtil.LOCK) {
			ServerUIUtil.promptAndStop(true, this);
			
			if(oldlps != null){
				LinkProjectManager.removeLinkProjectPhic(oldlps, false);
			}else{
				oldlps = LinkProjectManager.getProjLPSWithCreate(getCurrProjID());
			}
			LinkProjectManager.importLinkProject(oldlps, newVerHar);
			
			boolean changeToRoot = oldlps.isRoot();
			
			if(changeToRoot == false){
				//是否需要升级为root
				final LinkProjectStore oldroot = LinkProjectManager.searchRoot(true);
				if(oldroot == null){
					changeToRoot = true;
				}
			}
			
			LinkProjectManager.saveProjConfig(oldlps, changeToRoot, true);
		
			ServerUIUtil.restartResponsorServerDelayMode();
		}
		return true;
	}

	public static void setProjectOn() {
		ServerUIUtil.useMainCanvas = true;
		if(PropertiesManager.isTrue(PropertiesManager.p_IsMobiMenu) == false){
			PropertiesManager.setValue(PropertiesManager.p_IsMobiMenu, IConstant.TRUE);
			PropertiesManager.saveFile();
		}
	}

	private void doExportBiz(final Map<String, Object> map, final File fileExits, final File fileHadExits, final Designer self) {
		HCjar.toHar(map, fileExits);
		if(fileHadExits != null){
			HCjad.toHad(map, fileHadExits, ResourceUtil.getMD5(fileExits), (int)fileExits.length());
		}
		showHCMessage("Successful save as project to har file.", 
				"Success save as", self, true);
	}

	private Map<String, Object> buildMapFromTree() throws NodeInvalidException {
		return HCjar.toMap(root, shareFolders);
	}
	public static Window currLink;
	
	/**
	 * 注意：本方法被反射引用，并不要更名。
	 */
	public static boolean notifyCloseDesigner(){
		return Designer.getInstance().notifyCloseWindow();
	}
	
	/**
	 * 注意：本方法被反射引用，并不要更名。
	 */
	public static void closeLinkPanel() {
		if(Designer.currLink instanceof ClosableWindow){
			((ClosableWindow)Designer.currLink).notifyClose();
		}else{
			Designer.currLink.dispose();
		}
	}
	
	/**
	 * 注意：本方法被反射引用，并不要更名。
	 * @param parent
	 * @param newFrame
	 * @param relativeTo
	 */
	public static void showLinkPanel(JFrame parent, final boolean newFrame, final Component relativeTo){
		if(LinkProjectStatus.tryEnterStatus(parent, LinkProjectStatus.MANAGER_IMPORT) == false){
			return;
		}
		if(currLink != null && currLink.isShowing()){
			currLink.toFront();
		}else{
			LinkProjectManager.reloadLinkProjects();
			
			LinkProjectPanel linkp = new LinkProjectPanel(parent, newFrame, relativeTo);
			currLink = linkp.toShow();
		}
	}
	
	public void shiftProject(final LinkProjectStore lps){
		if(getCurrProjID().equals(lps.getProjectID())){

			refresh();
			if(discardModiWhenShift(instance)){
				String md5Editing = ResourceUtil.getMD5(new File(LinkProjectManager.EDIT_HAR));
				String md5back = ResourceUtil.getMD5(LinkProjectManager.buildBackEditFile(lps));

				if((md5Editing.equals(md5back) == false)){
					int out = App.showOptionDialog(instance,
							"current edit project is out of sync, override now?",
							"override edit?");
					if(out == JOptionPane.YES_OPTION){
						//丢弃物理变更
						LinkProjectManager.copyCurrEditFromStorage(lps);
					}
				}
				//丢弃内存变更
				loadDefaultEdit();
			}
		}else{
			if(discardModiWhenShift(instance)){
				LinkProjectStore oldlps = LinkProjectManager.getProjByID(getCurrProjID());
				
				if(oldlps != null){
					String md5Editing = ResourceUtil.getMD5(new File(LinkProjectManager.EDIT_HAR));
					String md5back = ResourceUtil.getMD5(LinkProjectManager.buildBackEditFile(oldlps));
	
					if((md5Editing.equals(md5back) == false)){
						int out = discardQues(instance);
						if(out == JOptionPane.YES_OPTION){
							//放弃源已修编工程，切换到宿工程
							LinkProjectManager.copyCurrEditFromStorage(lps);
							loadDefaultEdit();
						}
					}else{
						//切换到宿工程
						LinkProjectManager.copyCurrEditFromStorage(lps);
						loadDefaultEdit();
					}
				}else{
					//Link中已删除，但default仍保留
					//切换到宿工程
					LinkProjectManager.copyCurrEditFromStorage(lps);
					loadDefaultEdit();
				}
			}			
		}
	}

	private void showWizard() {
		JButton[] btns = {activeButton, deactiveButton, sampleButton, addItemButton, saveAsButton, loadButton};
		Wizard wiz = new Wizard(btns);
		wiz.showTip();
	}

	public boolean notifyCloseWindow() {
		if(isModified()){
			int out = modifyNotify();
			if(out == JOptionPane.YES_OPTION){
				Map<String, Object> map = save();
				if(map == null){
				}
			}
			if(out == JOptionPane.YES_OPTION || out == JOptionPane.NO_OPTION){
				dispose();
				return true;
			}
		}else{
			dispose();
			return true;
		}
		return false;
	}

	private boolean checkAndLoad(Map<String, Object> map, final IBiz succBiz) {
		if(checkLoadVer(map) == false){
			return false;
		}
		
		loadNodeFromMap(map);
		setModified(true);
		
		succBiz.start();
		return true;
	}

	public static void setProjectOff() {
		ServerUIUtil.useMainCanvas = false;
		
		PropertiesManager.setValue(PropertiesManager.p_IsMobiMenu, IConstant.FALSE);
		PropertiesManager.saveFile();
	}

	public static void main(String args[]) {
		//keepMethodFromKillByProguard
		if(args.length > 10000000){
			//provent proguard del unused method.
			Designer.startAutoUpgradeBiz();
			Designer.closeLinkPanel();
			Designer.notifyCloseDesigner();
		}
	}
}

class NodeTreeCellRenderer extends DefaultTreeCellRenderer {
	NodeTreeCellRenderer() {
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		Component compo = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		Object o = ((DefaultMutableTreeNode) value).getUserObject();
		if (o instanceof HPNode) {
			HPNode country = (HPNode) o;
			if (HPNode.isNodeType(country.type, HPNode.MASK_ROOT)) {
				((JLabel)compo).setIcon(Designer.iconRoot);
			} else if (HPNode.isNodeType(country.type, HPNode.MASK_MENU_ITEM)) {
				((JLabel)compo).setIcon(Designer.iconMenuItem);
			} else if (HPNode.isNodeType(country.type, HPNode.MASK_MENU)) {
				((JLabel)compo).setIcon(Designer.iconMenu);
			} else if (HPNode.isNodeType(country.type, HPNode.MASK_SHARE_TOP)) {
				((JLabel)compo).setIcon(Designer.iconShareRoot);
			} else if (HPNode.isNodeType(country.type, HPNode.MASK_SHARE_RB_FOLDER)) {
				((JLabel)compo).setIcon(Designer.iconShareRBFolder);
			} else if (HPNode.isNodeType(country.type, HPNode.MASK_SHARE_RB)) {
				((JLabel)compo).setIcon(Designer.iconShareRB);
			} else if (country.type == HPNode.MASK_RESOURCE_FOLDER_JAR) {
				((JLabel)compo).setIcon(Designer.iconJarFolder);
			} else if (country.type == HPNode.MASK_RESOURCE_JAR) {
				((JLabel)compo).setIcon(Designer.iconJar);
			} else if (country.type == HPNode.MASK_EVENT_FOLDER) {
				((JLabel)compo).setIcon(Designer.iconEventFolder);
			} else if (country.type == HPNode.MASK_EVENT_ITEM) {
				((JLabel)compo).setIcon(Designer.iconEventItem);
			}
			((JLabel)compo).setText(country.toString());
		} else {
			((JLabel)compo).setIcon(null);
			((JLabel)compo).setText("" + value);
		}
		
		return compo;
	}
	
}