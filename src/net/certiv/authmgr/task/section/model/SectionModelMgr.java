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
package net.certiv.authmgr.task.section.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.certiv.authmgr.app.preferences.Prefs;
import net.certiv.authmgr.app.preferences.PrefsKey;
import net.certiv.authmgr.app.util.Util;
import net.certiv.authmgr.db.dao.elems.DocLine;
import net.certiv.authmgr.db.dao.elems.DocPage;
import net.certiv.authmgr.db.dao.elems.DocPageList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class SectionModelMgr {

	private static final String eol = System.getProperty("line.separator");
	private static final String[] exts = { "*.txt", "*.*" };

	private Shell parent;
	private String projectDir;
	private String nameBCPattern;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 */
	public SectionModelMgr(Shell parent) {
		this.parent = parent;
	}

	public void saveBCTrainingData(String sourceName, DocPageList docStructure) {
		projectDir = Prefs.getString(PrefsKey.PROJECT_DIR);
		nameBCPattern = Prefs.getString(PrefsKey.BC_NAME_PATTERN);

		String fileName = sourceName;
		if (fileName != null && fileName.length() > 0) {
			if (!fileName.startsWith(nameBCPattern)) {
				fileName = nameBCPattern + fileName;
			}
			int prd = fileName.lastIndexOf('.');
			if (prd != -1) {
				fileName = fileName.substring(0, prd) + ".txt";
			}
		}

		FileDialog fd = new FileDialog(parent, SWT.OPEN);
		fd.setText("Save training data");
		fd.setFilterPath(projectDir);
		fd.setFileName(fileName);
		fd.setFilterExtensions(exts);
		fileName = fd.open();

		File file = new File(fileName);
		if (file.exists()) {
			String title = "Confirm overwrite";
			String message = "Overwrite " + fileName + "?";
			boolean yes = MessageDialog.openConfirm(parent, title, message);
			if (yes) {
				file.delete();
			} else {
				return;
			}
		}

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			for (int pageNum = 0; pageNum < docStructure.size(); pageNum++) {
				DocPage page = docStructure.getDocPageAtIdx(pageNum);
				for (int lineNum = 0; lineNum < page.size(DocPage.RANK_ORDER); lineNum++) {
					DocLine line = page.getDocLine(lineNum, DocPage.RANK_ORDER);
					String lsec = DocLine.partitions[line.lineSection].trim();
					String lcon = Util.tokenFilterAll(line.lineContent).trim();
					String lst2 = line.lineStructure2.trim();
					String s = lsec + " " + lcon + " " + lst2;
					out.write(s + eol);
				}
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
