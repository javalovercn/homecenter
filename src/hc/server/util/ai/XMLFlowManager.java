package hc.server.util.ai;

import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class XMLFlowManager extends TableSM {
	private final PreparedStatement isReachMax;
	private final PreparedStatement getIDPreparedState, getScreenIDPreparedState;
	
	public XMLFlowManager(final AIPersistentManager mgr) throws SQLException {
		super("XML_FLOW", mgr);
		
		getIDPreparedState = mgr.getConnection().prepareStatement("SELECT id FROM " + tableName + " WHERE " +
				"screenID = ? AND referLableID = ? AND jsCmd = ? AND locKey = ?;");
		isReachMax = mgr.getConnection().prepareStatement("SELECT COUNT(jsCmd) FROM " + tableName + " WHERE " +
				"screenID = ? AND jsCmd = ? AND locKey = ?;");
		getScreenIDPreparedState = mgr.getConnection().prepareStatement("SELECT screenID FROM " + tableName + " WHERE referLableID = ?;");
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
	
	@Override
	public String getCreateTableBody() {
		return "(" +
				"id INTEGER," +
				"referLableID INTEGER," +
				"jsCmd varchar(50), " +
				"locKey varchar(300), " +
				"screenID varchar(100)" +
				")";
	}
	
	@Override
	public String getIndexColumn() {
		return "screenID";
	}
	
	private PreparedStatement deleteScreenPreparedState;
	
	public final void deleteScreenID(final String screenID) throws SQLException {
		mgr.labelSM.deleteScreenIDFromXML(screenID);
		
		if(deleteScreenPreparedState == null){
			deleteScreenPreparedState = mgr.getConnection().prepareStatement("DELETE FROM " + tableName + " WHERE screenID = ?;");
		}
		deleteScreenPreparedState.setString(1, screenID);
		deleteScreenPreparedState.executeUpdate();
	}
	
	/**
	 * 
	 * @param jsCmd
	 * @param locKey
	 * @param screenID
	 * @param referLableID
	 * @return -1 means not found.
	 */
	public final int searchXMLData(final String jsCmd, final String locKey, final String screenID, final int referLableID){
		final int id = -1;
		try{
			getIDPreparedState.setString(1, screenID);
			getIDPreparedState.setInt(2, referLableID);
			getIDPreparedState.setString(3, jsCmd);
			getIDPreparedState.setString(4, locKey);
			
			final ResultSet rs = getIDPreparedState.executeQuery();
			final boolean hasRecord = rs.next();
			if(hasRecord){
				rs.getInt(1);
			}
			//rs.close();
			
			return id;
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return id;
	}

	public final boolean isReachMax(final String jsCmd, final String locKey, final String screenID){
		try{
			isReachMax.setString(1, screenID);
			isReachMax.setString(2, jsCmd);
			isReachMax.setString(3, locKey);
			
			final ResultSet rs = isReachMax.executeQuery();
			if(rs.next()){
				final int count = rs.getInt(1);
				if(count > AIUtil.MAX_LABLE_NUM_IN_SAME_LOC){
					L.V = L.WShop ? false : LogManager.warning("too many records for locKey = " + locKey + " AND screenID = " + screenID);
					return true;
				}
			}
			//rs.close();
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return false;
	}
	
	private PreparedStatement appendPreparedState;
	
	public final void appendXMLData(final int id, final String jsCmd, final String locKey, final String screenID,
			final int referLableID){
		try{
			if(appendPreparedState == null){
				appendPreparedState = mgr.getConnection().prepareStatement("INSERT INTO " + tableName + 
						" (id, jsCmd, locKey, screenID, referLableID) VALUES (?, ?, ?, ?, ?);");
			}
			appendPreparedState.setInt(1, id);
			appendPreparedState.setString(2, jsCmd);
			appendPreparedState.setString(3, locKey);
			appendPreparedState.setString(4, screenID);
			appendPreparedState.setInt(5, referLableID);
			
			appendPreparedState.executeUpdate();
		}catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

}
