/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;

/* package */ class ASTRewriteFormatter {

	public static class NodeMarker {
		public Object data;
		public int offset;
		public int length;		
	}
	
	private static class ExtendedFlattener extends ASTFlattener {
		
		private ArrayList fExistingNodes;
		private ASTRewrite fRewrite;
		
		public ExtendedFlattener(ASTRewrite rewrite) {
			fExistingNodes= new ArrayList();
			fRewrite= rewrite;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#preVisit(ASTNode)
		 */
		public void preVisit(ASTNode node) {
			Object trackData= fRewrite.getTrackedNodeData(node);
			if (trackData != null) {
				addMarker(trackData, fResult.length(), 0);
			}
			Object placeholderData= fRewrite.getPlaceholderData(node);
			if (placeholderData != null) {
				addMarker(placeholderData, fResult.length(), 0);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#postVisit(ASTNode)
		 */
		public void postVisit(ASTNode node) {
			Object placeholderData= fRewrite.getPlaceholderData(node);
			if (placeholderData != null) {
				fixupLength(placeholderData, fResult.length());
			}
			Object trackData= fRewrite.getTrackedNodeData(node);
			if (trackData != null) {
				addMarker(trackData, fResult.length(), -1);
			}
		}
	
		private NodeMarker addMarker(Object annotation, int startOffset, int length) {
			NodeMarker marker= new NodeMarker();
			marker.offset= startOffset;
			marker.length= length;
			marker.data= annotation;
			fExistingNodes.add(marker);
			return marker;
		}
	
		private void fixupLength(Object data, int endOffset) {
			for (int i= fExistingNodes.size()-1; i >= 0 ; i--) {
				NodeMarker marker= (NodeMarker) fExistingNodes.get(i);
				if (marker.data == data) {
					marker.length= endOffset - marker.offset;
					return;
				}
			}
		}

		/**
		 * @return
		 */
		public ArrayList getMarkers() {
			return fExistingNodes;
		}
	}
	
	private ASTRewrite fRewrite;

	public ASTRewriteFormatter(ASTRewrite rewrite) {
		super();
		fRewrite= rewrite;
	}
	
	/**
	 * Returns the string accumulated in the visit formatted using the default formatter.
	 * Updates the existing node's positions.
	 *
	 * @return the serialized and formatted code.
	 */	
	public String getFormattedResult(ASTNode node, int initialIndentationLevel, String lineDelimiter, Collection resultingMarkers) {
		
		ExtendedFlattener flattener= new ExtendedFlattener(fRewrite);
		node.accept(flattener);

		ArrayList markers= flattener.getMarkers();
		
		int nExistingNodes= markers.size();

		int[] positions= new int[nExistingNodes*2];
		for (int i= 0, k= 0; i < nExistingNodes; i++) {
			NodeMarker curr= (NodeMarker) markers.get(i);
			
			int startPos= curr.offset;
			int length= curr.length;
			if (length == -1) {
				startPos--;
			}
			positions[k++]= startPos;
			if (length > 0) {
				positions[k++]= startPos + length - 1;
			}
		}		
		
		Hashtable map= JavaCore.getOptions();
		map.put(JavaCore.FORMATTER_LINE_SPLIT, String.valueOf(9999));
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(map);
		String formatted= formatter.format(flattener.getResult(), initialIndentationLevel, positions, lineDelimiter);
		
		for (int i= 0, k= 0; i < nExistingNodes; i++) {
			int startPos= positions[k++];
			NodeMarker curr= (NodeMarker) markers.get(i);
			resultingMarkers.add(curr);
			
			int markerLength= curr.length;
			if (markerLength == -1) {
				startPos++;
				curr.length= 0;
			}
			curr.offset= startPos;
			if (markerLength > 0) {
				int endPos= positions[k++] + 1;
				curr.length= endPos - startPos;
			}
		}
		return formatted;
	}	
	
	
	
	
}
