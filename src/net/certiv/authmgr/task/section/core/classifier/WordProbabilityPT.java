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

import java.io.Serializable;

import net.sf.classifier4J.ICategorisedClassifier;
import net.sf.classifier4J.IClassifier;
import net.sf.classifier4J.util.CompareToBuilder;
import net.sf.classifier4J.util.EqualsBuilder;
import net.sf.classifier4J.util.HashCodeBuilder;
import net.sf.classifier4J.util.ToStringBuilder;

/**
 * Represents the probability of a particular word. The user of this object can either:
 * <ol>
 * <li>Set a specific probability for a particular word <I>or </I></li>
 * <li>Define the matching and non-matching counts for the particular word. This class
 * then calculates the probability for you.</li>
 * </ol>
 * 
 * @author Nick Lothian
 * @author Peter Leschev
 * @author Gbr
 */
public class WordProbabilityPT implements Comparable<WordProbabilityPT>, Serializable {

	private static final long serialVersionUID = 3770587694818683277L;

	private static final int UNDEFINED = -1;

	private String word = "";
	private String category = ICategorisedClassifier.DEFAULT_CATEGORY;

	private long matchingCount = UNDEFINED;
	private long nonMatchingCount = UNDEFINED;

	private long trainingPartition = UNDEFINED;
	private long trainingCategory = UNDEFINED;

	private double probability = IClassifier.NEUTRAL_PROBABILITY;

	// /////////////////////////////////////////////////////////////////////

	public WordProbabilityPT() {
		setMatchingCount(0);
		setNonMatchingCount(0);
	}

	public WordProbabilityPT(String w) {
		setWord(w);
		setMatchingCount(0);
		setNonMatchingCount(0);
	}

	public WordProbabilityPT(String c, String w) {
		setCategory(c);
		setWord(w);
		setMatchingCount(0);
		setNonMatchingCount(0);
	}

	public WordProbabilityPT(String w, double probability) {
		setWord(w);
		setProbability(probability);
	}

	public WordProbabilityPT(String w, long matchingCount, long nonMatchingCount) {
		setWord(w);
		setMatchingCount(matchingCount);
		setNonMatchingCount(nonMatchingCount);
	}

	public void setWord(String w) {
		this.word = w;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public void setMatchingCount(long matchingCount) {
		if (matchingCount < 0) {
			throw new IllegalArgumentException("matchingCount must be greater than 0");
		}
		this.matchingCount = matchingCount;
	}

	public void setNonMatchingCount(long nonMatchingCount) {
		if (nonMatchingCount < 0) {
			throw new IllegalArgumentException("nonMatchingCount must be greater than 0");
		}
		this.nonMatchingCount = nonMatchingCount;
	}

	public void registerMatch() {
		if (matchingCount == Long.MAX_VALUE) {
			throw new UnsupportedOperationException("Long.MAX_VALUE reached, can't register more matches");
		}
		matchingCount++;
	}

	public void registerNonMatch() {
		if (nonMatchingCount == Long.MAX_VALUE) {
			throw new UnsupportedOperationException("Long.MAX_VALUE reached, can't register more matches");
		}
		nonMatchingCount++;
	}

	/**
	 * @return
	 */
	public double getProbability() {
		return probability;
	}

	public long getMatchingCount() {
		if (matchingCount == UNDEFINED) {
			throw new UnsupportedOperationException("MatchingCount has not been defined");
		}
		return matchingCount;
	}

	public long getNonMatchingCount() {
		if (nonMatchingCount == UNDEFINED) {
			throw new UnsupportedOperationException("nonMatchingCount has not been defined");
		}
		return nonMatchingCount;
	}

	public String getWord() {
		return word;
	}

	public String getCategory() {
		return category;
	}

	public boolean equals(Object o) {
		if (!(o instanceof WordProbabilityPT)) {
			return false;
		}
		WordProbabilityPT rhs = (WordProbabilityPT) o;
		return new EqualsBuilder().append(getWord(), rhs.getWord()).append(getCategory(), rhs.getCategory()).isEquals();
	}

	public int compareTo(WordProbabilityPT rhs) {
		return new CompareToBuilder().append(this.getCategory(), rhs.getCategory())
				.append(this.getWord(), rhs.getWord()).toComparison();
	}

	public String toString() {
		return new ToStringBuilder(this).append("word", word).append("category", category)
				.append("probability", probability).append("matchingCount", matchingCount)
				.append("nonMatchingCount", nonMatchingCount).toString();
	}

	public int hashCode() {
		// you pick a hard-coded, randomly chosen, non-zero, odd number
		// ideally different for each class
		return new HashCodeBuilder(17, 37).append(word).append(category).toHashCode();
	}

	/**
	 * @return Returns the trainingCategory.
	 */
	public long getTrainingCategory() {
		return trainingCategory;
	}

	/**
	 * @param trainingCategory The trainingCategory to set.
	 */
	public void setTrainingCategory(long trainingCategory) {
		this.trainingCategory = trainingCategory;
	}

	/**
	 * @return Returns the trainingPartition.
	 */
	public long getTrainingPartition() {
		return trainingPartition;
	}

	/**
	 * @param trainingPartition The trainingPartition to set.
	 */
	public void setTrainingPartition(long trainingPartition) {
		this.trainingPartition = trainingPartition;
	}
}
