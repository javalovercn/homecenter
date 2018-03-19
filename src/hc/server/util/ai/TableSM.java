package hc.server.util.ai;

import hc.core.util.ExceptionReporter;

import java.sql.Statement;

public abstract class TableSM {
	protected final String tableName;
	final AIPersistentManager mgr;

	/**
	 * CREATE TABLE IF NOT EXISTS HC (rowid int, rowdata varchar(1000));
	 * 
	 * @param tableName
	 */
	public TableSM(final String tableName, final AIPersistentManager mgr) {
		this.tableName = tableName.toUpperCase();
		this.mgr = mgr;

		createTableIfNotExists();
	}

	public final void dropTable() {
		final String dropTableSql = "drop table " + this.tableName + ";";
		try {
			final Statement state = mgr.getConnection().createStatement();
			state.execute(dropTableSql);
			state.close();
		} catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	public final String toPattern(final String text) {
		return null;
	}

	private final void createTableIfNotExists() {
		final String sql = "CREATE TABLE IF NOT EXISTS " + this.tableName + " " + getCreateTableBody();
		try {
			final Statement state = mgr.getConnection().createStatement();
			state.execute(sql);
			state.close();
		} catch (final Throwable e) {
			ExceptionReporter.printStackTrace(e);
		}

		final String indexColumn = getIndexColumn();
		if (indexColumn != null) {
			final String idxSql = "CREATE INDEX IF NOT EXISTS IDX_" + tableName + " ON " + tableName + " (" + indexColumn + ")";

			try {
				final Statement state = mgr.getConnection().createStatement();
				state.execute(idxSql);
				state.close();
			} catch (final Throwable e) {
				ExceptionReporter.printStackTrace(e);
			}
		}
	}

	/**
	 * for example : (rowid int, rowdata varchar(1000))
	 * 
	 * @return
	 */
	public abstract String getCreateTableBody();

	public abstract String getIndexColumn();
}
