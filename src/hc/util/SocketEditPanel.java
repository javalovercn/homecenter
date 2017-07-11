package hc.util;

import hc.App;
import hc.core.ContextManager;
import hc.core.util.ExceptionReporter;
import hc.core.util.ThreadPriorityManager;
import hc.res.ImageSrc;
import hc.server.ActionListenerRun;
import hc.server.HCActionListener;
import hc.server.util.ContextSecurityConfig;
import hc.server.util.HCEnableHeaderRenderer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

public abstract class SocketEditPanel extends JPanel{
	final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	
	private final JCheckBox checkLimitOn;
	private final JCheckBox accessPrivateAddress;
	JPanel formatPanel;
	JPanel actionPanel;
	JPanel portPanel;
	JPanel hostPanel;
	JLabel descLabel;
	private final JTable tableList;
	private final JTextField hostField;
	private final IntTextField portField, portFromField, portToField;
	private final ButtonGroup hostAndIPGroup = new ButtonGroup();
	private final ButtonGroup portAndRangeGroup = new ButtonGroup();
	private final JRadioButton hostRadioBtn = new JRadioButton("host/IPv4/IPv6 :");
	private final JRadioButton rangeRadioBtn = new JRadioButton("range :");
	private final JRadioButton ipRadioBtn = new JRadioButton("ipv4 :");
	private final JRadioButton portRadioBtn = new JRadioButton("port :");
	private final JButton deleteBtn = new JButton(new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
	private final JButton addBtn = new JButton(new ImageIcon(ImageSrc.ADD_SMALL_ICON));
	public Vector<SocketDesc> allowSockets;
	private final JCheckBox checkAccept = new JCheckBox(SocketDesc.STR_ACCEPT);
	private final JCheckBox checkconnect = new JCheckBox(SocketDesc.STR_CONNECT);
	private final JCheckBox checklisten = new JCheckBox(SocketDesc.STR_LISTEN);
	private final JCheckBox checkresolve = new JCheckBox(SocketDesc.STR_RESOLVE);
	private final IPv4Field ipField = new IPv4Field("");
	private int currRow;
	private final int columnHost = 0;
	private final int columnPort = 1;
	private final int columnAction = 2;
	private final int columnNum = columnAction + 1;
	JPanel mainEditPanel = new JPanel();
	private boolean isSelectModify = true;
	
	public final void switchEditable(final boolean isEdit){
		accessPrivateAddress.setEnabled(isEdit);
		
		mainEditPanel.setEnabled(isEdit);
		tableList.setEnabled(isEdit);
		hostField.setEnabled(isEdit);
		ipField.setEnabled(isEdit);
		
		portField.setEnabled(isEdit);
		portFromField.setEnabled(isEdit);
		portToField.setEnabled(isEdit);
		
		checkAccept.setEnabled(isEdit);
		checkconnect.setEnabled(isEdit);
		checklisten.setEnabled(isEdit);
//		checkresolve.setEnabled(isEdit);
		
		hostRadioBtn.setEnabled(isEdit);
		ipRadioBtn.setEnabled(isEdit);
		portRadioBtn.setEnabled(isEdit);
		rangeRadioBtn.setEnabled(isEdit);
		
		formatPanel.setEnabled(isEdit);
		actionPanel.setEnabled(isEdit);
		portPanel.setEnabled(isEdit);
		hostPanel.setEnabled(isEdit);
		
		addBtn.setEnabled(isEdit);
		deleteBtn.setEnabled(isEdit);
		
		descLabel.setEnabled(isEdit);
		
		if(isEdit){
			if(allowSockets != null && allowSockets.size() > 0){
				tableList.clearSelection();
				tableList.getSelectionModel().setSelectionInterval(0, 0);
			}else{
				setInitEditPanelEnable(false);
				deleteBtn.setEnabled(false);
				hostField.setEnabled(false);
				portField.setEnabled(false);
			}
		}
	}

	public SocketEditPanel(){
		checkLimitOn = new JCheckBox("limit socket/connect");
		checkLimitOn.setToolTipText("" +
				"<html>" +
				"if not selected, then allow access all public address and private address." +
				"<BR>" +
				"if selected and there is no record in table, it means block all for current project." +
				"</html>");
		
		accessPrivateAddress = new JCheckBox("access private address");
		accessPrivateAddress.setToolTipText("" +
				"<html>" +
				"access private address :" +
				"<BR>" +
				"&nbsp;·&nbsp;10.*.*.*<BR>" +
				"&nbsp;·&nbsp;127.*.*.*, localhost<BR>" +
				"&nbsp;·&nbsp;169.254.*.*<BR>" +
				"&nbsp;·&nbsp;172.16.*.*, 172.31.*.*<BR>" +
				"&nbsp;·&nbsp;192.168.*.*<BR>" +
				"&nbsp;·&nbsp;224.0.0.0 - 239.255.255.255" +
				"</html>");
		
		accessPrivateAddress.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				getCSCSource().setAccessPrivateAddress(accessPrivateAddress.isSelected());
				notifyModify();
			}
		}, threadPoolToken));			
		
		checkLimitOn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				notifyModify();
				final boolean limitOn = checkLimitOn.isSelected();
				forceSwitchEdit(limitOn);
			}
		}, threadPoolToken));
		
		final AbstractTableModel modelSocket = new AbstractTableModel() {
			@Override
			public Object getValueAt(final int rowIndex, final int columnIndex) {
				if(rowIndex >= allowSockets.size()){
					return "";
				}
				
				final SocketDesc socket = allowSockets.elementAt(rowIndex);
				if(columnIndex == columnHost){
					return socket.isIPMode()?socket.getIp():socket.getHost();
				}else if(columnIndex == columnPort){
					final String port = socket.getPort();
					final String portFrom = socket.getPortFrom();
					final String portTo = socket.getPortTo();
					return socket.isRangeMode()?(((portFrom==null)?"":portFrom) + " - " + ((portTo==null)?"":portTo)):port;
				}else if(columnIndex == columnAction){
					return socket.getActionDesc();
				}
				return "";
			}
			@Override
			public int getRowCount() {
				return allowSockets==null?0:allowSockets.size();//Android环境下会出现null，估计与某些标签未实现
			}
			@Override
			public int getColumnCount() {
				return columnNum;
			}
			@Override
			public String getColumnName(final int columnIndex) {
				if(columnIndex == columnHost){
					return "Host";
				}else if(columnIndex == columnPort){
					return "Port";
				}else if(columnIndex == columnAction){
					return "Action";
				}else{
					return "";
				}
			}
		};
		tableList = new JTable(modelSocket);
		tableList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		
		final HCEnableHeaderRenderer oldRend = new HCEnableHeaderRenderer(tableList.getTableHeader().getDefaultRenderer());
		tableList.getTableHeader().setDefaultRenderer(oldRend);
		
		tableList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if(e.getValueIsAdjusting() == false){
					final ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			        if (lsm.isSelectionEmpty()) {
			        	return;
			        } else {
			            final int minIndex = lsm.getMinSelectionIndex();
			            final int maxIndex = lsm.getMaxSelectionIndex();
			            for (int i = minIndex; i <= maxIndex; i++) {
			                if (lsm.isSelectedIndex(i)) {
			                	currRow = i;
			                	ContextManager.getThreadPool().run(new Runnable() {
			    					@Override
									public void run() {
					                	isSelectModify = true;
					                	updateFieldsFromTable();
					                	try{
					                		Thread.sleep(ThreadPriorityManager.UI_WAIT_MS);
					                	}catch (final Exception e) {
										}
					                	isSelectModify = false;
			    					}}, threadPoolToken);
			                	return;
			                }
			            }
			        }
				}
			}
		});
		
		final int columnSize = 10;
		final int portColumnSize = 4;
		
		final KeyListener refreshListener = new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
			}
			
			@Override
			public void keyReleased(final KeyEvent e) {
				App.invokeLaterUI(updateTableFromFields);
			}
			
			@Override
			public void keyPressed(final KeyEvent e) {
			}
		};
		
		final ActionListener actionActionListener = new HCActionListener(new ActionListenerRun() {
			@Override
			public void run() {
				if(checkMinAction((JCheckBox)getActionEvent().getSource())){
					return;
				}
				
				final SocketDesc socket = allowSockets.elementAt(currRow);
				updateCheckToDataBlock(socket);
				notifyModify();
			}
			
			private final boolean checkMinAction(final JCheckBox checkBox){
				if(checkAccept.isSelected() == false && checkconnect.isSelected() == false && checklisten.isSelected() == false){
					ContextManager.getThreadPool().run(new Runnable() {
						@Override
						public void run() {
							checkBox.setSelected(true);
						}
					}, threadPoolToken);
					return true;
				}
				return false;
			}
		}, threadPoolToken);
		
		checkAccept.addActionListener(actionActionListener);
		checkconnect.addActionListener(actionActionListener);
		checklisten.addActionListener(actionActionListener);
		checkresolve.addActionListener(actionActionListener);
		
		checkresolve.setEnabled(false);

		hostField = new JTextField(columnSize);
		hostField.addKeyListener(refreshListener);
		
		ipField.addKeyListener(refreshListener);
		
		portField = new IntTextField(1, 65535);
		portField.addKeyListener(refreshListener);
		
		portFromField = new IntTextField(1, 65535);
		portFromField.addKeyListener(refreshListener);
		
		portToField = new IntTextField(1, 65535);
		portToField.addKeyListener(refreshListener);
		
		portField.setColumns(portColumnSize);
		portFromField.setColumns(portColumnSize);
		portToField.setColumns(portColumnSize);
		
		hostRadioBtn.setToolTipText("<html>host name : www.google.com" +
				"<br>ipv6 : <STRONG>[</STRONG>::ffff:8.8.8.8<STRONG>]</STRONG>, <STRONG>[]</STRONG> is required.</html>");
		
		hostAndIPGroup.add(hostRadioBtn);
		hostRadioBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final boolean isHost = hostRadioBtn.isSelected();
				switchToHost(isHost);
			}
		}, threadPoolToken));
		hostAndIPGroup.add(ipRadioBtn);
		ipRadioBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final boolean isIP = ipRadioBtn.isSelected();
				switchToHost(!isIP);
			}
		}, threadPoolToken));
		
		portAndRangeGroup.add(portRadioBtn);
		portRadioBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final boolean isPort = portRadioBtn.isSelected();
				switchToPort(isPort);
			}
		}, threadPoolToken));
		portAndRangeGroup.add(rangeRadioBtn);
		rangeRadioBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final boolean isRange = rangeRadioBtn.isSelected();
				switchToPort(!isRange);
			}
		}, threadPoolToken));
		
		final JPanel editPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints editgc = new GridBagConstraints();
		editgc.fill = GridBagConstraints.BOTH;
		editgc.weightx = 1.0;
		editgc.weighty = 1.0;
		{
			hostPanel = new JPanel(new GridLayout(4, 1));
			
			hostPanel.setBorder(new TitledBorder("host :"));
			hostPanel.add(hostRadioBtn);
			hostPanel.add(hostField);
			hostPanel.add(ipRadioBtn);
			hostPanel.add(ipField);
			
			editgc.gridx = 0;
			editPanel.add(hostPanel, editgc);
		}
		{
			portPanel = new JPanel(new GridLayout(4, 1));
			
			portPanel.setBorder(new TitledBorder("port :"));
			portPanel.add(portRadioBtn);
			portPanel.add(portField);
			portPanel.add(rangeRadioBtn);
			
			{
				final JPanel subPanel = new JPanel(new GridBagLayout());
				final GridBagConstraints c = new GridBagConstraints();
				c.fill = GridBagConstraints.BOTH;
				c.weightx = 1.0;
				c.weighty = 1.0;
				subPanel.add(portFromField, c);
				c.gridx = 1;
				c.fill = GridBagConstraints.NONE;
				subPanel.add(new JLabel(" - "));
				c.gridx = 2;
				c.fill = GridBagConstraints.BOTH;
				subPanel.add(portToField, c);
				
				portPanel.add(subPanel);
			}
			editgc.gridx = 1;
			editPanel.add(portPanel, editgc);
		}
		
		{
			actionPanel = new JPanel(new GridLayout(4, 1));
			
			actionPanel.setBorder(new TitledBorder("action :"));
			actionPanel.add(checkAccept);
			actionPanel.add(checkconnect);
			actionPanel.add(checklisten);
			actionPanel.add(checkresolve);
			
			editgc.gridx = 2;
			editPanel.add(actionPanel, editgc);
			
			checkAccept.setToolTipText(SocketDesc.ACCEPT_TIP);
			checkconnect.setToolTipText(SocketDesc.CONNECT_TIP);
			checklisten.setToolTipText(SocketDesc.LISTEN_TIP);
			checkresolve.setToolTipText(SocketDesc.RESOLVE_TIP);
		}
		
		{
			hostField.setNextFocusableComponent(portField);
			portField.setNextFocusableComponent(checkAccept);
			checkAccept.setNextFocusableComponent(checkconnect);
			checkconnect.setNextFocusableComponent(checklisten);
			checklisten.setNextFocusableComponent(ipRadioBtn);
			ipRadioBtn.setNextFocusableComponent(ipField);
			ipField.setNextFocusableComponent(rangeRadioBtn);
			rangeRadioBtn.setNextFocusableComponent(portFromField);
			portFromField.setNextFocusableComponent(portToField);
		}
		
		mainEditPanel.setLayout(new GridBagLayout());
		int gridyIdx = 0;
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.gridy = gridyIdx++;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			c.weighty = 1.0;
			final JScrollPane scrollPane = new JScrollPane(tableList);
			scrollPane.setPreferredSize(new Dimension(500, 80));
			final JPanel panel = new JPanel(new BorderLayout());
			panel.add(scrollPane, BorderLayout.CENTER);
			{
				final JPanel btnList = new JPanel(new GridBagLayout());
				final GridBagConstraints gc = new GridBagConstraints();
				gc.fill = GridBagConstraints.NONE;
				btnList.add(addBtn, gc);
				gc.gridy = 1;
				btnList.add(deleteBtn, gc);
				panel.add(btnList, BorderLayout.EAST);
			}
			mainEditPanel.add(panel, c);
		}
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.gridy = gridyIdx++;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			c.weighty = 1.0;
			mainEditPanel.add(editPanel, c);
		}
		{
			final GridBagConstraints c = new GridBagConstraints();
			c.gridy = gridyIdx++;
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			c.weighty = 1.0;
			formatPanel = new JPanel(new BorderLayout());
			formatPanel.setBorder(new TitledBorder("host and port examples :"));
			descLabel = new JLabel("<html>" +
					" <STRONG>·</STRONG> www.sun.com" +
					//", *.sun.com:80" +
					", [::ffff:8.8.8.8]" +
					"<br> <STRONG>·</STRONG> 8.8.8.8:1234" +
					", 8.8.8.8:1234-" +
					"<br> <STRONG>·</STRONG> localhost:1024-" +
					", localhost:-1024" +
					", localhost:1024-2048" +
					"</html>", SwingConstants.LEADING);
			formatPanel.add(descLabel, BorderLayout.CENTER);
			mainEditPanel.add(formatPanel, c);
		}
		mainEditPanel.setBorder(new TitledBorder("allowable public/private address list :"));
		
		{
			setLayout(new BorderLayout());
			
			add(checkLimitOn, BorderLayout.NORTH);
			{
				final JPanel panel = new JPanel(new BorderLayout());
				panel.add(accessPrivateAddress, BorderLayout.NORTH);
				panel.add(mainEditPanel, BorderLayout.CENTER);
				
				add(panel, BorderLayout.CENTER);
			}
			add(new JLabel("      "), BorderLayout.LINE_START);
		}
		
		initBtn();
	}
	
	public void notifyLostEditPanelFocus(){
	}
	
	public abstract void notifyModify();
	
	public abstract ContextSecurityConfig getCSCSource();
	
	public final void notifySocketLimitOn(final boolean isOn){
		ContextSecurityConfig.setSocketLimitOn(getCSCSource(), isOn);
	}
	
	public final boolean isSocketLimitOn(){
		return ContextSecurityConfig.isSocketLimitOn(getCSCSource());
	}
	
	private final Runnable updateTableFromFields = new Runnable() {
		@Override
		public void run() {
			if(allowSockets == null || allowSockets.size() == 0){
				return;
			}
			
			final SocketDesc socket = allowSockets.elementAt(currRow);
			
			if(hostField.isEnabled()){
				socket.setHost(hostField.getText());
			}else{
				socket.setIp(ipField.getAddress());
			}
			
			if(portField.isEnabled()){
				socket.setPort(portField.getText());
			}else{
				socket.setPortFrom(portFromField.getText());
				socket.setPortTo(portToField.getText());
			}
			
			updateCheckToDataBlock(socket);
			
			if(isSelectModify == false){
				notifyModify();
			}
		}
	};
	
	public final void refresh(final ContextSecurityConfig cscOld){
		final ContextSecurityConfig csc = getCSCSource();
		
//		if(cscOld != csc){
//			System.err.println("refresh must the same object!");
//			throw new Error("refresh must the same object!");
//		}
		
		if(csc != null){//有可能为null，比如测试
			loadFromCSC(csc);
		}
		
		final boolean isLimitOn = isSocketLimitOn();
		forceSwitchEdit(isLimitOn);
		
		checkLimitOn.setSelected(isLimitOn);
		accessPrivateAddress.setSelected(isLimitOn && csc.isAccessPrivateAddress());

		final TableModel model = tableList.getModel();
		if(model instanceof AbstractTableModel){
			((AbstractTableModel)model).fireTableDataChanged(); 
		}
		tableList.repaint();
		
		if(isLimitOn && allowSockets != null && allowSockets.size() > 0){
			tableList.getSelectionModel().setSelectionInterval(0, 0);
		}
	}
	
	public final void updateFieldsFromTable(){
		if(allowSockets.size() == 0){
			return;
		}
		
		final SocketDesc socket = allowSockets.elementAt(currRow);
		
		{
			final boolean isIPAddr = socket.isIPMode();
			
			if(isIPAddr){
				hostField.setText("");
				ipField.setAddress(socket.getIp());
			}else{
				hostField.setText(socket.getHost());
				ipField.setAddress(null);
			}
			hostField.setEnabled(!isIPAddr);
			ipField.setEnabled(isIPAddr);
			
			hostRadioBtn.setSelected(!isIPAddr);
			ipRadioBtn.setSelected(isIPAddr);
		}
		
		{
			final boolean isRange = socket.isRangeMode();
			
			if(isRange){
				portField.setText("");
				portFromField.setText(socket.getPortFrom());
				portToField.setText(socket.getPortTo());
			}else{
				portField.setText(socket.getPort());
				portFromField.setText("");
				portToField.setText("");
			}
			portField.setEnabled(!isRange);
			portFromField.setEnabled(isRange);
			portToField.setEnabled(isRange);
			
			portRadioBtn.setSelected(!isRange);
			rangeRadioBtn.setSelected(isRange);
		}
		
		checkAccept.setSelected(socket.isAcceptAction());
		checkconnect.setSelected(socket.isConnectAction());
		checklisten.setSelected(socket.isListenAction());
		checkresolve.setSelected(socket.isResolveAction());
	}
	
	private final void clearEditPanel(){
		hostField.setText("");
		ipField.setAddress("");
		
		portField.setText("");
		portFromField.setText("");
		portToField.setText("");
		
//		accessPrivateAddress.setSelected(false);//与record数无关
		
		checkAccept.setSelected(false);
		checkconnect.setSelected(false);
		checklisten.setSelected(false);
		checkresolve.setSelected(false);
		
		hostRadioBtn.setSelected(true);
		ipRadioBtn.setSelected(false);
		
		portRadioBtn.setSelected(true);
		rangeRadioBtn.setSelected(false);
	}
	
	/**
	 * 记录数为0或转新增时，切换编辑状态。
	 * @param isEnable
	 */
	private final void setInitEditPanelEnable(final boolean isEnable){
//		accessPrivateAddress.setEnabled(isEnable);//此行与记录数为0或多条无关。
		
		hostRadioBtn.setEnabled(isEnable);
		portRadioBtn.setEnabled(isEnable);
		ipRadioBtn.setEnabled(isEnable);
		rangeRadioBtn.setEnabled(isEnable);
		
		ipField.setEnabled(isEnable);
		portFromField.setEnabled(isEnable);
		portToField.setEnabled(isEnable);
		
		checkAccept.setEnabled(isEnable);
		checkconnect.setEnabled(isEnable);
		checklisten.setEnabled(isEnable);
//		checkresolve.setEnabled(isEnable);
	}
	
	private final void initBtn(){
		addBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				notifyModify();
				
				setInitEditPanelEnable(true);
				
				ContextSecurityConfig.addDefaultNewSocket(allowSockets);
				final int size = allowSockets.size();
				tableList.clearSelection();
				
				deleteBtn.setEnabled(true);
				hostField.setEnabled(true);
				portField.setEnabled(true);
				
				hostField.requestFocus();
				
				final TableModel model = tableList.getModel();
				if(model instanceof AbstractTableModel){
					((AbstractTableModel)model).fireTableDataChanged(); 
				}
				
				tableList.setRowSelectionInterval(size - 1, size - 1);
				
				ContextManager.getThreadPool().run(new Runnable() {
					@Override
					public void run() {
						try{
							Thread.sleep(500);
						}catch (final Exception e) {
						}
						hostField.selectAll();
					}
				}, threadPoolToken);
			}
		}, threadPoolToken));
		
		deleteBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				notifyModify();
				
				final int currRow = tableList.getSelectedRow();
				
				allowSockets.remove(currRow);
				
				tableList.clearSelection();
				try{
					final int size = allowSockets.size();
					if(size == 0){
						deleteBtn.setEnabled(false);
						clearEditPanel();
						setInitEditPanelEnable(false);
						
						hostField.setEnabled(false);
						portField.setEnabled(false);
					}
					if(currRow < size){
						tableList.setRowSelectionInterval(currRow, currRow);
					}else{
						if(size == 0){
//							tableList.setRowSelectionInterval(0, 0);
						}else{
							tableList.setRowSelectionInterval(size - 1, size - 1);
						}
					}
				}catch (final Exception e) {
					ExceptionReporter.printStackTrace(e);
				}
				tableList.updateUI();
			}
		}));
	}
	
	private final void switchToHost(final boolean isHost) {
		hostField.setEnabled(isHost);
		ipField.setEnabled(!isHost);

		
		final SocketDesc elementAt = allowSockets.elementAt(currRow);
		
		elementAt.setIPMode(!isHost);
		
		if(isHost){
			ipField.setAddress("");
			elementAt.setIp("");
		}else{
			hostField.setText("");
			elementAt.setHost("");
		}
		
		tableList.updateUI();
	}
	
	private final void loadFromCSC(final ContextSecurityConfig csc){
		csc.loadToVector();
		csc.copyToSocketPanel(this);
		updateFieldsFromTable();
	}

	private final void switchToPort(final boolean isPort) {
		portField.setEnabled(isPort);
		portFromField.setEnabled(!isPort);
		portToField.setEnabled(!isPort);
		
		final SocketDesc elementAt = allowSockets.elementAt(currRow);
		elementAt.setPortMode(isPort);
		
		if(isPort){
			portFromField.setText("");
			portToField.setText("");
			elementAt.setPortFrom("");
			elementAt.setPortTo("");
		}else{
			portField.setText("");
			elementAt.setPort("");
		}
		
		tableList.repaint();
		tableList.updateUI();
	}

	private void forceSwitchEdit(final boolean limitOn) {
		notifySocketLimitOn(limitOn);
		switchEditable(limitOn);
		
		final HCEnableHeaderRenderer hr = (HCEnableHeaderRenderer)tableList.getTableHeader().getDefaultRenderer();
		hr.setEnabled(limitOn);
		
		final DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();
		dtcr.setEnabled(limitOn);
		for (int i = 0; i < columnNum; i++) {
			tableList.getColumnModel().getColumn(i).setCellRenderer(dtcr);
		}
	}

	private final void updateCheckToDataBlock(final SocketDesc socket) {
		final boolean isAccept = checkAccept.isSelected();
		final boolean isConnect = checkconnect.isSelected();
		final boolean isListen = checklisten.isSelected();
		
		if(isAccept || isConnect || isListen){
			if(checkresolve.isSelected() == false){
				checkresolve.setSelected(true);
			}
		}
		
		socket.setAction(SocketDesc.ACCEPT, isAccept);
		socket.setAction(SocketDesc.CONNECT, isConnect);
		socket.setAction(SocketDesc.LISTEN, isListen);
		socket.setAction(SocketDesc.RESOLVE, checkresolve.isSelected());
		
		tableList.updateUI();//有可能更新check_resolve，所以要刷新
	}
}
