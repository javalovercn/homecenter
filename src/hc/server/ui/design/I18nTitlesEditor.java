package hc.server.ui.design;

import hc.App;
import hc.res.ImageSrc;
import hc.server.HCActionListener;
import hc.server.ui.design.hpj.BaseMenuItemNodeEditPanel;
import hc.util.ResourceUtil;
import hc.util.StoreableHashMapWithModifyFlag;

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
import javax.swing.JComboBox;
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

	private static final Vector<String> defaultLang = buildLangVector();
	private static Vector<String> buildLangVector(){
		final Vector<String> v = new Vector<String>(5);
		v.add("ar");
		v.add("da");
		v.add("de");
		v.add("en");
		v.add("es");
		v.add("fr");
		v.add("hi");
		v.add("it");
		v.add("ja");
		v.add("ko");
		v.add("pt");
		v.add("ru");
		v.add("th");
		v.add("zh");
		return v;
	}
	private static final Vector<String> defaultCountry = buildCountryVector();
	private static Vector<String> buildCountryVector(){
		final Vector<String> v = new Vector<String>(5);
		v.add("");
		v.add("AE");
		v.add("CA");
		v.add("CN");
		v.add("DE");
		v.add("ES");
		v.add("EU");
		v.add("FR");
		v.add("GB");
		v.add("IN");
		v.add("IT");
		v.add("JP");
		v.add("KR");
		v.add("RU");
		v.add("US");
		return v;
	}
	
	private final StoreableHashMapWithModifyFlag map;
	private final JComboBox jcbLanguage;//JRE 6 is NOT <STRING> allowed
	private final JComboBox jcbCountry;
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
	
	public I18nTitlesEditor(final StoreableHashMapWithModifyFlag map){
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
		
		jcbLanguage = new JComboBox(defaultLang);
		jcbCountry = new JComboBox(defaultCountry);
		
		jcbLanguage.setMaximumRowCount(10);
		jcbCountry.setMaximumRowCount(10);
		
		jcbLanguage.setEditable(true);
		jcbCountry.setEditable(true);
		
		final JPanel editPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		editPanel.add(new JLabel(LANG + " :"));
		editPanel.add(jcbLanguage);
		editPanel.add(new JLabel(COUNTRY + " :"));
		editPanel.add(jcbCountry);
		editPanel.add(new JLabel(VALUE + " :"));
		valueField = new JTextField("", 10);
		editPanel.add(valueField);
		addBtn = new JButton((String)ResourceUtil.get(9016) + "/" + (String)ResourceUtil.get(9017), new ImageIcon(ImageSrc.ADD_SMALL_ICON));
		editPanel.add(addBtn);
		
		addBtn.addActionListener(new HCActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final String lang = (String)jcbLanguage.getSelectedItem();
				if(lang.length() < 2 || lang.length() > 3){
					App.showMessageDialog(Designer.getInstance(), "language must be two or three letters!", "Error", App.ERROR_MESSAGE);
					return;
				}
				
				final String country = (String)jcbCountry.getSelectedItem();
				final int country_len = country.length();
				if((country_len != 0 && country_len < 2) || country_len > 2){
					boolean isNumeric = true;
					try{
						Integer.parseInt(country);
					}catch (final Exception ex) {
						isNumeric = false;
					}
					if(isNumeric && country.length() == 3){//a UN M.49 numeric-3 area code, es-419
					}else{
						App.showMessageDialog(Designer.getInstance(), "country / region must be two letters or UN M.49 numeric-3 area code!", "Error", App.ERROR_MESSAGE);
						return;
					}
				}
				
				final String valueText = valueField.getText();
				if(valueText.length() == 0){
					App.showMessageDialog(Designer.getInstance(), "value can NOT be empty!", "Error", App.ERROR_MESSAGE);
					return;
				}
				
				final String key = lang.toLowerCase() + ((country.length() == 0)?"":(SPLIT_LANG_COUNTRY + country.toUpperCase()));
				addKeyValuePare(key, valueText);
				refreshTableUI();
			}
		});
		
		removeBtn = new JButton((String)ResourceUtil.get(9018), new ImageIcon(ImageSrc.REMOVE_SMALL_ICON));
		removeBtn.setEnabled(false);
		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final int selectedIdx = table.getSelectedRow();
				if(selectedIdx >= 0 && selectedIdx < keyVector.size()){
					keyVector.remove(selectedIdx);
					valueVector.remove(selectedIdx);
					
					refreshTableUI();
				}
			}
		});
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
					final int splitIdx = key.indexOf(SPLIT_LANG_COUNTRY);
					if(splitIdx > 0){
						jcbLanguage.setSelectedItem(key.substring(0, splitIdx));
						jcbCountry.setSelectedItem(key.substring(splitIdx + 1));
					}else{
						jcbLanguage.setSelectedItem(key);
						jcbCountry.setSelectedItem("");
					}
					
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
				"1. <STRONG>en" + SPLIT_LANG_COUNTRY + "US</STRONG> is for United States only.<BR>" +
				"2. <STRONG>en</STRONG> is for all english language country, if <STRONG>en"+ SPLIT_LANG_COUNTRY + "??</STRONG> is NOT found.<BR>" +
				"3. <STRONG>language</STRONG> : ISO 639 alpha-2 or alpha-3 codes,<BR>" +
				"4. <STRONG>country / region</STRONG> : ISO 3166 alpha-2 or a UN M.49 numeric-3 area code, <BR>" +
				"5. input in ComboBox directly if it is NOT listed in ComboBox." +
				"</html>"));

		this.setLayout(new BorderLayout());
		this.add(editPanel, BorderLayout.NORTH);
		this.add(listPanel, BorderLayout.CENTER);
		this.add(descPanel, BorderLayout.SOUTH);
		
		listPanel.setPreferredSize(new Dimension(200, 200));
	}

	private final void addKeyValuePare(final String key, final String value) {
		final int size = keyVector.size();
		int i = 0;
		for (; i < size; i++) {
			final String k1 = keyVector.get(i);
			final int result = k1.compareTo(key);
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

	public final static void showEditor(final StoreableHashMapWithModifyFlag map, final ActionListener listener, final Component relativeTo, final JFrame parent){
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
