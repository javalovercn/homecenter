/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009 Thomas E. Enebo <tom.enebo@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jrubyparser.ast;

import org.jrubyparser.NodeVisitor;
import org.jrubyparser.SourcePosition;
import org.jrubyparser.RegexpOptions;

/**
 * Represents a simple regular expression literal.
 */
public class RegexpNode extends Node implements ILiteralNode {
	private String value;
	private RegexpOptions options;

	public RegexpNode(SourcePosition position, String value, RegexpOptions options) {
		super(position);

		this.value = value;
		this.options = options;
	}

	@Override
	public boolean isSame(Node node) {
		if (!super.isSame(node))
			return false;

		RegexpNode other = (RegexpNode) node;
		if (getValue() == null && other.getValue() == null) {
			if (getOptions() == null && other.getOptions() == null)
				return true;
			if (getOptions() == null || other.getOptions() == null)
				return false;
		} else if (getValue() == null || other.getValue() == null) {
			return false;
		} else if (getOptions() == null && other.getOptions() == null) {
			return getValue().equals(other.getValue());
		} else if (getOptions() == null || other.getOptions() == null) {
			return false;
		}

		return getValue().equals(other.getValue()) && getOptions().equals(other.getOptions());
	}

	public NodeType getNodeType() {
		return NodeType.REGEXPNODE;
	}

	public <T> T accept(NodeVisitor<T> iVisitor) {
		return iVisitor.visitRegexpNode(this);
	}

	/**
	 * Gets the options.
	 * 
	 * @return the options
	 */
	public RegexpOptions getOptions() {
		return options;
	}

	/**
	 * Gets the value.
	 * 
	 * @return Returns a ByteList
	 */
	public String getValue() {
		return value;
	}
}
