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
package net.certiv.authmgr.task.section.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import net.certiv.authmgr.app.preferences.Prefs;
import net.certiv.authmgr.app.preferences.PrefsKey;
import net.certiv.authmgr.app.stages.StageEnum;
import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.db.clients.DocumentServiceClient;
import net.certiv.authmgr.db.dao.elems.DocPageList;
import net.certiv.authmgr.db.dao.elems.NoteDataList;
import net.certiv.authmgr.db.dao.model.Corpus;
import net.certiv.authmgr.db.dao.model.DocStage;
import net.certiv.authmgr.db.dao.model.Document;
import net.certiv.authmgr.db.dao.model.Repository;
import net.certiv.authmgr.db.dao.service.DocumentServiceDao;
import net.certiv.authmgr.scheduler.task.AbstractTask;
import net.certiv.authmgr.scheduler.task.Op;
import net.certiv.nav.events.PersistException;

import org.eclipse.core.runtime.IProgressMonitor;

public class SectionTask extends AbstractTask {

	private static final boolean debug = true;
	private static final StageEnum state = StageEnum.CLASSIFYLINES;

	private DocumentServiceClient docServiceClient;
	@SuppressWarnings("unused")
	private Repository rep;
	private Corpus cor;

	public SectionTask(Repository rep, Corpus corpus, int interval, int count) {
		super();
		this.rep = rep;
		this.cor = corpus;
		setTaskName("Section Task");
		setTaskDescription(corpus.getCorpusName());
		setRepeatCount(repeatCount);
		setInterval(interval);
		setTotalTime(5 * Prefs.getInt(PrefsKey.PROCESS_CONCURRENT));
		announceStatusChange(Op.PENDING);
	}

	public void runDelegate(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		if (debug) Log.debug(this, "Section task run starting");
		announceStatusUpdate("Starting");

		docServiceClient = new DocumentServiceClient();
		docServiceClient.setDocumentService(new DocumentServiceDao());

		monitor.worked(1);

		// get convertable documents
		Collection<Document> docs = getDocuments(docServiceClient, cor, state);
		if (docs.size() == 0) {
			announceStatusUpdate("No documents for sectioning");
			return;
		}

		monitor.worked(1);
		int counter = Prefs.getInt(PrefsKey.PROCESS_CONCURRENT);

		// get the file to process
		for (Document doc : docs) {
			announceStatusUpdate(doc.getRepPathPart() + doc.getRepDocName());
			monitor.worked(1);

			// build and process the document through the section classifier
			DocStage prevStage = getPrevStage(doc, state);
			DocPageList pages = (DocPageList) prevStage.getPages();
			NoteDataList notes = (NoteDataList) prevStage.getNotes();
			String text = prevStage.getText();

			// setup and classify each line using the default model name
			ClassifySections cs = new ClassifySections(this);
			pages = cs.classifyLines(pages);

			announceStatusUpdate("Classified " + doc.getRepPathPart() + doc.getRepDocName());
			monitor.worked(1);

			// save the converted document and update the status
			DocStage stage = new DocStage();
			stage.setStage(doc, state.toString(), text, pages, notes);
			doc.attach(stage);
			doc.setRevisionLevel(doc.getRevisionLevel() + 1);
			doc.setStatus(state.next().toString());
			try {
				docServiceClient.saveOrUpdate(doc);
				announceStatusUpdate("Stored " + doc.getRepPathPart() + doc.getRepDocName());
			} catch (PersistException e) {
				announceStatusUpdate("Store failed " + doc.getRepPathPart() + doc.getRepDocName());
				throw new InvocationTargetException(e, "Failed to save");
			}
			monitor.worked(1);

			// end game
			counter--;
			if (counter == 0) break;
		}
		docServiceClient.close();
	}
}
