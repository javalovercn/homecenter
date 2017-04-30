package hc.server;

import hc.core.util.Stack;
import hc.server.ui.design.hpj.HCScriptException;
import hc.util.StringBuilderCacher;

import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallContext {
	private static final Stack free = new Stack(8);
	private static final Pattern linePattern = Pattern.compile("<script>:(\\d+)");
	public final static String UN_KNOWN_TARGET = "unknown";

	public static CallContext getFree(){
		synchronized (free) {
			if(free.size() == 0){
				return new CallContext();
			}else{
				return (CallContext)free.pop();
			}
        }
	}
	
	public static void cycle(final CallContext callCtx){
		if(callCtx == null){
			return;
		}
		
		callCtx.reset();
		
		synchronized (free) {
			free.push(callCtx);
        }		
	}
	
	private final static String WARN = " warning:";
	
	private final String removeWarning(final String msg){
		//<script>:20 warning: singleton on non-persistent Java type #<Class:0x7f178b3c> (http://wiki.jruby.org/Persistence)
		//NameError: uninitialized constant JButton
		final int wIdx = msg.indexOf(WARN);
		if(wIdx < 0){
			return msg;
		}
		
		final int newLineIdx = msg.indexOf('\n');
		if(newLineIdx > 0 && newLineIdx > wIdx){
			return msg.substring(newLineIdx + 1);
		}
		
		return msg;
	}
	
	private final void buildInvocationErrMsg(final Throwable t, final StringBuilder sb){
//java.lang.reflect.InvocationTargetException
//	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
//	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
//	at java.lang.reflect.Method.invoke(Method.java:606)
//	at hc.util.ClassUtil.invokeWithExceptionOut(ClassUtil.java:172)
//	at hc.server.ui.design.engine.HCJRubyEngine.runScriptlet(HCJRubyEngine.java:123)
//	at hc.server.ui.design.engine.RubyExector.runAndWaitOnEngine(RubyExector.java:133)
//	at hc.server.ui.design.engine.RubyExector$1.run(RubyExector.java:61)
//	at hc.core.util.ThreadPool$1.run(ThreadPool.java:44)
//	at hc.core.util.RecycleThread.run(RecycleThread.java:27)
//	at java.lang.Thread.run(Thread.java:744)
//Caused by: org.jruby.embed.EvalFailedException: java.lang.NullPointerException
//	at org.jruby.embed.internal.EmbedEvalUnitImpl.run(EmbedEvalUnitImpl.java:137)
//	... 11 more
//Caused by: java.lang.NullPointerException
//	at hc.server.ui.Dialog.getMobileWidth(Dialog.java:605)
//	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
//	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
//	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
//	at java.lang.reflect.Method.invoke(Method.java:606)
//	at org.jruby.javasupport.JavaMethod.invokeDirectWithExceptionHandling(JavaMethod.java:440)
//	at org.jruby.javasupport.JavaMethod.invokeDirect(JavaMethod.java:304)
//	at org.jruby.java.invokers.InstanceMethodInvoker.call(InstanceMethodInvoker.java:52)
//	at java.lang.invoke.MethodHandle.invokeWithArguments(MethodHandle.java:599)
//	at org.jruby.runtime.invokedynamic.InvocationLinker.invocationFallback(InvocationLinker.java:132)
//	at $_lt_script_gt_.method__5$RUBY$initialize(<script>:47)
//	at $_lt_script_gt_$method__5$RUBY$initialize.call($_lt_script_gt_$method__5$RUBY$initialize)
//	at org.jruby.runtime.callsite.CachingCallSite.cacheAndCall(CachingCallSite.java:316)
//	at org.jruby.runtime.callsite.CachingCallSite.callBlock(CachingCallSite.java:145)
//	at org.jruby.runtime.callsite.CachingCallSite.call(CachingCallSite.java:149)
//	at org.jruby.RubyClass.newInstance(RubyClass.java:848)
//	at org.jruby.RubyClass$INVOKER$i$newInstance.call(RubyClass$INVOKER$i$newInstance.gen)
//	at org.jruby.internal.runtime.methods.JavaMethod$JavaMethodZeroOrNBlock.call(JavaMethod.java:261)
//	at org.jruby.java.proxies.ConcreteJavaProxy$3.call(ConcreteJavaProxy.java:141)
//	at java.lang.invoke.MethodHandle.invokeWithArguments(MethodHandle.java:599)
//	at org.jruby.runtime.invokedynamic.InvocationLinker.invocationFallback(InvocationLinker.java:132)
//	at $_lt_script_gt_.__file__(<script>:70)
//	at $_lt_script_gt_.__file__(<script>)
//	at org.jruby.ast.executable.AbstractScript.__file__(AbstractScript.java:38)
//	at org.jruby.Ruby.runScriptBody(Ruby.java:829)
//	at org.jruby.embed.internal.EmbedEvalUnitImpl.run(EmbedEvalUnitImpl.java:119)
//	... 11 more
		
		final StackTraceElement[] ste = t.getStackTrace();
		for (int i = 0; i < ste.length; i++) {
			final StackTraceElement one = ste[i];
			if(i > 0){
				sb.append("\n");
			}
			sb.append(one.toString());
		}
		
		final Throwable cause = t.getCause();
		if(cause != null){
			sb.append("\nCaused by : ");
			sb.append(cause.getMessage());
			sb.append("\n");
			
			buildInvocationErrMsg(cause, sb);
		}
	}
	
	public final void setError(String errMsg, final String script, Throwable t){
		isError = true;
		
		boolean isInvocationException = false;
		while(t instanceof InvocationTargetException){
			isInvocationException = true;
			t = ((InvocationTargetException)t).getTargetException();
		}
		if(isInvocationException){
//			if(L.isInWorkshop){
//				ResourceUtil.printStackTrace(t);
//			}
			final StringBuilder sb = StringBuilderCacher.getFree();
			sb.append(t.getMessage());
			sb.append("\n");
			buildInvocationErrMsg(t, sb);
			errMsg = sb.toString();
			StringBuilderCacher.cycle(sb);
		}
		
		jrubyErrMessage = removeWarning(errMsg);

		rubyScripts = script;
		rubyThrowable = t;
	}
	
	public final String getMessage(){
		proc();
		return sexception.getMessage();
	}
	
	public final int getLineNumber(){
		proc();
		return sexception.getLineNumber();
	}
	
	private final void proc(){
		if(isProccessed){
			return;
		}
		isProccessed = true;
		
		final char[] chars = jrubyErrMessage.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if(chars[i] == '\n'){
				sexception.setMessage(new String(chars, 0, i));
				break;
			}
		}
		
		final Matcher matcher = linePattern.matcher(jrubyErrMessage);
		if(matcher.find()){
			final String group = matcher.group(1);
			sexception.setLineNumber(Integer.parseInt(group));
		}
	}
	
	public final void reset(){
		isError = false;
		isProccessed = false;
		targetURL = null;
		jrubyErrMessage = null;
		rubyThrowable = null;
		rubyScripts = null;
		sexception.reset();
	}
	
	private boolean isProccessed;
	public boolean isError;
	public String targetURL;
	private String jrubyErrMessage;
	public Throwable rubyThrowable;
	public String rubyScripts;
	private final HCScriptException sexception = new HCScriptException("");

}
