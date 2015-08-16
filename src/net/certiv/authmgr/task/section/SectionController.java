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
package net.certiv.authmgr.task.section;

import net.certiv.authmgr.app.preferences.Prefs;
import net.certiv.authmgr.app.preferences.PrefsKey;
import net.certiv.authmgr.app.stages.StageEnum;
import net.certiv.authmgr.db.dao.model.Corpus;
import net.certiv.authmgr.db.dao.model.Repository;
import net.certiv.authmgr.scheduler.task.AbstractController;
import net.certiv.authmgr.scheduler.task.AbstractTask;
import net.certiv.authmgr.task.section.core.SectionTask;

public class SectionController extends AbstractController {

	public static final String ID = "net.certiv.authmgr.task.section.SectionController";

	public SectionController() {
		super();
		state = StageEnum.CLASSIFYLINES;
		enabled = Prefs.getBoolean(PrefsKey.SECTION_ENABLED);
	}

	@Override
	public String getDisplayName() {
		return "Section Controller";
	}

	@Override
	public String getDisplayStage() {
		return "Classify document sections";
	}

	@Override
	public AbstractTask taskFactory(Repository rep, Corpus cor, int interval, int count) {
		return new SectionTask(rep, cor, interval, count);
	}
}
