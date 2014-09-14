package hc.util;

import hc.core.util.Stack;
import hc.server.AbstractDelayBiz;

/**
 * 添加执行逻辑于重新连接之前
 * @author homecenter
 *
 */
public class ConnectionManager {
	private static Stack delayBiz = null;
	
	public static void addBeforeConnectionBiz(AbstractDelayBiz biz){
		if(delayBiz == null){
			delayBiz = new Stack();
		}
		delayBiz.push(biz);
	}
	
	public static void startBeforeConnectBiz(){
		if(delayBiz != null){
			AbstractDelayBiz biz;
			do{
				biz = (AbstractDelayBiz)delayBiz.pop();
				if(biz != null){
					try{
						biz.doBiz();
					}catch (Throwable e) {
					}
				}
			}while(biz != null);
		}
	}
}
