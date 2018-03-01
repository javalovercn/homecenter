package hc.server.util.ai;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RLabelKeySM extends TableSM {
	private final PreparedStatement query;
	private final PreparedStatement insert;

	public RLabelKeySM(final AIPersistentManager mgr) throws SQLException {
		super("R_LABEL_KEY", mgr);

		query = mgr.getConnection().prepareStatement(
				"SELECT labelID FROM " + tableName + " WHERE keyID = ? AND labelID = ?;");
		insert = mgr.getConnection()
				.prepareStatement("INSERT INTO " + tableName + " (labelID, keyID) VALUES (?, ?);");
	}

	@Override
	public String getCreateTableBody() {
		return "(" + "labelID INTEGER," + "keyID INTEGER" + ")";
	}

	@Override
	public String getIndexColumn() {
		return "keyID";
	}

	public final void appendIfNotExists(final int labelID, final int keyID) throws SQLException {
		query.setInt(1, keyID);
		query.setInt(2, labelID);

		boolean has = false;
		final ResultSet rs = query.executeQuery();
		if (rs.next()) {
			has = true;
		}
		// rs.close();

		if (has) {
			return;
		}

		insert.setInt(1, labelID);
		insert.setInt(2, keyID);
		insert.executeUpdate();
	}

}
