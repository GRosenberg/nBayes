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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.app.util.Util;
import net.sf.classifier4J.DefaultStopWordsProvider;
import net.sf.classifier4J.DefaultTokenizer;
import net.sf.classifier4J.ICategorisedClassifier;
import net.sf.classifier4J.IClassifier;
import net.sf.classifier4J.IStopWordProvider;
import net.sf.classifier4J.ITokenizer;
import net.sf.classifier4J.bayesian.WordsDataSourceException;

import org.apache.commons.lang.StringUtils;

/**
 * A Bayes classifier implementation that determines the partition, or subspace from among
 * a set of subspaces of a sample space Y, with the greatest relative probability of
 * containing a given attribute set of word terms. The classifier returns an
 * identification of the partition for the one subspace that is the best probabilistic
 * match for containing a given attribute set of word terms.
 * <p>
 * Background: <br>
 * ----------- <br>
 * The relative probability of matching a particular partition is calculated by
 * determining the combined relative probabilities of the word terms in a given attribute
 * set. Combined probability is calculated as a product of probabilities of each word term
 * in the attribute set that also occurs in the partition. The standard definition of
 * conditional probability for an event B given event A is given as: P(B|A) = P(A and
 * B)/P(A) which states that the conditional probability of event B given event A is equal
 * to the probability of the compound event (A and B) divided by the probability of event
 * A.
 * <p>
 * Implementation: <br>
 * --------------- <br>
 * In this implementation, a partition is used to associate messages, each a particular
 * attribute set of word terms, either prior existing or new. The association with prior
 * existing messages is established through exemplar training of messages as
 * �matching� a specified partition. The partition includes, for each word term, the
 * prior probability of occurrence of that term in messages matching the partition. New
 * messages are used as a query to determine the probabalisitic association of particular
 * messages with the individual partitions.
 * <p>
 * Bi is a partition that is one of a set of partitions Bi, i=1..n. <br>
 * Eo is a message that consists of an attribute set of word terms Wj, j=1..m.
 * <p>
 * The problem is to determine the partition Bi that is the closest probabilistic match
 * for a present message Eo, given the assumption that every message E is classifiable
 * into one of the partitions Bi. That is, max[P(Bi|Eo)]i=1..n which states the maximum
 * value of the conditional probability that message Eo is in partition Bi.
 * <p>
 * To calculate P(Bi|Eo) for each partition Bi, the Bayes rule can be used:
 * <p>
 * P(Bi|Eo) = [P(Eo|Bi) * P(Bi)]/P(Eo)
 * <p>
 * where P(Eo|Bi) is the conditional probability that the word terms associated with the
 * given partition Bi includes the word terms that occur in Eo; P(Bi) is the probability
 * of the given partition or, equivalently, the probability of any message being in
 * partition Bi; and P(Eo) is the probability of the given message or, equivalently, the
 * probability of any partition corresponding to a given message.
 * <p>
 * Since each computation involves the value P(Eo), it can be ignored as a constant
 * scaling factor. The computation then becomes:
 * <p>
 * P(Bi|Eo) = P(Eo|Bi) * P(Bi), which expands to:
 * <p>
 * P(Bi|Wj),j=1..m = P(Wj|Bi),j=1..m * P(Bi)
 * <p>
 * where the term P(Wj|Bi), j=1..m denotes the product of the Wj word term occurrence
 * probabilities, determined for each word term Wx that exists in the set of word terms
 * prior associated with the partition Bi; where for the term P(Wj|Bi), the word term
 * occurrence probability for any individual term Wx is calculated as the number of times
 * Wx appears in Bi divided by the total number of words terms in Bi and where the term
 * P(Bi) is calculated as the total number of word terms prior associated with the
 * partition Bi divided by the total number of word terms prior associated with all of the
 * partitions Bi, i=1..n.
 * <p>
 * Full expansion of the Bayes rule: <br>
 * --------------------------------- <br>
 * To include the denominator term P(Eo) in the Bayes equation
 * <p>
 * P(Bi|E) = [P(E|Bi) * P(Bi)]/P(Eo) recognize that, by definition,
 * <p>
 * P(Eo) = P(EoBi) + P(Eoi(not)Bi)
 * <p>
 * Using the product rule, the right-hand side becomes:
 * <p>
 * P(Eo) = [P(Eo|Bi) * P(Bi)] + [P(Eo|(not)Bi) * P((not)Bi)]
 * <p>
 * which is numerically the same as
 * <p>
 * P(Eo) = [P(Eo|Bi) * P(Bi)] + [[1 - P(Eo|Bi)] * [1 - P(Bi)]]
 * <p>
 * Computation is then easy, since we already have solutions for each of the right-hand
 * side terms.
 * <p>
 * Product of Probabilities, Precision and Underflow Prevention: <br>
 * ------------------------------------------------------------- <br>
 * As a practical matter, the product of probabilities computation required by Bayes is
 * subject to precision loss and, at not unreasonable extremes, floating-point underflow.
 * The loss of precision may not be a problem since we are most interested in the product
 * series that produces the maximum valued probability. Floating-point underflow is a
 * fatal error, however. The standard approach to avoiding the product of probabilities
 * problem is to instead use a sum of logs of the probabilities, since log(x * y) = log(x)
 * + log(y). While this sum of log probability score will be proportional to the product
 * of probabilities score � the partition with highest final un-normalized log
 * probability score will still represent the most probable partition match, the scale and
 * relative distributions among the scores will differ, making analysis of the
 * differential partition scores more difficult to analyze for, e.g., partition parameter
 * distinctiveness. Additionally, calculation of log values is (generally thought)
 * computationally intensive. So, until we can prove out where the true issues lie, do
 * nothing for now.
 * 
 * @author Gbr
 */
public class BayesPartitionClassifier {

	private boolean debug = false;

	PersistantWordsDataSource wordsData;
	ITokenizer tokenizer;
	IStopWordProvider stopWordProvider;

	private boolean isCaseSensitive = false;
	private static final double MAX_PROBABILITY = 1;
	/** the reserved partition to store sample space totals */
	public static final String SPACE_TOTALS = "SPACE_TOTALS";
	/** Accumulator data structure for training counts */
	HashMap<String, HashMap<String, Integer>> categoryCounter = null;

	/**
	 * Default constructor that implements by default a PersistantWordsDataSource and a
	 * default DefaultTokenizer set to BREAK_ON_WORD_BREAKS.
	 */
	public BayesPartitionClassifier() {
		this(new PersistantWordsDataSource(), new DefaultTokenizer(DefaultTokenizer.BREAK_ON_WORD_BREAKS));
	}

	/**
	 * Constructor for BayesianClassifier that specifies a datasource. The
	 * DefaultTokenizer (set to BREAK_ON_WORD_BREAKS) will be used.
	 * 
	 * @param wd a {@link net.sf.classifier4J.bayesian.IWordsDataSource}
	 */
	public BayesPartitionClassifier(PersistantWordsDataSource wd) {
		this(wd, new DefaultTokenizer(DefaultTokenizer.BREAK_ON_WORD_BREAKS));
	}

	/**
	 * Constructor for BayesianClassifier that specifies a datasource & tokenizer
	 * 
	 * @param wd a {@link net.sf.classifier4J.bayesian.IWordsDataSource}
	 * @param tokenizer a {@link net.sf.classifier4J.ITokenizer}
	 */
	public BayesPartitionClassifier(PersistantWordsDataSource wd, ITokenizer tokenizer) {
		this(wd, tokenizer, new DefaultStopWordsProvider());
	}

	/**
	 * Constructor for BayesianClassifier that specifies a datasource, tokenizer and stop
	 * words provider
	 * 
	 * @param wd a {@link net.sf.classifier4J.bayesian.IPartitionedWordsDataSource}
	 * @param tokenizer a {@link net.sf.classifier4J.ITokenizer}
	 * @param swp a {@link net.sf.classifier4J.IStopWordProvider}
	 */
	public BayesPartitionClassifier(PersistantWordsDataSource wd, ITokenizer tokenizer, IStopWordProvider swp) {
		if (wd == null) {
			throw new IllegalArgumentException("IWordsDataSource can't be null");
		}
		this.wordsData = wd;

		if (tokenizer == null) {
			throw new IllegalArgumentException("ITokenizer can't be null");
		}
		this.tokenizer = tokenizer;

		if (swp == null) {
			throw new IllegalArgumentException("IStopWordProvider can't be null");
		}
		this.stopWordProvider = swp;
	}

	// ///////////////////////////////////////////////////////////////////////////
	/**
	 * Classify the given string against the default category. The return value is a map
	 * of the partition labels and the partial probability score for each label. The
	 * highest score corresponds to the best matching partition.
	 * 
	 * @param input the string to classify
	 * @return a List of the partitions and corresponding partial probability scores from
	 *         the classification.
	 */
	public String classify(String input) throws WordsDataSourceException {
		return classify(ICategorisedClassifier.DEFAULT_CATEGORY, input);
	}

	/**
	 * Classify the given string against the given category. The return value is a map of
	 * the partition labels and the partial probability score for each label. The highest
	 * score corresponds to the best matching partition.
	 * 
	 * @param category the category to classify against.
	 * @param input the string to classify
	 * @return the index of the best matching partition or, if only a single partition
	 *         exists in the category, then the match/nonmatch score.
	 */
	public String classify(String category, String input) throws WordsDataSourceException {
		if (category == null) {
			throw new IllegalArgumentException("category cannot be null");
		}
		if (input == null) {
			throw new IllegalArgumentException("input cannot be null");
		}
		checkPartitionsSupported(category);
		return classify(category, tokenizer.tokenize(input));
	}

	/**
	 * Implementation of a partial probability Bayes classifier. Determines the
	 * conditional probability for the given word set in each of the partitions that exist
	 * in the given category. Assumes that the word set is a match to one of the
	 * partitions. Also assumes word term independence per standard naive Bayes.
	 * 
	 * @param category the category to check
	 * @param words the word set to match to a partition
	 * @return the label of the partition with the highest partial probability for the
	 *         given set of words
	 * @author Gbr
	 */
	protected String classify(String category, String words[]) throws WordsDataSourceException {

		PartitionProbabilities results = new PartitionProbabilities(category);
		String[] publicPartitions = wordsData.getPartitionList(category, SPACE_TOTALS);
		if (publicPartitions != null) {
			for (int i = 0; i < publicPartitions.length; i++) {
				String partition = publicPartitions[i];
				if (debug) Log.debug(this, "classify() - Partition: " + partition);
				double score = calculatePartialProbability(category, partition, words);
				BigDecimal scoreBD = new BigDecimal(score).setScale(16, BigDecimal.ROUND_HALF_UP);
				if (debug) Log.debug(this, Util.leftAlign(partition + ":", 14) + scoreBD);
				results.setScore(partition, score); // normaliseSignificance(score));
			}
		}
		return results.maxScorePartition();
	}

	// ///////////////////////////////////////////////////////////////////////////
	public void teachMatch(String category, String partition, String input) throws WordsDataSourceException {
		if (category == null) {
			throw new IllegalArgumentException("category cannot be null");
		}
		if (partition == null) {
			throw new IllegalArgumentException("partition cannot be null");
		}
		if (input == null) {
			throw new IllegalArgumentException("input cannot be null");
		}
		checkPartitionsSupported(category);
		teachMatch(category, partition, tokenizer.tokenize(input));
	}

	/**
	 * Accumulate word term matches against the given category and partition. Also
	 * accumuates a sample space total count in the special reserved partition
	 * "SPACE_TOTALS".
	 * 
	 * @param category the given category to annotate
	 * @param partition the given cagetory to annotate
	 * @param words the word array to accumulate
	 * @throws WordsDataSourceException
	 */
	protected void teachMatch(String category, String partition, String words[]) throws WordsDataSourceException {

		for (int i = 0; i < words.length; i++) {
			if (isClassifiableWord(words[i])) {
				addTrainingCount(category, partition);
				wordsData.addMatch(category, partition, transformWord(words[i]));
				wordsData.addMatch(category, SPACE_TOTALS, transformWord(words[i]));
			}
		}
	}

	/**
	 * Initializes the classifier to begin a training session.
	 */
	public void initTrainingCount() {
		categoryCounter = new HashMap<String, HashMap<String, Integer>>();
	}

	/**
	 * @param category
	 * @param partition
	 */
	private void addTrainingCount(String category, String partition) {
		if (categoryCounter.containsKey(category)) {
			HashMap<String, Integer> partitionCounter = categoryCounter.get(category);
			if (partitionCounter.containsKey(partition)) {
				int cnt = partitionCounter.get(partition).intValue();
				partitionCounter.put(partition, new Integer(cnt + 1));
			} else {
				partitionCounter.put(partition, new Integer(1));
			}
		} else {
			HashMap<String, Integer> partitionCounter = new HashMap<String, Integer>();
			partitionCounter.put(partition, new Integer(1));
			categoryCounter.put(category, partitionCounter);
		}
	}

	/**
	 * Gets the training count for single partition within a given category.
	 * 
	 * @param category
	 * @param partition
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private int getTrainingCount(String category, String partition) {
		if (categoryCounter.containsKey(category)) {
			HashMap partitionCounter = categoryCounter.get(category);
			if (partitionCounter.containsKey(partition)) {
				return ((Integer) partitionCounter.get(partition)).intValue();
			}
		}
		return 0;
	}

	/**
	 * Gets the training count for an entire category.
	 * 
	 * @param category
	 * @return
	 * @throws WordsDataSourceException
	 */
	private int getTrainingCount(String category) throws WordsDataSourceException {
		int pCntTotal = 0;
		String[] partitions = wordsData.getPartitionList(category, SPACE_TOTALS);
		for (int i = 0; i < partitions.length; i++) {
			pCntTotal += getTrainingCount(category, partitions[i]);
		}
		return pCntTotal;
	}

	/**
	 * Updates the word term probabilities in all of the partitions within the given
	 * category. Relies on a special reserved partition "SPACE_TOTALS" to contain sample
	 * space total occurrence counts.
	 * 
	 * @param category
	 * @throws WordsDataSourceException
	 */
	public void updateWordProbabilites(String category) throws WordsDataSourceException {

		int pCntTotal = getTrainingCount(category);
		String[] partitions = wordsData.getPartitionList(category, SPACE_TOTALS);
		for (int i = 0; i < partitions.length; i++) {
			int pCnt = getTrainingCount(category, partitions[i]);
			wordsData.updateWordProbabilities(category, partitions[i], SPACE_TOTALS, pCnt, pCntTotal);
		}
	}

	// ///////////////////////////////////////////////////////////////////////////////
	/**
	 * Determines the Bayesian score for a specific word set.
	 * 
	 * @param category the category to check
	 * @param partition the partition to check
	 * @param words the word set to match to a partition
	 * @throws WordsDataSourceException
	 * @todo need an option to only use the "X" most "important" words when calculating
	 *       overall probability "important" is defined as being most distant from
	 *       NEUTAL_PROBABILITY
	 */
	/* public for testing */
	public double calculatePartialProbability(String category, String partition, String[] words)
			throws WordsDataSourceException {

		WordProbabilityPT[] wps = collectWordProbabilities(category, partition, words);

		if (wps == null || wps.length == 0) {
			return IClassifier.NEUTRAL_PROBABILITY;
		} else {
			// need to calculate P(Eo|Bi) * P(Bi) as numerator
			// and P(Eo|Bi) + [[1 - P(E|Bi)] * [1 - P(Bi)]] as denominator

			double spaceWordsCount = wordsData.getMatchWordCount(category);
			double partitionWordsCount = wordsData.getMatchWordCount(category, partition);
			if (debug) Log.debug(this, "Partial probability over: " + category + ":" + partition + " = "
					+ partitionWordsCount + "/" + spaceWordsCount);

			double pBi = partitionWordsCount / spaceWordsCount;
			double negPBi = 1 - pBi;
			double pEoBi = MAX_PROBABILITY;
			double negPEoBi = MAX_PROBABILITY;
			for (int i = 0; i < wps.length; i++) {
				double wordProbability = wps[i].getProbability();
				pEoBi *= wordProbability;
				negPEoBi *= (1 - wordProbability);
				// report(category, partition, wps[i].getWord(), pBi, pEoBi,
				// wps[i].getProbability());
			}
			double minProbability = (0.001 / spaceWordsCount);
			for (int j = 0; j < words.length - wps.length; j++) {
				pEoBi *= minProbability;
				negPEoBi *= (1 - minProbability);
			}
			double numerator = (pEoBi * pBi);
			double denominator = pEoBi + (negPEoBi * negPBi);
			if (debug) Log.debug(this, "Bayes: " + category + ":" + partition + "> " + numerator + "/" + denominator);
			return numerator / denominator;
		}
	}

	@SuppressWarnings("unused")
	private void report(String category, String partition, String word, double pBi, double pEoB, double wordProb) {
		BigDecimal pBiBD = new BigDecimal(pBi).setScale(5, BigDecimal.ROUND_HALF_UP);
		BigDecimal pEoBiBD = new BigDecimal(pEoB).setScale(8, BigDecimal.ROUND_HALF_UP);
		BigDecimal probBD = new BigDecimal(wordProb).setScale(8, BigDecimal.ROUND_HALF_UP);
		String wordAbbr = StringUtils.abbreviate(word, 13);
		wordAbbr = StringUtils.rightPad(wordAbbr, 14);
		Log.warn(this, "Bayes: " + wordAbbr + pEoBiBD + " (" + probBD + ":" + pBiBD + ")");
	}

	/**
	 * Produces a word probability list continaing only those words of the given word list
	 * that also exist as prior samples in the given partition.
	 * 
	 * @param category the category to check
	 * @param partition the partition to check
	 * @param words the word set to match to a partition
	 * @return array of the existant words term objects
	 * @throws WordsDataSourceException
	 */
	private WordProbabilityPT[] collectWordProbabilities(String category, String partition, String[] words)
			throws WordsDataSourceException {

		if (words == null) {
			return new WordProbabilityPT[0];
		} else {
			List<WordProbabilityPT> wps = new ArrayList<WordProbabilityPT>();
			for (int i = 0; i < words.length; i++) {
				if (isClassifiableWord(words[i])) {
					WordProbabilityPT wp = wordsData.getWordProbability(category, partition, transformWord(words[i]));
					if (wp != null) {
						wps.add(wp);
					}
				}
			}
			return wps.toArray(new WordProbabilityPT[wps.size()]);
		}
	}

	private boolean isClassifiableWord(String word) {
		if (word == null || "".equals(word) || stopWordProvider.isStopWord(word)) {
			return false;
		} else if (word.matches("^\\d+.*") || word.matches(".+\\d{2}.*")) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Allows transformations to be done to word. This implementation transforms the word
	 * to lowercase if the classifier is in case-insenstive mode.
	 * 
	 * @param word
	 * @return the transformed word
	 * @throws IllegalArgumentException if a null is passed
	 */
	protected String transformWord(String word) {
		if (word != null) {
			if (!isCaseSensitive) {
				return word.toLowerCase();
			} else {
				return word;
			}
		} else {
			throw new IllegalArgumentException("Null cannot be passed");
		}
	}

	/* public for testing */
	public static double normaliseSignificanceX(double sig) {
		if (Double.compare(IClassifier.UPPER_BOUND, sig) < 0) {
			return IClassifier.UPPER_BOUND;
		} else if (Double.compare(IClassifier.LOWER_BOUND, sig) > 0) {
			return IClassifier.LOWER_BOUND;
		} else {
			return sig;
		}
	}

	@SuppressWarnings("cast")
	private void checkPartitionsSupported(String category) {
		if (!(wordsData instanceof IPartitionedWordsDataSource)) {
			throw new IllegalArgumentException("The Data Source does not support a partitioned sample space.");
		}
	}
}
