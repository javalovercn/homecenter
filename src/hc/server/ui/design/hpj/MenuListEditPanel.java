package hc.server.ui.design.hpj;

import hc.App;
import hc.server.HCActionListener;
import hc.server.ui.ClientDesc;
import hc.server.ui.NumberFormatTextField;
import hc.server.ui.design.Designer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class MenuListEditPanel extends NodeEditPanel {
	public static final int MAX_MENUITEM = 50;

	public MenuListEditPanel() {
		table.setModel(new AbstractTableModel() {
			public String getColumnName(int columnIndex) {
				return colTitle[columnIndex];
			}

			public int getRowCount() {
				return body.length;
			}

			public int getColumnCount() {
				return colTitle.length;
			}

			public Object getValueAt(int rowIndex, int columnIndex) {
				return body[rowIndex][columnIndex];
			}
		});

		final DefaultTableCellRenderer centerCellRender = new DefaultTableCellRenderer(){
	        public Component getTableCellRendererComponent(
	                JTable table, Object value, boolean isSelected,
	                boolean hasFocus, int row, int column) {
	        	setHorizontalAlignment(CENTER);
		        return super.getTableCellRendererComponent(table, value,
                        isSelected, hasFocus, row, column);
			}
        };
		table.getColumnModel().getColumn(0).setCellRenderer(centerCellRender);

		table.setRowSelectionAllowed(true);

		upButton.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				int selectedRow = table.getSelectedRow();
				
				int toIdx = selectedRow - 1;
				swapRow(selectedRow, toIdx);
				table.setRowSelectionInterval(toIdx, toIdx);
				notifyModified();
			}
		}, threadPoolToken));
		downButton.addActionListener(new HCActionListener(new Runnable() {
			public void run() {
				int selectedRow = table.getSelectedRow();
				
				int toIdx = selectedRow + 1;
				swapRow(selectedRow, toIdx);
				table.setRowSelectionInterval(toIdx, toIdx);
				notifyModified();
			}
		}, threadPoolToken));
		
		ListSelectionModel rowSM = table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				refreshButton();
			}
		});

//		定义表格头样式
//		final JTableHeader tableHeader = table.getTableHeader();
//		Font oldFont = tableHeader.getFont();
//		tableHeader.setFont(new Font(oldFont.getName(), Font.BOLD, oldFont.getSize()));
//		tableHeader.setBackground(Color.YELLOW);
		
		tablePanel.setLayout(new BorderLayout());
		tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
//		table.setFillsViewportHeight(true);
		
		buttonPanel.setLayout(new GridLayout(1, 3, ClientDesc.hgap, ClientDesc.vgap));
		buttonPanel.add(upButton);
		buttonPanel.add(downButton);
		
		namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		namePanel.add(new JLabel("Name : "));
		nameFiled.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				currItem.name = nameFiled.getText();
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
		nameFiled.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				currItem.name = nameFiled.getText();
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified();
			}
		}, threadPoolToken));
		nameFiled.setColumns(10);
		namePanel.add(nameFiled);
		
		final JLabel colNumLabel = new JLabel("Column Number : ");
		namePanel.add(colNumLabel);
		colNumFiled.getDocument().addDocumentListener(new DocumentListener() {
			private void modify(){
				if(isInited == false){
					return;
				}
				final String inputColNum = colNumFiled.getText();
				currItem.colNum = Integer.parseInt(inputColNum);
				notifyModified();
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
//				modify();
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
		colNumFiled.addActionListener(new HCActionListener(new Runnable() {
			@Override
			public void run() {
				currItem.colNum = Integer.parseInt(colNumFiled.getText());
				App.invokeLaterUI(updateTreeRunnable);
				notifyModified();
			}
		}, threadPoolToken));
		colNumLabel.setToolTipText("<html>column number of each row, which display current menu in mobile." +
				"<BR>set 0 to auto</html>");
		colNumFiled.setColumns(5);
		namePanel.add(colNumFiled);
		
		setLayout(new BorderLayout());
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(namePanel);
		topPanel.add(buttonPanel);
		add(topPanel, BorderLayout.NORTH);
		add(tablePanel, BorderLayout.CENTER);
//		add(buttonPanel, BorderLayout.SOUTH);
	}
	
	String body[][] = new String[MAX_MENUITEM][3];
	String colTitle[] = { "No", "Name", "URL" };//"Type", 

	final JPanel tablePanel = new JPanel();
	final JTable table = new JTable();
	final JPanel buttonPanel = new JPanel();
	
	final JPanel namePanel = new JPanel();
	final JTextField nameFiled = new JTextField();
	final NumberFormatTextField colNumFiled = new NumberFormatTextField();
	final JTextField menuIDField = new JTextField();
	
	final JButton upButton = new JButton("up", Designer.loadImg("up_22.png"));
	final JButton downButton = new JButton("down", Designer.loadImg("down_22.png"));
	
//	TablePacker tp = new TablePacker(TablePacker.ALL_ROWS, true);
	int size;
	HPMenu currItem;
	
	private boolean isInited = false;
	private void notifyModified(){
		if(isInited){
			currItem.getContext().modified.setModified(true);
		}
	}


	private void swapRow(final int fromIdx, final int toIdx){
		for (int i = 1; i < body[0].length; i++) {
			String v1 = body[toIdx][i];
			body[toIdx][i] = body[fromIdx][i];
			body[fromIdx][i] = v1;
		}
		
		TreeNode fromNode = currNode.getChildAt(fromIdx + HCjar.SKIP_SUB_MENU_ITEM_NUM);
		currNode.remove(fromIdx + HCjar.SKIP_SUB_MENU_ITEM_NUM);
		currNode.insert((MutableTreeNode)fromNode, toIdx + HCjar.SKIP_SUB_MENU_ITEM_NUM);
		
		App.invokeLaterUI(updateTreeRunnable);
	}
	
	@Override
	public void init(MutableTreeNode data, JTree tree){
		super.init(data, tree);
		
		for (int i = 0; i < MAX_MENUITEM; i++) {
			for (int j = 0; j < body[0].length; j++) {
				body[i][j] = "";
			}
		}
		
		isInited = false;
		
		currItem = (HPMenu)currNode.getUserObject();
		nameFiled.setText(currItem.name);
		colNumFiled.setText(String.valueOf(currItem.colNum));
		
		size = data.getChildCount() - HCjar.SKIP_SUB_MENU_ITEM_NUM;
		
		for (int i = 0; i < size; i++) {
			TreeNode tn = data.getChildAt(i + HCjar.SKIP_SUB_MENU_ITEM_NUM);
			HPMenuItem mi = (HPMenuItem)((DefaultMutableTreeNode)tn).getUserObject();
			body[i][0] = String.valueOf(i + 1);
			body[i][1] = mi.name;
//			body[i][2] = String.valueOf(mi.type);
			body[i][2] = mi.url;
		}
		
		table.setRowSelectionInterval(0, 0);
		
		refreshButton();
		
		isInited = true;
	}
	
	private void refreshButton(){
		int selectedRow = table.getSelectedRow();
		int editRowNum = selectedRow + 1;
			
		if(editRowNum > size){
			downButton.setEnabled(false);
			upButton.setEnabled(false);
		}else{
			if(editRowNum == size){
				downButton.setEnabled(false);
			}else{
				downButton.setEnabled(true);
			}
			
			if(editRowNum == 1){
				upButton.setEnabled(false);
			}else{
				upButton.setEnabled(true);
			}
		}
	}
}
