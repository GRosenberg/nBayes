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

import java.io.IOException;
import java.util.regex.Pattern;

import net.certiv.authmgr.app.preferences.Prefs;
import net.certiv.authmgr.app.preferences.PrefsKey;
import net.certiv.authmgr.app.util.Log;
import net.certiv.authmgr.app.util.Util;
import net.certiv.authmgr.db.dao.elems.DocLine;
import net.certiv.authmgr.db.dao.elems.DocPage;
import net.certiv.authmgr.db.dao.elems.DocPageList;
import net.certiv.authmgr.task.section.core.classifier.BayesPartitionClassifier;
import net.certiv.authmgr.task.section.core.classifier.CombinedTokenFilter;
import net.certiv.authmgr.task.section.core.classifier.PersistantWordsDataSource;
import net.sf.classifier4J.bayesian.WordsDataSourceException;

/**
 * @author Gbr
 */
public class ClassifySections {

	private boolean debug = false;

	/** the classification engine */
	private BayesPartitionClassifier classifier;

	private SectionTask task;
	private String currentBCModel;
	private int windowSize;

	// compiled regex's
	private static final Pattern outline = Pattern
			.compile("^\\s*\\(?(\\p{Upper}{1,3}|\\p{Lower}{1,3}|\\p{Digit}{1,3})[\\.\\)]\\s+");
	private static final Pattern capsline = Pattern.compile("^\\s*(\\p{Upper}+\\s+)*\\p{Upper}+$");
	private static final Pattern caps1word = Pattern.compile("^\\s*\\p{Upper}+\\b");
	private static final Pattern caps1lett = Pattern.compile("^\\s*\\p{Upper}\\p{Lower}");
	private static final Pattern capsmid2word = Pattern.compile(".*\\p{Graph}\\s+\\b\\p{Upper}+\\b.*\\b\\p{Upper}+\\b");
	private static final Pattern capsmidword = Pattern.compile(".*\\p{Graph}\\s+\\b\\p{Upper}+\\b");
	private static final Pattern capsmid2letter = Pattern
			.compile(".*\\p{Graph}\\s+\\p{Upper}+.*\\b.*\\b\\p{Upper}+.*\\b");
	private static final Pattern capsmidletter = Pattern.compile(".*\\p{Graph}\\s+\\p{Upper}+.*\\b");
	private static final Pattern affirmed = Pattern.compile("\\baffirmed\\b");
	private static final Pattern judge = Pattern.compile("\\bjudge\\b");
	private static final Pattern brief = Pattern.compile("\\bbrief\\b");
	private static final Pattern versus = Pattern.compile("\\bv\\b");
	private static final Pattern and = Pattern.compile("\\band\\b");
	private static final Pattern background = Pattern.compile("\\bbackground\\b");
	private static final Pattern discussion = Pattern.compile("\\bdiscussion\\b");
	private static final Pattern conclusion = Pattern.compile("\\bconclusion\\b");
	private static final Pattern analysis = Pattern.compile("\\banalysis\\b");
	private static final Pattern decided = Pattern.compile("\\bdecided\\b");
	private static final Pattern dissent = Pattern.compile("\\bdissent\\b");
	private static final Pattern enbanc = Pattern.compile("\\benbanc\\b");
	private static final Pattern percuriam = Pattern.compile("\\b\\s*?curium\\b");
	private static final Pattern concur = Pattern.compile("\\bconcur\\b");
	private static final Pattern end_period = Pattern.compile(".*\\.[\\p{Punct}\u2019\u201D]*\\s{0,1}\\d{0,2}$");
	private static final Pattern end_colon = Pattern.compile(".*\\:\\s{0,1}\\d{0,2}$");
	private static final Pattern end_comma = Pattern.compile(".*\\,\\s{0,1}\\d{0,2}$");

	/** for now, using a fixed category */
	public static final String categoryFixed = "Sections";

	/**
	 * Initializes the Bayesian classifier to use a persistant, multiple category
	 * classification model. Uses the default classification model.
	 */
	public ClassifySections(SectionTask task) {
		this(task, null);
	}

	/**
	 * Initializes the Bayesian classifier to use a persistant, multiple category
	 * classification model.
	 * 
	 * @param modelName The name of the classification model to use.
	 * @throws IOException
	 */
	public ClassifySections(SectionTask task, String modelName) {
		this.task = task;
		currentBCModel = Prefs.getString(PrefsKey.CUR_CLASSIFY);
		windowSize = Prefs.getInt(PrefsKey.TRAINING_WINDOW);
		String sw = Prefs.getString(PrefsKey.STOP_WORDS_LIST);

		PersistantWordsDataSource pds;
		if (modelName != null && modelName.length() > 0) {
			pds = new PersistantWordsDataSource(modelName);
		} else {
			pds = new PersistantWordsDataSource(currentBCModel);
		}
		CombinedTokenFilter tok = null;
		try {
			tok = new CombinedTokenFilter(sw);
		} catch (IOException e) {
			e.printStackTrace();
		}
		classifier = new BayesPartitionClassifier(pds, tok);
	}

	/**
	 * Multi-stage classification process.
	 * <UL>
	 * <LI>Annotate the line content to produce a generic line structure specification,
	 * which is stored back to the DocLine object.</LI>
	 * <LI>Generate a sliding-window based specific line structure specification.</LI>
	 * <LI>Perform the classification operation for each line and store the best match
	 * category index back to the DocLine objects within the DocPages structure.</LI>
	 * </UL>
	 * 
	 * @param docStructure
	 * @return
	 */
	public DocPageList classifyLines(DocPageList docStructure) {
		// first, annotate the individual lines
		task.announceStatusUpdate("Working...");
		int sizePages = docStructure.size();
		for (int i = 0; i < sizePages; i++) {
			DocPage dp = (DocPage) docStructure.get(i);
			int sizeLines = dp.size(DocPage.RANK_ORDER);
			for (int j = 0; j < sizeLines; j++) {
				StringBuffer ls = generateLineStructure(dp, j);
				if (i == 0 && j == 0) {
					ls.insert(0, "begin## ");
				} else if (i == (sizePages - 1) && j == (sizeLines - 1)) {
					ls.append("end## ");
				}
				String s = ls.toString();
				dp.getDocLine(j, DocPage.RANK_ORDER).lineStructure = s;
				Thread.yield();
			}
		}

		// second, classify the current line within a sliding window
		int negwindow = -windowSize;
		int poswindow = windowSize + 1;
		int count = 0;

		task.announceStatusUpdate("Working [page=" + "0/" + sizePages + ", line=" + count + "]");
		for (int i = 0; i < sizePages; i++) {
			long start = System.currentTimeMillis();
			task.announceStatusUpdate("Working [page=" + (i + 1) + "/" + sizePages + ", line=" + count + "]");
			DocPage dp = docStructure.getDocPageAtIdx(i);
			int sizeLines = dp.size(DocPage.RANK_ORDER);
			for (int j = 0; j < sizeLines; j++) {
				int negSlide = ((j + negwindow) > 0) ? negwindow : -j;
				int posSlide = ((j + poswindow) < dp.size(DocPage.RANK_ORDER)) ? poswindow : sizeLines - j;

				// walk through the sliding window and accumulate structure
				// elements
				StringBuffer sb = new StringBuffer();
				for (int k = j + negSlide; k < j + posSlide; k++) {
					DocLine dl = dp.getDocLine(k, DocPage.RANK_ORDER);
					String offstr = String.valueOf(k - j);
					String lineStruc = new String(dl.lineStructure);
					// make line structure element relative to this line
					String q = lineStruc.replaceAll("##", offstr);
					sb.append(q);
				}
				// classify each line
				DocLine dl = dp.getDocLine(j, DocPage.RANK_ORDER);
				String ls2 = sb.toString();
				dl.lineStructure2 = ls2;
				dl.lineSection = classifyLine(dl.lineContent, dl.lineStructure2);
				count++;
				if (count % 10 == 0) {
					task.announceStatusUpdate("Working [page=" + (i + 1) + "/" + sizePages + ", line=" + count + "]");
				}
				Thread.yield();
			}
			long stop = System.currentTimeMillis();
			task.announceStatusUpdate("Page classification time: " + (stop - start) + "ms");
		}
		return docStructure; // return - not needed?
	}

	/**
	 * Perform the actual line classification operation. Submit the line for
	 * classification and return the current category index of the best/maximum
	 * classification match.
	 * 
	 * @param elementSet The combined line content and fully quaified line structure
	 *            specification.
	 * @return Index of the category match.
	 */
	private int classifyLine(String content, String structure) {
		// construct the final classifier ready message
		String elements = Util.tokenFilterAll(new String(content)) + " " + structure;
		// Log.debug(this, "Classifying: " + elements);

		int result = 0;
		try {
			String section = classifier.classify(categoryFixed, elements);
			if (debug) Log.debug(this, "Classified: " + section + " >>> " + elements);
			result = DocLine.getSectionIndex(section);
		} catch (WordsDataSourceException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Primary line structure annotation routine. Detects structural characteristics and
	 * emmits corresponding markers.
	 * 
	 * @param page the current page being annotated
	 * @param line the line being annotated
	 * @return the line annotation string.
	 */
	private StringBuffer generateLineStructure(DocPage page, int line) {
		StringBuffer lnStrucDesc = new StringBuffer();
		boolean fullline = false;
		boolean blankline = false;

		// walk through the attributes and generate generic line structure
		// elements
		DocLine lnStruc = page.getDocLine(line, DocPage.RANK_ORDER);

		// type
		if ((lnStruc.type & DocLine.BLANK) == DocLine.BLANK) {
			lnStrucDesc.append("blank## ");
			blankline = true;
		}
		// if only text
		if (lnStruc.type == DocLine.TEXT) {
			lnStrucDesc.append("text## ");
		}
		if ((lnStruc.type & DocLine.NOTE) == DocLine.NOTE) {
			lnStrucDesc.append("note## ");
		}
		if ((lnStruc.type & DocLine.HEADER) == DocLine.HEADER) {
			lnStrucDesc.append("header## ");
		}
		if ((lnStruc.type & DocLine.FOOTER) == DocLine.FOOTER) {
			lnStrucDesc.append("footer## ");
		}

		// ln, rank, and out-of-order line
		if (!Util.within(lnStruc.ln, lnStruc.rank, 3)) {
			lnStrucDesc.append("lnumb" + lnStruc.ln + "## ");
			lnStrucDesc.append("lrank" + lnStruc.rank + "## ");
			lnStrucDesc.append("lnscrambled## ");
		}

		// posY
		// lnStrucDesc.append("" + lnStruc.posY + "## ");

		float lmargin = page.getLeftPageMargin();
		float rmargin = page.getRightPageMargin();
		float indent = page.getPageIndent() * lmargin;
		// Log.warn(this, "LineKeys: " + page.getPageNumber() + ":" + line + "::" +
		// lmargin +
		// ":" + indent + ":" + rmargin);

		// posX and posXmax (using indent factor)
		// -- indents: 0.5 inch is typical; dependent on font?
		if (Util.within(lnStruc.posX, lmargin + indent, 16.0f)) {
			lnStrucDesc.append("indent## ");
		} else if (Util.within(lnStruc.posX, lmargin + (2 * indent), 16.0f)) {
			lnStrucDesc.append("indent2## ");
		} else if (lnStruc.posX > lmargin + (2 * indent + 16.0f)) {
			lnStrucDesc.append("indent_multi## ");
		}

		if (debug) Log.debug(this, "Line metrics: " + Math.round(lmargin) + ":" + Math.round(lmargin + indent) + ":"
				+ Math.round(lnStruc.posX) + ":" + Math.round(lnStruc.posXmax) + ":" + Math.round(rmargin));

		// line length metrics: not significant if a blank/no visible characters
		// line
		if (!blankline) {

			// -- line widths (wide: wider than L/R indented text; short: just a
			// few
			// chars)
			if (Util.within(lnStruc.posX, lmargin, 16.0f) && Util.within(lnStruc.posXmax, rmargin, 16.0f)) {
				lnStrucDesc.append("lnfull## "); // full line
				fullline = true;
			} else if (Util.within(lnStruc.posX, lmargin + indent, 16.0f)
					&& Util.within(lnStruc.posXmax, rmargin, 16.0f)) {
				lnStrucDesc.append("lnindent## "); // left indent full line
			} else if (Util.within(lnStruc.posX, lmargin + 2 * indent, 16.0f)
					&& Util.within(lnStruc.posXmax, rmargin, 16.0f)) {
				lnStrucDesc.append("lndblindent## "); // dbl left indent full
				// line
			} else if (Util.within(lnStruc.posX, lmargin + indent, 16.0f)
					&& Util.within(lnStruc.posXmax, rmargin - indent, 16.0f)) {
				lnStrucDesc.append("lnlrindent## "); // left/right indent
			} else if (lnStruc.posXmax - lnStruc.posX > 0.5 * (rmargin - lmargin)) {
				lnStrucDesc.append("lninter## ");
			} else if (lnStruc.posXmax - lnStruc.posX <= indent) {
				lnStrucDesc.append("lnshort## ");
			} else {
				lnStrucDesc.append("lnmed## ");
			}

			// -- line centering: balanced line not long
			if (Util.within((rmargin - lnStruc.posXmax), (lnStruc.posX - lmargin), 16.0f) && !fullline) {
				lnStrucDesc.append("centered## ");
			}
			// -- line left aligned (at least left aligned)
			if (Util.within(lnStruc.posX, lmargin, 16.0f)) {
				lnStrucDesc.append("lnleft## ");
			}
			// -- line right aligned (at least right aligned)
			if (Util.within(lnStruc.posXmax, rmargin, 16.0f)) {
				lnStrucDesc.append("lnright## ");
			}

		}

		// fontWidth
		// lnStrucDesc.append("" + lnStruc.fontWidth + "## ");

		// fontSize: not significant if a blank/no visible characters line
		// lnStrucDesc.append("" + lnStruc.fontSize + "## ");
		if (!blankline) {
			if (lnStruc.fontSize > 13) {
				lnStrucDesc.append("fontlrg## ");
			} else if (lnStruc.fontSize < 10) {
				lnStrucDesc.append("fontsml## ");
			} else {
				lnStrucDesc.append("fontmed## ");
			}
		}

		// veritical line spacing
		if (lnStruc.spacing < 10) {
			lnStrucDesc.append("spaced_vsm## ");
		} else if (lnStruc.spacing > 30) {
			lnStrucDesc.append("spaced_vlg## ");
		} else {
			switch (Math.round(lnStruc.spacing)) {
				case 10:
				case 12:
					lnStrucDesc.append("spaced_sml## ");
					break;
				case 15:
					lnStrucDesc.append("spaced_med## ");
					break;
				case 20:
					lnStrucDesc.append("spaced_lrg## ");
					break;
				case 30:
					lnStrucDesc.append("spaced_xlg## ");
					break;
			}
		}

		// page oriented stuff
		if (lnStruc.rank == 0) {
			lnStrucDesc.append("start_page## ");
		}
		if (lnStruc.rank == page.getNoteFirstLine()) {
			lnStrucDesc.append("start_notes## ");
		}
		if (lnStruc.rank == page.size(DocPage.RANK_ORDER) - 1) {
			lnStrucDesc.append("end_page## ");
		}

		// identify significant structure of content
		// simple clean string
		String s = lnStruc.lineContent.trim();

		// line beginnings: things that look like header outline markers
		if (outline.matcher(s).matches()) {
			lnStrucDesc.append("outline## ");
		}

		// completely clean string
		String sf = Util.tokenFilterAll(s).trim();

		// line beginings: first letter and first word
		if (capsline.matcher(sf).matches()) {
			lnStrucDesc.append("capsline## ");
		} else if (caps1word.matcher(sf).matches()) {
			lnStrucDesc.append("caps1word## ");
		} else if (caps1lett.matcher(sf).matches()) {
			lnStrucDesc.append("caps1letter## ");
		}

		// words in the middle
		if (capsmid2word.matcher(sf).matches()) {
			lnStrucDesc.append("capsmid2word## ");
		} else if (capsmidword.matcher(sf).matches()) {
			lnStrucDesc.append("capsmidword## ");
		}

		// init caps in the middle
		if (capsmid2letter.matcher(sf).matches()) {
			lnStrucDesc.append("capsmid2letter## ");
		} else if (capsmidletter.matcher(sf).matches()) {
			lnStrucDesc.append("capsmidletter## ");
		}

		// just numbers
		if (sf.trim().replaceAll("\\s", "").matches("\\d+")) {
			lnStrucDesc.append("numsline## ");
		}

		String sfl = sf.toLowerCase();
		if (affirmed.matcher(sfl).matches()) {
			lnStrucDesc.append("keyaffirmed## ");
		}
		if (judge.matcher(sfl).matches()) {
			lnStrucDesc.append("keyjudge## ");
		}
		if (brief.matcher(sfl).matches()) {
			lnStrucDesc.append("keybrief## ");
		}
		if (versus.matcher(sfl).matches()) {
			lnStrucDesc.append("keyversus## ");
		}
		if (and.matcher(sfl).matches()) {
			lnStrucDesc.append("keyand## ");
		}
		if (background.matcher(sfl).matches()) {
			lnStrucDesc.append("keybackground## ");
		}
		if (discussion.matcher(sfl).matches()) {
			lnStrucDesc.append("keydiscussion## ");
		}
		if (conclusion.matcher(sfl).matches()) {
			lnStrucDesc.append("keyconclusion## ");
		}
		if (analysis.matcher(sfl).matches()) {
			lnStrucDesc.append("keyanalysis## ");
		}
		if (decided.matcher(sfl).matches()) {
			lnStrucDesc.append("keydecided## ");
		}
		if (dissent.matcher(sfl).matches()) {
			lnStrucDesc.append("keydissent## ");
		}
		if (enbanc.matcher(sfl).matches()) {
			lnStrucDesc.append("keyenbanc## ");
		}
		if (percuriam.matcher(sfl).matches()) {
			lnStrucDesc.append("keypercuriam## ");
		}
		if (concur.matcher(sfl).matches()) {
			lnStrucDesc.append("keyconcur## ");
		}

		// line endings: period, colon, and comma are thought significant
		String sx = Util.tokenFilterXml(s).trim();
		if (end_period.matcher(sx).matches()) {
			lnStrucDesc.append("end_period## ");
		} else if (end_colon.matcher(sx).matches()) {
			lnStrucDesc.append("end_colon## ");
		} else if (end_comma.matcher(sx).matches()) {
			lnStrucDesc.append("end_comma## ");
		}

		// return the structure description
		// Log.debug(this, "Structure: " + lnStrucDesc);
		return lnStrucDesc;
	}
}
