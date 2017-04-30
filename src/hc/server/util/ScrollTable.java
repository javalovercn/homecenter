package hc.server.util;

import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

public class ScrollTable extends JTable {
	public ScrollTable(final TableModel dm) {
        super(dm);
    }
	
	private boolean trackViewportWidth = false;
    private boolean inited = false;
    private boolean ignoreUpdates = false;
    private boolean isAddForParent = false;

    @Override
    protected void initializeLocalVars() {
        super.initializeLocalVars();
        inited = true;
        updateColumnWidth();
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        updateColumnWidth();
        if(isAddForParent == false){
        	isAddForParent = true;
            getParent().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent e) {
                    invalidate();
                }
            });
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (!ignoreUpdates) {
            updateColumnWidth();
        }
        ignoreUpdates = false;
    }
    
	@Override
	public boolean getScrollableTracksViewportWidth() {
        return trackViewportWidth;
    }
	
	@Override
    public void tableChanged(final TableModelEvent e) {
        super.tableChanged(e);
        if (inited) {
            updateColumnWidth();
        }
    }
	
	protected void updateColumnWidth() {
        if (getParent() != null) {
            int width = 0;
            for (int col = 0; col < getColumnCount(); col++) {
                int colWidth = 0;
                for (int row = 0; row < getRowCount(); row++) {
                    final int prefWidth = getCellRenderer(row, col).
                            getTableCellRendererComponent(this, getValueAt(row, col), false, false, row, col).
                            getPreferredSize().width;
                    colWidth = Math.max(colWidth, prefWidth + getIntercellSpacing().width);
                }

                final TableColumn tc = getColumnModel().getColumn(convertColumnIndexToModel(col));
                tc.setPreferredWidth(colWidth);
                width += colWidth;
            }

            Container parent = getParent();
            if (parent instanceof JViewport) {
                parent = parent.getParent();
            }

            trackViewportWidth = width < parent.getWidth();
        }
    }
}
