package hc.server.ui.design.code;

import hc.core.IConstant;
import hc.core.util.IntValue;
import hc.core.util.StringUtil;
import hc.server.data.StoreDirManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class CodeInvokeCounter {
	private static final String PRE_UN_INVOKE_COUNT = "un_invoke_count";
	static final String CLASS_UN_INVOKE_COUNT_DYN_STATIC = PRE_UN_INVOKE_COUNT + "DynStatic";
	static final String CLASS_UN_INVOKE_COUNT_STRUCT = PRE_UN_INVOKE_COUNT + "Struct";
	static final File invokeCounter = new File(StoreDirManager.CFG_DIR, "ide_member_usage.txt");
	static final String splitter = StringUtil.SPLIT_LEVEL_2_JING;

	final HashMap<String, HashMap<String, IntValue>> itemCounter = new HashMap<String, HashMap<String, IntValue>>(128);

	public final synchronized void addOne(final CodeItem item) {
		if (item.fmClass.startsWith(PRE_UN_INVOKE_COUNT, 0)) {
			return;
		}

		if ((++item.invokeCounter.value) == Integer.MAX_VALUE) {
			final Iterator<Entry<String, HashMap<String, IntValue>>> iter = itemCounter.entrySet().iterator();
			while (iter.hasNext()) {
				final Entry<String, HashMap<String, IntValue>> entry = iter.next();
				final HashMap<String, IntValue> methodMap = entry.getValue();

				final Iterator<Entry<String, IntValue>> methodIter = methodMap.entrySet().iterator();
				while (methodIter.hasNext()) {
					final Entry<String, IntValue> methodEntry = methodIter.next();
					final IntValue counter = methodEntry.getValue();
					if (counter.value > 1) {
						counter.value /= 2;
					}
				}
			}
		}
	}

	public final void initCounter(final ArrayList<CodeItem> list) {
		final int size = list.size();
		for (int i = 0; i < size; i++) {
			initCounter(list.get(i));
		}
	}

	public final synchronized void initCounter(final CodeItem item) {
		if (isRecordableItemType(item.type)) {
			item.invokeCounter = get(item.fmClass, item.codeDisplay);
		}
	}

	public final synchronized void loadLastSave() {
		try {
			final InputStream is = new FileInputStream(invokeCounter);
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, IConstant.UTF_8));
			String str = null;
			while (true) {
				str = reader.readLine();
				if (str != null) {
					final String[] items = StringUtil.splitToArray(str, splitter);
					if (items.length == 3) {
						try {
							final int counter = Integer.parseInt(items[2]);
							final String className = items[0];
							final String method = items[1];
							final IntValue counterIntArr = get(className, method);
							counterIntArr.value = counter;
						} catch (final Exception e) {
						}
					}
				} else {
					break;
				}
			}

			reader.close();
		} catch (final Exception e) {
			// e.printStackTrace();//新安装时，会产生此异常，故关闭
		}
	}

	private final IntValue get(final String className, final String method) {
		HashMap<String, IntValue> classMap = itemCounter.get(className);
		if (classMap == null) {
			classMap = new HashMap<String, IntValue>(64);
			itemCounter.put(className, classMap);
		}
		IntValue counterIntArr = classMap.get(method);
		if (counterIntArr == null) {
			counterIntArr = new IntValue();
			classMap.put(method, counterIntArr);
		}
		return counterIntArr;
	}

	public final synchronized void reset() {
		// save();
		// itemCounter.clear();
		// loadLastSave();
	}

	public final synchronized void save() {
		BufferedWriter writer = null;
		try {
			final OutputStream is = new FileOutputStream(invokeCounter);
			writer = new BufferedWriter(new OutputStreamWriter(is, IConstant.UTF_8));

			final Iterator<Entry<String, HashMap<String, IntValue>>> iter = itemCounter.entrySet().iterator();
			while (iter.hasNext()) {
				final Entry<String, HashMap<String, IntValue>> entry = iter.next();
				final String className = entry.getKey();
				final HashMap<String, IntValue> methodMap = entry.getValue();

				final Iterator<Entry<String, IntValue>> methodIter = methodMap.entrySet().iterator();
				while (methodIter.hasNext()) {
					final Entry<String, IntValue> methodEntry = methodIter.next();
					final IntValue counter = methodEntry.getValue();

					if (counter.value == 0) {
						continue;
					}

					final String methodName = methodEntry.getKey();

					writer.append(className);
					writer.append(splitter);
					writer.append(methodName);
					writer.append(splitter);
					writer.append(String.valueOf(counter.value));

					writer.newLine();
				}
			}
		} catch (final Throwable e) {
			e.printStackTrace();
		} finally {
			try {
				writer.flush();
				writer.close();
			} catch (final Exception e) {
			}
		}
	}

	public final boolean isRecordableItemType(final int itemType) {
		return itemType == CodeItem.TYPE_CLASS || itemType == CodeItem.TYPE_METHOD || itemType == CodeItem.TYPE_FIELD
				|| itemType == CodeItem.TYPE_CSS_VAR || itemType == CodeItem.TYPE_CSS;
	}
}
