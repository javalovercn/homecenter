package hc.server.ui.design;

import hc.App;
import hc.UIActionListener;
import hc.core.ConditionWatcher;
import hc.core.IConstant;
import hc.core.IWatcher;
import hc.res.ImageSrc;
import hc.server.AbstractDelayBiz;
import hc.server.DelayServer;
import hc.server.FileSelector;
import hc.server.HCTablePanel;
import hc.server.LinkMenuManager;
import hc.server.ui.ClientDesc;
import hc.server.ui.LinkProjectStatus;
import hc.server.ui.ServerUIUtil;
import hc.server.ui.design.hpj.HCjad;
import hc.server.ui.design.hpj.HCjar;
import hc.util.HttpUtil;
import hc.util.IBiz;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

public class LinkProjectPanel extends ProjectListPanel{
	final String saveAndApply = (String) ResourceUtil.get(1017) + " + " + (String) ResourceUtil.get(9041);

	final HCTablePanel tablePanel;
	final JButton designBut, importBut, editBut, removeBut, upBut, downBut;
	final Vector<LinkEditData> delList = new Vector<LinkEditData>(0);
	boolean isChanged = false;
	boolean isCancelOp = false;
	public static final String OP_NEXT_START_UP = "nextStartUp";
	public static final String OP_ASK = "ask";
	public static final String OP_IMMEDIATE = "immediate";
	
	final JRadioButton rb_startup = new JRadioButton(OP_NEXT_START_UP);
	final JRadioButton rb_ask = new JRadioButton(OP_ASK);
	final JRadioButton rb_imme = new JRadioButton(OP_IMMEDIATE);
	final JCheckBox ch_autoUpgrade = new JCheckBox("auto download and upgrade project");
	ListSelectionListener listSelectListener;
	
	public static final String getNewLinkedInProjOp(){
		final String op = PropertiesManager.getValue(PropertiesManager.p_OpNewLinkedInProjVer, OP_NEXT_START_UP);
		if(op.equals(OP_NEXT_START_UP) || op.equals(OP_ASK) || op.equals(OP_IMMEDIATE)){
			return op;
		}else{
			return OP_NEXT_START_UP;
		}
	}
	
	private void transRootToOtherActive(){
		for (int i = 0; i < LinkProjectManager.MAX_LINK_PROJ_NUM; i++) {
			LinkEditData led = (LinkEditData)data[i][IDX_OBJ_STORE];
			if(led != null){
				if(led.lps.isActive()){
					led.lps.setRoot(true);
					led.op = LinkProjectManager.STATUS_MODIFIED;
					return;
				}
			}
		}
	}
	
	private LinkProjectStore searchRoot(){
		for (int i = 0; i < LinkProjectManager.MAX_LINK_PROJ_NUM; i++) {
			LinkEditData led = (LinkEditData)data[i][IDX_OBJ_STORE];
			if(led != null){
				if(led.lps.isRoot()){
					return led.lps;
				}
			}else{
				return null;
			}
		}
		return null;
	}
	final JPanel contentPane;
	final JButton okBut;
	final JButton jbCancle;
	final UIActionListener jbOKAction;
	final UIActionListener cancelAction;
	final Window dialog;
	final Component relativeTo;
	public LinkProjectPanel(JFrame owner, final boolean newFrame, final Component relativeTo) {
		super();
		final String title = (String)ResourceUtil.get(9059);
		dialog = App.buildCloseableWindow(newFrame, owner, title, true);
		final JFrame self = (owner==null && (dialog instanceof JFrame))?(JFrame)dialog:owner;
		this.relativeTo = relativeTo;
		
		contentPane = new JPanel();
		
		contentPane.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
		
		ImageIcon designIco = null;
		try {
			designIco = new ImageIcon(ImageIO.read(ResourceUtil.getResource("hc/res/designer_22.png")));
		} catch (IOException e2) {
			e2.printStackTrace();
		}    					

		designBut = new JButton((String)ResourceUtil.get(9014), designIco);
		designBut.setToolTipText("<html>load selected project to designer." +
				"<br>all modifications will be discarded, please click '<strong>" + saveAndApply + "</strong>' first.</html>");
		upBut = new JButton((String)ResourceUtil.get(9019), new ImageIcon(ImageSrc.UP_SMALL_ICON));
		downBut = new JButton((String)ResourceUtil.get(9020), new ImageIcon(ImageSrc.DOWN_SMALL_ICON));
		removeBut = new JButton((String)ResourceUtil.get(9018), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
		removeBut.setToolTipText("<html>Note : delete project in deploy area, not in editing area." +
				"<br>the current editing project may be modified, so it is exist even if you click me.</html>");
		importBut = new JButton((String)ResourceUtil.get(9016) + "▼", new ImageIcon(ImageSrc.ADD_SMALL_ICON));
		importBut.setToolTipText("add HAR project.");
		editBut = new JButton((String)ResourceUtil.get(9068), new ImageIcon(ImageSrc.MODIFY_SMALL_ICON));
		editBut.setToolTipText("modify the link name and comment of selected project.");
		Object[] defaultRow = {"", null};
		final AbstractTableModel tableModel = new AbstractTableModel() {
			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				if(columnIndex == COL_NO){
					data[rowIndex][columnIndex] = aValue;
				} else {
					final LinkEditData led = (LinkEditData)data[rowIndex][IDX_OBJ_STORE];
					final LinkProjectStore lps = led.lps;
					if(lps == null){
						return;
					}
					if(columnIndex == COL_PROJ_ID){
						lps.setProjectID((String)aValue);
					}else if(columnIndex == COL_VER){
							lps.setVersion((String)aValue);
					}else if(columnIndex == COL_IS_ROOT){
						if(lps.isRoot()){
							lps.setRoot(false);
							lps.setActive(false);
							
							transRootToOtherActive();
						}else{
							final LinkProjectStore root_lps = searchRoot();
							if(root_lps != null){
								root_lps.setRoot(false);
	//							if(root_lps.getProjectID().equals(HCURL.ROOT_MENU)){
	//								root_lps.setProjectID("oldroot");
	//							}
							}
							lps.setRoot((Boolean)aValue);
							lps.setActive(true);
						}
						led.op = (LinkProjectManager.STATUS_MODIFIED);
						
						tablePanel.table.repaint();
					}else if(columnIndex == COL_PROJ_ACTIVE){
//						if(lps.isRoot()){
//							if(lps.isActive()){
//								JPanel panel = new JPanel(new BorderLayout());
//								panel.add(new JLabel("root project must be active!", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), BorderLayout.CENTER);
//								App.showCenterPanel(panel, 0, 0, (String)ResourceUtil.get(IContext.ERROR), false, null, null, null, null, self, true, false, null, false);
//								return;
//							}
//						}
						lps.setActive((Boolean)aValue);
						if((Boolean)aValue){
							if(searchRoot() == null){
								lps.setRoot(true);
							}
						}else{
							if(lps.isRoot()){
								lps.setRoot(false);
								//将root移交给其它工程
								transRootToOtherActive();
							}
						}
						tablePanel.table.repaint();
						led.op = (LinkProjectManager.STATUS_MODIFIED);
					}else if(columnIndex == COL_PROJ_LINK_NAME){
						lps.setLinkName((String)aValue);
					}else if(columnIndex == COL_PROJ_DESC){
						lps.setProjectRemark((String)aValue);
					}else if(columnIndex == COL_UPGRADE_URL){
						lps.setProjectUpgradeURL((String)aValue);
					}
				}
			}
			
			@Override
			public void removeTableModelListener(TableModelListener l) {
			}
			
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				if(rowIndex < dataRowNum 
						&& (columnIndex == COL_PROJ_ACTIVE 
						  ||columnIndex == COL_IS_ROOT)){
					return true;
				}
				return false;
			}
			
			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if(rowIndex >= dataRowNum){
					return null;
				}

				if(columnIndex == COL_NO){
					return data[rowIndex][columnIndex];
				} else {
					final LinkEditData led = (LinkEditData)data[rowIndex][IDX_OBJ_STORE];
					final LinkProjectStore lps = led==null?null:led.lps;
					if(columnIndex == COL_IS_ROOT){
						return (lps==null)?Boolean.FALSE:(lps.isRoot());
					}else if(columnIndex == COL_PROJ_ID){
						return (lps==null)?"":lps.getProjectID();
					}else if(columnIndex == COL_VER){
						return (lps==null)?LinkProjectStore.DEFAULT_UNKOWN_VER:lps.getVersion();
					}else if(columnIndex == COL_PROJ_ACTIVE){
						return (lps==null)?Boolean.FALSE:lps.isActive();
					}else if(columnIndex == COL_PROJ_LINK_NAME){
						return (lps==null)?"":lps.getLinkName();
					}else if(columnIndex == COL_PROJ_DESC){
						return (lps==null)?"":lps.getProjectRemark();
					}else if(columnIndex == COL_UPGRADE_URL){
						return (lps==null)?"":lps.getProjectUpgradeURL();
					}else{
						return null;
					}
				}
			}
			
			@Override
			public int getRowCount() {
				return data.length;
			}
			
			@Override
			public String getColumnName(int columnIndex) {
				return colNames[columnIndex].toString();
			}
			
			@Override
			public int getColumnCount() {
				return colNames.length;
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				switch (columnIndex) {
                case COL_IS_ROOT:
                    return Boolean.class;
                case COL_PROJ_ACTIVE:
                    return Boolean.class;
                default:
                    return String.class;
				}
			}
			
			@Override
			public void addTableModelListener(TableModelListener l) {
			}
		};
		
		dataRowNum = lpsVector.size();
		
		tablePanel = new HCTablePanel(tableModel, data, colNames, defaultRow, dataRowNum, 
				upBut, downBut, removeBut, importBut, editBut,
				//upOrDownMovingBiz
				new AbstractDelayBiz(null){
					@Override
					public void doBiz() {
						isChanged = true;
					}},
				//Remove
				new AbstractDelayBiz(null) {
					@Override
					public void doBiz() {
						final AbstractDelayBiz selfBiz = this;
						final Object[] rows = (Object[])getPara();
						JPanel askPanel = new JPanel();
						askPanel.add(new JLabel("Are you sure to remove project?", App.getSysIcon(App.SYS_QUES_ICON), SwingConstants.LEFT));
						App.showCenterPanel(askPanel, 0, 0, "Confirm Delete?", true, null, null, new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								final LinkEditData led = (LinkEditData)rows[IDX_OBJ_STORE];
								final LinkProjectStore lps = led.lps;
								
								if(lps.isRoot()){
									for (int i = 0; i < dataRowNum; i++) {
										final LinkEditData ledzero = (LinkEditData)data[i][IDX_OBJ_STORE];
										if(lps != ledzero.lps && ledzero.lps.isActive()){
											ledzero.lps.setRoot(true);
											break;
										}
									}
								}
								
								delProjInList(led);
								
								boolean[] back = {true};
								selfBiz.setPara(back);
							}
						}, null, self, true, false, removeBut, false, false);
					}
				}, 
				//import
				new AbstractDelayBiz(null) {
					@Override
					public void doBiz() {
						final AbstractDelayBiz selfBiz = this;
						
						final JPopupMenu pop = new JPopupMenu();
						JMenuItem addFromRemote = new JMenuItem("from remote URL...");
						JMenuItem addFromLocal = new JMenuItem("from local disk...");
						
						pop.addPopupMenuListener(new PopupMenuListener() {
							@Override
							public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
							}
							
							@Override
							public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
							}
							
							@Override
							public void popupMenuCanceled(PopupMenuEvent e) {
								selfBiz.setPara(Boolean.FALSE);
							}
						});
						
						pop.add(addFromLocal);
						pop.add(addFromRemote);
						
						addFromRemote.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null) {
									@Override
									public void doBiz() {
										doAddOp();
									}
								});
							}
							public void doAddOp() {
								String url = null;
								try{
									url = JOptionPane.showInputDialog(self, "project download/upgrade URL : ");
								}catch (Exception ex) {
								}finally{
									if(url == null){
										selfBiz.setPara(Boolean.FALSE);
										return;
									}
								}
								
								File fileHar = LinkProjectManager.getTempFileName(Designer.HAR_EXT);
								Properties had = new Properties();
								try{
									String strharurl = null;
									//支持har和had下载
									if(url.endsWith(Designer.HAR_EXT)){
										strharurl = url;
									}else{
										LinkProjectManager.loadHAD(url, had);
										strharurl = had.getProperty(HCjad.HAD_HAR_URL, HCjad.convertToExtHar(url));
									}
									PropertiesManager.addDelFile(fileHar);
									
									String hadmd5 = had.getProperty(HCjad.HAD_HAR_MD5, "");
									
									final Window[] waiting = {null};
									
									final boolean[] done = {false};
									ConditionWatcher.addWatcher(new IWatcher() {
										{
											App.showCenterMessageOnTop(self, true, "downloading...", waiting);
										}
										@Override
										public boolean watch() {
											if(done[0] == true && waiting[0] != null){
												waiting[0].dispose();
												return true;
											}else{
												return false;
											}
										}
										@Override
										public void setPara(Object p) {
										}
										
										@Override
										public boolean isNotCancelable() {
											return false;
										}
										
										@Override
										public void cancel() {
										}
									});
									boolean succ = HttpUtil.download(fileHar, new URL(strharurl));
									done[0] = true;
									if(succ == false){
										throw new Exception("http connection error");
									}
									
							        if((hadmd5.length() > 0 && ResourceUtil.getMD5(fileHar).toLowerCase().equals(hadmd5.toLowerCase()))
							        		|| hadmd5.length() == 0){
							        	addProjFromLocal(self, selfBiz, fileHar);
							        }else{
							        	throw new Exception("md5 error, try after a minute");
							        }
								}catch (Exception ex) {
									JOptionPane.showConfirmDialog(self, "Fail download, Exception : " + ex.toString(), "Fail download", JOptionPane.OK_OPTION);
									selfBiz.setPara(Boolean.FALSE);
									return;
								}
							}
						});
						addFromLocal.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								File file = FileSelector.selectImageFile(importBut, FileSelector.HAR_FILTER, true);
								addProjFromLocal(self, selfBiz, file);
							}
						});
						
						isCancelOp = false;
						pop.show(importBut, importBut.getWidth() - pop.getPreferredSize().width, importBut.getHeight());
						pop.updateUI();//在Window JRE 6环境下，需此行才正常，否则出现白框
						
						while(true){
							try{
								Thread.sleep(200);
							}catch (Exception e) {
							}
							Object back = selfBiz.getPara();
							if(back != null){
								if(back instanceof Boolean){
									selfBiz.setPara(null);
								}
								break;
							}
							if(isCancelOp){
								selfBiz.setPara(null);
								break;
							}
						}
					}
				}, true);
		final DefaultTableCellRenderer centerCellRender = new DefaultTableCellRenderer(){
	        public Component getTableCellRendererComponent(
	                JTable table, Object value, boolean isSelected,
	                boolean hasFocus, int row, int column) {
	        	setHorizontalAlignment(CENTER);
		        return super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
			}
        };
        
        ListSelectionModel selectModel = tablePanel.table.getSelectionModel();
        selectModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listSelectListener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final int selectedRow = tablePanel.table.getSelectedRow();
				if(selectedRow >= 0){
					LinkEditData led = (LinkEditData)data[selectedRow][IDX_OBJ_STORE];
					designBut.setEnabled(led != null && led.status == LinkProjectManager.STATUS_DEPLOYED);
				}
			}
		};
		selectModel.addListSelectionListener(listSelectListener);
    	
        designBut.setEnabled(dataRowNum > 0);

    	tablePanel.table.getColumnModel().getColumn(COL_NO).setCellRenderer(centerCellRender);
        tablePanel.table.getColumnModel().getColumn(COL_VER).setCellRenderer(centerCellRender);
        
        initTable(tablePanel.table);
		
		editBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LinkEditData led = (LinkEditData)data[tablePanel.table.getSelectedRow()][IDX_OBJ_STORE];
				LinkProjectStore lps = led.lps;
				
				LinkNamePanel panel = showInputLinkName(self, lps.getLinkName(), lps.getProjectRemark(), editBut);
				if(checkIsCancle(panel)){
					return;
				}
				final String newLinkName = panel.linkNameField.getText();
				final String newComment = panel.projRemarkField.getText();
				if(newLinkName.equals(lps.getLinkName())
						&& newComment.equals(lps.getProjectRemark())){
					//相同或取消
				}else{
					led.op = (LinkProjectManager.STATUS_MODIFIED);

					lps.setLinkName(newLinkName);
					lps.setProjectRemark(newComment);
					
					tablePanel.table.updateUI();
				}
			}
		});
		JPanel buttonsList = new JPanel();
		buttonsList.setLayout(new GridLayout(1, 6, ClientDesc.hgap, ClientDesc.vgap));
		buttonsList.add(designBut);
		buttonsList.add(upBut);
		buttonsList.add(downBut);
		buttonsList.add(removeBut);
		buttonsList.add(editBut);
		buttonsList.add(importBut);

		final JScrollPane scrollpane = new JScrollPane(tablePanel.table);
		scrollpane.setPreferredSize(new Dimension(800, 250));
		contentPane.add(scrollpane, BorderLayout.CENTER);
		contentPane.add(buttonsList, BorderLayout.NORTH);
		{
			final JLabel comp = new JLabel("choose apply mode :");
			JPanel panel = new JPanel(new BorderLayout());
			final JPanel group = new JPanel(new FlowLayout());
			{
				String op = getNewLinkedInProjOp();
				
				final ButtonGroup bg = new ButtonGroup();
				rb_startup.setToolTipText("new version project(s) will apply at next start up.");
				bg.add(rb_startup);
				group.add(rb_startup);
				if(op.equals(OP_NEXT_START_UP)){
					rb_startup.setSelected(true);
				}
				
				rb_ask.setToolTipText("ask whether apply immediately or not");
				bg.add(rb_ask);
				group.add(rb_ask);
				if(op.equals(OP_ASK)){
					rb_ask.setSelected(true);
				}
				
				rb_imme.setToolTipText("apply immediately automatically.");
				bg.add(rb_imme);
				group.add(rb_imme);
				if(op.equals(OP_IMMEDIATE)){
					rb_imme.setSelected(true);
				}
				
				ch_autoUpgrade.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						final boolean selected = ch_autoUpgrade.isSelected();
						comp.setEnabled(selected);
						rb_startup.setEnabled(selected);
						rb_ask.setEnabled(selected);
						rb_imme.setEnabled(selected);
					}
				});
				ch_autoUpgrade.setToolTipText("<html>enable or disable upgrading for those projects which are <strong>active</strong> and with <strong>" + upgradeURL + "</strong></html>");
				ch_autoUpgrade.setSelected(!PropertiesManager.getValue(
					PropertiesManager.p_EnableLinkedInProjUpgrade, IConstant.TRUE).equals(IConstant.TRUE));
				ch_autoUpgrade.doClick();
			}
			JPanel titl_group = new JPanel(new FlowLayout(FlowLayout.LEADING));
			titl_group.add(comp);
			titl_group.add(group);
			
			{
				JPanel checkPanel = new JPanel(new BorderLayout());
				checkPanel.setBorder(new TitledBorder(""));
				
				checkPanel.add(ch_autoUpgrade, BorderLayout.NORTH);
				checkPanel.add(titl_group, BorderLayout.CENTER);
				panel.add(checkPanel, BorderLayout.NORTH);				
			}
			panel.add(new JLabel("<html><STRONG>Description</STRONG>:" + 
					"<BR><strong>is Root</strong> : root project will be presented as main menu, other active project(s) are presented as folders of main menu." +
					"<BR>if mobile is online, " +
						"click '<strong>" + saveAndApply + "</strong>' will break off service, " +
								"and reload all active projects, you would <strong>NO</strong> restart HomeCenter.</html>"), BorderLayout.CENTER);
			contentPane.add(panel, BorderLayout.SOUTH);
		}
		ImageIcon okIco = null;
		try {
			okIco = new ImageIcon(ImageIO.read(ImageSrc.OK_ICON));
		} catch (IOException e1) {
		}
		okBut = new JButton(saveAndApply, okIco);
		final String exitText = (String) ResourceUtil.get(1011);
		
		final ActionListener cancelListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LinkProjectStatus.exitStatus();
				isCancelOp = true;
			}
		};
		jbCancle = new JButton(((exitText == null)?(String) ResourceUtil.get(1018):exitText),
				new ImageIcon(ImageSrc.CANCEL_ICON));
		cancelAction = new UIActionListener() {
			@Override
			public void actionPerformed(Window window, JButton ok,
					JButton cancel) {
				window.dispose();
				if (cancelListener != null) {
					cancelListener.actionPerformed(null);
				}
			}
		};
		final ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAndApply(self);
			}
		};
		jbOKAction = new UIActionListener() {
			@Override
			public void actionPerformed(Window window, JButton ok,
					JButton cancel) {
				try {
					if (listener != null) {
						listener.actionPerformed(null);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};

		designBut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
				LinkProjectStatus.exitStatus();
				
				final LinkEditData de_led = (LinkEditData)data[tablePanel.table.getSelectedRow()][IDX_OBJ_STORE];
				LinkProjectStore lps = de_led.lps;
				
				if(new File(LinkProjectManager.EDIT_HAR).exists() == false){
					//如果第一次是从ProjManager进入到设计器，防止初始加载缺省工程，所以要进行复制
					LinkProjectManager.copyCurrEditFromStorage(lps);
				}
				
				Designer d = Designer.getInstance();
				if(d == null){
					LinkMenuManager.startDesigner();
				}
				Designer.getInstance().shiftProject(lps);
			}
		});

	}

	public Window toShow(){
		return App.showCenterPanelWindow(contentPane, 0, 0, true, okBut,
				jbCancle, jbOKAction, cancelAction, dialog, relativeTo, true, false);//isResizabel=false,会导致漂移
	}

	private void checkAndStoreData() {
		final int size = dataRowNum;
		Object[] objs = new Object[size];
		
		//检查Root和Active条件相关约束
		boolean hasRoot = false;
		boolean hasActive = false;
		LinkProjectStore firstActive = null;
		for (int i = 0; i < size; i++) {
			LinkProjectStore lps = ((LinkEditData)data[i][IDX_OBJ_STORE]).lps;
			if(lps.isRoot()){
				lps.setActive(true);
				hasRoot = true;
				break;
			}
			if(lps.isActive()){
				if(firstActive == null){
					firstActive = lps;
				}
				hasActive = true;
			}
		}
		if(hasActive && hasRoot == false){
			firstActive.setRoot(true);
		}
		
		
		for (int i = 0; i < size; i++) {
			objs[i] = ((LinkEditData)data[i][IDX_OBJ_STORE]).lps.toSerial();
		}
		
		projIDSet.refill(objs);
		projIDSet.save();
		
		LinkProjectManager.reloadLinkProjects();
	}
	
	protected LinkNamePanel showInputLinkName(final JFrame self, final String linkName, final String mem, final Component relativeTo) {
		final LinkNamePanel panel = new LinkNamePanel(linkName, mem);
		App.showCenterPanel(panel, 0, 0, "Input Link Name", false, null, null,
			null, //cancel
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					panel.linkNameField.setText(panel.CANCLE);
				}
			}, self, true, false, relativeTo, false, false);//isNewFrame必须，因为JRE6会导致锁，所以不在addProjFromLocal中使用
		return panel;
	}

	public boolean checkIsCancle(LinkNamePanel panel) {
		return panel.linkNameField.getText().equals(panel.CANCLE);
	}

	private void addProjFromLocal(final JFrame self, final AbstractDelayBiz selfBiz, final File file) {
		if(file != null){
			final Map<String, Object> map = HCjar.loadHar(file, false);
			if(map.isEmpty()){
				JOptionPane.showMessageDialog(self, "Error link other project from har file.", 
						"Error link other project", JOptionPane.ERROR_MESSAGE);
			}else{
				final String licenseURL = ((String)map.get(HCjar.PROJ_LICENSE)).trim();
				if(licenseURL.length() > 0){
					IBiz biz = new IBiz(){
						@Override
						public void setMap(HashMap map) {
						}

						@Override
						public void start() {
							loadToTable(self, selfBiz, file, map);
						}
					};
					App.showHARProjectAgreeLicense("License of [" + map.get(HCjar.PROJ_NAME) + "]", licenseURL, biz, true, self);
					return;
				}
				loadToTable(self, selfBiz, file, map);
				return;
			}
		}
		selfBiz.setPara(Boolean.FALSE);
	}

	private void loadToTable(final JFrame self, final AbstractDelayBiz selfBiz,
			final File file, Map<String, Object> map) {
		int delOldProjIndex = -1;
		String linkName = "";
		String linkRemark = "";
		boolean delIsRoot = false;
		
		//检查是否存在同名的工程
		final String proj_id = (String)map.get(HCjar.PROJ_ID);
		for (int i = 0; i < dataRowNum; i++) {
			final LinkEditData led = (LinkEditData)data[i][IDX_OBJ_STORE];
			final LinkProjectStore lps = led.lps;
			
			if(lps.getProjectID().equals(proj_id)){
//						tablePanel.table.setRowSelectionInterval(i, i);
				
				int out = App.showOptionDialog(self, "project ID [" + proj_id + "] is exists, overide it now? ", "override exists project");
				if(out == JOptionPane.YES_OPTION){
					delProjInList(led);
					
					delOldProjIndex = i;
					linkName = lps.getLinkName();
					linkRemark = lps.getProjectRemark();
					delIsRoot = lps.isRoot();
				}else{
					selfBiz.setPara(Boolean.FALSE);
					return;
				}
				break;
			}
		}
		
//				if(delOldProjIndex < 0){
//					LinkNamePanel panel = showInputLinkName(self, "", "", editBut);
//					if(checkIsCancle(panel)){
//						selfBiz.setPara(Boolean.FALSE);
//						return;
//					}else{
//						linkName = panel.linkNameField.getText();
//						linkRemark = panel.projRemarkField.getText();
//					}
//				}
		
		final LinkEditData led = new LinkEditData();
		final LinkProjectStore lps = new LinkProjectStore();
		led.lps = lps;
		led.filePath = (file.getAbsolutePath());
		led.op = (LinkProjectManager.STATUS_NEW);
		led.status = (LinkProjectManager.STATUS_NEW);
		
		lps.setActive(true);
		lps.setLinkName(linkName);
		lps.setProjectRemark(linkRemark);
		lps.setProjectID(proj_id);
		lps.copyFrom(map);
		Object[] libName = {
			"", led
		};
		
		//必须要提前
		dataRowNum++;
		
		if(delOldProjIndex < 0){
			selfBiz.setPara(libName);
		}else{
			data[delOldProjIndex][IDX_OBJ_STORE] = led;
			led.lps.setRoot(delIsRoot);
			
			//因为下行无对象返回，所以本处进行刷新
			tablePanel.table.updateUI();
			selfBiz.setPara(Boolean.FALSE);
		}
		
		return;
	}

	private void delProjInList(final LinkEditData led) {
		delList.add(led);
		dataRowNum--;
	}

	private void saveAndApply(final JFrame self) {
		synchronized (ServerUIUtil.LOCK) {
			//将已发布，且准备进行删除的进行删除操作
			for (int i = 0; i < delList.size(); i++) {
				LinkEditData led = delList.elementAt(i);
				LinkProjectStore lps = led.lps;
				if(led.status == LinkProjectManager.STATUS_DEPLOYED){
					final boolean result = LinkProjectManager.removeLinkProjectPhic(lps, true);
					
					isChanged = isChanged?true:result;
				}
			}
			delList.removeAllElements();
			for (int i = 0; i < dataRowNum; i++) {
				LinkEditData led = (LinkEditData)data[i][IDX_OBJ_STORE];
				LinkProjectStore lps = led.lps;
				if(led.status == LinkProjectManager.STATUS_NEW){
					File source = new File(led.filePath);
					final boolean result = LinkProjectManager.importLinkProject(lps, source);
					led.status = LinkProjectManager.STATUS_DEPLOYED;
					isChanged = isChanged?true:result;
				}else if(led.op == LinkProjectManager.STATUS_MODIFIED){
					isChanged = true;
				}
				led.op = (LinkProjectManager.STATUS_NEW);
			}
			if(rb_startup.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpNewLinkedInProjVer, OP_NEXT_START_UP);
			}else if(rb_ask.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpNewLinkedInProjVer, OP_ASK);
			}else if(rb_imme.isSelected()){
				PropertiesManager.setValue(PropertiesManager.p_OpNewLinkedInProjVer, OP_IMMEDIATE);
			}

			PropertiesManager.setValue(PropertiesManager.p_EnableLinkedInProjUpgrade, ch_autoUpgrade.isSelected()?IConstant.TRUE:IConstant.FALSE);
			
			PropertiesManager.saveFile();
			if(isChanged){
				DelayServer.getInstance().addDelayBiz(new AbstractDelayBiz(null){
					@Override
					public void doBiz() {
//								L.V = L.O ? false : LogManager.log("restarting service...");
						Window[] tipWindow = {null};
						if(ServerUIUtil.promptAndStop(true, self) == false){
							App.showCenterMessageOnTop(self, true, saveAndApply + "...", tipWindow);
						}
						
						checkAndStoreData();
						
						//由于上行已更新，所以可以采用searchRoot
						final LinkProjectStore root = LinkProjectManager.searchRoot(true);//必须查询为active状态的。
						if(root != null){
							Designer.setProjectOn();
						}else{
							Designer.setProjectOff();	
						}
						
						Designer design = Designer.getInstance();
						if(design != null){
							design.refresh();
						}
						
						//启动远屏或菜单
						ServerUIUtil.restartResponsorServerDelayMode();
						if(tipWindow[0] != null){
							try{
								Thread.sleep(1000);
							}catch (Exception e) {
							}
							tipWindow[0].dispose();
						}
						
						listSelectListener.valueChanged(null);//强制刷新当前行
						tablePanel.table.repaint();//检查完善Root,Active，故刷新
						isChanged = false;
					}
				});
			}
		}
	}

}
