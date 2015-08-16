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

import org.agilemore.agilegrid.AgileGrid;
import org.agilemore.agilegrid.DefaultCellEditorProvider;

public class ClassifyCellEditorProvider extends DefaultCellEditorProvider {

	public ClassifyCellEditorProvider(AgileGrid grid) {
		super(grid);
	}

	@Override
	public boolean canEdit(int row, int col) {
		return false;
	}
}
