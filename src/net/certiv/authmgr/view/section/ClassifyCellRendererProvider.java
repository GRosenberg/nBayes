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

import net.certiv.authmgr.grid.TextWrapCellRenderer;

import org.agilemore.agilegrid.AgileGrid;
import org.agilemore.agilegrid.DefaultCellRendererProvider;
import org.agilemore.agilegrid.ICellRenderer;
import org.agilemore.agilegrid.renderers.HeaderCellRenderer;
import org.agilemore.agilegrid.renderers.TextCellRenderer;

public class ClassifyCellRendererProvider extends DefaultCellRendererProvider {

	private ICellRenderer header;
	private TextWrapCellRenderer wrapCellRenderer;
	private TextCellRenderer textCellRenderer;

	public ClassifyCellRendererProvider(AgileGrid grid) {
		super(grid);
		header = new HeaderCellRenderer(grid, ICellRenderer.STYLE_PUSH | ICellRenderer.INDICATION_SELECTION);

		textCellRenderer = new TextCellRenderer(grid, ICellRenderer.INDICATION_COMMENT);
		textCellRenderer.setAlignment(ICellRenderer.ALIGN_HORIZONTAL_LEFT | ICellRenderer.ALIGN_VERTICAL_TOP);

		wrapCellRenderer = new TextWrapCellRenderer(grid, ICellRenderer.INDICATION_COMMENT);
		wrapCellRenderer.setLineStyle(TextWrapCellRenderer.RIGHT_LINE | TextWrapCellRenderer.BOTTOM_LINE);
		wrapCellRenderer.setAlignment(ICellRenderer.ALIGN_HORIZONTAL_LEFT | ICellRenderer.ALIGN_VERTICAL_TOP);
	}

	@Override
	public ICellRenderer getCellRenderer(int row, int col) {
		if (col < 6) return textCellRenderer;
		if (col == 6) return wrapCellRenderer;
		return super.getCellRenderer(row, col);

	}

	@Override
	public ICellRenderer getLeftHeadRenderer(int col) {
		return header;
	}

	@Override
	public ICellRenderer getTopHeadRenderer(int row) {
		return header;
	}
}
