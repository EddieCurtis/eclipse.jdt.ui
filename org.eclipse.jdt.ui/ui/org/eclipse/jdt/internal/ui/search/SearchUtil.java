/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * This class contains some utility methods for J Search.
 */
public class SearchUtil extends JavaModelUtil {

	public static IJavaElement getJavaElement(IMarker marker) {
		try {
			String handleId= (String)marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID);
			IJavaElement je= JavaCore.create(handleId);
			if (!marker.getAttribute(IJavaSearchUIConstants.ATT_IS_WORKING_COPY, false)) {
				if (je != null&& je.exists())
					return je;
				else {
					IJavaElement fixedJe= fixCUName(marker, handleId);
					if (fixedJe != null)
						return fixedJe;
					else
						return je;
				}
			}

			ICompilationUnit cu= findCompilationUnit(je);
			if (cu == null)
				return null;
			// Find working copy element
			IWorkingCopy[] workingCopies= JavaUI.getSharedWorkingCopies();
			int i= 0;
			while (i < workingCopies.length) {
				if (workingCopies[i].getOriginalElement().equals(cu)) {
					je= findInWorkingCopy(workingCopies[i], je, true);
					break;
				}
				i++;
			}
			if (je != null && !je.exists())
				je= cu.getElementAt(marker.getAttribute(IMarker.CHAR_START, 0));
			return je;
		} catch (CoreException ex) {
			ExceptionHandler.handle(ex, SearchMessages.getString("Search.Error.createJavaElement.title"), SearchMessages.getString("Search.Error.createJavaElement.message")); //$NON-NLS-2$ //$NON-NLS-1$
			return null;
		}
	}

	private static IJavaElement fixCUName(IMarker marker, String handle) {
			// FIXME: This is a dirty fix for 1GCE1EI: ITPJUI:WINNT - Can't handle rename of resource
			if (handle != null) {
				String resourceName= ""; //$NON-NLS-1$
				if (marker.getResource() != null)
					resourceName= marker.getResource().getName();
				if (!handleContainsWrongCU(handle, resourceName)) {
				 	handle= computeFixedHandle(handle, resourceName);
					IJavaElement je= JavaCore.create(handle);
				 	if (je != null && je.exists()) {
				 		try {
							marker.setAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID, handle);
				 		} catch (CoreException ex) {
				 			// leave old attribute
				 		} finally {
							return je;
				 		}
				 	}
				}
			}
			return null;
	}
	
	private static boolean handleContainsWrongCU(String handle, String resourceName) {
		int start= handle.indexOf('{');
		int end= handle.indexOf(".java"); //$NON-NLS-1$
		if (start >= end)
			return false;
		String name= handle.substring(start + 1, end + 5);
		return name.equals(resourceName);
	}
	
	private static String computeFixedHandle(String handle, String resourceName) {
		int start= handle.indexOf('{');
		int end= handle.indexOf(".java"); //$NON-NLS-1$
		handle= handle.substring(0, start + 1) + resourceName + handle.substring(end + 5);
		return handle;
	}

	// --------------- Util methods needed for working copies ---------------

	/**
	 * Returns an array of all editors. If the identical content is presented in
	 * more than one editor, only one of those editor parts is part of the result.
	 * 
	 * @return an array of all editor parts.
	 */
	public static IEditorPart[] getEditors() {
		Set inputs= new HashSet(7);
		List result= new ArrayList(0);
		IWorkbench workbench= JavaPlugin.getDefault().getWorkbench();
		IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int x= 0; x < pages.length; x++) {
				IEditorPart[] editors= pages[x].getDirtyEditors();
				for (int z= 0; z < editors.length; z++) {
					result.add(editors[z]);
				}
			}
		}
		return (IEditorPart[])result.toArray(new IEditorPart[result.size()]);
	}

	/** 
	 * Returns the working copy of the given java element.
	 * @param javaElement the javaElement for which the working copyshould be found
	 * @param reconcile indicates whether the working copy must be reconcile prior to searching it
	 * @return the working copy of the given element or <code>null</code> if none
	 */	
	private static IJavaElement findInWorkingCopy(IWorkingCopy workingCopy, IJavaElement element, boolean reconcile) throws JavaModelException {
		if (workingCopy != null) {
			if (reconcile) {
				synchronized (workingCopy) {
					workingCopy.reconcile();
					return SearchUtil.findInCompilationUnit((ICompilationUnit)workingCopy, element);
				}
			} else {
					return SearchUtil.findInCompilationUnit((ICompilationUnit)workingCopy, element);
			}
		}
		return null;
	}

	/**
	 * Returns the compilation unit for the given java element.
	 * 
	 * @param	element the java element whose compilation unit is searched for
	 * @return	the compilation unit of the given java element
	 */
	static ICompilationUnit findCompilationUnit(IJavaElement element) {
		if (element == null)
			return null;

		if (element.getElementType() == IJavaElement.COMPILATION_UNIT)
			return (ICompilationUnit)element;
			
		if (element instanceof IMember)
			return ((IMember)element).getCompilationUnit();

		return findCompilationUnit(element.getParent());
	}

	/*
	 * Copied from JavaModelUtil and patched to allow members which do not exist.
	 * The only case where this is a problem is for methods which have same name and
	 * paramters as a constructor. The constructor will win in such a situation.
	 * 
	 * @see JavaModelUtil#findMemberInCompilationUnit(ICompilationUnit, IMember)
	 */		
	public static IMember findMemberInCompilationUnit(ICompilationUnit cu, IMember member) throws JavaModelException {
		if (member.getElementType() == IJavaElement.TYPE) {
			return findTypeInCompilationUnit(cu, getTypeQualifiedName((IType)member));
		} else {
			IType declaringType= findTypeInCompilationUnit(cu, getTypeQualifiedName(member.getDeclaringType()));
			if (declaringType != null) {
				IMember result= null;
				switch (member.getElementType()) {
				case IJavaElement.FIELD:
					result= declaringType.getField(member.getElementName());
					break;
				case IJavaElement.METHOD:
					IMethod meth= (IMethod) member;
					// XXX: Begin patch ---------------------
					boolean isConstructor;
					if (meth.exists())
						isConstructor= meth.isConstructor();
					else
						isConstructor= declaringType.getElementName().equals(meth.getElementName());
					// XXX: End patch -----------------------
					result= findMethod(meth.getElementName(), meth.getParameterTypes(), isConstructor, declaringType);
					break;
				case IJavaElement.INITIALIZER:
					result= declaringType.getInitializer(1);
					break;					
				}
				if (result != null && result.exists()) {
					return result;
				}
			}
		}
		return null;
	}

	/*
	 * XXX: Unchanged copy from JavaModelUtil
	 */
	public static IJavaElement findInCompilationUnit(ICompilationUnit cu, IJavaElement element) throws JavaModelException {
		
		if (element instanceof IMember)
			return findMemberInCompilationUnit(cu, (IMember) element);
		
		int type= element.getElementType();
		switch (type) {
			case IJavaElement.IMPORT_CONTAINER:
				return cu.getImportContainer();
			
			case IJavaElement.PACKAGE_DECLARATION:
				return find(cu.getPackageDeclarations(), element.getElementName());
			
			case IJavaElement.IMPORT_DECLARATION:
				return find(cu.getImports(), element.getElementName());
			
			case IJavaElement.COMPILATION_UNIT:
				return cu;
		}
		
		return null;
	}
	
	/*
	 * XXX: Unchanged copy from JavaModelUtil
	 */
	private static IJavaElement find(IJavaElement[] elements, String name) {
		if (elements == null || name == null)
			return null;
			
		for (int i= 0; i < elements.length; i++) {
			if (name.equals(elements[i].getElementName()))
				return elements[i];
		}
		
		return null;
	}

	public static String toString(IWorkingSet[] workingSets) {
		Arrays.sort(workingSets, new WorkingSetComparator());
		String result= ""; //$NON-NLS-1$
		if (workingSets != null && workingSets.length > 0) {
			boolean firstFound= false;
			for (int i= 0; i < workingSets.length; i++) {
				String workingSetName= workingSets[i].getName();
				if (firstFound)
					result= SearchMessages.getFormattedString("SearchUtil.workingSetConcatenation", new String[] {result, workingSetName}); //$NON-NLS-1$
				else {
					result= workingSetName;
					firstFound= true;
				}
			}
		}
		return result;
	}
}
