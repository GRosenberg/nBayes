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

import net.certiv.authmgr.db.dao.model.Document;
import net.certiv.authmgr.nav.bind.model.AbstractPresModel;
import net.certiv.nav.IDataManager;
import net.certiv.nav.Navigator;
import net.certiv.nav.events.NavigateEvent;
import net.certiv.nav.events.NavigateEventListener;

public class ClassifyPresModel<T extends IDataManager<U>, U> extends AbstractPresModel<T, U>
		implements NavigateEventListener {

	public static final String DOCUMENT = "document";

	/** the bean */
	private Document document;

	public ClassifyPresModel(Navigator<T, U> nav) {
		super(nav);
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		Document oldValue = this.document;
		this.document = document;
		firePropertyChange(DOCUMENT, oldValue, this.document);
	}

	// //////// Navigation Event Support //////////////////////////////////
	public void onBeforeNavigate(NavigateEvent e) throws Exception {
		// REQUIRED: remove content change listener from old instance
		if (getDocument() != null) getDocument().removePropertyChangeListener(getBeanHandler());
	}

	public void onAfterNavigate(NavigateEvent e) throws Exception {
		// REQUIRED: (re)bind the UI to the active cursor record
		setDocument((Document) getNav().getUIObjectAtCursor());
		// add the content change listener to the present instance
		getDocument().addPropertyChangeListener(getBeanHandler());
	}
}
