/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package org.javassist.convert;

import org.javassist.CannotCompileException;
import org.javassist.CtClass;
import org.javassist.bytecode.BadBytecode;
import org.javassist.bytecode.CodeAttribute;
import org.javassist.bytecode.CodeIterator;
import org.javassist.bytecode.ConstPool;
import org.javassist.bytecode.MethodInfo;
import org.javassist.bytecode.Opcode;

/**
 * Transformer and its subclasses are used for executing code transformation specified by
 * CodeConverter.
 *
 * @see org.javassist.CodeConverter
 */
public abstract class Transformer implements Opcode {
	private Transformer next;

	public Transformer(Transformer t) {
		next = t;
	}

	public Transformer getNext() {
		return next;
	}

	public void initialize(ConstPool cp, CodeAttribute attr) {
	}

	public void initialize(ConstPool cp, CtClass clazz, MethodInfo minfo) throws CannotCompileException {
		initialize(cp, minfo.getCodeAttribute());
	}

	public void clean() {
	}

	public abstract int transform(CtClass clazz, int pos, CodeIterator it, ConstPool cp) throws CannotCompileException, BadBytecode;

	public int extraLocals() {
		return 0;
	}

	public int extraStack() {
		return 0;
	}
}
