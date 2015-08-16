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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.certiv.authmgr.app.util.Log;
import net.sf.classifier4J.ICategorisedClassifier;
import net.sf.classifier4J.bayesian.WordsDataSourceException;

/**
 * DataSource used by BayesianClassifier to persistantly manage the word sets in multiple
 * named categories. As stored, the data is a gzipped, XML encoded representation of the
 * in-memory HashMap (categories) of HashMaps of word probabilities.
 * <p>
 * Typical multiple partition use (training): <code>
 * 		PersistantWordsDataSource pds = new PersistantWordsDataSource();
 * 		BayesianPartitionClassifier classifier = new BayesianPartitionClassifier(pds, new DefaultTokenizer(DefaultTokenizer.BREAK_ON_WHITESPACE));
 * 		classifier.teachMatch("categoryname1", "partition1", "#1set of words matches partion one");
 * 		classifier.teachMatch("categoryname1", "partition2", "#2set of words goes with partition two");
 * 		classifier.teachMatch("categoryname1", "partition2", "#3group of words matches partition two also");
 * 		pds.save("/path/name");
 * </code>
 * <p>
 * Typical multiple partition use (query): <code>
 * 		PersistantWordsDataSource pds = new PersistantWordsDataSource("/path/name");
 * 		BayesianClassifier classifier = new BayesianClassifier(pds, new DefaultTokenizer(DefaultTokenizer.BREAK_ON_WHITESPACE));
 * 		double partitionLable1 = classifier.classify("categoryname1", "my string of words");
 * </code>
 * <p>
 * The result of the partitioned classification is not a score value, but an index into
 * the list of partition labels. This breaks the interface contract for this method
 * relative to the simple BayesianClassifier, but would otherwise require a complete
 * redesign of the interface structure. So for now we tolerate it.
 * 
 * @author Gbr
 * @version 0.1
 */
public class PersistantWordsDataSource implements IPartitionedWordsDataSource {

	/** Top level in-memory data structure storing the model as a hashmap of hashmaps. */
	private HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>> categories;
	// private boolean xmlStore;
	// private boolean compressedStore;

	private static final String DEFAULT_PARTITION = "DEFAULT";

	/**
	 * Creates a new persistant data source initialized to an empty category HashMap.
	 */
	public PersistantWordsDataSource() {
		categories = new HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>>();
	}

	/**
	 * Creates a new persistant data source.
	 * 
	 * @param modelName Canonical name of the file containing the model.
	 */
	public PersistantWordsDataSource(String modelName) {
		loadModel(modelName);
	}

	/**
	 * Loads a model from a named file. Creates a new model if none exists.
	 * 
	 * @param modelName The canonical name.
	 * @return The HashMap data structure contained in the file.
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>> loadModel(String modelName) {
		try {
			// keep an instance reference to the data file
			categories = (HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>>) modelReader(
					new File(modelName));
			return categories;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>> m1 = new HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>>();
		HashMap<String, HashMap<String, WordProbabilityPT>> m2 = new HashMap<String, HashMap<String, WordProbabilityPT>>();
		HashMap<String, WordProbabilityPT> m3 = new HashMap<String, WordProbabilityPT>();
		m2.put(DEFAULT_PARTITION, m3);
		m1.put(ICategorisedClassifier.DEFAULT_CATEGORY, m2);
		saveModel(modelName, m1);
		categories = m1;
		return categories;
	}

	/**
	 * Saves the current model to a named file.
	 * 
	 * @param modelName Canonical name of the file to save to.
	 */
	public void saveModel(String modelName) {
		saveModel(modelName, categories);
	}

	/**
	 * Saves a model to a named file.
	 * 
	 * @param modelName Canonical name of the file to save to.
	 * @param data The HashMap data object to be saved.
	 */
	public void saveModel(String modelName, HashMap<String, HashMap<String, HashMap<String, WordProbabilityPT>>> data) {
		try {
			modelWriter(new File(modelName), data);
		} catch (IOException e) {
			Log.warn(this, "Save Model I/O failure.");
			e.printStackTrace();
		}
	}

	/**
	 * Creates a reader for a given file. Detects whether the file is gzipped or not based
	 * on whether the suffix contains ".gz".
	 * 
	 * @param f The File in which the object is stored.
	 * @return The object contained in the file.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Object modelReader(File f) throws ClassNotFoundException, IOException {
		ObjectInput input;
		// XMLDecoder input;
		Object data;

		if (f.getName().endsWith(".gz")) {
			input = new ObjectInputStream(new GZIPInputStream(new FileInputStream(f)));
			// input = new XMLDecoder(new GZIPInputStream(new FileInputStream(f)));
		} else {
			input = new ObjectInputStream(new FileInputStream(f));
			// input = new XMLDecoder(new FileInputStream(f));
		}
		data = input.readObject();
		input.close();
		return data;
	}

	/**
	 * Creates a writer for a given file. Detects whether the file is gzipped or not based
	 * on whether the suffix contains ".gz". Writes the given object to the file.
	 * 
	 * @param model The File in which the model is stored.
	 * @throws IOException
	 */
	private void modelWriter(File f, Object data) throws IOException {

		ObjectOutput out;
		// XMLEncoder out;

		// Serialize to a file
		if (f.getName().endsWith(".gz")) {
			// Log.warn(this, "Compressing model to " + f.getName());
			out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(f)));
			// out = new XMLEncoder(new GZIPOutputStream(new FileOutputStream(f)));
		} else {
			out = new ObjectOutputStream(new FileOutputStream(f));
			// out = new XMLEncoder(new FileOutputStream(f));
		}
		out.writeObject(data);
		out.close();
	}

	/**
	 * Returns the word probability of the given word.
	 * 
	 * @param category The category to check against
	 * @param partition The partition to check against
	 * @param word The word to calculate the probability of
	 * @return The word probability if the word exists, null otherwise;
	 */
	@SuppressWarnings("rawtypes")
	public WordProbabilityPT getWordProbability(String category, String partition, String word) {

		if (categories.containsKey(category)) {
			HashMap partitions = categories.get(category);
			if (partitions.containsKey(partition)) {
				HashMap words = (HashMap) partitions.get(partition);
				if (words.containsKey(word)) {
					return (WordProbabilityPT) words.get(word);
				}
			}
		}
		return null;
	}

	/* old: P(Wj) = [Wj{Bi}/(Wj{S} - Wj{Bi})] [(E{S} - E{Bi})/ E{Bi}] */

	/**
	 * After training a partition, update the word probability factors for each of the
	 * words in the partition. The raw probability of an individual word term is
	 * <p>
	 * P(Wj) = A / (A + B), where
	 * <p>
	 * A = (Wj{Bi}/ E{Bi}) <br>
	 * B = (Wj{S} - Wj{Bi}) / (E{S} - E{Bi})
	 * <p>
	 * where <br>
	 * E{Bi} is the count of partition training messages, and <br>
	 * E{S} is the total number of training messages.
	 * <p>
	 * The raw word probability P(Wj) is thus calculated as the ratio of the number of
	 * occurrences of a given word in the messages of a given partition and the number of
	 * occurrences of that term in the messages of all other partitions of the full sample
	 * space.
	 * <p>
	 * A normalized word probability nP(Wj) is
	 * <p>
	 * nP(Wj) = (0.5 + [Wj{Bi} * P(Wj)]) / (1 + Wj{Bi})
	 * 
	 * @param category the category to update
	 * @param partition the label of the partition to update
	 * @param totals the label of the reserved partition that contains the word term
	 *            totals for the category.
	 * @param cnt
	 * @param cntTotal
	 */
	@SuppressWarnings("rawtypes")
	public void updateWordProbabilities(String category, String partition, String totals, int cnt, int cntTotal) {
		if (categories.containsKey(category)) {
			HashMap partitions = categories.get(category);
			if (partitions.containsKey(partition) && partitions.containsKey(totals)) {
				HashMap words = (HashMap) partitions.get(partition);
				HashMap sums = (HashMap) partitions.get(totals);
				for (Iterator it = words.keySet().iterator(); it.hasNext();) {
					String word = (String) it.next();
					WordProbabilityPT wp = (WordProbabilityPT) words.get(word);
					WordProbabilityPT wps = (WordProbabilityPT) sums.get(word);
					double mcnt = wp.getMatchingCount();
					double tcnt = wps.getMatchingCount();
					double A = mcnt / cnt;
					double B = (tcnt - mcnt) / (cntTotal - cnt);
					double ratio = A / (A + B);
					ratio = (0.5f + (mcnt * ratio)) / (1.0f + mcnt);
					wp.setProbability(ratio);
					wp.setTrainingPartition(cnt);
					wp.setTrainingCategory(cntTotal);
					Log.debug(this, "WordProbability: " + word + "(" + mcnt + "/" + tcnt + "=" + ratio + ")");
				}
				return;
			}
		}
		Log.warn(this, "Word prbabilities update failed.");
	}

	/**
	 * Add a matching word to the data source in the given partition of the given
	 * category.
	 * 
	 * @param category The category to be modified.
	 * @param partition The partition to be modified.
	 * @param word the word that matches
	 */
	public void addMatch(String category, String partition, String word) throws WordsDataSourceException {
		HashMap<String, HashMap<String, WordProbabilityPT>> partitions;
		HashMap<String, WordProbabilityPT> words;

		if (categories.containsKey(category)) {
			partitions = categories.get(category);
			if (partitions.containsKey(partition)) {
				words = partitions.get(partition);
			} else {
				words = new HashMap<String, WordProbabilityPT>();
				partitions.put(partition, words);
			}
		} else {
			partitions = new HashMap<String, HashMap<String, WordProbabilityPT>>();
			categories.put(category, partitions);
			words = new HashMap<String, WordProbabilityPT>();
			partitions.put(partition, words);
		}
		WordProbabilityPT wp;
		if (words.containsKey(word)) {
			wp = words.get(word);
			wp.setMatchingCount(wp.getMatchingCount() + 1);
		} else {
			wp = new WordProbabilityPT(word, 1, 0);
			words.put(word, wp);
		}
	}

	/**
	 * Produces an array listing of the public partition labels within the given category.
	 * Excludes the given non-public, i.e., reserved partition labels.
	 * 
	 * @param category the partition to check
	 * @param nonPublics a space separated series of reserved partition labels.
	 * @return the array list of public partition labels.
	 * @throws WordsDataSourceException
	 */
	public String[] getPartitionList(String category, String nonPublics) throws WordsDataSourceException {
		String[] fullList = getPartitionList(category);
		List<String> l = new LinkedList<String>(Arrays.asList(fullList));
		String[] npa = nonPublics.split("\\s");
		for (String element : npa) {
			l.remove(element);
		}
		return l.toArray(new String[l.size()]);
	}

	/**
	 * Produces an array listing of all of the partition labels, including for any
	 * reserved partitions, that exist in the given category.
	 * 
	 * @param category the category to check
	 * @return an array containing the partition labels or null if either the category
	 *         does not exist or there are not multiple partitions in the category.
	 * @throws WordsDataSourceException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String[] getPartitionList(String category) throws WordsDataSourceException {
		if (categories.containsKey(category)) {
			HashMap partitions = categories.get(category);
			if (partitions.size() > 1) {
				String[] sa = new String[partitions.size()];
				return (String[]) partitions.keySet().toArray(sa);
			}
		}
		return null;
	}

	/**
	 * Returns the matched, not unique, word count for the given category. Since the
	 * entire data set will be contained within a single partition, the word count total
	 * corresponds to a total count of the samples within the sample space.
	 * 
	 * @param category the category to search
	 * @return a count of the matched words
	 */
	public int getMatchWordCount(String category) throws WordsDataSourceException {
		int count = 0;
		String[] partitionList = getPartitionList(category);
		if (partitionList != null) {
			for (String element : partitionList) {
				count += getMatchWordCount(category, element);
			}
		}
		return count;
	}

	/**
	 * Returns the matched, not unique, word count for the given partition within the give
	 * category. This corresponds to the partial count of samples occuring within a given
	 * partition of the sample space.
	 * 
	 * @param category the category to search
	 * @param partition the partition to total
	 * @return a count of the matched words
	 */
	@SuppressWarnings("rawtypes")
	public int getMatchWordCount(String category, String partition) {
		int count = 0;
		if (categories.containsKey(category)) {
			HashMap partitions = categories.get(category);
			if (partitions.containsKey(partition)) {
				HashMap wordsMap = (HashMap) partitions.get(partition);
				for (Iterator it = wordsMap.keySet().iterator(); it.hasNext();) {
					String key = (String) it.next();
					WordProbabilityPT wp = (WordProbabilityPT) wordsMap.get(key);
					count += wp.getMatchingCount();
				}
			}
		}
		return count;
	}
}
