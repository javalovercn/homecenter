package hc.server.ui.design;

import hc.util.PropertiesManager;
import hc.util.PropertiesSet;
import hc.util.ResourceUtil;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class ProjectListPanel {
	public static final int COL_NO = 0, COL_IS_ROOT = 1, COL_PROJ_ID = 2, COL_VER = 3, COL_PROJ_ACTIVE = 4, 
			COL_PROJ_LINK_NAME = 5, COL_PROJ_DESC = 6, COL_UPGRADE_URL = 7;
	final int COL_NUM = 2;
	final int IDX_OBJ_STORE = 1;
	final LinkProjectStore oldRootlps;

	final PropertiesSet projIDSet = AddHarHTMLMlet.getLinkProjSet();
	final PropertiesSet projWidthSet = new PropertiesSet(PropertiesManager.S_LINK_PROJECTS_COLUMNS_WIDTH);

	final String upgradeURL = (String)ResourceUtil.get(8023);
	final Object[] colNames = {(String)ResourceUtil.get(9109),//No, 序号 
			(String)ResourceUtil.get(8017),//"is Root", 
			(String)ResourceUtil.get(8018),//"Project ID", 
			(String)ResourceUtil.get(8019),//"Version", 
			(String)ResourceUtil.get(8020),//"Active", 
			(String)ResourceUtil.get(8021),//"Link Name", 
			(String)ResourceUtil.get(8022),//"Comment", 
			upgradeURL,//upgradeURL
			};

	final Vector<Object[]> data = new Vector<Object[]>();//[COL_NUM];
	int dataRowNum = 0;
	final Vector<LinkProjectStore> lpsVector;
	public ProjectListPanel(){
		final int size = projIDSet.size();
		lpsVector = new Vector<LinkProjectStore>(size);
		LinkProjectStore tmp_lps = null;
		for (int i = 0; i < size; i++) {
			final String item = projIDSet.getItem(i);
			final LinkProjectStore lp = new LinkProjectStore();
			lp.restore(item);
			if(lp.isRoot()){
				tmp_lps = lp;
			}
			lpsVector.add(lp);
		}
		oldRootlps = tmp_lps;
		
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
		
		{
			int i = 0;
			final Iterator<LinkProjectStore> itx = LinkProjectManager.getLinkProjsIterator(true);
			while(itx.hasNext()){
				final LinkEditData led = new LinkEditData();
				led.lps = itx.next();
				led.op = (LinkProjectManager.STATUS_NEW);
				led.status = (LinkProjectManager.STATUS_DEPLOYED);

				data.add(new Object[COL_NUM]);
				data.elementAt(i)[0] = String.valueOf(i+1);
				data.elementAt(i++)[IDX_OBJ_STORE] = led;
			}
		}
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
