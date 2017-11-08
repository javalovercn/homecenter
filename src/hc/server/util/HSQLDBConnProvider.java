package hc.server.util;

import hc.core.IConstant;
import hc.core.L;
import hc.core.util.ExceptionReporter;
import hc.core.util.LogManager;
import hc.server.data.StoreDirManager;
import hc.server.ui.ProjectContext;
import hc.server.ui.ServerUIAPIAgent;
import hc.server.util.ai.AIPersistentManager;
import hc.util.ResourceUtil;

import java.io.File;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import third.hsqldb.cmdline.SqlFile;
import third.quartz.impl.jdbcjobstore.HSQLDBDelegate;
import third.quartz.utils.ConnectionProvider;

public class HSQLDBConnProvider implements ConnectionProvider{
	final String key;
	final ProjectContext projectContext;
	final String domainName;
	final String password;
	
	public HSQLDBConnProvider(final String key, final ProjectContext projectContext,
			final String domainName){
		this.key = key;
		this.projectContext = projectContext;
		this.domainName = domainName;
		this.password = ServerUIAPIAgent.getDBPassword(projectContext);

	}
	
	@Override
	public final void shutdown() throws SQLException {
		L.V = L.WShop ? false : LogManager
				.log("Scheduler ready to shutdown db connection : " + absPath);
		ResourceUtil.shutdownHSQLDB(getConnection(), false);
		L.V = L.WShop ? false : LogManager
				.log("Scheduler successful shutdown db connection : " + absPath);
	}

	@Override
	public final void initialize() throws SQLException {
		System.out.println("init");
	}

	final String user = "project_user";
	boolean isInited = false;
	String absPath;

	private final File buildCronDir() {
		return projectContext.getPrivateFile(StoreDirManager.HC_SYS_FOR_USER_PRIVATE_DIR
				+ StoreDirManager.CRON_SUB_DIR_FOR_USER_PRIVATE_DIR);
	}

	@Override
	public final Connection getConnection() throws SQLException {
		Connection connection = null;

		if (isInited == false) {
			synchronized (this) {
				if (isInited == false) {
					final File cronDir = buildCronDir();
					final File dDir = new File(cronDir, domainName);
					final File dbName = new File(dDir, domainName);// ByteUtil.toHex(StringUtil.getBytes(
					
					final boolean isNewDB = (dDir.isDirectory() == false);
					absPath = dbName.getAbsolutePath();
					connection = buildNewConn();

					if (isNewDB || getLastCompactMS(projectContext, domainName) == 0) {
						try {
							L.V = L.WShop ? false : LogManager.log("init quartz scheduler database.");

							final SqlFile sqlFile = new SqlFile(
									new InputStreamReader(
											HSQLDBDelegate.class
													.getResourceAsStream("tables_hsqldb.sql"),
											IConstant.UTF_8), "init", System.out,
									IConstant.UTF_8, false, null);
							sqlFile.setConnection(connection);
							sqlFile.execute();

							projectContext.run(new Runnable() {
								@Override
								public void run() {
									try {
										// HSQLDB doesn't write changes
										// immediately to disk after a commit
										Thread.sleep(500);// wait WRITE DELAY
									} catch (final Exception e) {
									}
									setLastCompactMS(projectContext, domainName);
								}
							});

						} catch (final Exception e) {
							// ThreadConfig.putValue(ThreadConfig.SCHEDULER_THROWN_EXCEPTION,
							// e);
							ExceptionReporter.printStackTrace(e);
						}
					}

					connection = compackDB(connection, domainName);

					isInited = true;// 要置于最后，以保证absPath != null
				}
			}
		}

		if (connection == null) {
			connection = buildNewConn();
		}

		return connection;
	}

	private final Connection buildNewConn() throws SQLException {
		return AIPersistentManager.getConnection(absPath, user, password);
	}

	private final long getLastCompactMS(final ProjectContext ctx, final String cronDomain) {
		final String prop = ServerUIAPIAgent.PROJ_CRON_DB_COMPACT_MS + cronDomain;
		final String value = ServerUIAPIAgent.getSysPropertiesOnProj(ctx, prop);
		if (value == null) {
			return 0;
		} else {
			try {
				return Long.parseLong(value);
			} catch (final Exception e) {
			}
		}
		return 0;
	}
	
	private final void setLastCompactMS(final ProjectContext ctx, final String cronDomain) {
		final String prop = ServerUIAPIAgent.PROJ_CRON_DB_COMPACT_MS + cronDomain;
		ServerUIAPIAgent.setSysPropertiesOnProj(ctx, prop, String.valueOf(System.currentTimeMillis()));
		ctx.saveProperties();
	}
	
	private final Connection compackDB(Connection connection, final String domainName)
			throws SQLException {
		if (System.currentTimeMillis() - getLastCompactMS(projectContext, domainName) > ServerUIAPIAgent.getProjDesignConf(projectContext).compactDayMS) {// 低频率能增强数据库安全
			try {
				LogManager.log("compacting cron database : " + domainName + " in project [" + projectContext.getProjectID() + "].");
				final Statement state = connection.createStatement();
				state.execute(ResourceUtil.SHUTDOWN_COMPACT);
				state.close();
				LogManager.log("done compacting cron database : " + domainName + " in project [" + projectContext.getProjectID() + "].");
			} catch (final Exception e) {
				ExceptionReporter.printStackTrace(e);
			}
			setLastCompactMS(projectContext, domainName);
			connection = buildNewConn();
		}
		return connection;
	}
}
