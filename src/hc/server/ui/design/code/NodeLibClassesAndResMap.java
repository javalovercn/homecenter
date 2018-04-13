package hc.server.ui.design.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import hc.core.util.BooleanValue;
import hc.server.ui.design.hpj.HPNode;

public class NodeLibClassesAndResMap {
	private final Vector<HPNode> vectorKey = new Vector<HPNode>();
	private final Vector<String[]> vectorValue = new Vector<String[]>();
	private final Vector<ClassImporter> vectorClassImporter = new Vector<ClassImporter>();
	
	public final void reset() {
		vectorKey.clear();
		vectorValue.clear();
		vectorClassImporter.clear();
	}
	
	public final void appendClassImport(final CodeWindow windowMaybeNull, 
			final String preCodeLower, final char[] preCodeLowerChars, final int preCodeLowerCharsLen, 
			final ArrayList<CodeItem> vector, final HashSet<String> excludeImports) {
		final BooleanValue isLoadedThirdLibsForDoc = windowMaybeNull.codeHelper.isLoadedThirdLibsForDoc;
		if(isLoadedThirdLibsForDoc.value == false) {
			synchronized (isLoadedThirdLibsForDoc) {
				if(isLoadedThirdLibsForDoc.value == false) {
					try {
						isLoadedThirdLibsForDoc.wait();
					} catch (final InterruptedException e) {
					}
				}
			}
		}
		final int size = vectorClassImporter.size();
		for (int i = 0; i < size; i++) {
			vectorClassImporter.elementAt(i).appendClassImport(windowMaybeNull, preCodeLower, preCodeLowerChars, preCodeLowerCharsLen, vector, excludeImports);
		}
	}

	public final String[] searchLibClassesAndRes(final String libName) {
		final int size = vectorKey.size();
		for (int i = 0; i < size; i++) {
			if (getLibName(i).equals(libName)) {
				return vectorValue.get(i);
			}
		}
		return null;
	}

	private final String getLibName(final int i) {
		return vectorKey.get(i).name;
	}

	/**
	 * 注意：对于JRuby脚本节点，res为null
	 * @param node 有可能为JRuby节点，或jar节点
	 * @param res
	 */
	public final void put(final HPNode node, String[] res) {
		vectorKey.add(node);
		
		if(res == null) {
			res = new String[0];
		}
		vectorValue.add(res);
		
		final int totalSize = res.length;
		final HashMap<String, ClassImportItem> map = new HashMap<String, ClassImportItem>(totalSize);
		final Vector<String> keyList = new Vector<String>(totalSize);
		
		ClassImporter.loadSet(map, keyList, res, totalSize, true);
		vectorClassImporter.add(new ClassImporter(map, keyList, getLibName(vectorKey.size() - 1)));
	}
	
	public final void remove(final HPNode node) {
		final int size = vectorKey.size();
		for (int i = 0; i < size; i++) {
			if (vectorKey.get(i) == node) {
				vectorKey.remove(i);
				vectorValue.remove(i);
				vectorClassImporter.remove(i);
				return;
			}
		}
	}

	public final String[] getLibNames() {
		final int size = vectorKey.size();
		final String[] keyStrs = new String[size];

		for (int i = 0; i < size; i++) {
			keyStrs[i] = getLibName(i);
		}

		return keyStrs;
	}
}
