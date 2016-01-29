package hc.server.ui.design.code;

public class TabBlock {
	public final int startIdx;
	public final int[] parameterBeginOffsetIdx;
	public final int[] parameterEndOffsetIdx;
	public int lastParameterIdxBeforeStack;
	
	public TabBlock(final int startIdx, final char[] methodAndParameter, final int countParameterNum){
		this.startIdx = startIdx;
		
		parameterBeginOffsetIdx = new int[countParameterNum];
		parameterEndOffsetIdx = new int[countParameterNum];
		
		final int size = methodAndParameter.length;
		
		int i = 0;
		while(methodAndParameter[i++] != '('){
		}

		int paramenterIdx = 0;
		int lastTagIdx = 0;
		boolean isNextParameter = true;
		for (;i < size; i++) {
			if(methodAndParameter[i] == ','){
				parameterEndOffsetIdx[paramenterIdx] = i - lastTagIdx;
				lastTagIdx = i;
				paramenterIdx++;
				isNextParameter = true;
				continue;
			}else if(methodAndParameter[i] == ' '){
				continue;
			}else if(methodAndParameter[i] == ')'){
				parameterEndOffsetIdx[paramenterIdx] = i - lastTagIdx;
				return;
			}else{
				if(isNextParameter){
					parameterBeginOffsetIdx[paramenterIdx] = i - lastTagIdx;
					lastTagIdx = i;
					isNextParameter = false;
				}
			}
		}
	}
	
	/**
	 * 如果没有参数，返回0
	 * @param methodAndParameter
	 * @return
	 */
	public static final int countParameterNum(final char[] methodAndParameter){
		int searchIdx = 0;
		final int size = methodAndParameter.length;
		
		while(searchIdx < size && methodAndParameter[searchIdx++] != '('){
		}

		//不是方法，是属性
		if(searchIdx == size){
			return 0;
		}
		
		if(methodAndParameter[searchIdx] == ')'){
			return 0;
		}
		
		int countParameterNum = 1;
		
		for (; searchIdx < size; searchIdx++) {
			if(methodAndParameter[searchIdx] == ','){
				countParameterNum++;
			}
		}
		
		return countParameterNum;
	}
}
