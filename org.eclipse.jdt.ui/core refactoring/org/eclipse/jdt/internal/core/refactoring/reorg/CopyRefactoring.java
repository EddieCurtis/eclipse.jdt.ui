/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.reorg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.NullChange;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyCompilationUnitChange;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyPackageChange;
import org.eclipse.jdt.internal.core.refactoring.changes.CopyResourceChange;

public class CopyRefactoring extends ReorgRefactoring {

	private Set fAutoGeneratedNewNames;
	
	public CopyRefactoring(List elements){
		super(elements);
		fAutoGeneratedNewNames=  new HashSet(2);
	}
	
	/* non java-doc
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Copy elements";
	}
	
	/* non java-doc
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public final RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1);
		try{
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}
	
	/* non java-doc
	 * @see ReorgRefactoring#isValidDestinationForCusAndFiles(Object)
	 */
	boolean isValidDestinationForCusAndFiles(Object dest) throws JavaModelException {
		return getDestinationForCusAndFiles(dest) != null;
	}
	
	//-----
	private static boolean isNewNameOk(IPackageFragment dest, String newName) {
		return ! dest.getCompilationUnit(newName).exists();
	}
	
	private static boolean isNewNameOk(IContainer container, String newName) {
		return container.findMember(newName) == null;
	}

	private static boolean isNewNameOk(IPackageFragmentRoot root, String newName) {
		return ! root.getPackageFragment(newName).exists() ;
	}	
	
	private String createNewName(ICompilationUnit cu, IPackageFragment dest){
		if (isNewNameOk(dest, cu.getElementName()))
			return null;
		int i= 1;
		while (true){
			String newName= i == 1? "CopyOf" + cu.getElementName():  
														 "Copy_" + i + "_of_" + cu.getElementName();
			if (isNewNameOk(dest, newName) && ! fAutoGeneratedNewNames.contains(newName)){
				fAutoGeneratedNewNames.add(newName);
				return newName;
			}
			i++;
		}
	}
	
	private String createNewName(IResource res, IContainer container){
		if (isNewNameOk(container, res.getName()))
			return null;
		int i= 1;
		while (true){
			String newName= i == 1? "Copy of " + res.getName():  
														 "Copy (" + i + ") of " + res.getName();
			if (isNewNameOk(container, newName) && ! fAutoGeneratedNewNames.contains(newName)){
				fAutoGeneratedNewNames.add(newName);
				return newName;
			}
			i++;
		}	
	}
	
	private String createNewName(IPackageFragment pack, IPackageFragmentRoot root){
		if (isNewNameOk(root, pack.getElementName()))
			return null;
		int i= 1;
		while (true){
			String newName= i == 1? "CopyOf" + pack.getElementName():  
														 "Copy_" + i + "_of_" + pack.getElementName();
			if (isNewNameOk(root, newName) && ! fAutoGeneratedNewNames.contains(newName)){
				fAutoGeneratedNewNames.add(newName);
				return newName;
			}
			i++;
		}	
	}

	IChange createChange(ICompilationUnit cu) throws JavaModelException{
		Object dest= getDestinationForCusAndFiles(getDestination());
		if (dest instanceof IPackageFragment)
			return new CopyCompilationUnitChange(cu, (IPackageFragment)dest, createNewName(cu, (IPackageFragment)dest));

		Assert.isTrue(dest instanceof IContainer);//this should be checked before - in preconditions
		return new CopyResourceChange(getResource(cu), (IContainer)dest, createNewName(getResource(cu), (IContainer)dest));
	}
	
	IChange createChange(IPackageFragment pack) throws JavaModelException{
		IPackageFragmentRoot root= getDestinationForPackages(getDestination());
		String newName= createNewName(pack, root);
		if (JavaConventions.validatePackageName(newName).isOK())
			return new CopyPackageChange(pack, root, newName);
		else{
			if (root.getUnderlyingResource() instanceof IContainer){
				IContainer dest= (IContainer)root.getUnderlyingResource();
				IResource res= pack.getCorrespondingResource();
				return new CopyResourceChange(res, dest, createNewName(res, dest));
			}else
				return new NullChange();	
		}	
	}
	
	IChange createChange(IResource res) throws JavaModelException{
		IContainer dest= getDestinationForResources(getDestination());
		return new CopyResourceChange(res, dest, createNewName(res, dest));
	}}