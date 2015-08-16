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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.sf.classifier4J.DefaultTokenizer;
import net.sf.classifier4J.PorterStemmer;

public class CombinedTokenFilter extends DefaultTokenizer {

	StopWordFileProvider swfp;
	PorterStemmer stemmer;

	/**
	 * @throws IOException
	 */
	public CombinedTokenFilter() throws IOException {
		this(DefaultTokenizer.BREAK_ON_WHITESPACE, "DefaultStopWordsList.txt");
	}

	/**
	 * 
	 * @param filename Identifies the name of a textfile on the classpath that contains a
	 *            list of stop words, one on each line
	 */
	public CombinedTokenFilter(String filename) throws IOException {
		this(DefaultTokenizer.BREAK_ON_WHITESPACE, filename);
	}

	/**
	 * 
	 * @param filename Identifies the name of a textfile on the classpath that contains a
	 *            list of stop words, one on each line
	 */
	public CombinedTokenFilter(int tokenizerConfig, String filename) throws IOException {
		super(tokenizerConfig);
		swfp = new StopWordFileProvider(filename);
		stemmer = new PorterStemmer();
	}

	public String[] tokenize(String input) {
		// tokenize, stem, and stop list filter words
		String[] wordsIn = super.tokenize(input);
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(wordsIn));
		for (int idx = list.size() - 1; idx >= 0; idx--) {
			String word1 = list.get(idx);
			String word2 = stemWord(word1);
			if (swfp.isStopWord(word2)) {
				list.remove(idx);
			} else {
				list.set(idx, word2);
			}
		}
		// sort, scan, and remove duplicates
		Collections.sort(list);
		if (list.size() > 0) {
			for (int jdx = 1; jdx < list.size(); jdx++) {
				if (list.get(jdx).equals(list.get(jdx - 1))) {
					list.remove(jdx);
					jdx--;
				}
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * Convenience wrapper to perform the conversions necessary for stemming a single
	 * word.
	 * 
	 * @param word the word to stem.
	 * 
	 * @return the stemmed word.
	 */
	private String stemWord(String word) {
		char[] ca = word.toCharArray();
		stemmer.add(ca, ca.length);
		stemmer.stem();
		return stemmer.toString();
	}
}
