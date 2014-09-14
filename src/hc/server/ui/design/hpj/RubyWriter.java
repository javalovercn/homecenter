package hc.server.ui.design.hpj;

import java.io.IOException;
import java.io.Writer;

public class RubyWriter extends Writer{
	private static final int SIZE = 1024 * 1;
	final char[] bs = new char[SIZE];
	int writeIdx = 0;
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		if(writeIdx + len < SIZE){
			System.arraycopy(cbuf, off, bs, writeIdx, len);
			writeIdx += len;
		}
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