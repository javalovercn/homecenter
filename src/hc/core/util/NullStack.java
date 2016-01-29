package hc.core.util;

public class NullStack extends Stack {
	public final Object pop(){
		if(elementCount == 0){
			return null;
		}else{
			Object obj = elementData[--elementCount];
			elementData[elementCount] = null;
			return obj;
		}
	}
}
