package hc.server.ui.design.code;

import java.util.ArrayList;
import java.util.List;

public class CodeStaticHelper {
	protected final static String[] J2SE_CLASS_SET = convertArray(J2SEClassList.getList());
	protected final static int J2SE_CLASS_SET_SIZE = J2SE_CLASS_SET.length;
	
	protected final static String[] J2SE_PACKAGE_SET = buildPackageSet(J2SE_CLASS_SET, J2SE_CLASS_SET_SIZE);
	protected final static int J2SE_PACKAGE_SET_SIZE = J2SE_PACKAGE_SET.length;
	
	protected final static String[] HC_CLASS_SET = convertArray(J2SEClassList.getAPIList());
	protected final static int HC_CLASS_SET_SIZE = HC_CLASS_SET.length;
	
	protected final static String[] HC_PACKAGE_SET = buildPackageSet(HC_CLASS_SET, HC_CLASS_SET_SIZE);
	protected final static int HC_PACKAGE_SET_SIZE = HC_PACKAGE_SET.length;
	
	
	public static final void doNothing(){
	}
	
	protected final static String[] convertArray(final List<String> list){
		final int size = list.size();
		final String[] out = new String[size];
		
		for (int i = 0; i < size; i++) {
			out[i] = list.get(i);
		}
		
		return out;
	}

	private final static String[] buildPackageSet(final String[] set, final int size){
		final ArrayList<String> pkg = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			final String item = set[i];
			final String[] splits = item.split("\\.");
			final int splitSize = splits.length - 1;
			
			String appendPkg = "";
			for (int j = 0; j < splitSize; j++) {
				if(appendPkg.length() > 0){
					appendPkg += ".";
				}
				appendPkg += splits[j];
				if(pkg.contains(appendPkg) == false){
					pkg.add(appendPkg);
				}
			}
		}
		
		final int pkgSize = pkg.size();
		final String[] out = new String[pkgSize];
		for (int i = 0; i < pkgSize; i++) {
			out[i] = pkg.get(i);
		}
		
		return out;
	}
	
}
