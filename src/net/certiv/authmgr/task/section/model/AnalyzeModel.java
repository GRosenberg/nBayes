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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.certiv.authmgr.app.preferences.Prefs;
import net.certiv.authmgr.app.preferences.PrefsKey;
import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.app.util.Util;
import net.certiv.authmgr.task.section.core.classifier.PersistantWordsDataSource;
import net.certiv.authmgr.task.section.core.classifier.WordProbabilityPT;

import org.apache.commons.lang.StringUtils;

public class AnalyzeModel {

	// private String generatedClassify;

	public AnalyzeModel() {
		// generatedClassify = Prefs.getString(PrefsKey.BAK_CLASSIFY);
		// doAnalysis();
	}

	public void doAnalysis() {
		String modelName = Prefs.getString(PrefsKey.CUR_CLASSIFY);// generatedClassify;
		PersistantWordsDataSource pds = new PersistantWordsDataSource();
		HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>> categories = pds.loadModel(modelName);
		analyzeCategories(pds, categories);
	}

	private void analyzeCategories(PersistantWordsDataSource pds,
			HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>> catsMap) {
		Set<String> catKeys = catsMap.keySet();

		String categoryLabels = "";
		for (Iterator<String> it = catKeys.iterator(); it.hasNext();) {
			categoryLabels = categoryLabels + it.next() + " ";
		}
		Log.info(this, "Category collection: " + catKeys.size() + " = " + categoryLabels);

		// for each category
		for (Iterator<String> it = catKeys.iterator(); it.hasNext();) {
			String category = it.next();
			HashMap<String, HashMap<String, WordProbabilityPT>> partsMap = catsMap.get(category);
			analyzePartitions(pds, category, partsMap);
		}
	}

	private void analyzePartitions(PersistantWordsDataSource pds, String category,
			HashMap<String, HashMap<String, WordProbabilityPT>> partsMap) {

		PartitionKeyComparator keyComp = new PartitionKeyComparator(partsMap);
		TreeSet<String> partKeys = new TreeSet<String>(keyComp);
		partKeys.addAll(partsMap.keySet());

		String partitions = "";
		for (Iterator<String> it = partKeys.iterator(); it.hasNext();) {
			partitions = partitions + it.next() + " ";
		}
		Log.info(this, "Partition collection: " + partKeys.size() + " = " + partitions);

		// for each partition
		for (Iterator<String> it = partKeys.iterator(); it.hasNext();) {
			String partition = it.next();
			HashMap<String, WordProbabilityPT> wordsMap = partsMap.get(partition);
			analyzeWords(category, partition, wordsMap);
		}
	}

	private void analyzeWords(String category, String partition, HashMap<String, WordProbabilityPT> wordsMap) {

		// convert to sorted set
		WordProbPTComparator sorter = new WordProbPTComparator();
		TreeSet<WordProbabilityPT> wordProbs = new TreeSet<WordProbabilityPT>(sorter);
		wordProbs.addAll(wordsMap.values());

		// now accumulate and print statistics
		StringBuffer wordlist = new StringBuffer();
		int k = 0;
		for (Iterator<WordProbabilityPT> it = wordProbs.iterator(); it.hasNext() && k < 20; k++) {
			WordProbabilityPT wp = it.next();
			String word = wp.getWord();
			double prob = wp.getProbability();
			double count = wp.getMatchingCount();

			BigDecimal probBD = new BigDecimal(prob).setScale(8, BigDecimal.ROUND_HALF_UP);
			String countStr = Util.rightAlign("" + count, 6);
			String wordAbbr = StringUtils.abbreviate(word, 13);
			wordlist.append(Util.leftAlign(wordAbbr, 14) + probBD + "/" + countStr + "| ");
		}
		Log.info(this, Util.leftAlign(partition + ":", 14) + Util.rightAlign("" + wordProbs.size(), 5) + " = "
				+ wordlist.toString());
	}

	class WordProbPTComparator implements Comparator<WordProbabilityPT> {

		public int compare(WordProbabilityPT wordA, WordProbabilityPT wordB) {
			double pA = wordA.getProbability();
			double pB = wordB.getProbability();

			// Standard (ascending) comparator behavior:
			// If o1 < o2, return a negative value
			// If o1 == o2, return 0
			// If o1 > o2, return a positive value
			// Do Decending order, so the first N are the highest match counts
			if (pA < pB) {
				return 1;
			} else if (pA > pB) {
				return -1;
			} else {
				// Do Ascending order for alphabetical where counts are equal
				return wordA.getWord().compareTo(wordB.getWord());
			}
		}
	}

	class PartitionKeyComparator implements Comparator<String> {

		private HashMap<String, HashMap<String, WordProbabilityPT>> map;

		public PartitionKeyComparator(HashMap<String, HashMap<String, WordProbabilityPT>> map) {
			this.map = map;
		}

		public int compare(String key1, String key2) {
			int cnt1 = map.get(key1).size();
			int cnt2 = map.get(key2).size();

			// Standard (ascending) comparator behavior:
			// If o1 < o2, return a negative value
			// If o1 == o2, return 0
			// If o1 > o2, return a positive value
			// Do Decending order, so the first N are the highest match counts
			if (cnt1 < cnt2) {
				return 1;
			} else if (cnt1 > cnt2) {
				return -1;
			} else {
				// Do Ascending order for alphabetical where counts are equal
				return key1.compareTo(key2);
			}
		}
	}
}
