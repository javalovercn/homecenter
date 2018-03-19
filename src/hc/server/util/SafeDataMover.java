package hc.server.util;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import hc.core.L;
import hc.core.util.LogManager;
import hc.util.ResourceUtil;

public class SafeDataMover {
	final File src;
	final String[] excludeExtensions;
	final String[] excludeDirs;
	final File target;

	final HashMap<File, SubItems> targetItems = new HashMap<File, SubItems>(32);
	final HashMap<File, Long> sourceLastModi = new HashMap<File, Long>(32);
	final HashMap<File, Long> sourceFileLength = new HashMap<File, Long>(32);
	final HashMap<File, String> sourceFileMD5 = new HashMap<File, String>(32);

	public SafeDataMover(final File src, final File target, final String[] exclude, final String[] excludeDirs) {
		this.src = src;
		this.target = target;
		this.excludeExtensions = exclude;
		this.excludeDirs = excludeDirs;
	}

	public final void syncFirst() {
		targetItems.clear();
		sourceLastModi.clear();
		sourceFileLength.clear();
		sourceFileMD5.clear();

		loadTargetFilesSnap(target);

		sync(src, target, excludeExtensions, excludeDirs);
	}

	public final boolean syncAck() {
		return sync(src, target, excludeExtensions, excludeDirs);
	}

	/**
	 * true means changed.
	 * 
	 * @param parentSrc
	 * @param parentTarget
	 * @param excluesExtentions
	 * @return
	 */
	private final boolean sync(final File parentSrc, final File parentTarget, final String[] excluesExtentions,
			final String[] excludeDirs) {
		final SubItems targetSubItems = targetItems.get(parentTarget);
		if (targetSubItems == null) {
			copyToTarget(parentSrc, parentTarget);
			loadTargetFilesSnap(parentTarget);
			loadSourceFilesSnap(parentSrc);
			return true;
		}

		final File[] srcSubs = parentSrc.listFiles();
		if (srcSubs == null) {// 已被删除
			L.V = L.WShop ? false
					: LogManager.log("there is no thing in src : " + parentSrc.getAbsolutePath() + ", isDir : " + parentSrc.isDirectory());
			targetItems.remove(parentTarget);
			ResourceUtil.deleteDirectoryNow(parentTarget, true);
			return true;
		}

		final Vector<File> targetSubs = targetSubItems.subFiles;
		final Vector<String> targetNames = targetSubItems.subNames;

		boolean isChanged = false;

		final int subFileNum = srcSubs.length;
		final String[] srcSubNames = new String[subFileNum];
		int sameSubCount = 0;

		final boolean hasExcludes = excluesExtentions != null;
		final boolean hasExcludeDirs = excludeDirs != null;

		for (int i = 0; i < subFileNum; i++) {
			final File srcItem = srcSubs[i];
			final boolean isDir = srcItem.isDirectory();

			final String srcItemFileShortName = srcItem.getName();
			srcSubNames[i] = srcItemFileShortName;// 要置下行条件之前，否则可能为null

			if (hasExcludeDirs && isDir) {
				boolean isExcluds = false;
				for (int j = 0; j < excludeDirs.length; j++) {
					if (srcItemFileShortName.equals(excludeDirs[j])) {
						L.V = L.WShop ? false : LogManager.log("[SafeDataManager] exclude dir : " + srcItemFileShortName);
						isExcluds = true;
						break;
					}
				}
				if (isExcluds) {
					continue;
				}
			}

			if (hasExcludes && (isDir == false)) {
				boolean isExcluds = false;
				for (int j = 0; j < excluesExtentions.length; j++) {
					if (srcItemFileShortName.endsWith(excluesExtentions[j])) {
						L.V = L.WShop ? false : LogManager.log("[SafeDataManager] exclude file : " + srcItemFileShortName);
						isExcluds = true;
						break;
					}
				}
				if (isExcluds) {
					continue;
				}
			}

			final boolean isFindInTarget = targetNames.contains(srcItemFileShortName);

			final File subParentTarget = new File(parentTarget, srcItemFileShortName);
			if (isFindInTarget == false) {
				if (isDir) {
					copyToTarget(srcItem, subParentTarget);
					loadTargetFilesSnap(subParentTarget);
					loadSourceFilesSnap(srcItem);
				} else {
					final String md5 = cp(srcItem, subParentTarget);
					sourceLastModi.put(srcItem, srcItem.lastModified());
					sourceFileLength.put(srcItem, srcItem.length());
					sourceFileMD5.put(srcItem, md5);
				}
				targetSubs.add(subParentTarget);
				targetNames.add(srcItemFileShortName);
				isChanged = true;
				continue;
			} else {
				sameSubCount++;

				if (isDir) {
					isChanged = sync(srcItem, subParentTarget, null, null) || isChanged;
				} else {
					final long lastModified = srcItem.lastModified();
					if (lastModified == 0) {
						targetSubs.remove(subParentTarget);
						targetNames.remove(srcItemFileShortName);

						sourceLastModi.remove(srcItem);
						sourceFileLength.remove(srcItem);
						sourceFileMD5.remove(srcItem);
						isChanged = true;
					} else {
						final Long srcLastModi = sourceLastModi.get(srcItem);
						if (srcLastModi != null && lastModified == srcLastModi && srcItem.length() == sourceFileLength.get(srcItem)
								&& sourceFileMD5.get(srcItem).equals(getMD5(srcItem))) {
						} else {
							final String md5 = cp(srcItem, subParentTarget);
							sourceLastModi.put(srcItem, srcItem.lastModified());
							sourceFileLength.put(srcItem, srcItem.length());
							sourceFileMD5.put(srcItem, md5);
							isChanged = true;
						}
					}
				}
			}
		}

		final int targetSubNum = targetNames.size();
		if (subFileNum < targetSubNum || sameSubCount != subFileNum) {
			// 存在删除的目录或文件
			for (int i = targetSubNum - 1; i >= 0; i--) {
				final String subItem = targetNames.get(i);
				final File targetSubItemFile = targetSubs.get(i);
				final boolean isTargetSubItemFile = targetSubItemFile.isFile();

				boolean isKeep = false;
				for (int j = 0; j < srcSubNames.length; j++) {
					if (srcSubNames[j].equals(subItem)) {
						isKeep = true;
						break;
					}
				}

				if (isKeep) {
					if (hasExcludes && isTargetSubItemFile) {
						for (int j = 0; j < excluesExtentions.length; j++) {
							if (subItem.endsWith(excluesExtentions[j])) {
								L.V = L.WShop ? false : LogManager.log("[SafeDataManager] exclude file : " + subItem);
								isKeep = false;
								break;
							}
						}
					} else if (hasExcludeDirs && (isTargetSubItemFile == false)) {
						for (int j = 0; j < excludeDirs.length; j++) {
							if (subItem.equals(excludeDirs[j])) {
								L.V = L.WShop ? false : LogManager.log("[SafeDataManager] exclude dir : " + subItem);
								isKeep = false;
								break;
							}
						}
					}
				}

				if (isKeep == false) {
					targetSubs.remove(i);
					targetNames.remove(i);

					L.V = L.WShop ? false
							: LogManager.log("target exists, but not found in src, del : " + targetSubItemFile.getAbsolutePath());

					if (isTargetSubItemFile == false) {
						ResourceUtil.deleteDirectoryNow(targetSubItemFile, true);
					} else {
						final File subItemFile = new File(parentSrc, subItem);
						sourceLastModi.remove(subItemFile);
						sourceFileLength.remove(subItemFile);
						sourceFileMD5.remove(subItemFile);

						targetSubItemFile.delete();
					}
					isChanged = true;
				}

			}
		}
		return isChanged;
	}

	/**
	 * 如果文件不存在或删除，则返回空串。
	 * 
	 * @param file
	 * @return
	 */
	private final static String getMD5(final File file) {
		return ResourceUtil.getMD5(file);
	}

	static final void copyToTarget(final File srcParent, final File targetParent) {
		final File[] lists = srcParent.listFiles();
		if (lists == null) {
			L.V = L.WShop ? false
					: LogManager
							.log("there is no thing in src (cp) : " + srcParent.getAbsolutePath() + ", isDir : " + srcParent.isDirectory());
			return;
		}

		targetParent.mkdirs();

		L.V = L.WShop ? false
				: LogManager.log("copy dir src : " + srcParent.getAbsolutePath() + ", target : " + targetParent.getAbsolutePath());

		for (int i = 0; i < lists.length; i++) {
			final File item = lists[i];
			final File targetItem = new File(targetParent, item.getName());
			if (item.isDirectory()) {
				copyToTarget(item, targetItem);
			} else {
				cp(item, targetItem);
			}
		}
	}

	static final String cp(final File src, final File target) {
		L.V = L.WShop ? false : LogManager.log("cp file src : " + src.getAbsolutePath() + ", target : " + target.getAbsolutePath());
		final String md5 = getMD5(src);
		if (ResourceUtil.copy(src, target) == false) {
			// 拷贝途中被删除源
			target.delete();
		}
		return md5;
	}

	private final String[] toNames(final File[] files) {
		final int length = files.length;
		final String[] names = new String[length];
		for (int i = 0; i < length; i++) {
			final File item = files[i];
			names[i] = item.getName();
		}
		return names;
	}

	private final void loadSourceFilesSnap(final File parent) {
		final File[] list = parent.listFiles();

		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				final File sub = list[i];
				if (sub.isDirectory()) {
					loadSourceFilesSnap(sub);
				} else {
					sourceLastModi.put(sub, sub.lastModified());
					sourceFileLength.put(sub, sub.length());
					sourceFileMD5.put(sub, getMD5(sub));
				}
			}
		}
	}

	private final void loadTargetFilesSnap(final File parent) {
		final File[] list = parent.listFiles();// 不用考虑目录不存在
		final String[] listNames = toNames(list);

		final Vector<File> fileVector = new Vector<File>(Arrays.asList(list));
		final Vector<String> nameVector = new Vector<String>(Arrays.asList(listNames));
		targetItems.put(parent, new SubItems(fileVector, nameVector));// 如果目录为空，则长度为0

		for (int i = 0; i < list.length; i++) {
			final File sub = list[i];
			if (sub.isDirectory()) {
				loadTargetFilesSnap(sub);
			}
		}
	}
}

class SubItems {
	public final Vector<File> subFiles;
	public final Vector<String> subNames;

	public SubItems(final Vector<File> fs, final Vector<String> ns) {
		this.subFiles = fs;
		this.subNames = ns;
	}
}
