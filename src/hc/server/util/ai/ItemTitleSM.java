package hc.server.util.ai;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemTitleSM extends TableSM {
	public final PreparedStatement search, appendPreparedState, getScreenIDPreparedState;

	public ItemTitleSM(final AIPersistentManager mgr) throws SQLException{
		super("ITEM_TITLE", mgr);
		search = mgr.getConnection().prepareStatement("SELECT id FROM " + tableName + 
				" WHERE screenID = ? AND labelLocale = ? AND itemTitle = ?;");
		appendPreparedState = mgr.getConnection().prepareStatement("INSERT INTO " + tableName +
				" (id, referLableID, labelLocale, itemTitle, screenID) VALUES (?, ?, ?, ?, ?);");
		getScreenIDPreparedState = mgr.getConnection().prepareStatement("SELECT screenID FROM " + tableName + " WHERE referLableID = ?;");
	}
	
	@Override
	public String getCreateTableBody() {
		return "(" +
				"id INTEGER," +
				"referLableID INTEGER," +
				"labelLocale varchar(20)," +
				"itemTitle varchar(50)," +
				"screenID varchar(100)" +
				")";
	}

	@Override
	public String getIndexColumn() {
		return "screenID";
	}
	
	private PreparedStatement deleteScreenPreparedState;
	
	public final void deleteScreenID(final String screenID) throws SQLException {
		mgr.labelSM.deleteScreenIDFromTitle(screenID);
		
		if(deleteScreenPreparedState == null){
			deleteScreenPreparedState = mgr.getConnection().prepareStatement("DELETE FROM " + tableName + " WHERE screenID = ?;");
		}
		deleteScreenPreparedState.setString(1, screenID);
		deleteScreenPreparedState.executeUpdate();
	}
	
	public final String getScreenID(final int labelID) throws SQLException {
		getScreenIDPreparedState.setInt(1, labelID);
		
		final ResultSet rs = getScreenIDPreparedState.executeQuery();
		String screenID = null;
		if(rs.next()){
			screenID = rs.getString(1);
		}
		
		return screenID;
	}
	
	public final boolean hasTitle(final String screenID, final String labelLocale, final String itemLabel) throws SQLException {
		search.setString(1, screenID);
		search.setString(2, labelLocale);
		search.setString(3, itemLabel);
		
		final ResultSet rs = search.executeQuery();
		return rs.next();
	}
	
	public final void appendTitleData(final int referLableID, final String labelLocale, 
			final String itemlabel, final String screenID) throws SQLException {
		final int id = mgr.identitySM.getNextID(tableName);
		
		appendPreparedState.setInt(1, id);
		appendPreparedState.setInt(2, referLableID);
		appendPreparedState.setString(3, labelLocale);
		appendPreparedState.setString(4, itemlabel);
		appendPreparedState.setString(5, screenID);
		
		appendPreparedState.executeUpdate();
	}

}
