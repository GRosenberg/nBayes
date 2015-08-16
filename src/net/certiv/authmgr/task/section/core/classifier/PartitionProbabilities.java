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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import net.sf.classifier4J.ICategorisedClassifier;

/**
 * Represents the partition probabilities of a particular message. The possibility exists
 * to have multiple PartitionProbability objects, each calculated agaist a different
 * category, in order to evaluate the intersection of differently trained partitionScores.
 * 
 * @author Gbr
 */
public class PartitionProbabilities {

	/** the category that these probabilities correspond to */
	private String category = ICategorisedClassifier.DEFAULT_CATEGORY;
	/** the map of partition/score values */
	private HashMap<String, Double> partitionScores;

	public PartitionProbabilities() {
		this("");
	}

	public PartitionProbabilities(String category) {
		partitionScores = new HashMap<String, Double>();
		this.category = category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	/////// Tailored HashMap Wrappers ////////////////////////////////////////
	/**
	 * @param key
	 * @param value
	 * @return
	 */
	public void setScore(String partition, double score) {
		// double nScore = BayesPartitionClassifier.normaliseSignificance(score);
		partitionScores.put(partition, new Double(score));
	}

	/**
	 * @param key
	 * @return
	 */
	public double getScore(String partition) {
		return partitionScores.get(partition).doubleValue();
	}

	/**
	 * @param key
	 * @return
	 */
	public boolean containsPartition(String partition) {
		return partitionScores.containsKey(partition);
	}

	/**
	 * Given the accumulated set of partition scores, returns the partition label that
	 * corresponds to the partition with the maximum score.
	 * 
	 * @return the partition match label
	 */
	public String maxScorePartition() {
		String maxLabel = null;
		double maxScore = 0;
		for (Iterator<String> it = partitionScores.keySet().iterator(); it.hasNext();) {
			String label = it.next();
			double score = getScore(label);
			if (score > maxScore) {
				maxLabel = label;
				maxScore = score;
			}
		}
		return maxLabel;
	}

	/**
	 * Given the accumulated set of partition scores, returns a figure of merit
	 * representing the confidence that the maximum score identifies the correctly
	 * matching partition.
	 * 
	 * @return
	 */
	public double confidence() {
		return 1;
	}

	/////// Ordinary HashMap Wrappers ////////////////////////////////////////
	/**
	 * @return
	 */
	public Set<String> keySet() {
		return partitionScores.keySet();
	}

	/**
	 * @return
	 */
	public Collection<Double> values() {
		return partitionScores.values();
	}

	/**
	 * @return
	 */
	public int size() {
		return partitionScores.size();
	}

	/**
	 * @param key
	 * @return
	 */
	public Object remove(Object key) {
		return partitionScores.remove(key);
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		return partitionScores.isEmpty();
	}

	/**
	 *  
	 */
	public void clear() {
		partitionScores.clear();
	}

	///////////////////////////////////////////////////////////////////////////
}
