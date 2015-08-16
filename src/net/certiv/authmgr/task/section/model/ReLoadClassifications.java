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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import net.certiv.authmgr.app.preferences.Prefs;
import net.certiv.authmgr.app.preferences.PrefsKey;
import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.db.dao.elems.DocLine;
import net.certiv.authmgr.db.dao.elems.DocPage;
import net.certiv.authmgr.db.dao.elems.DocPageList;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class ReLoadClassifications {

	private Shell parent;
	private String fileName;
	private String nameBCPattern;
	private String projectDir;

	public ReLoadClassifications(Shell parent) {
		this.parent = parent;
	}

	public void reLoad(String fname, DocPageList docStructure) {
		this.fileName = fname;

		projectDir = Prefs.getString(PrefsKey.PROJECT_DIR);
		nameBCPattern = Prefs.getString(PrefsKey.BC_NAME_PATTERN);

		int pd = fileName.lastIndexOf('.');
		String baseName = fileName.substring(0, pd);
		String bcFileName = nameBCPattern + baseName + ".txt";
		File bcfile = new File(projectDir + File.separator + bcFileName);
		if (bcfile.exists()) {
			String title = "Reload section classifications";
			String message = "Reload from \"" + bcFileName + "\"?";
			boolean yes = MessageDialog.openQuestion(parent, title, message);
			if (yes) { // reload the stored classifications
				doReload(docStructure, bcfile);
			}
		} else {
			Log.warn(this, "Classification file not found: " + bcFileName);
		}
	}

	private void doReload(DocPageList docStructure, File f) {
		DocPageList dpl = docStructure;
		File file = f;
		int count = 0;
		int lines = 0;
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String str;
			for (int pgIdx = 0; pgIdx < dpl.size(); pgIdx++) {
				DocPage dp = docStructure.getDocPageAtIdx(pgIdx);
				for (int ln = 0; ln < dp.size(DocPage.RANK_ORDER); ln++) {
					DocLine dl = dp.getDocLine(ln, DocPage.RANK_ORDER);
					if ((str = in.readLine()) != null) {
						String[] training = str.split("\\s", 2); // pick off category name
						if (dl.lineSection != DocLine.getSectionIndex(training[0])) {
							String pgStr = StringUtils.leftPad("" + (pgIdx + 1), 3, "0");
							String lnStr = StringUtils.leftPad("" + ln, 2, "0");
							Log.warn(this, "[" + pgStr + ":" + lnStr + "] Classed as: "
									+ DocLine.partitions[dl.lineSection] + "; should be: " + training[0]);
							dl.lineSection = DocLine.getSectionIndex(training[0]);
							count++;
						}
						lines++;
					} else {
						Log.warn(this, "Ran out of classification lines.");
					}
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String percent = "" + (((lines - count) * 100) / lines) + "% accuracy";
		Log.warn(this, "Correction count: " + count + ":" + lines + " (" + percent + ")");
	}
}
