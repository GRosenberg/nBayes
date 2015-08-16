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
package net.certiv.authmgr.view.section;

import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import net.certiv.authmgr.app.stages.StageEnum;
import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.db.clients.DocumentServiceClient;
import net.certiv.authmgr.db.dao.model.DocStage;
import net.certiv.authmgr.db.dao.model.Document;
import net.certiv.authmgr.db.dao.service.DocumentServiceDao;
import net.certiv.authmgr.nav.bind.Bind;
import net.certiv.authmgr.nav.view.NavigationView;
import net.certiv.authmgr.nav.view.tree.ITreeNode;
import net.certiv.authmgr.nav.view.tree.TreeNode;
import net.certiv.nav.Navigator;
import net.certiv.nav.beans.swt.NavTB;
import net.certiv.nav.events.PersistException;

@SuppressWarnings("deprecation")
public class ClassifyView extends ViewPart {

	private static final boolean debug = true;

	public static final String ID = "net.certiv.authmgr.view.section.ClassifyView";

	private static final NavTB[] functions = { NavTB.SEPARATOR, NavTB.SAVE, NavTB.SEPARATOR, NavTB.DELETE };

	private Navigator<DocumentServiceClient, Document> nav;
	private DocumentServiceClient serviceClient;
	private ClassifyPresModel<DocumentServiceClient, Document> presModel;
	private ClassifyViewForm form;
	private DataBindingContext dbc;

	public ClassifyView() {
		super();
		if (debug) Log.debug(this, "Constructing view");
		initNavigationControls();
		initSelectionListener();
	}

	@Override
	public void createPartControl(Composite parent) { // construct the form
		if (debug) Log.debug(this, "Constructing part");
		form = new ClassifyViewForm(parent, SWT.NONE);
		form.getNavToolBar().setIcons(true);
		form.getNavToolBar().setLabels(false);
		form.getNavToolBar().loadToolBar(functions);
		form.getNavToolBar().setNavigator(nav);
		form.getNavStateBar().setNavigator(nav);

		// construct the context
		dbc = new DataBindingContext();
		// build the presentation model and link it to the Navigator
		presModel = new ClassifyPresModel<DocumentServiceClient, Document>(nav);
		// Create an observable to observe changes in the view model
		IObservableValue repValue = BeansObservables.observeValue(presModel, ClassifyPresModel.DOCUMENT);

		// Bind the fields to the current observable
		Bind.bind(dbc, form.getDocID(), repValue, Document.ID, Long.class);
		Bind.bind(dbc, form.getTextStatus(), repValue, Document.STATUS);
	}

	@Override
	public void setFocus() {
		// form.getDocRef().setFocus();
	}

	private void initNavigationControls() {
		serviceClient = new DocumentServiceClient();
		serviceClient.setDocumentService(new DocumentServiceDao());
		if (debug) Log.debug(this, "Service client created: " + (serviceClient != null));
		nav = new Navigator<DocumentServiceClient, Document>(serviceClient);
		if (debug) Log.debug(this, "Navigator created: " + (nav != null));
	}

	private void initSelectionListener() {
		// this allows editor selections to be received
		ISelectionService selservice = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		selservice.addPostSelectionListener(new ISelectionListener() {

			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (debug) Log.debug(this, "selectionChanged... ");
				if (part instanceof NavigationView && selection instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) selection;
					if (sel.getFirstElement() instanceof ITreeNode) {
						TreeNode node = (TreeNode) sel.getFirstElement();
						if (node.getRecord() instanceof Document) {
							navTo((Document) node.getRecord());
						}
					}
				}
			}
		});
	}

	protected void navTo(Document document) {
		try {
			serviceClient.clearFetchPlan();
			serviceClient.addToFetchPlan(Document.DOC_STAGES);
			nav.cmdStartQuery();
			Document doc = nav.getUIObjectAtCursor();
			doc.setRepPathPart(document.getRepPathPart());
			doc.setRepDocName(document.getRepDocName());
			nav.cmdExecuteQuery();
			doc = nav.getUIObjectAtCursor();
			Set<DocStage> stages = doc.getDocStages();
			if (debug) Log.debug(this, "Stages " + stages.size());
			ClassifyContentProvider cp = (ClassifyContentProvider) form.grid.getContentProvider();
			if (stages.size() > 0) {
				for (DocStage ds : stages) {
					if (ds.getStage().equals(StageEnum.CLASSIFYLINES.toString())) {
						cp.setRecord(ds);
						break;
					}
				}
			} else {
				cp.setRecord(null);
			}
			form.grid.redraw();
			form.grid.update();
		} catch (PersistException e) {
			Log.debug(this, "ClassifyView fetch failed.", e);
		}
	}
}
