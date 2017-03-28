package hc.server.ui.design;

import hc.App;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.server.ui.design.hpj.BaseMenuItemNodeEditPanel;
import hc.util.I18NStoreableHashMapWithModifyFlag;
import hc.util.ResourceUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

public class I18nTitlesEditor extends JPanel{
	private final String LANG = "language";
	private final String COUNTRY = "country / region";
	private final String VALUE = "value";
	private final String SPLIT_LANG_COUNTRY = "-";

	private final I18NStoreableHashMapWithModifyFlag map;
	private final JTextField langRegion;
	private final JButton addBtn, removeBtn;
	final JTable table;
	final Vector<String> keyVector, valueVector;
	final JTextField valueField;
	final Runnable refreshTableRunnable;
	
	public final void updateMap(){
		map.clear();//有可能删除
		final int size = keyVector.size();
		for (int i = 0; i < size; i++) {
			map.put(keyVector.elementAt(i), valueVector.elementAt(i));
		}
	}
	
	public I18nTitlesEditor(final I18NStoreableHashMapWithModifyFlag map){
		keyVector = new Vector<String>();
		valueVector = new Vector<String>();
		
		refreshTableRunnable = new Runnable() {
			@Override
			public void run() {
				final int lastSelected = table.getSelectedRow();
				((AbstractTableModel)table.getModel()).fireTableDataChanged();
				if(lastSelected >= 0 && lastSelected < keyVector.size()){
					table.setRowSelectionInterval(lastSelected, lastSelected);
				}
			}
		};
		
		this.map = map;
		//Language : ISO 639-1
		//Country (region) : ISO 3166-1 alpha-2 codes country code, The country (region) field is case insensitive, but Locale always canonicalizes to upper case.
		final int textFieldColumnNum = 10;
		langRegion = new JTextField(textFieldColumnNum);
		
		final JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		editPanel.add(new JLabel(LANG + " " + SPLIT_LANG_COUNTRY + " " + COUNTRY));
		editPanel.add(langRegion);
		editPanel.add(new JLabel(VALUE + " :"));
		valueField = new JTextField("", textFieldColumnNum);
		editPanel.add(valueField);
		addBtn = new JButton((String)ResourceUtil.get(9016) + "/" + (String)ResourceUtil.get(9017), new ImageIcon(ImageSrc.ADD_SMALL_ICON));
		editPanel.add(addBtn);
		
		addBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final String lang = langRegion.getText();
				if(lang.length() < 2){
					App.showMessageDialog(Designer.getInstance(), "language must be two or more letters!", "Error", App.ERROR_MESSAGE);
					return;
				}
				
				final String valueText = valueField.getText();
				if(valueText.length() == 0){
					App.showMessageDialog(Designer.getInstance(), "value can NOT be empty!", "Error", App.ERROR_MESSAGE);
					return;
				}
				
				addKeyValuePare(lang, valueText);
				refreshTableUI();
			}
		}));
		
		removeBtn = new JButton((String)ResourceUtil.get(9018), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
		removeBtn.setEnabled(false);
		removeBtn.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				final int selectedIdx = table.getSelectedRow();
				if(selectedIdx >= 0 && selectedIdx < keyVector.size()){
					keyVector.remove(selectedIdx);
					valueVector.remove(selectedIdx);
					
					refreshTableUI();
				}
			}
		}));
		editPanel.setBorder(new TitledBorder("Edit :"));
		
		final JPanel listPanel = new JPanel(new BorderLayout());
		
		table = new JTable();
		{
			final Enumeration enu = map.keys();
			while(enu.hasMoreElements()){
				final String key = (String)enu.nextElement();
				final String value = (String)map.get(key);
				
				addKeyValuePare(key, value);
			}
		}
		table.setModel(new AbstractTableModel() {
			@Override
			public final String getColumnName(final int columnIndex) {
				if(columnIndex == 0){
					return LANG + "(" + SPLIT_LANG_COUNTRY + COUNTRY + ")";
				}else if(columnIndex == 1){
					return VALUE;
				}else{
					return "";
				}
			}

			@Override
			public final int getRowCount() {
				return keyVector.size();
			}

			@Override
			public final int getColumnCount() {
				return 2;
			}

			@Override
			public final Object getValueAt(final int rowIndex, final int columnIndex) {
				if(columnIndex == 0){
					return keyVector.get(rowIndex);
				}else if(columnIndex == 1){
					return valueVector.get(rowIndex);
				}else{
					return "";
				}
			}
		});
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final int selectedIdx = table.getSelectedRow();
				if(selectedIdx >= 0 && selectedIdx < keyVector.size()){
					removeBtn.setEnabled(true);
					final String key = keyVector.elementAt(selectedIdx);
					final String value = valueVector.elementAt(selectedIdx);
				
					langRegion.setText(key);
					valueField.setText(value);
				}else{
					removeBtn.setEnabled(false);
				}
			}
		});
		
		listPanel.setBorder(new TitledBorder("List :"));
		listPanel.add(new JScrollPane(table), BorderLayout.CENTER);
		listPanel.add(removeBtn, BorderLayout.SOUTH);
		
		final JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		descPanel.setBorder(new TitledBorder("Description :"));
		descPanel.add(new JLabel("<html>" +
				"1. the aboves are equivalent with MenuItem.<STRONG>setText(Map)</STRONG> programmatically.<BR>" +
				"2. <STRONG>en" + SPLIT_LANG_COUNTRY + "US</STRONG> is for United States only.<BR>" +
				"3. <STRONG>en</STRONG> is for all english language country, if <STRONG>en"+ SPLIT_LANG_COUNTRY + "??</STRONG> is NOT found.<BR>" +
				"4. <STRONG>language</STRONG> : ISO 639 alpha-2 or alpha-3 codes,<BR>" +
				"5. <STRONG>country / region</STRONG> : ISO 3166 alpha-2 or a UN M.49 numeric-3 area code, <BR>" +
				"6. input in ComboBox directly if it is NOT listed in ComboBox." +
				"</html>"));

		this.setLayout(new BorderLayout());
		this.add(editPanel, BorderLayout.NORTH);
		this.add(listPanel, BorderLayout.CENTER);
		this.add(descPanel, BorderLayout.SOUTH);
		
		listPanel.setPreferredSize(new Dimension(200, 200));
	}

	private final void addKeyValuePare(final String key, final String value) {
		final int size = keyVector.size();
		final String lowerCaseKey = key.toLowerCase();
		int i = 0;
		for (; i < size; i++) {
			final String k1 = keyVector.get(i);
			final int result = k1.toLowerCase().compareTo(lowerCaseKey);
			if(result == 0){
				keyVector.remove(i);
				valueVector.remove(i);
				break;
			}else if(result > 0){
				break;
			}
		}
		
		keyVector.add(i, key);
		valueVector.add(i, value);
	}
	
	private void refreshTableUI() {
		SwingUtilities.invokeLater(refreshTableRunnable);
	}

	public final static void showEditor(final I18NStoreableHashMapWithModifyFlag map, final ActionListener listener, final Component relativeTo, final JFrame parent){
		final I18nTitlesEditor editor = new I18nTitlesEditor(map);
		App.showCenterPanelMain(editor, 0, 0, "Editor for " + BaseMenuItemNodeEditPanel.I18N_BTN_TEXT, true, null, null, new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				editor.updateMap();
				listener.actionPerformed(e);
			}
		}, null, parent, true, false, relativeTo, true, false);
	}
	
}
