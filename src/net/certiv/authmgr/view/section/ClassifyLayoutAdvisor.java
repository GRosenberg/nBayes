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

import net.certiv.authmgr.grid.WrapLayoutAdvisor;

import org.agilemore.agilegrid.AgileGrid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;

public class ClassifyLayoutAdvisor extends WrapLayoutAdvisor {

	public ClassifyLayoutAdvisor(AgileGrid grid) {
		super(grid);
	}

	@Override
	public String getTopHeaderLabel(int col) {
		switch (col) {
			case 0:
				return "Page/Line";
			case 1:
				return "Section";
			case 2:
				return "Content";
			default:
				return super.getTopHeaderLabel(col);
		}
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public int getColumnWidth(int col) {
		if (col == getColumnCount() - 1) {
			int tot = 0;
			for (int idx = 0; idx < getColumnCount() - 1; idx++) {
				tot += getColumnWidth(idx);
			}
			int width = agileGrid.getClientArea().width - tot;
			width -= agileGrid.getLinePixels() * (getColumnCount() + 1);
			if (isLeftHeaderVisible()) {
				width -= getLeftHeaderWidth();
				width -= agileGrid.getLinePixels();
			}
			if (width != super.getColumnWidth(col)) {
				setColumnWidth(col, width);
			}
		}
		return super.getColumnWidth(col);
	}

	@Override
	public void setColumnWidth(int col, int value) {
		super.setColumnWidth(col, value);
	}

	@Override
	public int getRowCount() {
		ClassifyContentProvider cp = (ClassifyContentProvider) agileGrid.getContentProvider();
		return cp.getRowCount();
	}

	@Override
	public int getRowHeight(int row) {
		return super.getRowHeight(row);
	}

	@Override
	public int getInitialRowHeight(int row) {
		ClassifyContentProvider cp = (ClassifyContentProvider) agileGrid.getContentProvider();
		String content = (String) cp.getContentAt(row, getColumnCount() - 1);
		if (content != null && content.length() > 0) {
			Text t = new Text(agileGrid, SWT.MULTI | SWT.WRAP | SWT.LEAD);
			int w = getColumnWidth(getColumnCount() - 1);
			t.setBounds(5000, 0, w, 100);
			t.setText(content);
			int lc = t.getLineCount();
			if (lc > 10) lc = 10;
			int lh = t.getLineHeight();
			t.dispose();
			int rowHeight = lc * lh + 4;
			setRowHeight(row, rowHeight);
			return rowHeight;
		}
		return super.getInitialRowHeight(row);
	}

	@Override
	public void setRowHeight(int row, int value) {
		super.setRowHeight(row, value);
	}
}
