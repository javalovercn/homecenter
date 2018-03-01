package hc.server.util.ai;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProjectTitleSM extends TableSM {
	public final PreparedStatement search, appendPreparedState;

	public ProjectTitleSM(final AIPersistentManager mgr) throws SQLException {
		super("PROJ_TITLE", mgr);

		search = mgr.getConnection().prepareStatement(
				"SELECT id FROM " + tableName + " WHERE labelLocale = ? AND projectTitle = ?;");
		appendPreparedState = mgr.getConnection().prepareStatement("INSERT INTO " + tableName
				+ " (id, referLableID, labelLocale, projectTitle) VALUES (?, ?, ?, ?);");
	}

	@Override
	public String getCreateTableBody() {
		return "(" + "id INTEGER," + "referLableID INTEGER," + "labelLocale varchar(20),"
				+ "projectTitle varchar(50)" + ")";
	}

	@Override
	public String getIndexColumn() {
		return null;
	}

	public final boolean hasTitle(final String labelLocale, final String title)
			throws SQLException {
		search.setString(1, labelLocale);
		search.setString(2, title);

		final ResultSet rs = search.executeQuery();
		return rs.next();
	}

	public final void appendTitleData(final int referLableID, final String labelLocale,
			final String projectTitle) throws SQLException {
		final int id = mgr.identitySM.getNextID(tableName);

		appendPreparedState.setInt(1, id);
		appendPreparedState.setInt(2, referLableID);
		appendPreparedState.setString(3, labelLocale);
		appendPreparedState.setString(4, projectTitle);

		appendPreparedState.executeUpdate();
	}

}
