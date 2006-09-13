/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.refactoring.descriptors;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

import org.eclipse.jdt.internal.core.refactoring.descriptors.DescriptorMessages;

/**
 * Refactoring descriptor for the move static members refactoring.
 * <p>
 * An instance of this refactoring descriptor may be obtained by calling
 * {@link RefactoringContribution#createDescriptor()} on a refactoring
 * contribution requested by invoking
 * {@link RefactoringCore#getRefactoringContribution(String)} with the
 * appropriate refactoring id.
 * </p>
 * <p>
 * Note: this class is not intended to be instantiated by clients.
 * </p>
 * 
 * @since 3.3
 */
public final class MoveStaticMembersDescriptor extends JavaRefactoringDescriptor {

	/** The delegate attribute */
	private static final String ATTRIBUTE_DELEGATE= "delegate"; //$NON-NLS-1$

	/** The deprecate attribute */
	private static final String ATTRIBUTE_DEPRECATE= "deprecate"; //$NON-NLS-1$

	/** The delegate attribute */
	private boolean fDelegate= false;

	/** The deprecate attribute */
	private boolean fDeprecate= false;

	/** The members attribute */
	private IMember[] fMembers;

	/** The type attribute */
	private IType fType= null;

	/**
	 * Creates a new refactoring descriptor.
	 */
	public MoveStaticMembersDescriptor() {
		super(IJavaRefactorings.MOVE_STATIC_MEMBERS);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void populateArgumentMap() {
		super.populateArgumentMap();
		fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, elementToHandle(getProject(), fType));
		fArguments.put(ATTRIBUTE_DELEGATE, Boolean.valueOf(fDelegate).toString());
		fArguments.put(ATTRIBUTE_DEPRECATE, Boolean.valueOf(fDeprecate).toString());
		for (int index= 0; index < fMembers.length; index++)
			fArguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (index + 1), elementToHandle(getProject(), fMembers[index]));
	}

	/**
	 * Determines whether the delegate for a member should be declared as
	 * deprecated.
	 * 
	 * @param deprecate
	 *            <code>true</code> to deprecate the delegate,
	 *            <code>false</code> otherwise
	 */
	public void setDeprecateDelegate(final boolean deprecate) {
		fDeprecate= deprecate;
	}

	/**
	 * Sets the destination type of the move operation.
	 * 
	 * @param type
	 *            the destination type
	 */
	public void setDestinationType(final IType type) {
		Assert.isNotNull(type);
		fType= type;
	}

	/**
	 * Determines whether the the original members should be kept as delegates
	 * to the moved ones.
	 * 
	 * @param delegate
	 *            <code>true</code> to keep the originals, <code>false</code>
	 *            otherwise
	 */
	public void setKeepOriginal(final boolean delegate) {
		fDelegate= delegate;
	}

	/**
	 * Sets the static members to move.
	 * 
	 * @param members
	 *            the members to move
	 */
	public void setMembers(final IMember[] members) {
		Assert.isNotNull(members);
		fMembers= members;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus validateDescriptor() {
		final RefactoringStatus status= super.validateDescriptor();
		if (fType == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.MoveStaticMembersDescriptor_no_type));
		if (fMembers == null)
			status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.MoveStaticMembersDescriptor_no_members));
		else {
			for (int index= 0; index < fMembers.length; index++) {
				if (fMembers[index] == null) {
					status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.MoveStaticMembersDescriptor_invalid_members));
					break;
				}
			}
		}
		return status;
	}
}