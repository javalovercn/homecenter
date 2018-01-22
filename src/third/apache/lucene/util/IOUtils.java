/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package third.apache.lucene.util;


import hc.server.util.lucene.Files;
import hc.server.util.lucene.Path;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Collection;

/** This class emulates the new Java 7 "Try-With-Resources" statement.
 * Remove once Lucene is on Java 7.
 * @lucene.internal */
public final class IOUtils {
  
  /**
   * UTF-8 {@link Charset} instance to prevent repeated
   * {@link Charset#forName(String)} lookups
   */
  @Deprecated
  public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");//禁用sun.nio.cs.StandardCharsets
  
  /**
   * UTF-8 charset string.
   */
  public static final String UTF_8 = Charset.forName("UTF-8").name();
  
  private IOUtils() {} // no instance

  /**
   * Closes all given <tt>Closeable</tt>s.  Some of the
   * <tt>Closeable</tt>s may be null; they are
   * ignored.  After everything is closed, the method either
   * throws the first exception it hit while closing, or
   * completes normally if there were no exceptions.
   * 
   * @param objects
   *          objects to call <tt>close()</tt> on
   */
  public static void close(Closeable... objects) throws IOException {
    close(Arrays.asList(objects));
  }
  
  /**
   * Closes all given <tt>Closeable</tt>s.
   * @see #close(Closeable...)
   */
  public static void close(Iterable<? extends Closeable> objects) throws IOException {
    Throwable th = null;

    for (Closeable object : objects) {
      try {
        if (object != null) {
          object.close();
        }
      } catch (Throwable t) {
        addSuppressed(th, t);
        if (th == null) {
          th = t;
        }
      }
    }

    reThrow(th);
  }

  /**
   * Closes all given <tt>Closeable</tt>s, suppressing all thrown exceptions.
   * Some of the <tt>Closeable</tt>s may be null, they are ignored.
   * 
   * @param objects
   *          objects to call <tt>close()</tt> on
   */
  public static void closeWhileHandlingException(Closeable... objects) {
    closeWhileHandlingException(Arrays.asList(objects));
  }
  
  /**
   * Closes all given <tt>Closeable</tt>s, suppressing all thrown exceptions.
   * @see #closeWhileHandlingException(Closeable...)
   */
  public static void closeWhileHandlingException(Iterable<? extends Closeable> objects) {
    for (Closeable object : objects) {
      try {
        if (object != null) {
          object.close();
        }
      } catch (Throwable t) {
      }
    }
  }
  
  /** adds a Throwable to the list of suppressed Exceptions of the first Throwable
   * @param exception this exception should get the suppressed one added
   * @param suppressed the suppressed exception
   */
  private static void addSuppressed(Throwable exception, Throwable suppressed) {
	suppressed.printStackTrace();
    if (exception != null && suppressed != null) {
//      exception.addSuppressed(suppressed);
    }
  }
  
  /**
   * Wrapping the given {@link InputStream} in a reader using a {@link CharsetDecoder}.
   * Unlike Java's defaults this reader will throw an exception if your it detects 
   * the read charset doesn't match the expected {@link Charset}. 
   * <p>
   * Decoding readers are useful to load configuration files, stopword lists or synonym files
   * to detect character set problems. However, it's not recommended to use as a common purpose 
   * reader.
   * 
   * @param stream the stream to wrap in a reader
   * @param charSet the expected charset
   * @return a wrapping reader
   */
  public static Reader getDecodingReader(InputStream stream, Charset charSet) {
    final CharsetDecoder charSetDecoder = charSet.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
    return new BufferedReader(new InputStreamReader(stream, charSetDecoder));
  }

  /**
   * Opens a Reader for the given resource using a {@link CharsetDecoder}.
   * Unlike Java's defaults this reader will throw an exception if your it detects 
   * the read charset doesn't match the expected {@link Charset}. 
   * <p>
   * Decoding readers are useful to load configuration files, stopword lists or synonym files
   * to detect character set problems. However, it's not recommended to use as a common purpose 
   * reader.
   * @param clazz the class used to locate the resource
   * @param resource the resource name to load
   * @param charSet the expected charset
   * @return a reader to read the given file
   * 
   */
  public static Reader getDecodingReader(Class<?> clazz, String resource, Charset charSet) throws IOException {
    InputStream stream = null;
    boolean success = false;
    try {
      stream = clazz
      .getResourceAsStream(resource);
      final Reader reader = getDecodingReader(stream, charSet);
      success = true;
      return reader;
    } finally {
      if (!success) {
        IOUtils.close(stream);
      }
    }
  }
  
  /**
   * Deletes all given files, suppressing all thrown IOExceptions.
   * <p>
   * Some of the files may be null, if so they are ignored.
   */
  public static void deleteFilesIgnoringExceptions(Path... files) {
    deleteFilesIgnoringExceptions(Arrays.asList(files));
  }
  
  /**
   * Deletes all given files, suppressing all thrown IOExceptions.
   * <p>
   * Some of the files may be null, if so they are ignored.
   */
  public static void deleteFilesIgnoringExceptions(Collection<? extends Path> files) {
    for (Path name : files) {
      if (name != null) {
        try {
          Files.delete(name);
        } catch (Throwable ignored) {
          // ignore
        }
      }
    }
  }
  
  /**
   * Deletes all given <tt>Path</tt>s, if they exist.  Some of the
   * <tt>File</tt>s may be null; they are
   * ignored.  After everything is deleted, the method either
   * throws the first exception it hit while deleting, or
   * completes normally if there were no exceptions.
   * 
   * @param files files to delete
   */
  public static void deleteFilesIfExist(Path... files) throws IOException {
    deleteFilesIfExist(Arrays.asList(files));
  }
  
  /**
   * Deletes all given <tt>Path</tt>s, if they exist.  Some of the
   * <tt>File</tt>s may be null; they are
   * ignored.  After everything is deleted, the method either
   * throws the first exception it hit while deleting, or
   * completes normally if there were no exceptions.
   * 
   * @param files files to delete
   */
  public static void deleteFilesIfExist(Collection<? extends Path> files) throws IOException {
    Throwable th = null;

    for (Path file : files) {
      try {
        if (file != null) {
          Files.deleteIfExists(file);
        }
      } catch (Throwable t) {
        addSuppressed(th, t);
        if (th == null) {
          th = t;
        }
      }
    }

    reThrow(th);
  }
  
  /**
   * Simple utility method that takes a previously caught
   * {@code Throwable} and rethrows either {@code
   * IOException} or an unchecked exception.  If the
   * argument is null then this method does nothing.
   */
  public static void reThrow(Throwable th) throws IOException {
    if (th != null) {
      if (th instanceof IOException) {
        throw (IOException) th;
      }
      reThrowUnchecked(th);
    }
  }

  /**
   * Simple utility method that takes a previously caught
   * {@code Throwable} and rethrows it as an unchecked exception.
   * If the argument is null then this method does nothing.
   */
  public static void reThrowUnchecked(Throwable th) {
    if (th != null) {
      if (th instanceof RuntimeException) {
        throw (RuntimeException) th;
      }
      if (th instanceof Error) {
        throw (Error) th;
      }
      throw new RuntimeException(th);
    }
  }

}
