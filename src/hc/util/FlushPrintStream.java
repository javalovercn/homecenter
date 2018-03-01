package hc.util;

import hc.core.util.ILog;

import java.io.PrintStream;
import java.util.Locale;

public class FlushPrintStream extends PrintStream {
	ILog ilog;

	public FlushPrintStream(PrintStream ps, ILog ilog) {
		super(ps, true);
		this.ilog = ilog;
	}

	@Override
	public void write(int b) {
		ilog.flush();
		super.write(b);
	}

	public void write(byte buf[], int off, int len) {
		ilog.flush();
		super.write(buf, off, len);
	}

	public void print(boolean b) {
		ilog.flush();
		super.print(b);
	}

	public void print(char c) {
		ilog.flush();
		super.print(c);
	}

	public void print(int i) {
		ilog.flush();
		super.print(i);
	}

	public void print(long l) {
		ilog.flush();
		super.print(l);
	}

	public void print(float f) {
		ilog.flush();
		super.print(f);
	}

	public void print(double d) {
		ilog.flush();
		super.print(d);
	}

	public void print(char s[]) {
		ilog.flush();
		super.print(s);
	}

	public void print(String s) {
		ilog.flush();
		super.print(s);
	}

	public void print(Object obj) {
		ilog.flush();
		super.print(obj);
	}

	public void println() {
		ilog.flush();
		super.println();
	}

	public void println(boolean x) {
		ilog.flush();
		super.println(x);
	}

	public void println(char x) {
		ilog.flush();
		super.println(x);
	}

	public void println(int x) {
		ilog.flush();
		super.println(x);
	}

	public void println(long x) {
		ilog.flush();
		super.println(x);
	}

	public void println(float x) {
		ilog.flush();
		super.println(x);
	}

	public void println(double x) {
		ilog.flush();
		super.println(x);
	}

	public void println(char x[]) {
		ilog.flush();
		super.println(x);
	}

	public void println(String x) {
		ilog.flush();
		super.println(x);
	}

	public void println(Object x) {
		ilog.flush();
		super.println(x);
	}

	public PrintStream printf(String format, Object... args) {
		ilog.flush();
		super.printf(format, args);
		return this;
	}

	public PrintStream printf(Locale l, String format, Object... args) {
		ilog.flush();
		super.printf(l, format, args);
		return this;
	}

	public PrintStream format(String format, Object... args) {
		ilog.flush();
		super.format(format, args);
		return this;
	}

	public PrintStream format(Locale l, String format, Object... args) {
		ilog.flush();
		super.format(l, format, args);
		return this;
	}

	public PrintStream append(CharSequence csq) {
		ilog.flush();
		super.append(csq);
		return this;
	}

	public PrintStream append(CharSequence csq, int start, int end) {
		ilog.flush();
		super.append(csq, start, end);
		return this;
	}

	public PrintStream append(char c) {
		ilog.flush();
		super.append(c);
		return this;
	}
}
