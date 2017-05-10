package hc.server.ui.design.hpj;

import javax.swing.text.AbstractDocument;

import org.jrubyparser.ast.Node;

public class InstanceVariableManager {
	/**
	 * 返回如"@abc"
	 * @param lineChars
	 * @param lineIdx
	 * @return
	 */
	public static String getCurrentEditingVar(final char[] lineChars, final int lineIdx){
		final int instanceVarStartIdx = isEditingVar(lineChars, lineIdx);
        if(instanceVarStartIdx >= 0){
        	int instanceVarEndIdx = lineIdx;
        	for (; instanceVarEndIdx <= lineChars.length; instanceVarEndIdx++) {
				final char c = lineChars[instanceVarEndIdx];
				if(isVariableChar(c)){
					continue;
				}else{
					break;
				}
			}
        	
        	return String.valueOf(lineChars, instanceVarStartIdx, instanceVarEndIdx - instanceVarStartIdx);
        }
        
        return null;
	}
	
	public static void replaceVariable(final String from, final String to, final AbstractDocument doc, 
			final Node root, final int scriptIdx){
//		if(from.length() > 0 && from.charAt(0) == '@'){
//    		
//    	}else{
//    		
//    	}
	}
	
	private static int isEditingVar(final char[] lineChars, final int lineIdx){
		for (int i = lineIdx; i >= 0; i--) {
			final char c = lineChars[i];
			if(isVariableChar(c)){
				continue;
			}
			
			if(c == '@'){
				return i;
			}
		}
		
		return -1;
	}
	
	public static boolean isVariableChar(final char oneChar){
		if((oneChar >= '0' && oneChar <= '9') 
				|| (oneChar >= 'a' && oneChar <= 'z') 
				|| (oneChar >='A' && oneChar <= 'Z') 
				|| oneChar == '_'){
			return true;
		}
		
		return false;
	}
}
