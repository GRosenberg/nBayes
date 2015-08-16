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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import net.certiv.authmgr.app.preferences.Prefs;
import net.certiv.authmgr.app.preferences.PrefsKey;
import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.task.section.core.ClassifySections;
import net.certiv.authmgr.task.section.core.classifier.BayesPartitionClassifier;
import net.certiv.authmgr.task.section.core.classifier.CombinedTokenFilter;
import net.certiv.authmgr.task.section.core.classifier.PersistantWordsDataSource;
import net.sf.classifier4J.bayesian.WordsDataSourceException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public class TrainClassifierModel {

	private Shell parent;

	private String projectDir;
	private String currentClassify;
	private String generatedClassify;
	private String nameBCPattern;

	private String stopWords;

	/**
	 * Constructor.
	 * 
	 * @param parent
	 */
	public TrainClassifierModel(Shell parent) {
		this.parent = parent;
	}

	private void loadPreferences() {
		projectDir = Prefs.getString(PrefsKey.PROJECT_DIR);
		currentClassify = Prefs.getString(PrefsKey.CUR_CLASSIFY);
		generatedClassify = Prefs.getString(PrefsKey.BAK_CLASSIFY);
		nameBCPattern = Prefs.getString(PrefsKey.BC_NAME_PATTERN);
		stopWords = Prefs.getString(PrefsKey.STOP_WORDS_LIST);
	}

	/**
	 * Create a trained model.
	 */
	public void trainBCModel() {
		loadPreferences();

		String title = "Train Classifier Model";
		String source = projectDir + File.separator + nameBCPattern + "*";
		String message = "Create --\r\n" + currentClassify + "\r\n  -- from --\r\n" + source;
		boolean yes = MessageDialog.openConfirm(parent, title, message);
		if (!yes) return;

		Log.info(this, "Training BC Model");

		// filter specifically for BC training files
		FileFilter fileFilter = new FileFilter() {

			public boolean accept(File file) {
				String name = file.getName();
				return file.isFile() && name.startsWith(nameBCPattern);
			}
		};

		// List of filtered files as input
		File f = new File(projectDir);
		File[] files = f.listFiles(fileFilter);
		Log.info(this, "Training on " + files.length + " files.");

		// Create the classifier
		CombinedTokenFilter tok;
		try {
			tok = new CombinedTokenFilter(stopWords);
		} catch (IOException e) {
			MessageDialog.openError(parent, title, "Failed to read StopWordsList");
			return;
		}
		PersistantWordsDataSource pds = new PersistantWordsDataSource();
		BayesPartitionClassifier classifier = new BayesPartitionClassifier(pds, tok);

		try {
			// Process files through the classifier to generate the Model
			int minLength = 100;
			int maxLength = 1;
			classifier.initTrainingCount();
			for (File bcFile : files) { // int idx = 0; idx < files.length; idx++) {
				Log.info(this, "File: " + bcFile.getPath());

				try {
					BufferedReader in = new BufferedReader(new FileReader(bcFile));
					String str;
					int count = 0;
					while ((str = in.readLine()) != null) {
						if (count % 15 == 0) {
							Log.info(this, "" + count);
						}
						// pick apart the training to get the partition name
						String[] training = str.split("\\s", 2);
						classifier.teachMatch(ClassifySections.categoryFixed, training[0], training[1]);
						count++;
						String[] counts = training[1].split("\\s");
						if (counts.length > maxLength) {
							maxLength = counts.length;
						}
						if (counts.length < minLength) {
							minLength = counts.length;
						}
					}
					Log.info(this, "" + count);
					in.close();
					// if (i >= 0) break; // cut short training for testing
				} catch (FileNotFoundException e) {
					MessageDialog.openError(parent, title, "Failed to open data file");
					Log.error(this, "Failed to find data file", e);
					return;
				} catch (IOException e) {
					MessageDialog.openError(parent, title, "Failed to read data file");
					Log.error(this, "Failed to read data file", e);
					return;
				}
			}
			Log.info(this, "Min/Max training lengths: " + minLength + ":" + maxLength);
			// assign word term probabilities to complete the training
			classifier.updateWordProbabilites(ClassifySections.categoryFixed);
		} catch (WordsDataSourceException e) {
			MessageDialog.openError(parent, title, "Failed to teach/update classifier model");
			Log.error(this, "Classifier training error", e);
			return;
		}

		File curFile = new File(currentClassify);
		if (curFile.exists()) {
			File genFile = new File(generatedClassify);
			if (genFile.exists()) genFile.delete();
			curFile.renameTo(genFile);
		}
		curFile = null;
		// and then save the resulting model
		pds.saveModel(currentClassify);

		// Analyze the model
		AnalyzeModel am = new AnalyzeModel();
		am.doAnalysis();
		Log.info(this, "Training BC Model Completed.");
	}
}
