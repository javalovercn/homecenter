package hc.server.util.ai;

import hc.core.util.ExceptionReporter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IdentitySM extends TableSM {
	public IdentitySM(final AIPersistentManager mgr) throws SQLException {
		super("IDENTITY_LIST", mgr);

		queryps = mgr.getConnection()
				.prepareStatement("SELECT id FROM IDENTITY_LIST WHERE identityValue = ?;");
		updateOnePS = mgr.getConnection()
				.prepareStatement("UPDATE IDENTITY_LIST set id = ? WHERE identityValue = ?;");
		insertPS = mgr.getConnection()
				.prepareStatement("INSERT INTO IDENTITY_LIST (id, identityValue) VALUES (?, ?);");
	}

	@Override
	public String getCreateTableBody() {
		return "(" + "id INTEGER," + "identityValue varchar(100)" + ")";
	}

	@Override
	public String getIndexColumn() {
		return null;
	}

	final PreparedStatement queryps, updateOnePS, insertPS;

	/**
	 * 
	 * @param tableName
	 * @return -1 means fail to getID
	 */
	public final int getNextID(final String tableName) {
		try {
			queryps.setString(1, tableName);
			final ResultSet rs = queryps.executeQuery();
			int out;
			if (rs.next()) {
				out = rs.getInt(1) + 1;
				// rs.close();

				updateOnePS.setInt(1, out);
				updateOnePS.setString(2, tableName);
				updateOnePS.executeUpdate();
			} else {
				out = 1;
				// rs.close();

				insertPS.setInt(1, out);
				insertPS.setString(2, tableName);
				insertPS.executeUpdate();
			}

			return out;
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}

		return -1;
	}

}
