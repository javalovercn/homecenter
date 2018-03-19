package hc.server.util.ai;

import hc.core.util.ExceptionReporter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CtrlManager extends TableSM {
	private final PreparedStatement appendPreparedState;
	private final PreparedStatement getIDPreparedState, getCtrlIDPreparedState;

	public CtrlManager(final AIPersistentManager mgr) throws SQLException {
		super("CTRLLOR", mgr);

		getIDPreparedState = mgr.getConnection()
				.prepareStatement("SELECT id FROM " + tableName + " WHERE " + "referLableID = ? AND ctrlID = ?;");
		appendPreparedState = mgr.getConnection()
				.prepareStatement("INSERT INTO " + tableName + " (id, referLableID, ctrlID) VALUES (?, ?, ?);");
		getCtrlIDPreparedState = mgr.getConnection().prepareStatement("SELECT ctrlID FROM " + tableName + " WHERE referLableID = ?;");
	}

	@Override
	public String getCreateTableBody() {
		return "(" + "id INTEGER," + "referLableID INTEGER," + "ctrlID varchar(100)" + ")";
	}

	@Override
	public String getIndexColumn() {
		return "referLableID";
	}

	public final String getCtrlID(final int labelID) throws SQLException {
		getCtrlIDPreparedState.setInt(1, labelID);

		final ResultSet rs = getCtrlIDPreparedState.executeQuery();
		String ctrlID = null;
		if (rs.next()) {
			ctrlID = rs.getString(1);
		}

		return ctrlID;
	}

	public final int searchCtrlData(final int referLableID, final String ctrlID) {
		int id = -1;
		try {
			getIDPreparedState.setInt(1, referLableID);
			getIDPreparedState.setString(2, ctrlID);

			final ResultSet rs = getIDPreparedState.executeQuery();
			final boolean hasRecord = rs.next();
			if (hasRecord) {
				id = rs.getInt(1);
			}
			// rs.close();

			return id;
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
		return id;
	}

	public final void appendCtrlData(final int id, final int referLableID, final String ctrlID) {
		try {
			appendPreparedState.setInt(1, id);
			appendPreparedState.setInt(2, referLableID);
			appendPreparedState.setString(3, ctrlID);

			appendPreparedState.executeUpdate();
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}
	}

}
