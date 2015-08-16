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

import java.util.HashMap;

import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.db.dao.elems.DocLine;
import net.certiv.authmgr.db.dao.elems.DocPage;
import net.certiv.authmgr.db.dao.elems.DocPageList;
import net.certiv.authmgr.db.dao.model.DocStage;
import net.certiv.authmgr.grid.util.PageIndex;

import org.agilemore.agilegrid.DefaultContentProvider;

public class ClassifyContentProvider extends DefaultContentProvider {

	private DocPageList pages;
	private int rowCount = 0;

	private HashMap<Integer, PageIndex> rowMap = new HashMap<Integer, PageIndex>();

	public ClassifyContentProvider() {
		super();
		setRecord(null);
	}

	public void setRecord(DocStage stage) {
		rowMap.clear();
		rowCount = 0;
		if (stage != null) {
			pages = (DocPageList) stage.getPages();
			for (DocPage page : pages) {
				for (DocLine line : page) {
					rowMap.put(rowCount, new PageIndex(page.getPageNumber(), line.rank));
					rowCount++;
				}
			}
		} else {
			pages = null;
		}
	}

	public int getRowCount() {
		return rowCount;
	}

	@Override
	public Object doGetContentAt(int row, int col) {
		if (pages != null) {
			Log.debug(this, "Fetching [row=" + row + ", col=" + col + "]");
			PageIndex pi = rowMap.get(row);
			DocPage page = pages.getDocPage(pi.page);
			DocLine line = page.getDocLine(pi.line, true);
			switch (col) {
				case 0: // page/line
					return asString(pi.page + ":" + pi.line);
				case 1: // classified section
					return asString(DocLine.partitions[line.lineSection]);
				case 2: // line content
					return asString(line.lineContent);
			}
		}
		return "";
	}

	private String asString(Object obj) {
		if (obj == null) return "<Null>";
		return obj.toString();
	}

	@Override
	public void doSetContentAt(int row, int col, Object value) {}
}
