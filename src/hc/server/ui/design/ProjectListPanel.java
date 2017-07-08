package hc.server.ui.design;

import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public abstract class ProjectListPanel extends BaseProjList {

	final PropertiesSet projWidthSet = new PropertiesSet(PropertiesManager.S_LINK_PROJECTS_COLUMNS_WIDTH);

	final Object[] colNames = {(String)ResourceUtil.get(9109),//No, 序号 
			(String)ResourceUtil.get(8017),//"is Root", 
			(String)ResourceUtil.get(8018),//"Project ID", 
			(String)ResourceUtil.get(8019),//"Version", 
			LinkProjectPanel.ACTIVE,//"Active", 
			(String)ResourceUtil.get(8021),//"Link Name", 
			(String)ResourceUtil.get(8022),//"Comment", 
			upgradeURL,//upgradeURL
			};

	public ProjectListPanel(){
//		for (int i = 0; i < LinkProjectManager.MAX_LINK_PROJ_NUM; i++) {
//			data.add(new Object[COL_NUM]);
//			for (int j = 0; j < COL_NUM; j++) {
//				if(j == 0){
//					data.elementAt(i)[j] = "";
//				}else if(j == IDX_OBJ_STORE){
//					data.elementAt(i)[j] = null;
//				}
//			}
//		}
		super();
	}

	public void initTable(final JTable table){
		//装入用户调整后的列宽度
		if(projWidthSet.size() > 0){
			final int columnCount = table.getColumnModel().getColumnCount();
			try{
				for (int j = 0; j < projWidthSet.size() && j < columnCount; j++) {
					table.getColumnModel().getColumn(j).setPreferredWidth(Integer.parseInt(projWidthSet.getItem(j)));
				}
			}catch (final Exception e) {
			}
		}

		//侦听事件，存储用户调整后的列宽度
		table.getTableHeader().addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved( final MouseEvent e ){}
			@Override
			public void mouseDragged( final MouseEvent e ){
				try{
					final TableColumn changedColumn = ((JTableHeader)e.getSource()).getResizingColumn();
					if(changedColumn != null){
						final TableColumnModel tcm = table.getTableHeader().getColumnModel();
						final int size = tcm.getColumnCount();
						final String[] widths = new String[size];
						for (int i = 0; i < size; i++) {
							widths[i] = String.valueOf(tcm.getColumn(i).getWidth());
						}
						projWidthSet.refill(widths);
						projWidthSet.save();
					}
				}catch (final Exception ex) {
				}
			}
		});	}
}
