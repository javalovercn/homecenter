package hc.server.ui.design.hpj;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class NameEditPanel extends NodeEditPanel {
	public NameEditPanel() {
		namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		namePanel.add(new JLabel("Name : "));
		nameFiled.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				item.name = nameFiled.getText();
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
		nameFiled.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				item.name = nameFiled.getText();
				tree.updateUI();
				item.getContext().modified.setModified(true);
			}
		});
		nameFiled.setColumns(20);
		namePanel.add(nameFiled);
		
		setLayout(new BorderLayout());
		add(namePanel, BorderLayout.NORTH);
	}
	

	
	final JPanel namePanel = new JPanel();
	final JTextField nameFiled = new JTextField();
	
	HPNode item;
	
	private boolean isInited = false;
	void notifyModified(){
		if(isInited){
			item.getContext().modified.setModified(true);
		}
	}

	@Override
	public void init(MutableTreeNode data, JTree tree){
		isInited = false;
		super.init(data, tree);
		
		item = (HPNode)currNode.getUserObject();
		nameFiled.setText(item.name);
		
		extendInit();
		
		isInited = true;
	}
	
	public void extendInit(){
		
	}
}
