/*******************************************************************************
 * Copyright (c) 2003-2015 G Rosenberg.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		N Lothian, P Leschev- initial API 
 *		G Rosenberg - implementation
 *******************************************************************************/
package net.certiv.authmgr.task.section.core.classifier;

import net.sf.classifier4J.bayesian.WordsDataSourceException;

/**
 * Interface used by BayesianClassifier to determine the probability of each word based on
 * a particular category.
 * 
 * @author Nick Lothian
 * @author Peter Leschev
 * @author Gbr
 */

public interface IPartitionedWordsDataSource {

	/**
	 * @param category the category to check against
	 * @param partition the partition to check against
	 * @param word The word to calculate the probability of
	 * @return The word probability if the word exists, null otherwise;
	 * 
	 * @throws WordsDataSourceException If there is a fatal problem. For example, the
	 *             database is unavailable
	 */
	public WordProbabilityPT getWordProbability(String category, String partition, String word)
			throws WordsDataSourceException;

	/**
	 * Add a matching word to the data source
	 * 
	 * @param category the category add the match to
	 * @param partition the partition to add the match to
	 * @param word the word that matches
	 * 
	 * @throws WordsDataSourceException If there is a fatal problem. For example, the
	 *             database is unavailable
	 */
	public void addMatch(String category, String Partition, String word) throws WordsDataSourceException;

}
