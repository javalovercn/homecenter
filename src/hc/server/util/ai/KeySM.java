package hc.server.util.ai;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class KeySM extends TableSM {
	private final PreparedStatement query, insert, queryLableID;

	public KeySM(final AIPersistentManager mgr, final RLabelKeySM rLabelKeySM) throws SQLException {
		super("KEY_LIST", mgr);

		query = mgr.getConnection().prepareStatement("SELECT id FROM " + tableName + " WHERE keyValue = ?;");
		insert = mgr.getConnection().prepareStatement("INSERT INTO " + tableName + " (id, keyValue) VALUES (?, ?);");
		queryLableID = mgr.getConnection()
				.prepareStatement("SELECT labelID " + "FROM " + rLabelKeySM.tableName + " LEFT JOIN " + tableName + " ON "
						+ rLabelKeySM.tableName + ".keyID = " + tableName + ".id " + "WHERE " + tableName + ".keyValue = ?" + "GROUP BY "
						+ rLabelKeySM.tableName + ".labelID;");
	}

	@Override
	public String getCreateTableBody() {
		return "(" + "id INTEGER," + "keyValue varchar(100)" + ")";
	}

	@Override
	public String getIndexColumn() {
		return "keyValue";
	}

	public final void queryLableID(final Query query) throws SQLException {
		final int size = query.itemNum;
		for (int i = 0; i < size; i++) {
			final String oneKey = query.search.get(i);

			queryLableID.setString(1, oneKey);
			final ResultSet rs = queryLableID.executeQuery();
			while (rs.next()) {
				final int labelID = rs.getInt(1);

				final LabelData data = mgr.labelSM.getLabelData(labelID);
				final MatchScore score = new MatchScore();
				Matcher.matchRecord(data.splitKeys, query, score);
				if (score.matchKeyNum > 0) {
					score.labelData = data;
					score.fromKey = oneKey;
					query.addScore(score);
				}
			}
		}
	}

	public final void appendIfNotExists(final int labelID, final String key) throws SQLException {
		int keyID = searchKey(key);

		if (keyID == -1) {
			keyID = mgr.identitySM.getNextID(tableName);

			insert.setInt(1, keyID);
			insert.setString(2, key);

			insert.executeUpdate();
		}

		mgr.rLabelKeySM.appendIfNotExists(labelID, keyID);
	}

	/**
	 * -1 means not found
	 * 
	 * @param key
	 * @return
	 * @throws SQLException
	 */
	private final int searchKey(final String key) throws SQLException {
		query.setString(1, key);

		final ResultSet rs = query.executeQuery();
		int keyID = -1;

		if (rs.next()) {
			keyID = rs.getInt(1);
		}
		// rs.close();
		return keyID;
	}

}
