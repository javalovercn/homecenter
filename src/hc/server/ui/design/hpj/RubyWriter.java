package hc.server.ui.design.hpj;

import hc.core.util.LogManager;
import hc.core.util.StringUtil;

import java.io.IOException;
import java.io.Writer;

public class RubyWriter extends Writer{
	//file:/Users/homecenter/Documents/eclipse_workspace/homecenter/test_run/./jruby.jar!/jruby/java/core_ext/object.rb:73 warning: already initialized constant String
	private static final char[] warning = " warning: ".toCharArray();
	
	private static final int SIZE = 1024 * 1;
	char[] bs = new char[SIZE];
	int writeIdx = 0;
	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {
		if(StringUtil.indexOf(cbuf, off, len, warning, 0, warning.length, 0) >= 0){
			LogManager.warning(new String(cbuf, off, len));
			return;
		}
		
		if(writeIdx + len >= SIZE){
			final char[] newbs = new char[bs.length * 2];
			System.arraycopy(bs, 0, newbs, 0, bs.length);
			bs = newbs;
		}
		System.arraycopy(cbuf, off, bs, writeIdx, len);
		writeIdx += len;
	}
	
	@Override
	public void flush() throws IOException {
	}
	
	@Override
	public void close() throws IOException {
	}
	
	public void reset(){
		writeIdx = 0;
	}

	public String getMessage(){
		if(writeIdx == 0){
			return "";
		}else{
			return new String(bs, 0, writeIdx);
		}
	}
}