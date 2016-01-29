package hc.server.util;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class HCEnableHeaderRenderer implements TableCellRenderer {
	final TableCellRenderer oldRend;
	boolean isEnable = true;
	
	public HCEnableHeaderRenderer(TableCellRenderer rend){
		this.oldRend = rend;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component comp = oldRend.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		comp.setEnabled(isEnable);
		return comp;
	}
	
	public void setEnabled(boolean enabled) {
		isEnable = enabled;
	}

}
