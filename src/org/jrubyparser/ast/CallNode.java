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

/**
 * A method or operator call.
 */
public class CallNode extends Node implements INameNode, IArgumentNode, BlockAcceptingNode {
    private Node receiverNode;
    private Node argsNode;
    protected Node iterNode;
    protected String name;
    protected String lexicalName;
    private boolean hasParens = false;

    public CallNode(final SourcePosition position, final Node receiverNode, final String name, final Node argsNode) {
        this(position, receiverNode, name, argsNode, null);
    }
    
    public CallNode(final SourcePosition position, final Node receiverNode, final String name, final Node argsNode, 
            final Node iterNode) {
        super(position);
        
        assert receiverNode != null : "receiverNode is not null";
        
        this.receiverNode = adopt(receiverNode);
        setArgs(argsNode);
        this.iterNode = adopt(iterNode);
        this.name = name;
        lexicalName = name;
    }


    /**
     * Checks node for 'sameness' for diffing.
     *
     * @param node to be compared to
     * @return Returns a boolean
     */
    @Override
    public boolean isSame(final Node node) {
        return super.isSame(node) && isNameMatch(((CallNode) node).getName());
    }


    @Override
	public NodeType getNodeType() {
        return NodeType.CALLNODE;
    }
    
    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    @Override
	public Object accept(final NodeVisitor iVisitor) {
        return iVisitor.visitCallNode(this);
    }
    
    @Deprecated
    public Node getIterNode() {
        return getIter();
    }
    
    @Override
	public Node getIter() {
        return iterNode;
    }
    
    public Node setIterNode(final Node iterNode) {
        setIter(iterNode);
        
        return this;
    }
    
    @Override
	public void setIter(final Node iter) {
        this.iterNode = adopt(iter);
    }

    /**
     * Gets the argsNode representing the method's arguments' value for this call.
     * @return argsNode
     */
    @Deprecated
    public Node getArgsNode() {
        return getArgs();
    }
    
    @Override
	public Node getArgs() {
        return argsNode;
    }
    
    /**
     * Set the argsNode.
     * 
     * @param argsNode set the arguments for this node.
     */
    @Deprecated
    public Node setArgsNode(final Node argsNode) {
        setArgs(argsNode);
        
        return getArgs();
    }
    
    @Override
	public void setArgs(Node argsNode) {
        if (argsNode == null) {
	    argsNode = new ListNode(getReceiver().getPosition());
        }
        this.argsNode = adopt(argsNode);
    }

    @Override
	public boolean hasParens() {
        return hasParens;
    }

    @Override
	public void setHasParens(final boolean hasParens) {
        this.hasParens = hasParens;
    }
    
    @Override
	public String getLexicalName() {
        return lexicalName;
    }

    /**
     * Gets the name.
	 * name is the name of the method called
     * @return name
     */
    @Override
	public String getName() {
        return name;
    }

    @Override
	public void setName(final String name) {
        this.name = name;
    }

    public void setLexicalName(final String lexcicalName) {
        this.lexicalName = lexcicalName;
    }

    @Override
	public boolean isNameMatch(final String name) {
        final String thisName = getName();
        
        return thisName != null && thisName.equals(name);
    }
    
    /**
     * Gets the receiverNode.
	 * receiverNode is the object on which the method is being called
     * @return receiverNode
     */
    @Deprecated
    public Node getReceiverNode() {
        return getReceiver();
    }
    
    public Node getReceiver() {
        return receiverNode;
    }
    
    public void setReceiver(final Node receiver) {
        this.receiverNode = adopt(receiver);
    }

    @Override
	public SourcePosition getNamePosition() {
        final SourcePosition pos = receiverNode.getPosition();
        
        return new SourcePosition(pos.getFile(), pos.getStartLine(), pos.getEndLine(),
                pos.getEndOffset(), pos.getEndOffset() + getName().length());
    }
    
    @Override
	public SourcePosition getLexicalNamePosition() {
        return getNamePosition();
    }
}
