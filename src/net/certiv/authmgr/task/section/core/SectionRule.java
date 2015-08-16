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

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class SectionRule implements ISchedulingRule {

	public boolean contains(ISchedulingRule rule) {
		return rule.getClass() == SectionRule.class;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		return rule.getClass() == SectionRule.class;
	}
}
