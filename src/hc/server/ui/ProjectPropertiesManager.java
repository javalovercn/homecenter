package hc.server.ui;

import hc.core.util.CCoreUtil;
import hc.server.data.StoreDirManager;
import hc.util.PropertiesMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Properties;

public class ProjectPropertiesManager {
	final Properties propertie;
	final File propertiesFile;

	/**
	 * 将原存储在hc_config下的用户级，迁移此处
	 * 
	 * @param sysMap
	 * @param projectID
	 */
	private final void loadFromSystemProp(final PropertiesMap sysMap, final String projectID) {
		boolean isAdd = false;
		{
			final Iterator<String> it = sysMap.keySet().iterator();
			while (it.hasNext()) {
				final String key = it.next();
				if (key.startsWith(CCoreUtil.SYS_RESERVED_KEYS_START, 0)) {
					continue;
				} else {
					isAdd = true;
					propertie.put(key, sysMap.get(key));
				}
			}
		}
		save();// 保存空文件，以标识数据切换完毕

		if (isAdd) {
			final Iterator<Object> it = propertie.keySet().iterator();
			while (it.hasNext()) {
				sysMap.remove(it.next());
			}
			sysMap.save();
		}
	}

	public final void save() {
		try {
			final FileOutputStream outputFile = new FileOutputStream(propertiesFile);
			propertie.store(outputFile, null);
			outputFile.close();
		} catch (final Throwable e) {
			e.printStackTrace();
		}
	}

	public ProjectPropertiesManager(final PropertiesMap sysMap, final ProjectContext projectContext, final String projectID) {
		propertie = new Properties();

		propertiesFile = projectContext.getPrivateFile(StoreDirManager.HC_SYS_FOR_USER_PRIVATE_DIR + StoreDirManager.PROJ_PROPERTIES);

		final boolean exists = propertiesFile.exists();
		if (exists == false) {
			loadFromSystemProp(sysMap, projectID);
			return;
		}

		try {
			if (exists) {
				final FileInputStream inputFile = new FileInputStream(propertiesFile);
				propertie.load(inputFile);
				inputFile.close();
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
