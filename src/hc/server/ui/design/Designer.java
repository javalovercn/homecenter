package hc.server.ui.design;

import hc.App;
import hc.UIActionListener;
import hc.core.ConditionWatcher;
import hc.core.ContextManager;
import hc.core.IConstant;
import hc.core.IContext;
import hc.core.IWatcher;
import hc.core.L;
import hc.core.util.CCoreUtil;
import hc.core.util.ExceptionReporter;
import hc.core.util.HCURL;
import hc.core.util.LogManager;
import hc.core.util.StringUtil;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.FileSelector;
import hc.server.HCActionListener;
import hc.server.HCButtonEnabledActionListener;
import hc.server.HCWindowAdapter;
import hc.server.J2SEContext;
import hc.server.JRubyInstaller;
import hc.server.PlatformManager;
import hc.server.ProcessingWindowManager;
import hc.server.SingleJFrame;
import hc.server.StarterManager;
import hc.server.ThirdlibManager;
import hc.server.data.StoreDirManager;
import hc.server.ui.ClientSession;
import hc.server.ui.ClosableWindow;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.LocationComponentListener;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.code.CodeHelper;
import hc.server.ui.design.code.CodeStaticHelper;
import hc.server.ui.design.hpj.HCShareFileResource;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.server.ui.design.hpj.HPItemContext;
import hc.server.ui.design.hpj.HPMenuEvent;
import hc.server.ui.design.hpj.HPNode;
import hc.server.ui.design.hpj.HPProject;
import hc.server.ui.design.hpj.HPShareJRubyFolder;
import hc.server.ui.design.hpj.HPShareJar;
import hc.server.ui.design.hpj.HPShareJarFolder;
import hc.server.ui.design.hpj.HPShareRoot;
import hc.server.ui.design.hpj.IModifyStatus;
import hc.server.ui.design.hpj.MenuManager;
import hc.server.ui.design.hpj.NodeEditPanel;
import hc.server.ui.design.hpj.NodeEditPanelManager;
import hc.server.ui.design.hpj.NodeInvalidException;
import hc.server.ui.design.hpj.ScriptEditPanel;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.IDArrayGroup;
import hc.server.util.JavaLangSystemAgent;
import hc.util.BaseResponsor;
import hc.util.Constant;
import hc.util.IBiz;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class Designer extends SingleJFrame implements IModifyStatus, BindButtonRefresher{
	private static final String HC_RES_MY_FIRST_HAR = "hc/res/MyFirst.har";

	final Runnable updateTreeRunnable = new Runnable() {
		@Override
		public void run() {
			tree.updateUI();
		}
	};
	
	public final ClientSession testSimuClientSession = new ClientSession();
	
	final Runnable buildMemWatcher(){
		return new Runnable() {
			Designer self = Designer.this;
			boolean isShowLowMem = false;
			final int lowMem60 = 60;
			int meetCount = 0;
			
			@Override
			public void run() {
				try{
					Thread.sleep(10 * 1000);//等待designer显示
				}catch (final Exception e) {
				}
				
				while(isShowLowMem == false){
					if(self.isVisible() == false){
						break;
					}
					
					final long freeMem = PlatformManager.getService().getFreeMem();
					if(freeMem < lowMem60){
						meetCount++;
						
						if(meetCount == 2){
							nm.clearCacheEditPanel();
							
							L.V = L.O ? false : LogManager.log("current free memory (M) : " + freeMem);
							isShowLowMem = true;
							final String lowMem = StringUtil.replace((String)ResourceUtil.get(9207), "{mem}", String.valueOf(lowMem60));
							
							final JPanel panel = new JPanel(new GridBagLayout());
							final GridBagConstraints c = new GridBagConstraints();
							c.anchor = GridBagConstraints.LINE_START;
							panel.add(new JLabel(lowMem, App.getSysIcon(App.SYS_WARN_ICON), SwingConstants.LEADING), c);
							final JCheckBox noDisplayAgain = new JCheckBox((String)ResourceUtil.get(9209));
							noDisplayAgain.addChangeListener(new ChangeListener() {
								@Override
								public void stateChanged(final ChangeEvent e) {
									final String isNoDisplay = noDisplayAgain.isSelected()?IConstant.FALSE:IConstant.TRUE;
									PropertiesManager.setValue(PropertiesManager.p_isLowMemWarnInDesigner, isNoDisplay);
									PropertiesManager.saveFile();
								}
							});
							c.gridy = 1;
							c.insets = new Insets(10, 0, 0, 0);
							panel.add(noDisplayAgain, c);
							
							App.showCenterPanel(panel, 0, 0, ResourceUtil.getWarnI18N(), false, null, null, null, null, self, true, false, null, false, true);
							
							return;
						}
					}else{
						meetCount = 0;
					}
					
					try{
						Thread.sleep(10 * 1000);
					}catch (final Exception e) {
					}
				}// end while
			}
		};
	}
	
	public static final String HAR_EXT = ".har";
	public static final String HAD_EXT = ".had";
	public static final String JAR_EXT = ".jar";
	public static final String MY_DEPLOY_PROJ = "myproj.jar";
	public static final String MY_DEPLOY_PROJ_HAR = "myproj.har";
	public static final String OLD_EDIT_JAR = "myedit.jar";
	
	public static final String EXAMPLE = "Demo";
	
	public static final int SUB_NODES_OF_ROOT_IN_NEW_PROJ = 1;
	
	private boolean needRebuildTestJRuby = true;
	
	public void setNeedRebuildTestJRuby(final boolean need){
		needRebuildTestJRuby = need;
	}
	
	public static void startAutoUpgradeBiz() {
		if(JRubyInstaller.checkInstalledJRuby() == false){
			JRubyInstaller.startInstall();
		}else{		
			try{
				LinkProjectManager.startLinkedInProjectUpgradeTimer();
			}catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
	}

	public static ImageIcon loadImg(final String path){
		return new ImageIcon(loadBufferedImage(path));
	}

	public static boolean isDirectKeycode(final int keycode){
		if(keycode == KeyEvent.VK_LEFT || keycode == KeyEvent.VK_RIGHT || keycode == KeyEvent.VK_UP 
				|| keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_HOME || keycode == KeyEvent.VK_END
				|| keycode == KeyEvent.VK_PAGE_UP || keycode == KeyEvent.VK_PAGE_DOWN){
			return true;
		}else{
			return false;
		}
	}
	
	public static boolean isCopyKeyEvent(final KeyEvent e, final boolean isJ2SEServer){
		if(isJ2SEServer){
			if(e.getKeyCode() == 157 && e.getModifiers() == 0){
				return true;
			}
		}
		return false;
	}
	
	public static BufferedImage loadBufferedImage(final String path) {
		try {
			return ImageIO.read(Designer.class.getClassLoader().getResource(ImageSrc.HC_SERVER_UI_DESIGN_RES + path));
		} catch (final Exception e) {
			return null;
		}
	}
	
	@Override
	public void dispose(){
		LinkProjectStatus.exitStatus();
		
		instance = null;
		codeHelper.release();
		super.dispose();
		
		if(System.getProperty(Constant.DESIGNER_IN_TEST) != null){
			System.exit(0);
		}
	}
	
	@Override
	public void updateSkinUI(){
		super.updateSkinUI();
		
		mg.updateSkinUI();
		nm.updateSkinUI();
		
		App.invokeLaterUI(updateTreeRunnable);
	}
	
	private boolean isModified;
	final MenuManager mg = new MenuManager();

	final JToolBar toolbar;
	JTree tree;
	// 上面JTree对象对应的model
	DefaultTreeModel model;
	DefaultMutableTreeNode mainMenuNode;
	
	// 定义几个初始节点
	DefaultMutableTreeNode root = createNewRoot();
	DefaultMutableTreeNode msbFolder = createMSBFoulder();
	
	public String getCurrProjID(){
		return ((HPProject)root.getUserObject()).id;
	}
	
	public String getCurrProjVer(){
		return ((HPProject)root.getUserObject()).ver;
	}
	
	public String getProjCSS(){
		return ((HPProject)root.getUserObject()).styles;
	}
	
	public void setProjCSS(final String css){
		((HPProject)root.getUserObject()).styles = css;
	}
	
	public static final int ROOT_SUB_FOLDER = 3;
	final DefaultMutableTreeNode[] shareFolders = new DefaultMutableTreeNode[ROOT_SUB_FOLDER];

	private DefaultMutableTreeNode createShareJRubyFolder() {
		final HPShareJRubyFolder sj = new HPShareJRubyFolder(HPNode.MASK_SHARE_RB_FOLDER, 
				"share jruby files");
		return new DefaultMutableTreeNode(sj);
	}
	
	private DefaultMutableTreeNode createShareJarFolder() {
		final HPShareJarFolder sj = new HPShareJarFolder(HPNode.MASK_RESOURCE_FOLDER_JAR, 
				"share jar files");
		return new DefaultMutableTreeNode(sj);
	}
	
	private DefaultMutableTreeNode createNativeLibFolder() {
		final HPShareJarFolder sj = new HPShareJarFolder(HPNode.MASK_SHARE_NATIVE_FOLDER, 
				"native lib files");
		return new DefaultMutableTreeNode(sj);
	}
	
	private DefaultMutableTreeNode createMSBFoulder(){
		final HPMenuEvent folder = new HPMenuEvent(HPNode.MASK_MSB_FOLDER, "IoT");
		final DefaultMutableTreeNode eventFold = new DefaultMutableTreeNode(folder);
		return eventFold;
	}
	
	private DefaultMutableTreeNode createNewRoot() {
		final String projID = LinkProjectManager.buildSysProjID();
		
		final ContextSecurityConfig csc = new ContextSecurityConfig(projID);
		csc.buildDefaultPermissions();
		
		return new DefaultMutableTreeNode(new HPProject(HPNode.MASK_ROOT, "Project", projID, HPProject.DEFAULT_VER, csc));
//				HPNode.MASK_ROOT, "Project", "", projID, HPProject.DEFAULT_VER, "",
//				null, "", "", "", "", csc, ""));
	}

	// 定义需要被拖动的TreePath
	TreePath movePath;
	public final static NodeEditPanel emptyNodeEditPanel = new NodeEditPanel() {
		@Override
		public void init(final MutableTreeNode data, final JTree tree) {
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
	final JButton addItemButton = new JButton("Add Item", loadImg("controller_24.png"));
	JButton newButton = new JButton("New Project", loadImg("new_24.png"));
	JButton saveAsButton = new JButton("Save as", loadImg("shareout_24.png"));
	JButton loadButton = new JButton("Load", loadImg("sharein_24.png"));
	JButton shiftProjButton = new JButton("Shift Project", new ImageIcon(ResourceUtil.loadImage("menu_24.png")));
	JButton helpButton = new JButton("Help", loadImg("faq_24.png"));
	JButton rebindButton = new JButton("Rebind", loadImg("device_24.png"));
	DefaultMutableTreeNode selectedNode;
	JPanel editPanel = new JPanel();

	private final NodeEditPanelManager nm = new NodeEditPanelManager();
	
	public final static ImageIcon iconMenuItem = loadImg("menuitem.png");

	public final static ImageIcon iconMenu = loadImg("menu.png");

	public final static ImageIcon iconRoot = loadImg("root.png");
	
	public final static ImageIcon iconDel = loadImg("del.png");
	
	public final static ImageIcon iconSaveAs = loadImg("save_16.png");
	
	public final static ImageIcon iconShareRoot = loadImg("share.png");
	
	public final static ImageIcon iconShareRBFolder = loadImg("rb_folder.png");
	
	public final static ImageIcon iconShareNativeFolder = loadImg("native_folder.png");
	
	public final static ImageIcon iconShareRB = loadImg("jruby.png");
	
	public final static ImageIcon iconShareNative = loadImg("native.png");
	
	public final static ImageIcon iconJar = loadImg("jar.png");
	
	public final static ImageIcon iconJarFolder = loadImg("jar_folder.png");
		
	public final static ImageIcon iconMSBFolder = loadImg("iot_16.png");
	public final static ImageIcon iconRobot = loadImg("robot_16.png");
	public final static ImageIcon iconDevice = loadImg("device_16.png");
	public final static ImageIcon iconDeviceGray = loadImg("device_gray_16.png");
	public final static ImageIcon iconConverter = loadImg("converter_16.png");
	
	public final static ImageIcon iconEventFolder = loadImg("radar.png");
	
	public final static ImageIcon iconEventItem = loadImg("radar_on.png");
	
	private void changeTreeNodeContext(final DefaultMutableTreeNode _node, final HPItemContext context){
		((HPNode)_node.getUserObject()).setContext(context);
		
		final int size = _node.getChildCount();
		for (int i = 0; i < size; i++) {
			changeTreeNodeContext((DefaultMutableTreeNode)_node.getChildAt(i), context);
		}
	}
	
	final HPItemContext itemContext;
	
	public static File switchHar(final File jar, final File har){
		if(jar.exists()){
			ThirdlibManager.copy(jar, har);
			jar.delete();
		}
		return har;
	}
	
	private static Designer instance;
	public final CodeHelper codeHelper;
	
	public static Designer getInstance(){
		CCoreUtil.checkAccess();
		
		return instance;
	}
	
	private static final int IDX_SHARE_JRUBY_FOLDER = 0;
	private static final int IDX_SHARE_JAR_FOLDER = 1;
	private static final int IDX_SHARE_NATIVE_FOLDER = 2;
	
	private DefaultMutableTreeNode getShareJarFolder(){
		return shareFolders[IDX_SHARE_JAR_FOLDER];
	}
	
	private final void setToolbarVisible(final JToolBar toolbar, final boolean isVisible){
		toolbar.setVisible(isVisible);
	}
	
	private final boolean isToolbarVisible(){
		return toolbar.isVisible();
	}
	
	public Designer() {
		setTitle((String)ResourceUtil.get(9034) + "[" + (String)ResourceUtil.get(9083) + "]");
		
		instance = this;
		
		if(PropertiesManager.isTrue(PropertiesManager.p_isLowMemWarnInDesigner, true)){
			ContextManager.getThreadPool().run(buildMemWatcher());
		}
		
		setIconImage(App.SYS_LOGO);
		setName("Designer");
		final ComponentListener cl = new LocationComponentListener(threadPoolToken);
		
		addComponentListener(cl);
		
		shareFolders[IDX_SHARE_JRUBY_FOLDER] = createShareJRubyFolder();
		shareFolders[IDX_SHARE_JAR_FOLDER] = createShareJarFolder();
		shareFolders[IDX_SHARE_NATIVE_FOLDER] = createNativeLibFolder();
		
		
		J2SEContext.appendTitleJRubyVer(this);
		
		itemContext = new HPItemContext();
		itemContext.modified = this;
		
		tree = new JTree(root);
		tree.getSelectionModel().setSelectionMode(  
                TreeSelectionModel.SINGLE_TREE_SELECTION); 
		model = (DefaultTreeModel) tree.getModel();		
		
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
		
		final MouseListener ml = new MouseListener() {
			@Override
			public void mousePressed(final MouseEvent e) {
			}

			// 鼠标松开时获得需要拖到哪个父节点
			@Override
			public void mouseReleased(final MouseEvent e) {
				ConditionWatcher.addWatcher(new IWatcher(){//确保正常次序处理完成
					@Override
					public boolean watch() {
						if(isToolbarVisible() == false){
							return false;
						}
						
						try{
							// 根据鼠标松开时的TreePath来获取TreePath
							final TreePath tp = tree.getPathForLocation(e.getX(), e.getY());

							if(tp == null){
								return true;
							}else{
								movePath = tp;
							}
							
							final Object obj = tp.getLastPathComponent();
							if (obj != null && (obj instanceof DefaultMutableTreeNode)) {
								selectedNode = (DefaultMutableTreeNode) obj;
							}else{
								selectedNode = null;
							}
							
							if (e.getButton() == MouseEvent.BUTTON3) {
								if(selectedNode != null){
									final Object o = selectedNode.getUserObject();
									if (o instanceof HPNode) {
										jumpToNode(selectedNode, model, tree);

										mg.popUpMenu(((HPNode) o).type, selectedNode, tree, e, self);
									}
								}
							}else if (e.getButton() == MouseEvent.BUTTON1) {
								if(selectedNode != null){
									final Object o = selectedNode.getUserObject();
									if (o instanceof HPNode) {
										final HPNode nodeData = (HPNode) o;
										notifySelectNode(selectedNode, nodeData);
									}
								}
							}
						}catch (final Throwable e) {
							ExceptionReporter.printStackTrace(e);
						}
						return true;
					}

					@Override
					public void setPara(final Object p) {
					}

					@Override
					public void cancel() {
					}

					@Override
					public boolean isCancelable() {
						return false;
					}
				});
			}

			@Override
			public void mouseClicked(final MouseEvent e) {
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
			}

			@Override
			public void mouseExited(final MouseEvent e) {
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
		
		toolbar = new JToolBar();
		setToolbarVisible(toolbar, false);
		setModified(false);
		
		{
			final Action saveAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							save();
						}
					}, threadPoolToken);
				}
			};
			ResourceUtil.buildAcceleratorKeyOnAction(saveAction, KeyEvent.VK_S);//同时支持Windows下的Ctrl+S和Mac下的Command+S
			saveButton.addActionListener(saveAction);
			saveButton.getActionMap().put("myAction", saveAction);
			saveButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
			        (KeyStroke) saveAction.getValue(Action.ACCELERATOR_KEY), "myAction");
		}
		
//		//检查是否有新版本
		final String lastSampleVer = PropertiesManager.getValue(PropertiesManager.p_LastSampleVer, "1.0");
		if(StringUtil.higher(J2SEContext.getSampleHarVersion(), lastSampleVer)){
			sampleButton.setIcon(loadImg("giftnew_24.png"));
		}
		sampleButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				if(quitToNewProj(self) == false){
					return;
				}
				
				try {
					final File tmpFileHar = ResourceUtil.getTempFileName(HAR_EXT);
					copyHarFromPath(ResourceUtil.getResource("hc/res/sample.har"), tmpFileHar);
					PropertiesManager.addDelFile(tmpFileHar);
					PropertiesManager.saveFile();
					final Map<String, Object> map = HCjar.loadHar(tmpFileHar, true);
					if(map.isEmpty()){
						final JPanel panel = new JPanel();
						panel.add(new JLabel("<html>Sorry, fail to load example project." +
								"<BR>please try after a minute</html>", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
						App.showCenterPanel(panel, 0, 0, "Network Error", false, null, null, null, null, self, true, false, null, false, false);
						return;
					}
					final IBiz succLoadBiz = new IBiz() {
						@Override
						public void start() {
							showHCMessage("" +
									"<html>Success load demo project." +
									"<BR><BR>" +
									"click '<STRONG>" + ACTIVE + "</STRONG>' button to apply it, login from mobile to view it.</html>", 
									EXAMPLE + " OK", self, true, null);
							
							sampleButton.setIcon(getSampleIcon());
							
							PropertiesManager.setValue(PropertiesManager.p_LastSampleVer, 	J2SEContext.getSampleHarVersion());
							PropertiesManager.saveFile();
						}
						
						@Override
						public void setMap(final HashMap map) {
						}
					};
					
					final String licenseURL = ((String)map.get(HCjar.PROJ_LICENSE)).trim();
					if(licenseURL.length() > 0){
						final IBiz biz = new IBiz(){
							@Override
							public void setMap(final HashMap map) {
							}

							@Override
							public void start() {
								if(checkAndLoad(map, succLoadBiz) == false){
									return;
								}
							}
						};
						App.showAgreeLicense("License of [" + map.get(HCjar.PROJ_NAME) + "]", false, licenseURL, biz, null, true);
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
				} catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
//					App.showMessageDialog(self, "Error download example, " +
//							"please try after few minutes.", "Error " + EXAMPLE, 
//							JOptionPane.ERROR_MESSAGE);
				}
			}
		}, threadPoolToken));
		sampleButton.setToolTipText("<html>" +
				"load a example project.<BR>" +
				"there are many powerful mobile menu items and sample code in it." +
//				"<BR><BR>Tip : when the version of online demo project is newer than you had imported, it will display new red star." +
				"</html>");
		toolbar.add(sampleButton);
		newButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				if(quitToNewProj(self) == false){
					return;
				}
				
				final HashMap<String, Object> map = new HashMap<String, Object>();
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
//				App.invokeLater(updateTreeRunnable);
			}
		}, threadPoolToken));
		newButton.setToolTipText("<html>create new HAR project.</html>");
		toolbar.add(newButton);
		
		addItemButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				MenuManager.addMenuItem(instance, addItemButton, mainMenuNode, msbFolder);
			}
		}, threadPoolToken));
		addItemButton.setToolTipText("<html>add a controller, UI Panel(Mlet), script of commands and etc." +
				"</html>");
		toolbar.add(addItemButton);
		
		saveButton.setToolTipText("<html>save to disk ("+ResourceUtil.getAbstractCtrlKeyText()+" + S)<BR>current project will be auto loaded when open this designer." +
//				"<BR><BR>Tip : Saving has no effect on a deployed project(which is maybe using by mobile)." +
				"<BR><BR>Note : only one project is edited at same time." +
				"</html>");
		toolbar.add(saveButton);
		
		activeButton.setToolTipText("<html>("+ResourceUtil.getAbstractCtrlKeyText()+" + D)<BR>after click activate, current project will be active and displayed to mobile when mobile login." +
				"<BR><BR><STRONG>Note :</STRONG>please <STRONG>re-activate</STRONG> after modifying current project." +
				"</html>");
		{
			final Action deployAction = new AbstractAction() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run(){
							App.invokeAndWaitUI(new Runnable(){
								@Override
								public void run(){
									disableManageButtons();
								}
							});
							
							final Window[] w = new Window[1];
							startDeploy(w);
							if(w[0] != null){
								w[0].dispose();
							}
							
							App.invokeLaterUI(new Runnable(){
								@Override
								public void run(){
									checkHARButtonsEnable();
								}
							});
						}
						
						/**
						 * 重要，请勿在Event线程中调用，
						 * @param w
						 */
						void startDeploy(final Window[] w) {
							Map<String, Object> map = null;
							if(isModified){
								final int out = modifyNotify();
								if(out == JOptionPane.YES_OPTION){
									map = save();
									if(map == null){
										return;
									}
								}else{
									return;
								}
							}
							
							w[0] = ProcessingWindowManager.showCenterMessage((String)ResourceUtil.get(9092));

							if(map == null){
								try{
									map = buildMapFromTree();
								}catch (final NodeInvalidException ex) {
									displayError(ex);
									return;
								}
							}
					
							if(deployProcess(map)){
//								deactiveButton.setEnabled(true);

//								App.invokeLater(new Runnable() {
//									@Override
//									public void run() {
										showHCMessage("Successful activate project, " +
												"mobile can access this resources now.", ACTIVE + " OK", self, true, null);
//									}
//								});
							}
						}
					}, threadPoolToken);
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
		
		buildRebindButton(rebindButton, threadPoolToken, this, self);
		toolbar.add(rebindButton);
		if(ServerUIUtil.isStarted() == false){
			rebindButton.setEnabled(false);
			activeButton.setEnabled(false);
			deactiveButton.setEnabled(false);
			checkHARButtonsEnableInBackground();
		}else{
			checkHARButtonsEnable();
		}
		
		deactiveButton.setToolTipText("<html>deactivate current project, excluding other project." +
				"<BR>If no project is active, then screen of desktop is accessed when mobile login." +
				"</html>");
		deactiveButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				App.invokeAndWaitUI(new Runnable(){
					@Override
					public void run(){
						disableManageButtons();
					}
				});
				
				try{
					doDeactive();
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
				}
				
				App.invokeLaterUI(new Runnable(){
					@Override
					public void run(){
						checkHARButtonsEnable();
					}
				});
			}
			
			public void doDeactive() {	
				final String currProjID = getCurrProjID();

				{
					//检查本工程的依赖性
					final Iterator<LinkProjectStore> its = LinkProjectManager.getLinkProjsIterator(true);
					final Vector<LinkProjectStore> stores = new Vector<LinkProjectStore>();
					while(its.hasNext()){
						final LinkProjectStore lps = its.next();
						if(lps.isActive()){
							if(lps.getProjectID().equals(currProjID)){
								//假定不含本工程
								continue;
							}
							stores.add(lps);
						}
					}
					
					if(LinkProjectManager.checkReferencedDependency(self, stores) == false){
						return;
					}
				}
				
				deactiveButton.setEnabled(false);

				final LinkProjectStore lps = LinkProjectManager.getProjByID(currProjID);
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
							ServerUIUtil.restartResponsorServerDelayMode(self, null);
							App.invokeLaterUI(new Runnable() {
								@Override
								public void run() {
									showHCMessage("Successful deactivate current project.", DEACTIVE + " OK", self, true, null);
								}
							});
						}else{
							//启动远屏或菜单
							ServerUIUtil.restartResponsorServerDelayMode(self, null);
//							App.invokeLater(new Runnable() {
//								@Override
//								public void run() {
									showHCMessage("Successful deactivate current project, " +
											"other project(s) is restart and serving now.", DEACTIVE + " OK", self, true, null);
//								}
//							});
						}
						
						checkHARButtonsEnableInBackground();
					}
				}else{
					final JPanel panel = new JPanel(new BorderLayout());
					panel.add(new JLabel("project [" + currProjID + "] is not active, you may have changed project ID!", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
					App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.ERROR), false, null, null, null, null, instance, true, false, null, false, false);
					return;
				}
			}
		}, threadPoolToken));
		toolbar.add(deactiveButton);
		saveAsButton.setToolTipText("<html>save current project to a har file on your disk, not all projects.</html>");
		saveAsButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				try{
					final Map<String, Object> map = buildMapFromTree();
					saveAs(map);
				}catch (final NodeInvalidException ex) {
					displayError(ex);
				}
			}
		}, threadPoolToken));
		toolbar.addSeparator();
//		if(new File(EDIT_HAR).exists() == false){
//			deployButton.setEnabled(false);
//			saveAsButton.setEnabled(false);
//		}
		loadButton.addActionListener(new HCButtonEnabledActionListener(loadButton, new Runnable() {
			@Override
			public void run() {
				final File file = FileSelector.selectImageFile(loadButton, FileSelector.HAR_FILTER, true);
				if(file != null){
					final Map<String, Object> map = HCjar.loadHar(file, true);
					if(map.isEmpty()){
						App.showMessageDialog(self, "Error load project from har file.", 
								"Error load", JOptionPane.ERROR_MESSAGE);
					}else{
						final String licenseURL = ((String)map.get(HCjar.PROJ_LICENSE)).trim();
						final IBiz succLoadBiz = new IBiz() {
							@Override
							public void start() {
								showHCMessage("Successful load project from har file.", 
										"Success load", self, true, null);//不需要relativeTo
							}
							
							@Override
							public void setMap(final HashMap map) {
							}
						};
						if(licenseURL.length() > 0){
							final IBiz biz = new IBiz(){
								@Override
								public void setMap(final HashMap map) {
								}

								@Override
								public void start() {
									if(checkAndLoad(map, succLoadBiz) == false){
										return;
									}
								}
							};
							App.showAgreeLicense("License of [" + map.get(HCjar.PROJ_NAME) + "]", false, licenseURL, biz, null, true);
							return;
						}
						if(checkAndLoad(map, succLoadBiz) == false){
							return;
						}

//						App.showMessageDialog(this, "Successful import project from jar file.", 
//									"Success load", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}
		}, threadPoolToken));
		loadButton.setToolTipText("<html>load and edit a project from a har file." +
				"<BR><BR>it will be setted as current project after click '" + saveButton.getText() + "'.</html>");

		toolbar.add(loadButton);
		toolbar.add(saveAsButton);

		shiftProjButton.addActionListener(new HCButtonEnabledActionListener(shiftProjButton, new Runnable() {
			@Override
			public void run() {
				showLinkPanel(self, false, shiftProjButton);
			}
		}, threadPoolToken, null));
		
		toolbar.addSeparator();

		shiftProjButton.setToolTipText("<html>add and delete projects, or choose other project for editing.</html>");

		toolbar.add(shiftProjButton);
//		toolbar.addSeparator();//末尾，故注释
		
//		helpButton.addActionListener(new HCActionListener(new Runnable() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				showWizard();
//			}
//		});
		helpButton.setToolTipText("show tutorial steps of using Mobile UI Designer.");
//		toolbar.add(helpButton);
//		toolbar.addSeparator();
		
		final JPanel treePanel = new JPanel();
		treePanel.setBorder(new TitledBorder("Menu Tree :"));
		final JScrollPane scrollPane = new JScrollPane(tree);
		treePanel.setLayout(new BorderLayout());
		treePanel.add(scrollPane, BorderLayout.CENTER);
		
		editPanel.setLayout(new BorderLayout());
		editPanel.add(nodeEditPanel.getMainPanel());
		
		final JSplitPane panelSubMRInfo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				treePanel, editPanel);

		setLayout(new BorderLayout());
		add(panelSubMRInfo, BorderLayout.CENTER);
		
		final JPanel toolbarLeftPanel = new JPanel();
		toolbarLeftPanel.setLayout(new BorderLayout());
		toolbarLeftPanel.add(toolbar, BorderLayout.WEST);
		add(toolbarLeftPanel, BorderLayout.NORTH);
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new HCWindowAdapter(new Runnable() {
			@Override
			public void run() {
				notifyCloseWindow();				
			}
		}, threadPoolToken));

		//该行命令不能置于loadMainProject之后，因为导致resize事件不正确
		toVisiableAndLocation(panelSubMRInfo);
		
		//必须要提前
		codeHelper = new CodeHelper();

		ContextManager.getThreadPool().run(new Runnable(){
			@Override
			public void run() {
				CodeStaticHelper.doNothing();
			}
		}, threadPoolToken);
		
		IDArrayGroup.showMsg(IDArrayGroup.MSG_SYSTEM_CLASS_LIMITED, App.SYS_INFO_ICON, (String)ResourceUtil.get(IContext.INFO), 
				"<html><font color=\"red\">Important</font> : " +
				"<BR>" +
				"<BR>the current HomeCenter provide build-in SecurityManager for HAR project." +
				"<BR>each JRuby code is based on reflection, java.lang.System (private SecurityManager security) is limited for reflection, " +
				"<BR><BR>please replace java.lang.System with " + JavaLangSystemAgent.class.getName() + " or upgrade JRE to 1.7" +
				"<BR><BR>for more, please read Java Doc about " + JavaLangSystemAgent.class.getName() + "</html>");
	}
	
	public static void buildRebindButton(final JButton rebindBut, final ThreadGroup tgt, final BindButtonRefresher refresher,
			final Frame frameOwner){
		rebindBut.setToolTipText("<html>" +
				"rebind (Not bind) reference device(s) of robot in active projects to real device(s)." +
				"<BR>service will be stopped before rebinding, and restart after rebinding." +
				"<BR><BR>binding (Not rebind) dialog will automatic pop-up after '<STRONG>" + ACTIVE + "</STRONG>' project if binding is required." +
				"<BR>this button will be disable if there is no reference device of robot in <STRONG>all</STRONG> active (not current) projects." +
				"</html>");
		
		rebindBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run(){
				App.invokeAndWaitUI(new Runnable(){
					@Override
					public void run(){
						refresher.disableManageButtons();
					}
				});
				
				ProcessingWindowManager.showCenterMessageOnTop(null, true, (String)ResourceUtil.get(9110), null);//processing...
				
				try{
					doRebind();
				}catch (final Throwable e) {
					ExceptionReporter.printStackTrace(e);
					ProcessingWindowManager.disposeProcessingWindow();
				}
				
				//由于改为JFrame型，不是JDialog，所以checkButtonsLaterUI置于action内
			}

			private void checkButtonsLaterUI() {
				ProcessingWindowManager.disposeProcessingWindow();
				
				App.invokeLaterUI(new Runnable(){
					@Override
					public void run(){
						refresher.checkHARButtonsEnable();
					}
				});
			}
			
			public void doRebind() {
				ServerUIUtil.stop();
				
				final MobiUIResponsor respo = (MobiUIResponsor)ServerUIUtil.buildMobiUIResponsorInstance();
				final BindRobotSource bindSource = new BindRobotSource(respo);
				
				DeviceBinderWizard out = null;
				try{
					out = DeviceBinderWizard.getInstance(bindSource, false, frameOwner, respo, tgt);
				}catch (final Throwable e) {
					L.V = L.O ? false : LogManager.log("user cancel connect device or JRuby code error!");
					ServerUIUtil.restartResponsorServerDelayMode(frameOwner, respo);
					return;
				}
				
				final DeviceBinderWizard binder = out;
				final UIActionListener jbOKAction = new UIActionListener() {
					@Override
					public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
						binder.save();
						window.dispose();
						ServerUIUtil.restartResponsorServerDelayMode(frameOwner, respo);
						
						checkButtonsLaterUI();
					}
				};
				final UIActionListener cancelAction = new UIActionListener() {
					@Override
					public void actionPerformed(final Window window, final JButton ok, final JButton cancel) {
						window.dispose();
						ServerUIUtil.restartResponsorServerDelayMode(frameOwner, respo);
						
						checkButtonsLaterUI();
					}
				};
				binder.setButtonAction(jbOKAction, cancelAction);
				binder.show();
			}
		}, tgt));
	}
	
	@Override
	public void disableManageButtons(){
		rebindButton.setEnabled(false);
		activeButton.setEnabled(false);
		deactiveButton.setEnabled(false);
	}

	@Override
	public final void checkHARButtonsEnable(){
		checkBindEnable(rebindButton);
		checkActiveEnable();
		checkDeactiveEnable();
	}
	
	private void checkHARButtonsEnableInBackground() {
		if(System.getProperty(Constant.DESIGNER_IN_TEST) == null){
			ContextManager.getThreadPool().run(new Runnable() {
				@Override
				public void run() {
					//尚未初始化完成或active，deactive后，可能会自动弹出本Bind窗口，为防止冲突，需在初始后，开启本按钮
					final int count = 0;
					while(ServerUIUtil.isStarted() == false){
						try{
							Thread.sleep(500);
						}catch (final Exception e) {
						}
					}
					checkHARButtonsEnable();
				}
			}, threadPoolToken);
		}
	}
	
	private final void checkActiveEnable(){
		activeButton.setEnabled(ServerUIUtil.isStarted());
	}
	
	private final void checkDeactiveEnable(){
		LinkProjectStore lps;
		deactiveButton.setEnabled(ServerUIUtil.isStarted() && (lps = LinkProjectManager.getProjByID(getCurrProjID())) != null && lps.isActive());
	}
	
	public static void checkBindEnable(final JButton rebindBut){
		if(ServerUIUtil.useHARProject && ServerUIUtil.isStarted()){
			final BaseResponsor base = ServerUIUtil.getResponsor();
			if(base instanceof MobiUIResponsor){
				if(((MobiUIResponsor)base).hasRobotReferenceDevice()){
					rebindBut.setEnabled(true);
					return ;
				}
			}
		}
		
		rebindBut.setEnabled(false);
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * @return
	 */
	private File loadDefaultEdit() {
		File edit_har = getDefaultEditFile();
		if(L.isInWorkshop){
			L.V = L.O ? false : LogManager.log("try load default HAR : " + edit_har.getAbsolutePath());
		}
		if(edit_har.exists() == false){
			return null;
		}
		
		final String last_edit_id = PropertiesManager.getValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID);
		if(last_edit_id != null){
			final LinkProjectStore lps = LinkProjectManager.getProjByID(last_edit_id);//有可能被删除，而为null
			if(lps != null){
				if(lps.isChangedBeforeUpgrade()){
					final int out = App.showOptionDialog(instance,
							"<html>current edited (modified) project is upgraded, " +
							"<BR>" +
							"<BR>click 'yes' to choose the newer version (lose old edited)," +
							"<BR>otherwise to choose old edited version." +
							"</html>",
							"override edit?");
					LinkProjectManager.copyCurrEditFromStorage(lps, out == JOptionPane.YES_OPTION);
				}else if(StringUtil.higher(lps.getVersion(), PropertiesManager.getValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_VER))){
					//升级后的版本高于edit_har(未修改)的版本
					final int out = App.showOptionDialog(instance,
							"<html>current edit (not modified) project is upgraded, " +
							"<BR>" +
							"<BR>click 'yes' to choose the newer version," +
							"<BR>otherwise to choose old edit version." +
							"</html>",
							"override edit?");
					if(out == JOptionPane.YES_OPTION){//仅在YES_OPTION下操作
						LinkProjectManager.copyCurrEditFromStorage(lps, false);//由于未修改，所以false和true是同一文件
					}
				}
			}
		}
		
		try{
			final Map<String, Object> map = HCjar.loadHar(edit_har, true);
			if(map.isEmpty()){
				ThirdlibManager.copy(edit_har, new File(ResourceUtil.getBaseDir(), LinkProjectManager.EDIT_BAK_HAR));
				throw new Exception("default har file error : empty properties map.");
			}
			loadNodeFromMap(map);
			if(last_edit_id == null || (last_edit_id.equals(map.get(HCjar.PROJ_ID)) == false)){
				recordEditProjInfo(map);
			}
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
			App.showMessageDialog(null, "<html>default har project is error, which is copied to <strong>" + LinkProjectManager.EDIT_BAK_HAR + "</strong>!" +
					"<BR>system will create default project.</html>", 
					"Project load error", 
					JOptionPane.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
			edit_har = null;
		}
		return edit_har;
	}

	private static final File getDefaultEditFile() {
		return new File(ResourceUtil.getBaseDir(), LinkProjectManager.EDIT_HAR);
	}
	
	public static final int STANDARD_WIDTH = 1024;
	public static final int STANDARD_HEIGHT = 768;

	private void toVisiableAndLocation(final JSplitPane panelSubMRInfo) {
		if(LocationComponentListener.hasLocation(this) && LocationComponentListener.loadLocation(this)){
			App.invokeAndWaitUI(new Runnable() {
				@Override
				public void run() {
					setVisible(true);
					toFront();
				}
			});
		}else{
			App.invokeAndWaitUI(new Runnable() {
				@Override
				public void run() {
					setPreferredSize(new Dimension(STANDARD_WIDTH, STANDARD_HEIGHT));
					pack();
				}
			});
			App.showCenter(this);
		}
		
		//一定要在setVisiable(true)之后
		final String dviLoca = PropertiesManager.getValue(PropertiesManager.p_DesignerDividerLocation);
		if(dviLoca == null){
			panelSubMRInfo.setDividerLocation(.3);
		}else{
			panelSubMRInfo.setDividerLocation(Integer.parseInt(dviLoca));
		}
		
		panelSubMRInfo.addPropertyChangeListener(new java.beans.PropertyChangeListener() {  
            @Override
			public void propertyChange(final java.beans.PropertyChangeEvent evt) {  
                if (evt.getPropertyName().equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {  
                    PropertiesManager.setValue(PropertiesManager.p_DesignerDividerLocation, 
                    		String.valueOf(panelSubMRInfo.getDividerLocation()));
                    PropertiesManager.saveFile();
                }  
            }  
        });
	}
	
	public final boolean loadMainProject(final JFrame self){
		try {
			Map<String, Object> map = HCjar.loadJar(ResourceUtil.getResource(HC_RES_MY_FIRST_HAR));
			
			loadNodeFromMap(map);
			
			map = save();
			
			if(map == null){
				return false;
			}
			
			//初建工程时，不作active操作
			//deployProcess(map);
			
			return true;
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}finally{
		}
		return false;
	}

	/**
	 * 本方法在JRuby安装完后，进行安装缺省har
	 */
	public static final void setupMyFirstAndApply(){
		try{
			final File fileHar = getDefaultEditFile();
			
			//因为有可能出现JRuby升级的情形
			if(fileHar.exists() == false){
				//复制/hc/res/MyFirst.har到myedit.har
				copyHarFromPath(ResourceUtil.getResource(HC_RES_MY_FIRST_HAR), fileHar);
				L.V = L.O ? false : LogManager.log("successful create default edit har project.");
				
				//发布缺省工程
				final Map<String, Object> map = AddHarHTMLMlet.getMap(fileHar);
				AddHarHTMLMlet.appendMapToSavedLPS(fileHar, map, true, true, null);
				LinkProjectManager.reloadLinkProjects();//不可少
				
				//active
				ServerUIUtil.promptAndStop(true, null);
				setProjectOn();
				ServerUIUtil.restartResponsorServer(null, null);
			}
		}catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

	private static void copyHarFromPath(final URL url, final File fileHar)	throws Exception {
		final InputStream is = url.openStream();
		final ByteArrayOutputStream outStream = new ByteArrayOutputStream();  
		final int BUFFER_SIZE = 4096;
		final byte[] data = new byte[BUFFER_SIZE];  
		int count = -1;  
		while((count = is.read(data,0,BUFFER_SIZE)) != -1) {
		    outStream.write(data, 0, count);  
		}
		final FileOutputStream fos = new FileOutputStream(fileHar);
		fos.write(outStream.toByteArray());
		fos.flush();
		fos.close();
		
		try{
			is.close();
		}catch (final Exception e) {
		}
	}
	
	private final void loadNodeFromMap(final Map<String, Object> map) {
		{
			L.V = L.O ? false : LogManager.log("Project [" + (String)map.get(HCjar.PROJ_ID) + "] " +
				"JRE version : " + (String)map.get(HCjar.JRE_VER) + ", " +
				"HomeCenter version : " + (String)map.get(HCjar.HOMECENTER_VER) + ", " +
				"JRuby version : " + (String)map.get(HCjar.JRUBY_VER) + "");
		}
		
		delAllChildren();
		
		msbFolder.removeFromParent();
		msbFolder.removeAllChildren();
		mainMenuNode = HCjar.toNode(map, root, msbFolder, shareFolders);
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
			jumpToNode(childNode, model, tree);
			notifySelectNode(childNode, (HPNode)childNode.getUserObject());
		}else{
			//没有菜单的空工程情形
		}
		addItemButton.setEnabled(hasMainMenu);
		activeButton.setEnabled(true);
		
		refresh();
		
		needRebuildTestJRuby = true;
		Designer.expandTree(tree);
		
		final DefaultMutableTreeNode shareJarFolder = getShareJarFolder();
		if(shareJarFolder.getChildCount() > 0){
			ConditionWatcher.addWatcher(new IWatcher() {
				final long startMS = System.currentTimeMillis();
				@Override
				public boolean watch() {
					if(System.currentTimeMillis() - startMS > ThreadPriorityManager.UI_CODE_HELPER_DELAY_MS){
						codeHelper.initCodeHelper(shareJarFolder);
						return true;
					}
					return false;
				}
				
				@Override
				public void setPara(final Object p) {
				}
				
				@Override
				public boolean isCancelable() {
					return false;
				}
				
				@Override
				public void cancel() {
				}
			});
		}
	}
	
	/**
	 * 重新加载lps
	 */
	public final void refresh(){
		//由于可能切换，所以刷新
		LinkProjectManager.reloadLinkProjects();
		
		checkHARButtonsEnable();
	}

	public void addNode(DefaultMutableTreeNode parentNode, final DefaultMutableTreeNode newNode) {
		changeTreeNodeContext(newNode, itemContext);
		
		if(parentNode == null){
			parentNode = selectedNode;
		}
		
		parentNode.add(newNode);
		// --------下面代码实现显示新节点（自动展开父节点）-------
		final TreeNode[] nodes = model.getPathToRoot(newNode);
		final TreePath path = new TreePath(nodes);
		tree.setSelectionPath(path);
//				tree.setExpandsSelectedPaths(true); 
		tree.scrollPathToVisible(path);
		App.invokeLaterUI(updateTreeRunnable);
		setModified(true);
		
		notifySelectNode(newNode, (HPNode)newNode.getUserObject());
		
		//检查是否是jar
		{
			if(isJarNode(newNode)){
				codeHelper.loadLibToCodeHelper(newNode);
			}
		}
	}
	
	private static boolean isJarNode(final DefaultMutableTreeNode node){
		final HPNode hpnode = (HPNode)node.getUserObject();
		if(hpnode instanceof HPShareJar){
			final HPShareJar jar = (HPShareJar)hpnode;
			if(jar.type == HPNode.MASK_RESOURCE_JAR){
				return true;
			}
		}
		return false;
	}
	
	public void saveFile(final int filter, final String ext){
		final Object userobj = selectedNode.getUserObject();
		File saveTo = FileSelector.selectImageFile(this, filter, false);
		if(saveTo != null){
			if(ext != null && ext.length() > 0){
				if(saveTo.toString().endsWith(ext)){
				}else{
					saveTo = new File(saveTo.getPath() + ext);//注意：无需getBaseDir
				}
			}
			
			FileOutputStream fos = null;
			try{
				fos = new FileOutputStream(saveTo);
				fos.write(((HCShareFileResource)userobj).content);
				fos.flush();
				fos.close();
				
				final JPanel ok = new JPanel();
				ok.add(new JLabel("Successful save file!", new ImageIcon(ImageSrc.OK_ICON), SwingConstants.LEFT));
				App.showCenterPanel(ok, 0, 0, "Save OK!", false, null, null, null, null, this, true, false, null, false, false);
			}catch (final Exception e) {
				final JPanel ok = new JPanel();
				ok.add(new JLabel("Error save file!", new ImageIcon(ImageSrc.CANCEL_ICON), SwingConstants.LEFT));
				App.showCenterPanel(ok, 0, 0, "Save Error!", false, null, null, null, null, this, true, false, null, false, false);
				ExceptionReporter.printStackTrace(e);
			}finally{
				try{
					fos.close();
				}catch (final Exception e) {
					
				}
			}
		}
	}
	
	private final void searchNode(final DefaultMutableTreeNode searchNodeResults, 
			final Vector<DefaultMutableTreeNode> rootNode, 
			final NodeMatcher matcher){
		if(matcher.match((HPNode)searchNodeResults.getUserObject())){
			rootNode.add(searchNodeResults);
		}
		final int count = searchNodeResults.getChildCount();
		for (int i = 0; i < count; i++) {
			searchNode((DefaultMutableTreeNode)searchNodeResults.getChildAt(i), rootNode, matcher);
		}
	}

	public void delNode() {
		final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)selectedNode.getParent();
		model.removeNodeFromParent(selectedNode);
		
		//检查是否是jar文档
		{
			if(isJarNode(selectedNode)){
				final HPShareJar jar = (HPShareJar)selectedNode.getUserObject();
				codeHelper.unloadLibFromCodeHelper(jar.name);
			}
		}
		
		setModified(true);
		
		{
			//检查如果是Mlet，则进行全局Styles删除检查
			if(NodeEditPanelManager.isMletNode((HPNode)selectedNode.getUserObject())){
				//如果不存在Mlet节点，则进行Styles清空
				final Vector<DefaultMutableTreeNode> results = new Vector<DefaultMutableTreeNode>();
				searchNode(root, results, new NodeMatcher() {
					@Override
					public boolean match(final HPNode node) {
						return NodeEditPanelManager.isMletNode(node);
					}
				});
				if(results.size() == 0){
					setProjCSS("");
				}
			}
		}
		
		selectedNode = null;

		jumpToNode(parent, model, tree);
		notifySelectNode(parent, (HPNode)parent.getUserObject());
	}
	
	public TreeNode getCurrSelectedNode(){
		CCoreUtil.checkAccess();
		
		return selectedNode;
	}

	public DefaultMutableTreeNode getMainMenuNode(){
		CCoreUtil.checkAccess();
		
		return mainMenuNode;
	}
	
	public DefaultMutableTreeNode getMSBFolderNode(){
		CCoreUtil.checkAccess();
		
		return msbFolder;
	}

	public void notifySelectNode(final DefaultMutableTreeNode sNode, final HPNode nodeData) {
		selectedNode = sNode;
		
		final NodeEditPanel nep = nm.switchNodeEditPanel(nodeData.type, nodeData, this);
		final NodeEditPanel oldPanel = nodeEditPanel;
		if(nodeEditPanel != null){
			nodeEditPanel.notifyLostEditPanelFocus();
		}
		
		if(nep == null){
			nodeEditPanel = Designer.emptyNodeEditPanel;
		}else{
			nodeEditPanel = nep;
		}
		
		App.invokeLaterUI(new Runnable() {
			@Override
			public void run() {
				nodeEditPanel.init(sNode, tree);//放入UI
				
				editPanel.remove(oldPanel.getMainPanel());
				editPanel.add(nodeEditPanel.getMainPanel());
				editPanel.validate();
				editPanel.revalidate();
				editPanel.repaint();
			}
		});
	}

	@Override
	public boolean isModified() {
		return isModified;
	}

	@Override
	public void setModified(final boolean modified) {
		J2SEContext.refreshActionMS(false);
		this.isModified = modified;
		saveButton.setEnabled(modified);
	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * @return 返回0表示，忽略修改
	 */
	private final int modifyNotify() {
		return App.showOptionDialog(instance,
				"project is modified and save now?",
				"save project now?");
	}
	
	public static void jumpToNode(final DefaultMutableTreeNode node, final DefaultTreeModel defaultTreeModel, final JTree tree2) {
		final TreeNode[] nodes = defaultTreeModel.getPathToRoot(node);
		final TreePath path = new TreePath(nodes);
		tree2.setSelectionPath(path);
	}
	
	private static void showHCMessage(final String message, final String title, final JFrame frame, final boolean model,
			final JComponent relativeTo){
		final JPanel panel = new JPanel();
		panel.add(new JLabel(message, new ImageIcon(ImageSrc.OK_ICON), SwingConstants.LEFT));
		App.showCenterPanel(panel, 0, 0, title, false, null, null, null, null, frame, model, false, relativeTo, false, false);
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * 检查内存变更
	 * @param self
	 * @return
	 */
	private boolean discardModiWhenShift(final Designer self) {
		if(self.isModified()){
			final int out = discardQues(self);
			if(out == JOptionPane.YES_OPTION){
				setModified(false);
				return true;
			}
			return false;
		}
		return true;
	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * @param self
	 * @return
	 */
	private int discardQues(final Designer self) {
		return App.showOptionDialog(self,
				"project is modified, discard all now?",
				"discard now?");
	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * @param self
	 * @return true:继续后续操作；false:中止
	 */
	private boolean quitToNewProj(final Designer self) {
		if(self.isModified()){
			final int out = modifyNotify();
			if(out == JOptionPane.YES_OPTION){
				final Map<String, Object> map = save();
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
		final DefaultMutableTreeNode tnode = createNewRoot();
		
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
		final HPShareRoot sr = new HPShareRoot(HPNode.MASK_SHARE_TOP, "resources");
		
		final DefaultMutableTreeNode shareRoot = new DefaultMutableTreeNode(sr);
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
	private boolean checkLoadVer(final Map<String, Object> map) {
		final String[] sample_ver = {(String)map.get(HCjar.HOMECENTER_VER), 
				(String)map.get(HCjar.JRE_VER), (String)map.get(HCjar.JRUBY_VER)};
		final String[] curr_ver = {StarterManager.getHCVersion(), String.valueOf(App.getJREVer()), 
				PropertiesManager.getValue(PropertiesManager.p_jrubyJarVer, "1.0")};
		final boolean[] isSampleHigh = {false, false, false};
		boolean isHigh = false;
		for (int i = 0; i < sample_ver.length; i++) {
			if(sample_ver[i] != null){
				if(StringUtil.higher(sample_ver[i], curr_ver[i])){
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
			
			final String[] whatVer = {"HomeCenter Ver", "JRE Ver", "JRuby Ver"};
			for (int i = 0; i < isSampleHigh.length; i++) {
				if(isSampleHigh[i] == false){
					continue;
				}
				final boolean isNotFirst = (sample.length() > 0);
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
			final JPanel panel = new JPanel();
			panel.add(new JLabel("<html><body>" + sample + "<BR>" + curr + "<BR><BR>" +
					"some data may ignore, or run error.<BR><BR>continue any more?" + 
//					(isSampleHigh[0]?("<BR>please 'enable auto upgrade' to upgrade HomeCenter Ver"):"") + 
					"</body></html>", 
					App.getSysIcon(App.SYS_WARN_ICON), SwingConstants.LEFT));
			App.showCenterPanel(panel, 0, 0, "version warning", true, null, null, new HCActionListener(){
				
				@Override
				public void actionPerformed(final ActionEvent e) {
				}
			}, new HCActionListener(){
				@Override
				public void actionPerformed(final ActionEvent e) {
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
				final Map<String, Object> map = buildMapFromTree();
				deployRunTest(map);
				needRebuildTestJRuby = false;
			}catch (final NodeInvalidException e) {
				displayError(e);
				return false;
			}
		}
		return true;
	}

	private Map<String, Object> save() {
		try{
			final Map<String, Object> map = buildMapFromTree();
			
			final File edit_har = getDefaultEditFile();
			HCjar.toHar(map, edit_har);
			
			App.invokeLaterUI(updateTreeRunnable);
			
			setModified(false);
			
			saveAsButton.setEnabled(true);
			
			LinkProjectStore oldlps = LinkProjectManager.getProjByID(getCurrProjID());
			if(oldlps == null){
				oldlps = LinkProjectManager.getProjLPSWithCreate(getCurrProjID());
				LinkProjectManager.importLinkProject(oldlps, edit_har, false, null);

//				final boolean isRoot = LinkProjectManager.getProjectSize()<2?true:oldlps.isRoot();
				//缺省创建时，root=false, active=false
				LinkProjectManager.saveProjConfig(oldlps, false, false);
			}

			recordEditProjInfo(map);
			
			LinkProjectManager.saveToLinkBack(oldlps, edit_har);
			return map;
		}catch (final NodeInvalidException e) {
			displayError(e);
			
			return null;
		}
	}

	public static void recordEditProjInfo(final Map<String, Object> map) {
		PropertiesManager.setValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_ID, (String)map.get(HCjar.PROJ_ID));
		PropertiesManager.setValue(PropertiesManager.p_LINK_CURR_EDIT_PROJ_VER, (String)map.get(HCjar.PROJ_VER));
		PropertiesManager.saveFile();
	}

	public final void displayError(final NodeInvalidException e) {
		ContextManager.getThreadPool().run(new Runnable() {//注意：上述方法可能在EventDispatcher中。所以要入pool
			@Override
			public void run() {
				final DefaultMutableTreeNode node1 = e.node;
				jumpToNode(node1, model, tree);
				notifySelectNode(node1, (HPNode)node1.getUserObject());
				
				final JPanel panel = new JPanel();
//				final String name1 = ((HPNode)node1.getUserObject()).name;
				panel.add(new JLabel("<html><body>" + e.getDesc() + "</body></html>", 
						App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEFT));
				App.showCenterPanel(panel, 0, 0, "Error Node", false, null, null, null, null, instance, true, false, null, false, false);
			}
		}, threadPoolToken);
	}

	public static void deployRunTest(Map<String, Object> deployMap) {
		ScriptEditPanel.terminateJRubyEngine();

		//清空runtemp
		final File[] subFiles = StoreDirManager.RUN_TEST_DIR.listFiles();
		if(subFiles != null){
			for (int i = 0; i < subFiles.length; i++) {
				subFiles[i].delete();
			}
		}
		
		final boolean forceBuild = (deployMap == null);
		if(forceBuild){
			//toMap不能上述相同代码合并，因前者存储时，对map是一次使用，并删除了二进制数据
			final File jarfile = getDefaultEditFile();
			deployMap = HCjar.loadHar(jarfile, true);
		}

		//因为升级到新版本时，可能恰好又是用户新装，所以需要检查文件是否存在
		if(ProjResponser.deloyToWorkingDir(deployMap, StoreDirManager.RUN_TEST_DIR) 
				|| forceBuild //注意：forceBuild必须放后面
				|| (ScriptEditPanel.runTestEngine == null)){//极端情形，即初始加载时，该对象为null，必须进行build
			ScriptEditPanel.rebuildJRubyEngine();
		}
	}

	private void saveAs(final Map<String, Object> map) {
		File file = FileSelector.selectImageFile(saveAsButton, FileSelector.HAR_FILTER, false);//saveAsButton parent and relativeTo
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
				final JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				panel.add(new JLabel("Override exit har file?", App.getSysIcon(App.SYS_QUES_ICON), 
						SwingConstants.LEADING));
				App.showCenterPanel(panel, 0, 0, "Override?", true, null, null, new HCActionListener(new Runnable() {
					@Override
					public void run() {
						doExportBiz(map, fileExits, fileHadExits, self);
					}
				}, threadPoolToken), null, this, true, false, saveAsButton, false, false);
			}else{
				doExportBiz(map, fileExits, fileHadExits, self);
			}
		}
	}

	private boolean deployProcess(final Map<String, Object> map) {
		LinkProjectStore oldlps = LinkProjectManager.getProjByID(getCurrProjID());
		if(oldlps != null){
			final String curVer = (String)map.get(HCjar.PROJ_VER);
			if(StringUtil.higher(oldlps.getVersion(), curVer)){
				final JPanel panel = new JPanel(new BorderLayout());
				panel.add(new JLabel("<html>the actived version is " + oldlps.getVersion() + ", and current version is " + curVer 
						+ "<br><br>system can't override the current version!</html>", 
						App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
				App.showCenterPanel(panel, 0, 0, "fail activate current project", false, null, null, null, null, instance, true, false, null, false, false);
				return false;
			}
		}
		
		final File newVerHar = getDefaultEditFile();

		synchronized (ServerUIUtil.LOCK) {
			ServerUIUtil.promptAndStop(true, this);
			
			setProjectOn();

			File oldEditBackFile = null;
			if(oldlps != null){
				oldEditBackFile = LinkProjectManager.removeLinkProjectPhic(oldlps, false);
			}else{
				oldlps = LinkProjectManager.getProjLPSWithCreate(getCurrProjID());
			}
			LinkProjectManager.importLinkProject(oldlps, newVerHar, false, oldEditBackFile);
			
			boolean changeToRoot = oldlps.isRoot();
			
			if(changeToRoot == false){
				//是否需要升级为root
				final LinkProjectStore oldroot = LinkProjectManager.searchRoot(true);
				if(oldroot == null){
					changeToRoot = true;
				}
			}
			
			LinkProjectManager.saveProjConfig(oldlps, changeToRoot, true);
		
			final BaseResponsor br = ServerUIUtil.restartResponsorServer(instance, null);
			
			checkHARButtonsEnableInBackground();
			
			if(br instanceof MobiUIResponsor){
				return true;
			}
		}
		return false;
	}

	public static void setProjectOn() {
		ServerUIUtil.useHARProject = true;

		if(PropertiesManager.isTrue(PropertiesManager.p_IsMobiMenu) == false){
			PropertiesManager.setValue(PropertiesManager.p_IsMobiMenu, IConstant.TRUE);
			PropertiesManager.saveFile();
		}
	}

	private void doExportBiz(final Map<String, Object> map, final File fileExits, final File fileHadExits, final Designer self) {
		final JPanel panel = new JPanel(new GridLayout(3, 2));
		panel.add(new JLabel("JRE version : "));
		
		final JTextField jre_field = new JTextField();
		jre_field.setText((String)map.get(HCjar.JRE_VER));
		panel.add(jre_field);
		panel.add(new JLabel("HomeCenter version : "));
		
		final JTextField hc_field = new JTextField();
		hc_field.setText((String)map.get(HCjar.HOMECENTER_VER));
		panel.add(hc_field);
		panel.add(new JLabel("JRuby version : "));
		
		final JTextField jruby_field = new JTextField();
		jruby_field.setText((String)map.get(HCjar.JRUBY_VER));
		panel.add(jruby_field);
		
		final JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.add(panel, BorderLayout.CENTER);
		{
			final JPanel descPanel = new JPanel(new BorderLayout());
			descPanel.setBorder(new TitledBorder("Description :"));
			descPanel.add(new JLabel("<html>these versions are design enviroment,<BR>you can change them for runtime enviroment.</html>"), BorderLayout.CENTER);
			centerPanel.add(descPanel, BorderLayout.SOUTH);
		}
		
		final ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				map.put(HCjar.JRE_VER, jre_field.getText());
				map.put(HCjar.HOMECENTER_VER, hc_field.getText());
				map.put(HCjar.JRUBY_VER, jruby_field.getText());
				
//				System.out.println("JRE : " + map.get(HCjar.JRE_VER));
//				System.out.println("HC : " + map.get(HCjar.HOMECENTER_VER));
//				System.out.println("JRUBY : " + map.get(HCjar.JRUBY_VER));
				
				HCjar.toHar(map, fileExits);
				if(fileHadExits != null){
					HCjad.toHad(map, fileHadExits, ResourceUtil.getMD5(fileExits), (int)fileExits.length());
				}
				final String hadDesc = (fileHadExits != null)?(", " + fileHadExits.getName()):"";
				showHCMessage("Successful save project to [" + fileExits.getName() + hadDesc + "]", 
						"Success save as", self, true, saveAsButton);
			}
		};
		App.showCenterPanel(centerPanel, 0, 0, "Confirm Versions?", false, null, null, listener, listener, self, true, false, saveAsButton, false, false);
	}

	private final Map<String, Object> buildMapFromTree() throws NodeInvalidException {
		return HCjar.toMap(root, shareFolders);
	}
	
	public static Window currLink;
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * 注意：本方法被反射引用，并不要更名。
	 */
	public static synchronized boolean notifyCloseDesigner(){
		return Designer.getInstance().notifyCloseWindow();
	}
	
	/**
	 * 注意：本方法被反射引用，并不要更名。
	 */
	public static synchronized void closeLinkPanel() {
		if(Designer.currLink == null){
			return;
		}
		
		if(Designer.currLink instanceof ClosableWindow){
			((ClosableWindow)Designer.currLink).notifyClose();
		}else{
			Designer.currLink.dispose();
		}
		Designer.currLink = null;
	}
	
	/**
	 * 注意：本方法被反射引用，并不要更名。
	 * @param parent
	 * @param newFrame
	 * @param relativeTo
	 */
	public static synchronized void showLinkPanel(final JFrame parent, final boolean newFrame, final Component relativeTo){
		if(LinkProjectStatus.tryEnterStatus(parent, LinkProjectStatus.MANAGER_IMPORT) == false){
			return;
		}
		if(currLink != null && currLink.isShowing()){
			currLink.toFront();
		}else{
			LinkProjectManager.reloadLinkProjects();
			
			final LinkProjectPanel linkp = new LinkProjectPanel(parent, newFrame, relativeTo);
			currLink = linkp.toShow();
		}
	}
	
	/**
	 * 重要，请勿在Event线程中调用，
	 * @param lps
	 */
	public final void shiftProject(final LinkProjectStore lps){
		if(discardModiWhenShift(instance)){
			final String newProjID = lps.getProjectID();
			if(getCurrProjID().equals(newProjID)){
//				final String md5Editing = ResourceUtil.getMD5(getDefaultEditFile());
//				final String md5back = ResourceUtil.getMD5(LinkProjectManager.buildBackEditFile(lps));
//				md5Editing.equals(md5back) == false
				
				//拉新以获得isChangedBeforeUpgrade
				refresh();
			}else{
				//切换新工程的backEdit
				LinkProjectManager.copyCurrEditFromStorage(lps, false);
			}

			loadDefaultEdit();
		}
	}

//	private void showWizard() {
//		JButton[] btns = {activeButton, deactiveButton, sampleButton, addItemButton, saveAsButton, loadButton};
//		Wizard wiz = new Wizard(btns);
//		wiz.showTip();
//	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * @return
	 */
	public final synchronized boolean notifyCloseWindow() {
		if(instance == null){
			return false;
		}
		
		if(isModified()){
			final int out = modifyNotify();
			if(out == JOptionPane.YES_OPTION){
				final Map<String, Object> map = save();
				if(map == null){
				}
			}
			if(out == JOptionPane.YES_OPTION || out == JOptionPane.NO_OPTION){
				instance = null;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						dispose();
					}
				});
				return true;
			}
		}else{
			instance = null;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					dispose();					
				}
			});
			return true;
		}
		return false;
	}

	private boolean checkAndLoad(final Map<String, Object> map, final IBiz succBiz) {
		if(checkLoadVer(map) == false){
			return false;
		}
		
		loadNodeFromMap(map);
		setModified(true);
		
		succBiz.start();
		return true;
	}

	/**
	 * 重要，请勿在Event线程中调用，
	 * @param loadInit
	 */
	public final void loadInitProject(final boolean loadInit) {
		if(loadInit){
			final File har = loadDefaultEdit();
			if(har == null || har.exists() == false){
				//必须先启thread，因为waiting是modal，会阻塞。
				ContextManager.getThreadPool().run(new Runnable(){
					@Override
					public void run(){
						if(loadMainProject(instance)){
							final JPanel jpanel = new JPanel(new BorderLayout());
							jpanel.add(new JLabel("<html>" +
									"<IMG src='http://homecenter.mobi/images/ok_16.png' width='16' height='16'/>&nbsp;" +
									"<STRONG>default project had been created successfully!</STRONG>" +
									"</html>", App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING));
							
							final ActionListener al = new HCActionListener(new Runnable() {
								@Override
								public void run() {
	//										showWizard();
								}
							}, threadPoolToken);
							App.showCenterPanel(jpanel, 0, 0, (String)ResourceUtil.get(IContext.INFO), 
									false, null, null, al, al, null, false, true, null, true, false);//jdk 8会出现漂移
						}else{
						}
						setToolbarVisible(toolbar, true);
					}
				});
			}else{
				setToolbarVisible(toolbar, true);
			}
		}else{
			setToolbarVisible(toolbar, true);
		}
	}

	public static void expandTree(final JTree tree) { 
		App.invokeAndWaitUI(new Runnable() {
			@Override
			public void run() {
				tree.updateUI();
			    final TreeNode root = (TreeNode) tree.getModel().getRoot(); 
			    expand(tree, new TreePath(root)); 
			}
		});
	}

	private static void expand(final JTree tree, final TreePath parent) { 
	        final TreeNode node = (TreeNode) parent.getLastPathComponent(); 
	        if (node.getChildCount() >= 0) { 
	            for (final Enumeration e = node.children(); e.hasMoreElements(); ) { 
	                final TreeNode n = (TreeNode) e.nextElement(); 
	                final TreePath path = parent.pathByAddingChild(n); 
	                expand(tree, path); 
	            } 
	        } 
	
	        tree.expandPath(parent); 
	//      tree.collapsePath(parent); 
	    }

	public static void setProjectOff() {
		ServerUIUtil.useHARProject = false;
		
		PropertiesManager.setValue(PropertiesManager.p_IsMobiMenu, IConstant.FALSE);
		PropertiesManager.saveFile();
	}

}

class NodeTreeCellRenderer extends DefaultTreeCellRenderer {
	NodeTreeCellRenderer() {
	}

	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value,
			final boolean selected, final boolean expanded, final boolean leaf, final int row,
			final boolean hasFocus) {
		final Component compo = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		final Object o = ((DefaultMutableTreeNode) value).getUserObject();
		if (o instanceof HPNode) {
			final HPNode country = (HPNode) o;
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
			} else if (country.type == HPNode.MASK_SHARE_NATIVE_FOLDER) {
				((JLabel)compo).setIcon(Designer.iconShareNativeFolder);
			} else if (country.type == HPNode.MASK_SHARE_NATIVE) {
				((JLabel)compo).setIcon(Designer.iconShareNative);
			} else if (country.type == HPNode.MASK_MSB_FOLDER) {
				((JLabel)compo).setIcon(Designer.iconMSBFolder);
			} else if (country.type == HPNode.MASK_MSB_ROBOT) {
				((JLabel)compo).setIcon(Designer.iconRobot);
			} else if (country.type == HPNode.MASK_MSB_DEVICE) {
				((JLabel)compo).setIcon(Designer.iconDevice);
			} else if (country.type == HPNode.MASK_MSB_CONVERTER) {
				((JLabel)compo).setIcon(Designer.iconConverter);
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