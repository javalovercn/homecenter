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

package org.javassist.compiler.ast;

import org.javassist.compiler.CompileError;

/**
 * Array initializer such as <code>{ 1, 2, 3 }</code>.
 */
public class ArrayInit extends ASTList {
	public ArrayInit(ASTree firstElement) {
		super(firstElement);
	}

	public void accept(Visitor v) throws CompileError {
		v.atArrayInit(this);
	}

	public String getTag() {
		return "array";
	}
}
