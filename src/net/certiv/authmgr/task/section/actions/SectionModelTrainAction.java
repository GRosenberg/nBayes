/*******************************************************************************
 * Copyright (c) 2003-2015 G Rosenberg.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *		G Rosenberg - initial API and implementation
 *******************************************************************************/
package net.certiv.authmgr.task.section.actions;

import net.certiv.authmgr.task.section.model.TrainClassifierModel;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate. The action proxy will be
 * created by the workbench and shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be delegated to it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class SectionModelTrainAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	/**
	 * The constructor.
	 */
	public SectionModelTrainAction() {}

	/**
	 * The action has been activated. The argument of the method represents the 'real'
	 * action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		TrainClassifierModel trainer = new TrainClassifierModel(window.getShell());
		trainer.trainBCModel();
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of the 'real'
	 * action here if we want, but this can only happen after the delegate has been
	 * created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {}

	/**
	 * We can use this method to dispose of any system resources we previously allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {}

	/**
	 * We will cache window object in order to be able to provide parent shell for the
	 * message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}
