package hc.server.ui.design.hpj;

import hc.App;
import hc.core.IContext;
import hc.server.ui.design.Designer;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ProjectNodeEditPanel extends NameEditPanel {
	final JPanel idPanel = new JPanel();
	final JTextField idField = new JTextField();
	final VerTextPanel verPanel = new VerTextPanel("project");
	final JTextField urlField = new JTextField();
	
	final JTextField contact = new JTextField();
	final JTextField copyright = new JTextField();
	final JTextField desc = new JTextField();
	final JTextField license = new JTextField();
	
	public ProjectNodeEditPanel(){
		super();
		
		idPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		final JLabel idLabel = new JLabel("ID : ");
		final JLabel tipLabel = new JLabel("<html>it is used to install and upgrade to identify this project to different from other." +
				"<BR>'root' is system reserved ID." +
				"<BR>valid char : 0-9, a-z, A-Z, _</html>");
//		tipLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		idPanel.add(idLabel);
		idField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				char keyCh = e.getKeyChar();
		        if ((keyCh >= '0' && keyCh <= '9') 
		        		|| (keyCh >= 'a' && keyCh <= 'z') 
		        		|| (keyCh >= 'A' && keyCh <= 'Z') 
		        		|| keyCh == '_'){
		        }else{
		        	e.setKeyChar('\0');
		        }
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
			}
		});	
		idField.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				((HPProject)item).id = idField.getText();
				notifyModified();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				modify();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				modify();
			}
		});
		idField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				((HPProject)item).id = idField.getText();
				tree.updateUI();
				item.getContext().modified.setModified(true);
			}
		});
		idField.setColumns(20);
		idPanel.add(idField);
		
		JPanel center = new JPanel();
		center.setLayout(new FlowLayout(FlowLayout.LEADING));
		{
			JPanel compose = new JPanel(new GridLayout(2, 1));
			compose.add(idPanel);
			compose.add(tipLabel);
			
			compose.setBorder(new TitledBorder("Project ID"));
			
			{
				JPanel composeAndVer = new JPanel(new GridBagLayout());
				GridBagConstraints c = new GridBagConstraints(0, 0, 1, 1, 0, 0,
						GridBagConstraints.WEST, GridBagConstraints.NONE,
						new Insets(0, 0, 0, 0), 0, 0);
				
				composeAndVer.add(compose, c);
				c.gridy = 1;
				composeAndVer.add(verPanel, c);
				JPanel upgradePanel = new JPanel(new BorderLayout());
				{
					JPanel urlPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
					urlField.setColumns(30);
					urlField.getDocument().addDocumentListener(new DocumentListener() {
						private void modify(){
							((HPProject)item).upgradeURL = urlField.getText();
							notifyModified();
						}
						@Override
						public void removeUpdate(DocumentEvent e) {
							modify();
						}
						
						@Override
						public void insertUpdate(DocumentEvent e) {
							modify();
						}
						
						@Override
						public void changedUpdate(DocumentEvent e) {
							modify();
						}
					});
					urlPanel.add(new JLabel("URL : "));
					urlPanel.add(urlField);
					JButton test = new JButton("Test HAD");
					test.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							Properties had = new Properties();
							try{
								had.load(new URL(urlField.getText()).openStream());
								
								JPanel jpanel = new JPanel(new BorderLayout());
								final StringBuffer sb = new StringBuffer();
								sb.append("<html>");
								final Enumeration<Object> en = had.keys();
								boolean isHead = true;
								
								while(en.hasMoreElements()){
									final Object key = en.nextElement();
									if(isHead){
										isHead = false;
									}else{
										sb.append("<br>");
									}
									sb.append("<strong>" + key + "</strong>: " + had.getProperty((String)key));
								}
								sb.append("</html>");
								
								jpanel.add(new JLabel(sb.toString(), App.getSysIcon(App.SYS_INFO_ICON), SwingConstants.LEADING), 
										BorderLayout.CENTER);
								App.showCenterPanel(jpanel, 0, 0, (String) ResourceUtil.get(IContext.INFO), 
										false, null, null, null, null, Designer.getInstance(), true, false, null, false, false);
							}catch (Exception ex) {
								JPanel jpanel = new JPanel(new BorderLayout());
								jpanel.add(new JLabel("fail to connect server file.", App.getSysIcon(App.SYS_ERROR_ICON), SwingConstants.LEADING), 
										BorderLayout.CENTER);
								App.showCenterPanel(jpanel, 0, 0, (String) ResourceUtil.get(IContext.ERROR), 
										false, null, null, null, null, Designer.getInstance(), true, false, null, false, false);
							}
						}
					});
					urlPanel.add(test);
					
					upgradePanel.add(urlPanel, BorderLayout.NORTH);
					upgradePanel.add(new JLabel("<html>the auto upgrade URL of new version of current project. if no upgrade, please keep blank." +
							"<br><br>for example, <strong>http://example.com/dir_or_virtual/tv.had</strong> , <strong>NOTE:</strong> it is <strong>had</strong> file, not <strong>har</strong> file." +
							"<br>please put both <strong>tv.har</strong>, <strong>tv.had</strong> in directory <strong>dir_or_virtual</strong> for download." +
							"<br><br><strong>had</strong> file provides version information which is used to determine upgrade or not." +
							"<br>click <strong>Save as</strong> button, <strong>had</strong> file is auto created with <strong>har</strong> file.</html>"),
							BorderLayout.CENTER);
					upgradePanel.setBorder(new TitledBorder("Upgrade URL"));
				}
				c.gridy = 2;
				composeAndVer.add(upgradePanel, c);
				
				c.gridy = 3;
				desc.setColumns(30);
				desc.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).desc = desc.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(desc, "Description : ", 30), c);
				c.gridy = 4;
				license.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).license = license.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(license, "Text license URL for current project : ", 30), c);
				c.gridy = 5;
				contact.setColumns(30);
				contact.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).contact = contact.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(contact, "Contact : ", 30), c);
				c.gridy = 6;
				copyright.setColumns(30);
				copyright.getDocument().addDocumentListener(new ModifyDocumentListener() {
					@Override
					public void modify() {
						((HPProject)item).copyright = copyright.getText();
						notifyModified();
					}
				});
				composeAndVer.add(buildItemPanel(copyright, "Copyright : ", 30), c);
				
				final VerTextField verField = verPanel.verTextField;
				verField.getDocument().addDocumentListener(new DocumentListener() {
					private void modify(){
						((HPProject)item).ver = verField.getText();
						notifyModified();
					}
					@Override
					public void removeUpdate(DocumentEvent e) {
						modify();
					}
					
					@Override
					public void insertUpdate(DocumentEvent e) {
						modify();
					}
					
					@Override
					public void changedUpdate(DocumentEvent e) {
						modify();
					}
				});
				verField.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						((HPProject)item).ver = verField.getText();
						tree.updateUI();
						item.getContext().modified.setModified(true);
					}
				});

				center.add(composeAndVer);
			}
		}
		
		add(new JScrollPane(center), BorderLayout.CENTER);		
	}

	@Override
	public void extendInit(){
		idField.setText(((HPProject)item).id);
		verPanel.verTextField.setText(((HPProject)item).ver);
		urlField.setText(((HPProject)item).upgradeURL);
		license.setText(((HPProject)item).license);
		desc.setText(((HPProject)item).desc);
		copyright.setText(((HPProject)item).copyright);
		contact.setText(((HPProject)item).contact);
	}
	
	private JPanel buildItemPanel(final JTextField field, final String label, final int colNum){
		field.setColumns(colNum);
		
		JPanel tmpPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		tmpPanel.setBorder(new TitledBorder(label));
//		tmpPanel.add(new JLabel(label));
		tmpPanel.add(field);
		
		return tmpPanel;
	}
}
