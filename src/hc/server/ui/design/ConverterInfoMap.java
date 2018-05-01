package hc.server.ui.design;

import java.util.HashMap;

import hc.server.msb.ConverterInfo;

public class ConverterInfoMap {
	private final HashMap<Integer, ConverterInfo> converterInfoMap = new HashMap<Integer, ConverterInfo>();
	private final HashMap<Integer, ConverterAndExt> converterMap = new HashMap<Integer, ConverterAndExt>();
	
	public final ConverterInfo getConverterInfo(final int hashCode) {
		return converterInfoMap.get(hashCode);
	}
	
	public final ConverterAndExt getConverterAndExt(final int hashCode) {
		return converterMap.get(hashCode);
	}
	
	public final void addConverterInfo(final ConverterAndExt converter, final ConverterInfo ci) {
		final int hashCode = converter.converter.hashCode();
		ci.hashCodeForConverter = hashCode;
		converterInfoMap.put(hashCode, ci);
		converterMap.put(hashCode, converter);
	}
}
