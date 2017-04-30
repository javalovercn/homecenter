package hc.server.util.ai;

import hc.core.util.ExceptionReporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LabelManager extends TableSM {
	public static final String PROJECT_TITLE = "project_title";
	
	public static final int LABEL_SRC_XML_DATA = 1;
	public static final int LABEL_SRC_XML_TOOLTIP = 2;
	public static final int LABEL_SRC_CTRL_ATTRIBUTE = 3;
	public static final int LABEL_SRC_ITEM = 4;
	public static final int LABEL_SRC_PROJ = 5;
	public static final int LABEL_SRC_MSG = 6;
	
	private final PreparedStatement appendPreparedState;

	public LabelManager(final AIPersistentManager mgr, final KeySM keySM) throws SQLException {
		super("LABEL_KEY", mgr);
		
		appendPreparedState = mgr.getConnection().prepareStatement("INSERT INTO " + tableName + 
			" (id, labelSrc, labelLocale, labelKey, splitKeys) VALUES (?, ?, ?, ?, ?);");
	}
	
	private PreparedStatement deleteLabelPreparedStateFromXML;
	
	public final void deleteScreenIDFromXML(final String screenID) throws SQLException {
		if(deleteLabelPreparedStateFromXML == null){
			deleteLabelPreparedStateFromXML = mgr.getConnection().prepareStatement("DELETE FROM " + tableName + 
					" WHERE id IN (SELECT referLableID FROM " + mgr.xmlSM.tableName + " WHERE screenID = ?);");
		}
		deleteLabelPreparedStateFromXML.setString(1, screenID);
		deleteLabelPreparedStateFromXML.executeUpdate();
		
		listLabel = null;
	}
	
	private PreparedStatement deleteLabelPreparedStateFromItem;
	
	public final void deleteScreenIDFromTitle(final String screenID) throws SQLException {
		if(deleteLabelPreparedStateFromItem == null){
			deleteLabelPreparedStateFromItem = mgr.getConnection().prepareStatement("DELETE FROM " + tableName + 
					" WHERE id IN (SELECT referLableID FROM " + mgr.itemTitleSM.tableName + " WHERE screenID = ?);");
		}
		deleteLabelPreparedStateFromItem.setString(1, screenID);
		deleteLabelPreparedStateFromItem.executeUpdate();
	}
	
	@Override
	public String getCreateTableBody() {
		return "(" +
				"id INTEGER," +
				"labelSrc INTEGER," +
				"labelLocale varchar(20)," +
				"labelKey varchar(" + (AIUtil.maxByteLen / 2) + "), " +
				"splitKeys VARBINARY(" + AIUtil.maxByteLen + ")" +//key1,key2,key3
				")";
	}
	
	@Override
	public String getIndexColumn() {
		return "id";
	}
	
	public final String getTarget(final int labelID, final int labelSrc) throws SQLException {
		if(labelSrc == LABEL_SRC_CTRL_ATTRIBUTE){
			return mgr.ctrlSM.getCtrlID(labelID);
		}else if(labelSrc == LABEL_SRC_XML_DATA || labelSrc == LABEL_SRC_XML_TOOLTIP){
			return mgr.xmlSM.getScreenID(labelID);
		}
		return null;
	}
	
	public final String getTitleTarget(final int labelID, final int labelSrc) throws SQLException {
		if(labelSrc == LABEL_SRC_ITEM){
			return mgr.itemTitleSM.getScreenID(labelID);
		}else if(labelSrc == LABEL_SRC_PROJ){
			return PROJECT_TITLE;
		}
		return null;
	}
	
	public final int appendData(final int src, final String labelLocale, final String lable, List<String> keys){
		if(keys == null){
			keys = new ArrayList<String>();
		}
		
		if(listLabel == null){
			loadData();
		}
		
		int id = 0;
		
		try{
			final Connection connection = mgr.getConnection();
			
			connection.setAutoCommit(false);
			
			id = mgr.identitySM.getNextID(mgr.labelSM.tableName);
			
			final int size = keys.size();
			for (int i = 0; i < size; i++) {
				mgr.keySM.appendIfNotExists(id, keys.get(i));
			}
			
			final byte[] splitKeys = AIUtil.toRecord(keys);
			
			appendPreparedState.setInt(1, id);
			appendPreparedState.setInt(2, src);
			appendPreparedState.setString(3, labelLocale);
			appendPreparedState.setString(4, lable);
			appendPreparedState.setBytes(5, splitKeys);
			
			appendPreparedState.executeUpdate();
			
			final LabelData data = new LabelData();
			data.id = id;
			data.lableSrc = src;
			data.labelLocale = labelLocale;
			data.label = lable;
			data.splitKeys = splitKeys;
			data.isAlive = true;
			
			listLabel.add(data);
			
			connection.commit();
			
			connection.setAutoCommit(true);
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		
		return id;
	}
	
	ArrayList<LabelData> listLabel;
	
	/**
	 * null means not found
	 * @param labelID
	 * @return
	 */
	public final LabelData getLabelData(final int labelID){
		if(listLabel == null){
			loadData();
		}
		
		final int size = listLabel.size();
		for (int i = 0; i < size; i++) {
			final LabelData data = listLabel.get(i);
			if(data.id == labelID){
				return data;
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param labelLocale
	 * @param label
	 * @return -1 means not found
	 */
	public final int getID(final int src, final String labelLocale, final String label){
		if(listLabel == null){
			loadData();
		}
		
		final int size = listLabel.size();
		for (int i = 0; i < size; i++) {
			final LabelData data = listLabel.get(i);
			if(src == data.lableSrc &&
					label.equals(data.label) &&
					labelLocale.equals(data.labelLocale)){
				data.isAlive = true;
				return data.id;
			}
		}
		
		return -1;
	}
	
	private final void loadData(){
		listLabel = new ArrayList<LabelData>(1024);
		try{
			final String sql = "SELECT id, labelSrc, labelLocale, labelKey, splitKeys FROM " + this.tableName + ";";
			final Statement listPreparedState = mgr.getConnection().createStatement();
			final ResultSet rs = listPreparedState.executeQuery(sql);
			while(rs.next()){
				final LabelData data = new LabelData();
				
				data.id = rs.getInt(1);
				data.lableSrc = rs.getInt(2);
				data.labelLocale = rs.getString(3);
				data.label = rs.getString(4);
				data.splitKeys = rs.getBytes(5);
				
				listLabel.add(data);
			}
			//rs.close();
			listPreparedState.close();
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}
	
}

class LabelData {
	public int id;
	public int lableSrc;
	public String labelLocale;
	public String label;
	public byte[] splitKeys;
	public boolean isAlive;
}
