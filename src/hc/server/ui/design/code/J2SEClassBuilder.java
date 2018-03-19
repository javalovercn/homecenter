package hc.server.ui.design.code;

import hc.core.util.ExceptionReporter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class J2SEClassBuilder {
	// public static void main(final String[] args){
	// execmain(args);
	// }

	/**
	 * 这个文件是用来生成J2SEClassList.java文件
	 * 
	 * @param args
	 */
	private static void execmain(final String[] args) {
		final java.util.List<String> list = getClassNameByJar(
				"/Library/Java/JavaVirtualMachines/jdk1.7.0_51.jdk/Contents/Home/jre/lib/rt.jar");

		final String className = "J2SEClassList";

		final StringBuilder builder = new StringBuilder(1024 * 1024);

		final String classFullName = J2SEClassBuilder.class.getName();
		final String packageName = classFullName.substring(0, classFullName.lastIndexOf("."));

		final String blockSystemClass = "";// "java.lang.System";

		builder.append("package " + packageName + ";\n");
		builder.append("\n");
		builder.append("public class " + className + " {\n");
		{
			builder.append("\tpublic static java.util.ArrayList<String> getList() {\n");
			{
				builder.append("\t\tfinal java.util.ArrayList<String> out = new java.util.ArrayList<String>(" + list.size() + ");\n");
				{
					final int size = list.size();
					for (int i = 0; i < size; i++) {
						final String cName = list.get(i);
						if (cName.equals(blockSystemClass)) {
							continue;
						}
						builder.append("\t\tout.add(\"" + cName + "\");\n");
					}
				}
				builder.append("\t\treturn out;\n");
			}
			builder.append("\t}\n");
		}

		builder.append("}");

		final String fileContent = builder.toString();
		{
			final String pathname = "./" + packageName.replace('.', '/') + "/" + className + ".java";
			final File file = new File(pathname);
			file.delete();

			BufferedReader bufferedReader = null;
			BufferedWriter bufferedWriter = null;
			try {
				bufferedReader = new BufferedReader(new StringReader(fileContent));
				bufferedWriter = new BufferedWriter(new java.io.FileWriter(file));
				final char buf[] = new char[1024];
				int len;
				while ((len = bufferedReader.read(buf)) != -1) {
					bufferedWriter.write(buf, 0, len);
				}
				bufferedWriter.flush();
				bufferedReader.close();
				bufferedWriter.close();

				System.out.println("successful create file " + pathname + ", total : " + list.size());
			} catch (final IOException e) {
				ExceptionReporter.printStackTrace(e);
			} finally {
				if (bufferedReader != null) {
					try {
						bufferedReader.close();
					} catch (final IOException e) {
						ExceptionReporter.printStackTrace(e);
					}
				}
			}
		}
	}

	private static java.util.List<String> getClassNameByJar(final String jarPath) {
		final java.util.List<String> myClassName = new ArrayList<String>();
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(jarPath, false);
			final Enumeration<JarEntry> entrys = jarFile.entries();
			while (entrys.hasMoreElements()) {
				final JarEntry jarEntry = entrys.nextElement();
				String entryName = jarEntry.getName();
				if (entryName.endsWith(".class")) {
					if (entryName.indexOf("$") > 0) {
						continue;
					}

					entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
					if (entryName.startsWith("java.") || entryName.startsWith("javax.")) {
					} else {
						continue;
					}
					myClassName.add(entryName);
				} else {
					if (entryName.startsWith("META-INF")) {
						continue;
					}
					entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
					if (entryName.startsWith("java.") || entryName.startsWith("javax.")) {
					} else {
						continue;
					}
					myClassName.add(entryName);
				}
			}
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		} finally {
			try {
				jarFile.close();
			} catch (final Throwable e) {
			}
		}

		Collections.sort(myClassName);

		return myClassName;
	}

	public static java.util.ArrayList<String> getClassAndResByJar(final File jarPath, final boolean needRes) {
		final java.util.ArrayList<String> myClassAndRes = new ArrayList<String>();
		try {
			final JarFile jarFile = new JarFile(jarPath, false);
			final Enumeration<JarEntry> entrys = jarFile.entries();
			while (entrys.hasMoreElements()) {
				final JarEntry jarEntry = entrys.nextElement();
				String entryName = jarEntry.getName();
				if (entryName.startsWith("META-INF")) {
					continue;
				}
				if (entryName.endsWith(".class")) {
					if (entryName.indexOf("$") > 0) {
						continue;
					}

					entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
					myClassAndRes.add(entryName);
				} else if (needRes) {
					if (entryName.endsWith("/")) {// 仅表示为包名或包路径
						continue;
					}

					// 注意：资源以/xx/11/aa.abc形式
					myClassAndRes.add("/" + entryName);// 采用绝对路径
				}
			}
		} catch (final Exception e) {
			ExceptionReporter.printStackTrace(e);
		}

		Collections.sort(myClassAndRes);

		return myClassAndRes;
	}
}
