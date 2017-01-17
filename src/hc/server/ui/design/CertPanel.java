package hc.server.ui.design;

import hc.App;
import hc.core.util.StringUtil;
import hc.res.ImageSrc;
import hc.server.AbstractDelayBiz;
import hc.server.HCActionListener;
import hc.server.HCTablePanel;
import hc.server.ProcessingWindowManager;
import hc.server.ui.ClientDesc;
import hc.server.ui.ClosableWindow;
import hc.server.util.SignHelper;
import hc.server.util.SignItem;
import hc.util.PropertiesManager;
import hc.util.ResourceUtil;
import hc.util.SecurityDataProtector;
import hc.util.StringBuilderCacher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

/**
 * 注意：必须是X509Certificate证书，参见{@link SignHelper#verifyJar(File, X509Certificate[])}
 *
 */
public class CertPanel extends CertListPanel{
	final ThreadGroup threadPoolToken = App.getThreadPoolToken();
	final String save = (String) ResourceUtil.get(1017);
	final boolean isLimitOneAlias = true;

	final HCTablePanel tablePanel;
	final JButton addBut, importBut, descBut, removeBut, saveBut, exitBut, changePwdBut, backupBut, restoreBut;
	boolean isChanged = false;

	ListSelectionListener listSelectListener;
	
	final JPanel contentPane;
	final JFrame dialog;
	
	private final void removeCurrAlias(){
		final int selectedIdx = tablePanel.table.getSelectedRow();
		if(selectedIdx >= 0){
			items.remove(selectedIdx);
			refreshButtons();
		}
	}

	private final void refreshButtons() {
		if(items.size() == 0){
			removeBut.setEnabled(false);
			descBut.setEnabled(false);
			if(isLimitOneAlias){
				addBut.setEnabled(true);
			}
		}else{
			removeBut.setEnabled(true);
			descBut.setEnabled(true);
			if(isLimitOneAlias){
				addBut.setEnabled(false);
			}
		}
	}
	
	JFileChooser fileChooser;
	
	private final boolean checkFileAll(final SignItem item, final JButton ok){
		if(item.chain == null){
			return false;
		}
		if(item.privateKey == null){
			return false;
		}
		
		ok.setEnabled(true);
		return true;
	}
	
	private final File chooseFile(final boolean isOpen){
		if(fileChooser == null){
			fileChooser = new JFileChooser(new File("."));
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			final String[] pfxFile = {SecurityDataProtector.DEV_CERT_FILE_EXT};
			final FileNameExtensionFilter pfxFilter = new FileNameExtensionFilter(SecurityDataProtector.DEV_CERT_FILE_EXT + " file", pfxFile);			
			fileChooser.setFileFilter(pfxFilter);
		}
		
		final int returnVal = isOpen?fileChooser.showOpenDialog(dialog):fileChooser.showSaveDialog(dialog);
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	        File selectedFile = fileChooser.getSelectedFile();
	        if(selectedFile.getName().endsWith("." + SecurityDataProtector.DEV_CERT_FILE_EXT) == false){
	        	selectedFile = new File(selectedFile.getAbsolutePath() + "." + SecurityDataProtector.DEV_CERT_FILE_EXT);
	        }
			return selectedFile;
	    } else {
	        return null;
	    }
	}
	
	public CertPanel(final Component relativeTo, final String certPassword) {
		super(certPassword);
		
		final String saveButtonText = (String)ResourceUtil.get(1017);
		final String title = (String)ResourceUtil.get(9220);
		dialog = (JFrame)App.buildCloseableWindow(true, null, title, false);
//		final JFrame self = (owner==null && (dialog instanceof JFrame))?(JFrame)dialog:owner;
		dialog.setLocationRelativeTo(relativeTo);
		
		contentPane = new JPanel();
		
		dialog.getContentPane().add(contentPane);
		
		contentPane.setLayout(new BorderLayout(ClientDesc.hgap, ClientDesc.vgap));
		
		addBut = new JButton((String)ResourceUtil.get(9016), new ImageIcon(ImageSrc.ADD_SMALL_ICON));
		addBut.setToolTipText("<html>" +
				"create a pare of self-signed public key and private key to current store.<BR><BR>" +
				"if you publish an app and then lose the key with which you signed your app, " +
				"<BR>you will not be able to publish any updates to your app." +
				"</html>");
		if(isLimitOneAlias){
			if(items.size() > 0){
				addBut.setEnabled(false);
			}
		}
		
		saveBut = new JButton(saveButtonText, new ImageIcon(ImageSrc.OK_ICON));
		changePwdBut = new JButton((String)ResourceUtil.get(1007), new ImageIcon(ImageSrc.PASSWORD_ICON));
		changePwdBut.setToolTipText("<html>set/change password of developer certificates." +
				"<BR>it will be effective after click <STRONG>[" + saveButtonText + "]</STRONG>.</html>");
		
		exitBut = new JButton((String)ResourceUtil.get(1011), new ImageIcon(ImageSrc.CANCEL_ICON));
		removeBut = new JButton((String)ResourceUtil.get(9018), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
//		removeBut.setToolTipText("<html>" + (String)ResourceUtil.get(9141) + "</html>");
		final String str_import = (String)ResourceUtil.get(9223);
		importBut = new JButton(str_import, new ImageIcon(ImageSrc.loadImageFromPath(ImageSrc.HC_IMPORT_22_PATH)));
		importBut.setToolTipText("<html>import private key, chain from CA to current store.</html>");
		final String str_backup = (String)ResourceUtil.get(9221);
		backupBut = new JButton(str_backup, new ImageIcon(ImageSrc.loadImageFromPath(ImageSrc.HC_BACKUP_22_PATH)));
		backupBut.setToolTipText("backup developer certificates to disk or USB.");
		backupBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final File saveFile = chooseFile(false);
				if(saveFile != null){
					ResourceUtil.copy(SecurityDataProtector.getDevCertFile(), saveFile);
					App.showMessageDialog(dialog, 
							new JLabel("<html>Successful backup developer certificates!" +
							"<BR>" +
							"<BR>" +
							"<STRONG>Important :</STRONG><BR>" +
							"please write down the password and place it in a safe place." +
							"<BR>if you lose the password and certificates, your HAR can NOT be signed with it again!</html>"), 
							ResourceUtil.getInfoI18N(), App.INFORMATION_MESSAGE, App.getSysIcon(App.SYS_INFO_ICON));
				}
			}
		}, threadPoolToken));
		
		backupBut.setEnabled(SecurityDataProtector.getDevCertFile().exists());
		
		final String str_restore = (String)ResourceUtil.get(9222);
		restoreBut = new JButton(str_restore, new ImageIcon(ImageSrc.loadImageFromPath(ImageSrc.HC_RESTORE_22_PATH)));
		restoreBut.setToolTipText("<html>restore developer certificates from a backup file," +
				"<BR>the current certificates will be overrided.</html>");
		restoreBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				if(CertPanel.this.hasItem()){
					final int result = App.showConfirmDialog(dialog, "<html>current certificates will be overried!<BR>" +
							"Are sure to restore?</html>", "continue restore?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, App.getSysIcon(App.SYS_QUES_ICON));
					if(result == JOptionPane.YES_OPTION){
						restoreCert();
					}
				}else{
					restoreCert();
				}
			}

			private final void restoreCert() {
				final File restoreFile = chooseFile(true);
				if(restoreFile != null){
					final JPasswordField field = new JPasswordField("", Designer.COLUMNS_PWD_DEV_CERT);

					final JPanel totalPanel = Designer.buildInputCertPwdPanel(field, false);
					
					final ActionListener listener = new ActionListener() {
						@Override
						public void actionPerformed(final ActionEvent e) {
							final String pwd = new String(field.getPassword());
							try{
								final SignItem[] r = SignHelper.getContentformPfx(restoreFile, pwd);
								
								//restore
								ResourceUtil.copy(restoreFile, SecurityDataProtector.getDevCertFile());
								
								if(PropertiesManager.getValue(PropertiesManager.p_DevCertPassword) != null){
									PropertiesManager.setValue(PropertiesManager.p_DevCertPassword, pwd);
								}
								PropertiesManager.saveFile();
								
								CertPanel.this.items.clear();
								for(int i = 0; i<r.length; i++){
									CertPanel.this.items.add(r[i]);
								}
								
								refreshTable();
								saveBut.setEnabled(false);
							}catch (final Throwable e1) {
								App.showMessageDialog(dialog, e1.getMessage(), ResourceUtil.getErrorI18N(), App.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
							}
						}
					};

					App.showCenterPanelMain(totalPanel, 0, 0, (String)ResourceUtil.get(1007), false, null, null, listener, null, dialog, true, false, null, false, false);
				}
			}
		}, threadPoolToken));
		
		descBut = new JButton((String)ResourceUtil.get(9095) , new ImageIcon(ImageSrc.loadImageFromPath(ImageSrc.HC_CERT_22_PATH)));
//		descBut.setToolTipText((String)ResourceUtil.get(9143));
		
		addBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				if(items.size() >= 1){
					final int result = App.showConfirmDialog(dialog, "one certificate is sufficient usually, are you continue?", "continue?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, App.getSysIcon(App.SYS_WARN_ICON));
					if(result == JOptionPane.YES_OPTION){
						add();
					}
				}else{
					add();
				}
			}
		}, threadPoolToken));
		
		saveBut.setEnabled(false);
		saveBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				saveBeforeCheckPwd(null);
			}
		}, threadPoolToken));
		
		changePwdBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final ActionListener listern = new HCActionListener(new Runnable() {
					@Override
					public void run() {
						App.showMessageDialog(dialog, "<html>it will be effective after click <STRONG>[" + saveButtonText + "]</STRONG>.</html>");
					}
				}, threadPoolToken);
				showInputCertPwd(listern);
			}
		}, threadPoolToken));
		
		exitBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				if(saveBut.isEnabled()){
					final int result = App.showConfirmDialog(dialog, "certificate has been modified, save changes?", "save changes?", 
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, App.getSysIcon(App.SYS_QUES_ICON));
					if(result == JOptionPane.YES_OPTION){
						final ActionListener disposeAction = new ActionListener() {
							@Override
							public void actionPerformed(final ActionEvent e) {
								dialog.dispose();
							}
						};
						saveBeforeCheckPwd(disposeAction);
					}else if(result == JOptionPane.NO_OPTION){
						dialog.dispose();
					}else{
					}
				}else{
					dialog.dispose();
				}
			}
		}, threadPoolToken));
		
		
		importBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
//				注意：
//				必须是X509Certificate证书，参见{@link SignHelper#verifyJar(File, X509Certificate[])}
				
				final JButton ok = App.buildDefaultOKButton();
				
				JLabel lb_public_key, lb_private_key, lb_chain;
				JButton btn_public_key, btn_private_key, btn_chain;
				final SignItem signItem = new SignItem();
				signItem.alias = "testAlias";
				
				lb_public_key = new JLabel("public key :");
				lb_private_key = new JLabel("private key :");
				lb_chain = new JLabel("chain :");
				
				final JTextField tf_public_key = new JTextField(15);
				final JTextField tf_private_key = new JTextField(15);
				final JTextField tf_chain = new JTextField(15);
				
				final String btnText = "choose...";
				btn_public_key = new JButton(btnText);
				btn_private_key = new JButton(btnText);
				btn_chain = new JButton(btnText);
				
				final JPanel listPane = new JPanel(new GridLayout(3, 2));
				{
					listPane.add(lb_public_key);
					final JPanel flowPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
					listPane.add(flowPane);
					flowPane.add(tf_public_key);
					flowPane.add(btn_public_key);
					btn_public_key.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final File file = chooseFile(true);
							if(file != null){
//								 final CertificateFactory certificatefactory=CertificateFactory.getInstance("X.509");
//								 final FileInputStream bais=new FileInputStream(files[0]);
//								 final X509Certificate Cert = (X509Certificate)certificatefactory.generateCertificate(bais);
//								 pk = Cert.getPublicKey();
								tf_public_key.setText(file.getName());
								checkFileAll(signItem, ok);
							}
						}
					}, threadPoolToken));
				}
				{
					listPane.add(lb_private_key);
					final JPanel flowPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
					listPane.add(flowPane);
					flowPane.add(tf_private_key);
					flowPane.add(btn_private_key);
					btn_private_key.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final File file = chooseFile(true);
							if(file != null){
								try{
									signItem.privateKey = SignHelper.loadPrivateKey(file);
								}catch (final Throwable e) {
									showException(e);
									return;
								}
								tf_private_key.setText(file.getName());
								checkFileAll(signItem, ok);
							}
						}
					}, threadPoolToken));
				}
				{
					listPane.add(lb_chain);
					final JPanel flowPane = new JPanel(new FlowLayout(FlowLayout.LEADING));
					listPane.add(flowPane);
					flowPane.add(tf_chain);
					flowPane.add(btn_chain);
					btn_chain.addActionListener(new HCActionListener(new Runnable() {
						@Override
						public void run() {
							final File file = chooseFile(true);
							if(file != null){
								FileInputStream fis = null;
								try{
									fis = new FileInputStream(file);
									final CertificateFactory cf=CertificateFactory.getInstance("X.509"); 
									signItem.chain = (X509Certificate)cf.generateCertificate(fis);
								}catch (final Exception e) {
									showException(e);
									return;
								}finally{
									try{
										fis.close();
									}catch (final Throwable e) {
									}
								}
								tf_chain.setText(file.getName());
								checkFileAll(signItem, ok);
							}
						}
					}, threadPoolToken));
				}
				
				final ActionListener listener = new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						try{
							items.add(signItem);
							if(items.size() == 1){
					    		tablePanel.table.getSelectionModel().setSelectionInterval(0, 0);
					    		refreshButtons();
							}
							notifyNeedToSave();
						}catch (final Exception e1) {
							e1.printStackTrace();
							showException(e1);
						}
					}
				};
				
				App.showCenterPanelMain(listPane, 0, 0, "import private key and chain from CA", true, null, null, listener, null, dialog, true, false, null, false, false);
			}
		}, threadPoolToken));
		
		removeBut.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				removeCurrAlias();
				notifyNeedToSave();
			}
		}, threadPoolToken));

		final AbstractTableModel tableModel = new AbstractTableModel() {
			@Override
			public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
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
				if(rowIndex >= getRowCount()){
					return null;
				}

				if(columnIndex == COL_ALIAS){
					return items.get(rowIndex).alias;
				}else if(columnIndex == COL_EXPIRES){
					return items.get(rowIndex).chain.getNotAfter().toLocaleString();
				}
				
				return null;
			}
			
			@Override
			public final int getRowCount() {
				return items.size();
			}
			
			@Override
			public String getColumnName(final int columnIndex) {
				return colNames[columnIndex].toString();
			}
			
			@Override
			public int getColumnCount() {
				return colNames.length;
			}
			
			@Override
			public Class<?> getColumnClass(final int columnIndex) {
                return String.class;
			}
			
			@Override
			public void addTableModelListener(final TableModelListener l) {
			}
		};
		
		final JButton virtualImport = new JButton("");//由于HCTablePanel内带更新逻辑，所以做一个虚的。
		final JButton virtualRemove = new JButton("");//由于HCTablePanel内带更新逻辑，所以做一个虚的。
		final JButton virtualUp = new JButton("");//由于HCTablePanel内带更新逻辑，所以做一个虚的。
		final JButton virtualDown = new JButton("");//由于HCTablePanel内带更新逻辑，所以做一个虚的。
		final JButton virtualEdit = new JButton("");//由于HCTablePanel内带更新逻辑，所以做一个虚的。
		
		tablePanel = new HCTablePanel(tableModel, null, colNames, items.size(), 
				virtualUp, virtualDown, virtualRemove, virtualImport, virtualEdit,
				//upOrDownMovingBiz
				new AbstractDelayBiz(null){
					@Override
					public final void doBiz() {
					}},
				//Remove
				new AbstractDelayBiz(null) {
					@Override
					public final void doBiz() {
					}
				}, 
				//import
				new AbstractDelayBiz(null) {
					@Override
					public final void doBiz() {
					}
				}, true, COL_NUM);
		
//		final DefaultTableCellRenderer centerCellRender = new DefaultTableCellRenderer(){
//	        @Override
//			public Component getTableCellRendererComponent(
//	                final JTable table, final Object value, final boolean isSelected,
//	                final boolean hasFocus, final int row, final int column) {
//	        	setHorizontalAlignment(CENTER);
//		        return super.getTableCellRendererComponent(table, value,
//                        isSelected, hasFocus, row, column);
//			}
//        };
//    	tablePanel.table.getColumnModel().getColumn(COL_ALIAS).setCellRenderer(centerCellRender);
//        tablePanel.table.getColumnModel().getColumn(COL_EXPIRES).setCellRenderer(centerCellRender);
        
        final ListSelectionModel selectModel = tablePanel.table.getSelectionModel();
        selectModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listSelectListener = new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final int selectedRow = tablePanel.table.getSelectedRow();
				final boolean hasRow = selectedRow >= 0;
				notifyHasRow(hasRow);
			}
		};
		selectModel.addListSelectionListener(listSelectListener);
    	
		notifyHasRow(items.size() > 0);

        initTable(tablePanel.table);
		
		descBut.addActionListener(new HCActionListener(new Runnable() {
			private final void append(final StringBuilder sb, final String item, final String value){
				if(sb.length() > 0){
					sb.append("<BR>");
					sb.append("<BR>");
				}
				
				sb.append("<STRONG>");
				sb.append(item);
				sb.append("</STRONG>");
				sb.append(" : ");
				sb.append(value);
			}
			
			@Override
			public void run() {
				final X509Certificate cert = items.get(tablePanel.table.getSelectedRow()).chain;
				final StringBuilder sb = StringBuilderCacher.getFree();
				
//				append(sb, "Issuer", cert.getIssuerX500Principal().toString());
				append(sb, "Subject", cert.getSubjectX500Principal().toString());
				append(sb, "NotBefore", cert.getNotBefore().toLocaleString());
				append(sb, "NotAfter", cert.getNotAfter().toLocaleString());
				append(sb, "SigAlgName", cert.getSigAlgName());
				
				final JPanel panel = new JPanel(new BorderLayout());
				panel.setBorder(new TitledBorder((String)ResourceUtil.get(9095)));
				final String sbStr = sb.toString();
				StringBuilderCacher.cycle(sb);
				
				panel.add(new JLabel("<html>" + sbStr + "</html>"));
				final ActionListener listener = null;
				App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(9095), false, null, null, listener, null, dialog, true, false, null, false, false);
			}
		}, threadPoolToken));
		final JToolBar buttonsList = new JToolBar();
		buttonsList.setLayout(new GridLayout(1, 0, ClientDesc.hgap, ClientDesc.vgap));
		buttonsList.add(addBut);
//		buttonsList.add(importBut);
//		buttonsList.add(removeBut);
		buttonsList.add(descBut);
		buttonsList.add(changePwdBut);
		buttonsList.addSeparator();
		buttonsList.add(restoreBut);
		buttonsList.add(backupBut);
		buttonsList.addSeparator();
		buttonsList.add(saveBut);
		buttonsList.add(exitBut);

		final JScrollPane scrollpane = new JScrollPane(tablePanel.table);
		scrollpane.setPreferredSize(new Dimension(350, 200));
		contentPane.add(scrollpane, BorderLayout.CENTER);
		contentPane.add(buttonsList, BorderLayout.NORTH);
	}

	/**
	 * 设置新密码或更改密码之用，不是输入密码之用
	 * @param extAction
	 */
	private final void showInputCertPwd(final ActionListener extAction){
		final String title_pwd = (String)ResourceUtil.get(1007);

		final JPanel panel1 = new JPanel();
		final JPasswordField field1 = new JPasswordField("", Designer.COLUMNS_PWD_DEV_CERT);
		final JPanel panel2 = new JPanel();
		final JPasswordField field2 = new JPasswordField("", Designer.COLUMNS_PWD_DEV_CERT);
		
		{
			panel1.setLayout(new BorderLayout());
	
			final JLabel jlPassword = new JLabel();
			jlPassword.setIcon(new ImageIcon(ImageSrc.PASSWORD_ICON));
			
			field1.setEchoChar('*');
			field1.enableInputMethods(true);
			field1.setHorizontalAlignment(SwingUtilities.RIGHT);
			field1.requestFocus();
			
			panel1.add(jlPassword, BorderLayout.WEST);
			panel1.add(field1, BorderLayout.CENTER);
		}
		
		{
			panel2.setLayout(new BorderLayout());
	
			final JLabel jlPassword = new JLabel();
			jlPassword.setIcon(new ImageIcon(ImageSrc.PASSWORD_ICON));
			
			field2.setEchoChar('*');
			field2.enableInputMethods(true);
			field2.setHorizontalAlignment(SwingUtilities.RIGHT);
			field2.requestFocus();
			
			panel2.add(jlPassword, BorderLayout.WEST);
			panel2.add(field2, BorderLayout.CENTER);
		}
		
		final JPanel descPanel = new JPanel(new BorderLayout());
		descPanel.setBorder(new TitledBorder((String)ResourceUtil.get(9095)));
		descPanel.add(new JLabel("<html>" +
				Designer.DESC_PASSWORD_OF_DEVELOPER_CERTIFICATE + "</html>"));
		
		final JPanel totalPanel = new JPanel(new BorderLayout());
		totalPanel.setBorder(new TitledBorder(title_pwd));
		
		final JPanel pwdPanel = new JPanel(new GridLayout(2, 1));
		pwdPanel.add(panel1);
		pwdPanel.add(panel2);
		
		totalPanel.add(pwdPanel, BorderLayout.NORTH);
		totalPanel.add(descPanel, BorderLayout.SOUTH);
		
		final ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if ((StringUtil.getBytes(field1.getText()).length >= App.MIN_PWD_LEN)
						&& field2.getText().equals(field1.getText())) {
					password = new String(field1.getPassword());
					notifyNeedToSave();
					if(extAction != null){
						extAction.actionPerformed(null);
					}
				} else {
					JOptionPane.showMessageDialog(dialog,
							StringUtil.replace((String)ResourceUtil.get(9077), "{min}", "" + App.MIN_PWD_LEN),//含密码不一致
							(String)ResourceUtil.get(9076), JOptionPane.ERROR_MESSAGE);
					showInputCertPwd(extAction);
				}
			}
		};
		
		App.showCenterPanelMain(totalPanel, 0, 0, title_pwd, false, null, null, listener, null, dialog, true, false, null, false, false);
	}
	
	public final JFrame toShow(){
		final ActionListener quitAction = new HCActionListener(new Runnable() {
			@Override
			public void run() {
				dialog.dispose();
			}
		}, threadPoolToken);

		if(dialog instanceof ClosableWindow){
			((ClosableWindow)dialog).setCloseAction(quitAction);
		}
		
		JRootPane rootPane = null;
			rootPane = dialog.getRootPane();
		if(rootPane != null){
			rootPane.registerKeyboardAction(quitAction,
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);
		}
		
		dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				quitAction.actionPerformed(null);
			}
		});
		
		dialog.pack();
		App.showCenter(dialog);
		return dialog;
	}

	private final void notifyNeedToSave(){
		saveBut.setEnabled(true);
		refreshTable();
	}

	private final void refreshTable() {
		tablePanel.table.updateUI();//repaint();
	}
	
	private void save() {
		try {
			SignHelper.savePfx(SecurityDataProtector.getDevCertFile(), password, items);
		} catch (final Exception e) {
			e.printStackTrace();
			showException(e);
		}
	}

	private final void showException(final Throwable e) {
		App.showMessageDialog(dialog, e.getMessage(), ResourceUtil.getErrorI18N(), App.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
	}

	public void notifyHasRow(final boolean hasRow) {
		removeBut.setEnabled(hasRow);
		descBut.setEnabled(hasRow);
	}

	private final void add() {
		final JPanel panel = new JPanel(new GridLayout(0, 2, ClientDesc.hgap, ClientDesc.vgap));
		final JTextField f_CN, f_OU, f_O, f_L, f_S, f_C, f_Alias;
		final SpinnerModel model = new SpinnerNumberModel(100, 1, 200, 1);
		final JSpinner yearSpinner = new JSpinner(model);
		final JSpinner.NumberEditor editor = new JSpinner.NumberEditor(yearSpinner, "0");
		yearSpinner.setEditor(editor);
		
		final int columns = Designer.COLUMNS_PWD_DEV_CERT;
		
		f_Alias = new JTextField("item1", columns);
//		panel.add(new JLabel(CertListPanel.COL_NAME_ALIAS));//it is not required to input alias name.
//		panel.add(f_Alias);
		
		panel.add(new JLabel(CertListPanel.COL_NAME_EXPIRES + " (" + (String)ResourceUtil.get(9226) + ")"));
		panel.add(yearSpinner);
		
		panel.add(new JLabel((String)ResourceUtil.get(9213)));
		f_CN = new JTextField("", columns);
		panel.add(f_CN);
		
		panel.add(new JLabel((String)ResourceUtil.get(9214)));
		f_OU = new JTextField("", columns);
		panel.add(f_OU);
		
		panel.add(new JLabel((String)ResourceUtil.get(9215)));
		f_O = new JTextField("", columns);
		panel.add(f_O);
		
		panel.add(new JLabel((String)ResourceUtil.get(9216)));
		f_L = new JTextField("", columns);
		panel.add(f_L);
		
		panel.add(new JLabel((String)ResourceUtil.get(9217)));
		f_S = new JTextField("", columns);
		panel.add(f_S);
		
		panel.add(new JLabel((String)ResourceUtil.get(9218)));
		f_C = new JTextField("", columns);
		panel.add(f_C);
		
		final ActionListener listener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final int year = Integer.parseInt(yearSpinner.getValue().toString());
				if(year < 20){
					final int result = App.showConfirmDialog(dialog, "the year of expires is too small, are you continue?", "continue?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, App.getSysIcon(App.SYS_WARN_ICON));
					if(result == JOptionPane.YES_OPTION){
						addCertificate(year);
					}
				}else{
					addCertificate(year);
				}
			}
			
			private final void addCertificate(final int year){
				ProcessingWindowManager.showCenterMessage("creating developer certificate...");
				
				final String x500Name = 
						"CN=" + format(f_CN.getText()) + 
						", OU=" + format(f_OU.getText()) + 
						", O=" + format(f_O.getText()) + 
						", L=" + format(f_L.getText()) + 
						", ST=" + format(f_S.getText()) + 
						", C=" + format(f_C.getText());
				
				final Date notBefore = new Date();  
			    final Calendar notAfterCal = Calendar.getInstance();
			    notAfterCal.setTime(notBefore);
			    notAfterCal.add(Calendar.YEAR, year);
			    final Date notAfter = notAfterCal.getTime();
			    try{
			    	final SignItem item = SignHelper.generateKeys(x500Name, f_Alias.getText(), notBefore, notAfter);
			    	items.add(item);
			    	if(items.size() == 1){
			    		tablePanel.table.getSelectionModel().setSelectionInterval(0, 0);
			    		refreshButtons();
			    	}
			    	notifyNeedToSave();
			    }catch (final Throwable e1) {
			    	showException(e1);
			    }finally{
			    	ProcessingWindowManager.disposeProcessingWindow();
			    }
			}
			
			private String format(final String src){
				return StringUtil.replace(src, ",", "\\,");
			}
		};
		
		App.showCenterPanelMain(panel, 0, 0, (String)ResourceUtil.get(9220), true, null, null, listener, null, dialog, true, false, null, false, false);
	}

	private final void savePfx() {
		try {
			SignHelper.savePfx(SecurityDataProtector.getDevCertFile(), password, items);
			saveBut.setEnabled(false);
		} catch (final Exception e1) {
			e1.printStackTrace();
			App.showMessageDialog(dialog, e1.getMessage(), ResourceUtil.getErrorI18N(), App.ERROR_MESSAGE, App.getSysIcon(App.SYS_ERROR_ICON));
		}
	}

	private final void saveBeforeCheckPwd(final ActionListener extAction) {
		if(password == null){
			final ActionListener savePfxAction = new HCActionListener(new Runnable() {
				@Override
				public void run() {
					savePfx();
					if(extAction != null){
						extAction.actionPerformed(null);
					}
				}
			}, threadPoolToken);
			showInputCertPwd(savePfxAction);
		}else{
			savePfx();
			if(extAction != null){
				extAction.actionPerformed(null);
			}
		}
	}

//	public static void main(final String[] args) throws Exception{
//		TestHelper.initForTester();
//		
//		final CertPanel linkp = new CertPanel(null, null, null);
//		linkp.toShow();
//	}

}