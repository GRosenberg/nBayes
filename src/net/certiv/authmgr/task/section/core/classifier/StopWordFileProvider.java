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
package net.certiv.authmgr.task.section.core.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.sf.classifier4J.IStopWordProvider;

public class StopWordFileProvider implements IStopWordProvider {

	private File sourcefile;
	private String[] words;

	public static final String DEFAULT_STOPWORD_PROVIDER_RESOURCENAME = "defaultStopWords.txt";

	/**
	 * @param filename Identifies the name of a textfile on the classpath that contains a
	 *            list of stop words, one on each line
	 */
	public StopWordFileProvider(String filename) throws IOException {
		sourcefile = new File(filename);
		init();
	}

	public StopWordFileProvider() throws IOException {
		this(DEFAULT_STOPWORD_PROVIDER_RESOURCENAME);
	}

	protected void init() throws IOException {
		ArrayList<String> wordsLst = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(sourcefile));

		String word;
		while ((word = reader.readLine()) != null) {
			wordsLst.add(word.trim());
		}
		words = wordsLst.toArray(new String[wordsLst.size()]);
		Arrays.sort(words);
	}

	/**
	 * @see net.sf.classifier4J.IStopWordProvider#isStopWord(java.lang.String)
	 */
	public boolean isStopWord(String word) {
		return (Arrays.binarySearch(words, word) >= 0);
	}
}
