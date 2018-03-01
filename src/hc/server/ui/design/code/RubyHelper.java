package hc.server.ui.design.code;

import hc.core.IConstant;
import hc.util.ResourceUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Vector;
import java.util.regex.Pattern;

public class RubyHelper {
	static CodeHelper codeHelper;

	public static final String STRING = "String";
	public static final String ARRAY = "Array";
	public static final String MATCH_DATA = "MatchData";
	public static final String TRUECLASS = "TrueClass";
	public static final String COMPLEX = "Complex";
	public static final String STRUCT = "Struct";
	public static final String TIME = "Time";
	public static final String ENUMERATOR = "Enumerator";
	public static final String THREAD = "Thread";
	public static final String ENUMERABLE = "Enumerable";
	public static final String RANGE = "Range";
	public static final String RANDOM = "Random";
	public static final String PROC = "Proc";
	public static final String OBJECT = "Object";
	public static final String NUMERIC = "Numeric";
	public static final String NIL_CLASS = "NilClass";
	public static final String MUTEX = "Mutex";
	public static final String MODULE = "Module";
	public static final String METHOD = "Method";
	public static final String KERNEL = "Kernel";
	public static final String MATH = "Math";
	public static final String MARSHAL = "Marshal";
	public static final String IO = "IO";
	public static final String INTEGER = "Integer";
	public static final String RATIONAL = "Rational";
	public static final String REGEXP = "Regexp";
	public static final String HASH = "Hash";
	public static final String DIR = "Dir";
	public static final String FLOAT = "Float";
	public static final String FIXNUM = "Fixnum";
	public static final String CLASS = "Class";
	public static final String FILE = "File";
	public static final String EXCEPTION = "Exception";
	public static final String BIGNUM = "Bignum";
	public static final String BASIC_OBJECT = "BasicObject";
	public static final String SYMBOL = "Symbol";
	public static final String SIGNAL = "Signal";
	public static final String QUEUE = "Queue";
	public static final String PROCESS = "Process";
	public static final String CONDITIONVARIABLE = "ConditionVariable";
	public static final String ENCODING = "Encoding";
	public static final String TRACEPOINT = "TracePoint";
	public static final String THREADGROUP = "ThreadGroup";
	public static final String SIZEDQUEUE = "SizedQueue";
	public static final String STANDARDERROR = "StandardError";
	public static final String TYPEERROR = "TypeError";
	public static final String ARGUMENTERROR = "ArgumentError";
	public static final String IOERROR = "IOError";
	public static final String EOFERROR = "EOFError";
	public static final String ENCODINGERROR = "EncodingError";
	public static final String FIBERERROR = "FiberError";
	public static final String RANGEERROR = "RangeError";
	public static final String FLOATDOMAINERROR = "FloatDomainError";
	public static final String INDEXERROR = "IndexError";
	public static final String KEYERROR = "KeyError";
	public static final String SCRIPTERROR = "ScriptError";
	public static final String LOADERROR = "LoadError";
	public static final String LOCALJUMPERROR = "LocalJumpError";
	public static final String NAMEERROR = "NameError";
	public static final String NOMEMORYERROR = "NoMemoryError";
	public static final String NOMETHODERROR = "NoMethodError";
	public static final String NOTIMPLEMENTEDERROR = "NotImplementedError";
	public static final String REGEXPERROR = "RegexpError";
	public static final String RUNTIMEERROR = "RuntimeError";
	public static final String SECURITYERROR = "SecurityError";
	public static final String SIGNALEXCEPTION = "SignalException";
	public static final String SYNTAXERROR = "SyntaxError";
	public static final String SYSTEMCALLERROR = "SystemCallError";
	public static final String SYSTEMSTACKERROR = "SystemStackError";
	public static final String THREADERROR = "ThreadError";
	public static final String UNCAUGHTTHROWERROR = "UncaughtThrowError";
	public static final String ZERODIVISIONERROR = "ZeroDivisionError";

	public static final String ANONYMOUS_CLASS_BASE_NAME = "";

	static final String RUBY_STATIC_MEMBER_DOC = "_ruby_static_doc_";
	static final Class JRUBY_STRUCT_CLASS = JRubyStruct.class;

	static final Class JRUBY_ZERODIVISIONERROR_CLASS = JRubyZeroDivisionError.class;
	static final Class JRUBY_UNCAUGHTTHROWERROR_CLASS = JRubyUncaughtThrowError.class;
	static final Class JRUBY_THREADERROR_CLASS = JRubyThreadError.class;
	static final Class JRUBY_SYSTEMSTACKERROR_CLASS = JRubySystemStackError.class;
	static final Class JRUBY_SYSTEMCALLERROR_CLASS = JRubySystemCallError.class;
	static final Class JRUBY_SYNTAXERROR_CLASS = JRubySyntaxError.class;
	static final Class JRUBY_SIGNALEXCEPTION_CLASS = JRubySignalException.class;
	static final Class JRUBY_SECURITYERROR_CLASS = JRubySecurityError.class;
	static final Class JRUBY_RUNTIMEERROR_CLASS = JRubyRuntimeError.class;
	static final Class JRUBY_REGEXPERROR_CLASS = JRubyRegexpError.class;
	static final Class JRUBY_NOTIMPLEMENTEDERROR_CLASS = JRubyNotImplementedError.class;
	static final Class JRUBY_NOMETHODERROR_CLASS = JRubyNoMethodError.class;
	static final Class JRUBY_NOMEMORYERROR_CLASS = JRubyNoMemoryError.class;
	static final Class JRUBY_NAMEERROR_CLASS = JRubyNameError.class;
	static final Class JRUBY_LOCALJUMPERROR_CLASS = JRubyLocalJumpError.class;
	static final Class JRUBY_LOADERROR_CLASS = JRubyLoadError.class;
	static final Class JRUBY_SCRIPTERROR_CLASS = JRubyScriptError.class;
	static final Class JRUBY_KEYERROR_CLASS = JRubyKeyError.class;
	static final Class JRUBY_INDEXERROR_CLASS = JRubyIndexError.class;
	static final Class JRUBY_FLOATDOMAINERROR_CLASS = JRubyFloatDomainError.class;
	static final Class JRUBY_RANGEERROR_CLASS = JRubyRangeError.class;
	static final Class JRUBY_FIBERERROR_CLASS = JRubyFiberError.class;
	static final Class JRUBY_ENCODINGERROR_CLASS = JRubyEncodingError.class;
	static final Class JRUBY_EOFERROR_CLASS = JRubyEOFError.class;
	static final Class JRUBY_IOERROR_CLASS = JRubyIOError.class;
	static final Class JRUBY_ARGUMENTERROR_CLASS = JRubyArgumentError.class;
	static final Class JRUBY_TYPEERROR_CLASS = JRubyTypeError.class;
	static final Class JRUBY_STANDARDERROR_CLASS = JRubyStandardError.class;
	static final Class JRUBY_ENUMERATOR_CLASS = JRubyEnumerator.class;
	static final Class JRUBY_ENUMERABLE_CLASS = JRubyEnumerable.class;
	static final Class JRUBY_STRING_CLASS = JRubyString.class;
	static final Class JRUBY_SYMBOL_CLASS = JRubySymbol.class;
	final static Class JRUBY_OBJECT_CLASS = JRubyObject.class;
	static final Class JRUBY_ARRAY_CLASS = JRubyArray.class;// buildJRubyArrayClass();
	static final Class JRUBY_ANONYMOUS_CLASS = JRubyAnonymous.class;
	static final Class JRUBY_TRACEPOINT_CLASS = JRubyTracePoint.class;
	static final Class JRUBY_THREADGROUP_CLASS = JRubyThreadGroup.class;

	static final Class JRUBY_EXCEPTION_CLASS = JRubyException.class;
	public static final Class JRUBY_CLASS_CLASS = JRubyClass.class;
	public static final Class JRUBY_TRUE_CLASS = JRubyTrue.class;
	static final Class JRUBY_ENCODING_CLASS = JRubyEncoding.class;
	static final Class JRUBY_FALSE_CLASS = JRubyFalse.class;
	static final Class JRUBY_KERNEL_CLASS = JRubyKernel.class;
	static final Class JRUBY_COMPLEX_CLASS = JRubyComplex.class;
	static final Class JRUBY_IO_CLASS = JRubyIO.class;
	static final Class JRUBY_RANGE_CLASS = JRubyRange.class;
	static final Class JRUBY_RANDOM_CLASS = JRubyRandom.class;
	static final Class JRUBY_RATIONAL_CLASS = JRubyRational.class;
	static final Class JRUBY_PROC_CLASS = JRubyProc.class;
	static final Class JRUBY_NUMERIC_CLASS = JRubyNumeric.class;
	static final Class JRUBY_NILCLASS_CLASS = JRubyNilClass.class;
	static final Class JRUBY_MUTEX_CLASS = JRubyMutex.class;
	static final Class JRUBY_MODULE_CLASS = JRubyModule.class;
	static final Class JRUBY_METHOD_CLASS = JRubyMethod.class;
	static final Class JRUBY_INTEGER_CLASS = JRubyInteger.class;
	static final Class JRUBY_FLOAT_CLASS = JRubyFloat.class;
	static final Class JRUBY_FIXNUM_CLASS = JRubyFixnum.class;
	static final Class JRUBY_BIGNUM_CLASS = JRubyBignum.class;
	static final Class JRUBY_BASICOBJECT_CLASS = JRubyBasicObject.class;
	static final Class JRUBY_REGEXP_CLASS = JRubyRegexp.class;
	static final Class JRUBY_TIME_CLASS = JRubyTime.class;
	static final Class JRUBY_MATH_CLASS = JRubyMath.class;
	static final Class JRUBY_MATCHDATA_CLASS = JRubyMatchData.class;
	static final Class JRUBY_HASH_CLASS = JRubyHash.class;
	static final Class JRUBY_FILE_CLASS = JRubyFile.class;
	static final Class JRUBY_DIR_CLASS = JRubyDir.class;
	static final Class JRUBY_MARSHAL_CLASS = JRubyMarshal.class;
	static final Class JRUBY_THREAD_CLASS = JRubyThread.class;
	static final Class JRUBY_SIGNAL_CLASS = JRubySignal.class;
	static final Class JRUBY_QUEUE_CLASS = JRubyQueue.class;
	static final Class JRUBY_SIZEDQUEUE_CLASS = JRubySizedQueue.class;
	static final Class JRUBY_PROCESS_CLASS = JRubyProcess.class;
	static final Class JRUBY_CONDITIONVARIABLE_CLASS = JRubyConditionVariable.class;

	static final String[] JRUBY_STR_MAP = { ARRAY, BASIC_OBJECT, BIGNUM, CLASS, COMPLEX,
			CONDITIONVARIABLE, ENCODING, ENUMERATOR, EXCEPTION, FILE, FIXNUM, FLOAT, DIR, HASH,
			INTEGER, IO, MARSHAL, MATH, METHOD, MODULE, MUTEX, NIL_CLASS, NUMERIC, OBJECT, PROC,
			PROCESS, QUEUE, RANDOM, RANGE, REGEXP, SIGNAL, SIZEDQUEUE, SYMBOL, THREAD, THREADGROUP,
			TIME, TRACEPOINT, STRUCT, STANDARDERROR, ARGUMENTERROR, IOERROR, EOFERROR,
			ENCODINGERROR, FIBERERROR, RANGEERROR, FLOATDOMAINERROR, INDEXERROR, KEYERROR,
			SCRIPTERROR, LOADERROR, LOCALJUMPERROR, NAMEERROR, NOMEMORYERROR, NOMETHODERROR,
			NOTIMPLEMENTEDERROR, REGEXPERROR, RUNTIMEERROR, SECURITYERROR, SIGNALEXCEPTION, STRING,
			SYNTAXERROR, SYSTEMCALLERROR, SYSTEMSTACKERROR, THREADERROR, UNCAUGHTTHROWERROR,
			ZERODIVISIONERROR, TYPEERROR };
	static final Class[] JRUBY_CLASS_MAP = { JRUBY_ARRAY_CLASS, JRUBY_BASICOBJECT_CLASS,
			JRUBY_BIGNUM_CLASS, JRUBY_CLASS_CLASS, JRUBY_COMPLEX_CLASS,
			JRUBY_CONDITIONVARIABLE_CLASS, JRUBY_ENCODING_CLASS, JRUBY_ENUMERATOR_CLASS,
			JRUBY_EXCEPTION_CLASS, JRUBY_FILE_CLASS, JRUBY_FIXNUM_CLASS, JRUBY_FLOAT_CLASS,
			JRUBY_DIR_CLASS, JRUBY_HASH_CLASS, JRUBY_INTEGER_CLASS, JRUBY_IO_CLASS,
			JRUBY_MARSHAL_CLASS, JRUBY_MATH_CLASS, JRUBY_METHOD_CLASS, JRUBY_MODULE_CLASS,
			JRUBY_MUTEX_CLASS, JRUBY_NILCLASS_CLASS, JRUBY_NUMERIC_CLASS, JRUBY_OBJECT_CLASS,
			JRUBY_PROC_CLASS, JRUBY_PROCESS_CLASS, JRUBY_QUEUE_CLASS, JRUBY_RANDOM_CLASS,
			JRUBY_RANGE_CLASS, JRUBY_REGEXP_CLASS, JRUBY_SIGNAL_CLASS, JRUBY_SIZEDQUEUE_CLASS,
			JRUBY_SYMBOL_CLASS, JRUBY_THREAD_CLASS, JRUBY_THREADGROUP_CLASS, JRUBY_TIME_CLASS,
			JRUBY_TRACEPOINT_CLASS, JRUBY_STRUCT_CLASS, JRUBY_STANDARDERROR_CLASS,
			JRUBY_ARGUMENTERROR_CLASS, JRUBY_IOERROR_CLASS, JRUBY_EOFERROR_CLASS,
			JRUBY_ENCODINGERROR_CLASS, JRUBY_FIBERERROR_CLASS, JRUBY_RANGEERROR_CLASS,
			JRUBY_FLOATDOMAINERROR_CLASS, JRUBY_INDEXERROR_CLASS, JRUBY_KEYERROR_CLASS,
			JRUBY_SCRIPTERROR_CLASS, JRUBY_LOADERROR_CLASS, JRUBY_LOCALJUMPERROR_CLASS,
			JRUBY_NAMEERROR_CLASS, JRUBY_NOMEMORYERROR_CLASS, JRUBY_NOMETHODERROR_CLASS,
			JRUBY_NOTIMPLEMENTEDERROR_CLASS, JRUBY_REGEXPERROR_CLASS, JRUBY_RUNTIMEERROR_CLASS,
			JRUBY_SECURITYERROR_CLASS, JRUBY_SIGNALEXCEPTION_CLASS, JRUBY_STRING_CLASS,
			JRUBY_SYNTAXERROR_CLASS, JRUBY_SYSTEMCALLERROR_CLASS, JRUBY_SYSTEMSTACKERROR_CLASS,
			JRUBY_THREADERROR_CLASS, JRUBY_UNCAUGHTTHROWERROR_CLASS, JRUBY_ZERODIVISIONERROR_CLASS,
			JRUBY_TYPEERROR_CLASS };

	static final RubyClassAndDoc[] JRUBY_CLASS = { new RubyClassAndDoc(JRUBY_STRING_CLASS, STRING),
			new RubyClassAndDoc(JRUBY_ARRAY_CLASS, ARRAY, "Ruby[]", new String[] { "to_java()" },
					null),
			new RubyClassAndDoc(JRUBY_BASICOBJECT_CLASS, BASIC_OBJECT),
			new RubyClassAndDoc(JRUBY_BIGNUM_CLASS, BIGNUM),
			new RubyClassAndDoc(JRUBY_RATIONAL_CLASS, RATIONAL),
			new RubyClassAndDoc(JRUBY_CLASS_CLASS, CLASS),
			new RubyClassAndDoc(JRUBY_COMPLEX_CLASS, COMPLEX),
			new RubyClassAndDoc(JRUBY_CONDITIONVARIABLE_CLASS, CONDITIONVARIABLE),
			new RubyClassAndDoc(JRUBY_ENCODING_CLASS, ENCODING),
			new RubyClassAndDoc(JRUBY_ENUMERATOR_CLASS, ENUMERATOR),
			new RubyClassAndDoc(JRUBY_ENUMERABLE_CLASS, ENUMERABLE),
			new RubyClassAndDoc(JRUBY_EXCEPTION_CLASS, EXCEPTION),
			new RubyClassAndDoc(JRUBY_FALSE_CLASS, "FalseClass"),
			new RubyClassAndDoc(JRUBY_FIXNUM_CLASS, FIXNUM),
			new RubyClassAndDoc(JRUBY_FLOAT_CLASS, FLOAT),
			new RubyClassAndDoc(JRUBY_FILE_CLASS, FILE), new RubyClassAndDoc(JRUBY_DIR_CLASS, DIR),
			new RubyClassAndDoc(JRUBY_HASH_CLASS, HASH),
			new RubyClassAndDoc(JRUBY_INTEGER_CLASS, INTEGER),
			new RubyClassAndDoc(JRUBY_IO_CLASS, IO),
			new RubyClassAndDoc(JRUBY_KERNEL_CLASS, KERNEL),
			new RubyClassAndDoc(JRUBY_MATCHDATA_CLASS, MATCH_DATA),
			new RubyClassAndDoc(JRUBY_MARSHAL_CLASS, MARSHAL),
			new RubyClassAndDoc(JRUBY_MATH_CLASS, MATH),
			new RubyClassAndDoc(JRUBY_METHOD_CLASS, METHOD),
			new RubyClassAndDoc(JRUBY_MODULE_CLASS, MODULE),
			new RubyClassAndDoc(JRUBY_MUTEX_CLASS, MUTEX),
			new RubyClassAndDoc(JRUBY_NILCLASS_CLASS, NIL_CLASS),
			new RubyClassAndDoc(JRUBY_NUMERIC_CLASS, NUMERIC),
			new RubyClassAndDoc(JRUBY_OBJECT_CLASS, OBJECT),
			new RubyClassAndDoc(JRUBY_PROC_CLASS, PROC),
			new RubyClassAndDoc(JRUBY_PROCESS_CLASS, PROCESS),
			new RubyClassAndDoc(JRUBY_QUEUE_CLASS, QUEUE),
			new RubyClassAndDoc(JRUBY_RANDOM_CLASS, RANDOM),
			new RubyClassAndDoc(JRUBY_RANGE_CLASS, RANGE),
			new RubyClassAndDoc(JRUBY_REGEXP_CLASS, REGEXP),

			new RubyClassAndDoc(JRUBY_SIGNAL_CLASS, SIGNAL),
			new RubyClassAndDoc(JRUBY_SIZEDQUEUE_CLASS, SIZEDQUEUE),
			new RubyClassAndDoc(JRUBY_SYMBOL_CLASS, SYMBOL),
			new RubyClassAndDoc(JRUBY_THREAD_CLASS, THREAD),
			new RubyClassAndDoc(JRUBY_THREADGROUP_CLASS, THREADGROUP),
			new RubyClassAndDoc(JRUBY_TIME_CLASS, TIME),
			new RubyClassAndDoc(JRUBY_TRUE_CLASS, TRUECLASS),
			new RubyClassAndDoc(JRUBY_TRACEPOINT_CLASS, TRACEPOINT),
			new RubyClassAndDoc(JRUBY_STRUCT_CLASS, STRUCT),

			new RubyClassAndDoc(JRUBY_ZERODIVISIONERROR_CLASS, ZERODIVISIONERROR),
			new RubyClassAndDoc(JRUBY_UNCAUGHTTHROWERROR_CLASS, UNCAUGHTTHROWERROR),
			new RubyClassAndDoc(JRUBY_THREADERROR_CLASS, THREADERROR),
			new RubyClassAndDoc(JRUBY_SYSTEMSTACKERROR_CLASS, SYSTEMSTACKERROR),
			new RubyClassAndDoc(JRUBY_SYSTEMCALLERROR_CLASS, SYSTEMCALLERROR),
			new RubyClassAndDoc(JRUBY_SYNTAXERROR_CLASS, SYNTAXERROR),
			new RubyClassAndDoc(JRUBY_SIGNALEXCEPTION_CLASS, SIGNALEXCEPTION),
			new RubyClassAndDoc(JRUBY_SECURITYERROR_CLASS, SECURITYERROR),
			new RubyClassAndDoc(JRUBY_RUNTIMEERROR_CLASS, RUNTIMEERROR),
			new RubyClassAndDoc(JRUBY_REGEXPERROR_CLASS, REGEXPERROR),
			new RubyClassAndDoc(JRUBY_NOTIMPLEMENTEDERROR_CLASS, NOTIMPLEMENTEDERROR),
			new RubyClassAndDoc(JRUBY_NOMETHODERROR_CLASS, NOMETHODERROR),
			new RubyClassAndDoc(JRUBY_NAMEERROR_CLASS, NAMEERROR),
			new RubyClassAndDoc(JRUBY_LOCALJUMPERROR_CLASS, LOCALJUMPERROR),
			new RubyClassAndDoc(JRUBY_SCRIPTERROR_CLASS, SCRIPTERROR),
			new RubyClassAndDoc(JRUBY_KEYERROR_CLASS, KEYERROR),
			new RubyClassAndDoc(JRUBY_INDEXERROR_CLASS, INDEXERROR),
			new RubyClassAndDoc(JRUBY_STANDARDERROR_CLASS, STANDARDERROR),
			new RubyClassAndDoc(JRUBY_ARGUMENTERROR_CLASS, ARGUMENTERROR),
			new RubyClassAndDoc(JRUBY_IOERROR_CLASS, IOERROR),
			new RubyClassAndDoc(JRUBY_EOFERROR_CLASS, EOFERROR),
			new RubyClassAndDoc(JRUBY_ENCODINGERROR_CLASS, ENCODINGERROR),
			new RubyClassAndDoc(JRUBY_FIBERERROR_CLASS, FIBERERROR),
			new RubyClassAndDoc(JRUBY_TYPEERROR_CLASS, TYPEERROR), };

	private static final Class buildJRubyArrayClass() {
		return Array.newInstance(JRUBY_OBJECT_CLASS, 0).getClass();
	}

	public static void main(final String[] args) {
		final String host = "http://ruby-doc.org/core-2.3.0/";
		final Vector<String> classList = new Vector<String>();
		final Vector<File> fileList = new Vector<File>();

		final File rubyDocBase = new File("./hc/res/docs/ruby/");
		if (rubyDocBase.exists() == false) {
			System.err.println("dir is not exists : " + rubyDocBase.getName());
			return;
		}
		final File[] items = rubyDocBase.listFiles();
		for (int i = 0; i < items.length; i++) {
			final File item = items[i];
			if (item.isDirectory()) {
				continue;
			}
			final String name = item.getName();
			final int classIdx = 4;
			final String str_2_2_0 = "2_2_0";
			int endClassIdx = name.indexOf(str_2_2_0, classIdx);
			if (endClassIdx == -1) {
				endClassIdx = name.indexOf(".", classIdx);
			}
			classList.add(name.substring(classIdx, endClassIdx));
			fileList.add(item);
		}

		final int size = classList.size();
		for (int i = size - 1; i >= 0; i--) {
			final String fileName = classList.get(i) + ".html";
			final String url = host + buildUrlPath(fileName);
			final String content = ResourceUtil.getStringFromURL(url, true);
			if (content.length() == 0) {
				System.err.println("fail to getcontent : " + url);
				continue;
			}
			final File file = new File(rubyDocBase, "Ruby" + fileName);
			InputStream stream;
			try {
				stream = new ByteArrayInputStream(content.getBytes(IConstant.UTF_8));
				ResourceUtil.saveToFile(stream, file);
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			System.out.println("successful write : " + fileName);
			try {
				Thread.sleep(3000);
			} catch (final Exception e) {
			}
			final File item = fileList.get(i);
			if ((item.getName().equals(fileName) == false) && item.isFile()
					&& item.delete() == false) {
				System.err.println("fail to delete file : " + item.getName());
				continue;
			}

		}

	}

	private final static String buildUrlPath(final String fileName) {
		if (fileName.startsWith("Mutex", 0)) {
			return "Thread/" + fileName;
		} else {
			return fileName;
		}
	}

	public static final RubyClassAndDoc searchRubyClass(final Class c) {
		for (int i = 0; i < JRUBY_CLASS.length; i++) {
			final RubyClassAndDoc rubyClassAndDoc = JRUBY_CLASS[i];
			if (c == rubyClassAndDoc.claz) {
				return rubyClassAndDoc;
			}
		}
		return null;
	}

	static String[] aliasType;
	static RubyClassAndDoc[] rcd;
	static Pattern[] typePatterns;
	static RubyClassAndDoc[] typercd;

	public static final RubyClassAndDoc searchRubyClassByDocReturn(final String returnStr) {
		final int length = JRUBY_CLASS.length;
		for (int i = 0; i < length; i++) {
			final RubyClassAndDoc rubyClassAndDoc = JRUBY_CLASS[i];
			final String docShortName = rubyClassAndDoc.docShortName;
			if (docShortName.equalsIgnoreCase(returnStr)
					|| returnStr.equalsIgnoreCase("a" + docShortName)
					|| returnStr.equalsIgnoreCase("an" + docShortName)
					|| returnStr.equalsIgnoreCase("a_" + docShortName)
					|| returnStr.equalsIgnoreCase("an_" + docShortName)
					|| returnStr.equalsIgnoreCase(docShortName + "_result")
					|| returnStr.endsWith("_" + docShortName.toLowerCase())) {
				return rubyClassAndDoc;
			}
		}

		if (aliasType == null) {
			aliasType = new String[] { "ary", "int", "true or false", "true, false,",
					"(true or false)", "true", "true/false", "enc", "enum", "number", "num", "obj",
					"hsh", "prng", "rat", "re", "real", "rng", "str", "sym", "thr", "mod",
					"integer or float", "thgrp" };
			rcd = new RubyClassAndDoc[] { searchRubyClassByShortName(ARRAY),
					searchRubyClassByShortName(INTEGER), searchRubyClassByShortName(TRUECLASS),
					searchRubyClassByShortName(TRUECLASS), searchRubyClassByShortName(TRUECLASS),
					searchRubyClassByShortName(TRUECLASS), searchRubyClassByShortName(TRUECLASS),
					searchRubyClassByShortName(ENCODING), searchRubyClassByShortName(ENUMERATOR),
					searchRubyClassByShortName(NUMERIC), searchRubyClassByShortName(NUMERIC),
					searchRubyClassByShortName(OBJECT), searchRubyClassByShortName(HASH),
					searchRubyClassByShortName(RANDOM), /*
														 * A pseudorandom number
														 * generator (PRNG)
														 */
					searchRubyClassByShortName(RATIONAL), searchRubyClassByShortName(REGEXP),
					searchRubyClassByShortName(NUMERIC), searchRubyClassByShortName(RANGE),
					searchRubyClassByShortName(STRING), searchRubyClassByShortName(SYMBOL),
					searchRubyClassByShortName(THREAD), searchRubyClassByShortName(MODULE),
					searchRubyClassByShortName(INTEGER), searchRubyClassByShortName(THREADGROUP) };
			if (aliasType.length != rcd.length) {
				System.err.println("fail equals length of array!");
				System.exit(0);
			}
		}

		for (int i = 0; i < aliasType.length; i++) {
			if (returnStr.equals(aliasType[i])) {
				return rcd[i];
			}
		}

		// if(typePatterns == null){
		// typePatterns = new Pattern[]{Pattern.compile("\\s*string\\s*",
		// Pattern.CASE_INSENSITIVE), Pattern.compile("\bfloat\b"),
		// Pattern.compile("\b(int|integer)\b"),
		// Pattern.compile("\b(true|false)\b")};
		// typercd = new RubyClassAndDoc[]{searchRubyClassByShortName(FLOAT),
		// searchRubyClassByShortName(NUMERIC),
		// searchRubyClassByShortName(TRUECLASS)};
		// }
		//
		// for (int i = 0; i < typePatterns.length; i++) {
		// if(typePatterns[i].matcher(returnStr).find()){
		// return typercd[i];
		// }
		// }

		return null;
	}

	public static final RubyClassAndDoc searchRubyClassByFullName(final String fullName) {
		for (int i = 0; i < JRUBY_CLASS.length; i++) {
			final RubyClassAndDoc rubyClassAndDoc = JRUBY_CLASS[i];
			if (fullName.equals(rubyClassAndDoc.claz.getName())) {
				return rubyClassAndDoc;
			}
		}
		return null;
	}

	public static final RubyClassAndDoc searchRubyClassByShortName(final String shortName) {
		for (int i = 0; i < JRUBY_CLASS.length; i++) {
			final RubyClassAndDoc rubyClassAndDoc = JRUBY_CLASS[i];
			if (shortName.equals(rubyClassAndDoc.docShortName)) {
				return rubyClassAndDoc;
			}
		}
		return null;
	}

	static Type j2seClassToRubyClass(Type baseType) {
		if (baseType == CodeHelper.J2SE_STRING_CLASS) {
			baseType = JRUBY_STRING_CLASS;
		} else if (baseType == boolean.class || baseType == Boolean.class) {
			baseType = JRUBY_TRUE_CLASS;
		} else if (baseType == byte.class || baseType == char.class || baseType == short.class
				|| baseType == int.class || baseType == long.class || baseType == Byte.class
				|| baseType == Character.class || baseType == Short.class
				|| baseType == Integer.class || baseType == Long.class) {
			baseType = JRUBY_FIXNUM_CLASS;
		} else if (baseType == float.class || baseType == double.class || baseType == Float.class
				|| baseType == Double.class) {
			baseType = JRUBY_FLOAT_CLASS;
		}
		return baseType;
	}

	static boolean isEnumerable(final Type type) {
		return type == JRUBY_ARRAY_CLASS || type == JRUBY_RANGE_CLASS
				|| type == JRUBY_ENUMERATOR_CLASS || type == JRUBY_IO_CLASS
				|| type == JRUBY_DIR_CLASS || type == JRUBY_HASH_CLASS
				|| type == JRUBY_STRUCT_CLASS;
	}
}

class JRubyObject {
}

class JRubyArray extends JRubyObject {
}

class JRubyString extends JRubyObject {
}

class JRubyIO extends JRubyObject {
}

class JRubyFile extends JRubyIO {
}

class JRubyDir extends JRubyObject {
}

class JRubyHash extends JRubyObject {
}

class JRubyMatchData extends JRubyObject {
}

class JRubyMath {// undefined method `superclass' for Math:Module
}

class JRubyTime extends JRubyObject {
}

class JRubyFixnum extends JRubyInteger {
}

class JRubyBignum extends JRubyFixnum {
	// 1. Whenever a Fixnum exceeds this range, it is automatically converted to
	// a Bignum object
	// 2. If an operation with a Bignum result has a final value that will fit
	// in a Fixnum, the result will be returned as a Fixnum.
}

class JRubyStruct extends JRubyObject {
}

class JRubyMarshal {// undefined method `superclass' for Marshal:Module
}

class JRubyThread extends JRubyObject {
}

class JRubyBasicObject {
}

class JRubyFloat extends JRubyNumeric {
}

class JRubyInteger extends JRubyNumeric {
}

class JRubyMethod {
}

class JRubyModule {
}

class JRubyMutex {
}

class JRubyNilClass {
}

class JRubyNumeric extends JRubyObject {
}

class JRubyProc {
}

class JRubyClass {
}

class JRubyRandom {
}

class JRubyRange extends JRubyObject {
}

class JRubyTrue extends JRubyObject {
}

class JRubyFalse extends JRubyObject {
}

class JRubyException {
}

class JRubyKernel {
}

class JRubyRational extends JRubyNumeric {
}

class JRubyComplex extends JRubyNumeric {
}

class JRubyEnumerator extends JRubyObject {
}

class JRubyEnumerable {
}

class JRubyRegexp extends JRubyObject {
}

class JRubyAnonymous {
}

class JRubySymbol extends JRubyObject {
}

class JRubySignal {
}

class JRubyQueue extends JRubyObject {
}

class JRubyProcess {
}

class JRubyConditionVariable extends JRubyObject {
}

class JRubyEncoding extends JRubyObject {
}

class JRubyTracePoint extends JRubyObject {
}

class JRubyThreadGroup extends JRubyObject {
}

class JRubySizedQueue extends JRubyObject {
}

class JRubyStandardError extends JRubyException {
}

class JRubyTypeError extends JRubyStandardError {
}

class JRubyArgumentError extends JRubyStandardError {
}

class JRubyIOError extends JRubyStandardError {
}

class JRubyEOFError extends JRubyIOError {
}

class JRubyEncodingError extends JRubyStandardError {
}

class JRubyFiberError extends JRubyStandardError {
}

class JRubyRangeError extends JRubyStandardError {
}

class JRubyFloatDomainError extends JRubyRangeError {
}

class JRubyIndexError extends JRubyStandardError {
}

class JRubyKeyError extends JRubyIndexError {
}

class JRubyScriptError extends JRubyException {
}

class JRubyLoadError extends JRubyScriptError {
}

class JRubyLocalJumpError extends JRubyStandardError {
}

class JRubyNameError extends JRubyStandardError {
}

class JRubyNoMemoryError extends JRubyException {
}

class JRubyNoMethodError extends JRubyNameError {
}

class JRubyNotImplementedError extends JRubyScriptError {
}

class JRubyRegexpError extends JRubyStandardError {
}

class JRubyRuntimeError extends JRubyStandardError {
}

class JRubySecurityError extends JRubyException {
}

class JRubySignalException extends JRubyException {
}

class JRubySyntaxError extends JRubyScriptError {
}

class JRubySystemCallError extends JRubyStandardError {
}

class JRubySystemStackError extends JRubyException {
}

class JRubyThreadError extends JRubyStandardError {
}

class JRubyUncaughtThrowError extends JRubyArgumentError {// 实际文档为ArgError???
}

class JRubyZeroDivisionError extends JRubyStandardError {
}