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
package net.certiv.authmgr.view.section;

import net.certiv.nav.beans.swt.NavStateBar;
import net.certiv.nav.beans.swt.NavToolBar;

import org.agilemore.agilegrid.AgileGrid;
import org.agilemore.agilegrid.SWTX;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ClassifyViewForm extends Composite {

	private Group group1 = null;
	private Label label1 = null;
	private Text textID = null;
	private NavToolBar navToolBar = null;
	private NavStateBar navStateBar = null;
	private Group group = null;
	protected AgileGrid grid = null;
	private Label label = null;
	private Label label2 = null;
	private Text textStatus = null;

	public ClassifyViewForm(Composite parent, int style) {
		super(parent, style);
		initialize();
	}

	private void initialize() {
		createNavToolBar();
		createNavStateBar();
		createGroup1();
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginWidth = 10;
		gridLayout.marginHeight = 10;
		this.setLayout(gridLayout);
		@SuppressWarnings("unused")
		Label filler = new Label(this, SWT.NONE);
		createGridGroup();
		this.setSize(new Point(580, 320));
	}

	/**
	 * This method initializes navToolBar
	 */
	private void createNavToolBar() {
		GridLayout gridLayout5 = new GridLayout();
		gridLayout5.marginHeight = 0;
		gridLayout5.verticalSpacing = 0;
		GridData gridData26 = new org.eclipse.swt.layout.GridData();
		gridData26.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData26.grabExcessHorizontalSpace = false;
		navToolBar = new NavToolBar(this, SWT.NONE);
		navToolBar.setLayout(gridLayout5);
		navToolBar.setLayoutData(gridData26);
	}

	/**
	 * This method initializes navStateBar
	 */
	private void createNavStateBar() {
		GridLayout gridLayout6 = new GridLayout();
		gridLayout6.numColumns = 5;
		gridLayout6.marginHeight = 0;
		GridData gridData24 = new GridData();
		gridData24.horizontalAlignment = org.eclipse.swt.layout.GridData.END;
		navStateBar = new NavStateBar(this, SWT.NONE);
		navStateBar.setLayoutData(gridData24);
		navStateBar.setLayout(gridLayout6);
	}

	/**
	 * This method initializes group1
	 */
	private void createGroup1() {
		GridData gridData1 = new GridData();
		gridData1.horizontalSpan = 3;
		gridData1.widthHint = 80;
		GridData gridData = new GridData();
		gridData.widthHint = 10;
		GridData gridData8 = new GridData();
		gridData8.widthHint = 30;
		GridData gridData11 = new GridData();
		gridData11.horizontalAlignment = GridData.FILL;
		gridData11.verticalAlignment = GridData.FILL;
		GridData gridData13 = new GridData();
		gridData13.widthHint = 20;
		GridLayout gridLayout4 = new GridLayout();
		gridLayout4.numColumns = 7;
		group1 = new Group(this, SWT.NONE);
		group1.setText("Record");
		group1.setLayoutData(gridData11);
		group1.setLayout(gridLayout4);
		label1 = new Label(group1, SWT.NONE);
		label1.setText("ID");
		label1.setLayoutData(gridData13);
		textID = new Text(group1, SWT.BORDER | SWT.RIGHT | SWT.READ_ONLY);
		textID.setEditable(false);
		textID.setLayoutData(gridData8);
		textID.setEnabled(false);
		label = new Label(group1, SWT.NONE);
		label.setText("");
		label.setLayoutData(gridData);
		label2 = new Label(group1, SWT.NONE);
		label2.setText("Status");
		textStatus = new Text(group1, SWT.BORDER);
		textStatus.setEditable(false);
		textStatus.setLayoutData(gridData1);
		textStatus.setEnabled(false);
	}

	/**
	 * This method initializes group
	 */
	private void createGridGroup() {
		GridData gridData14 = new GridData();
		gridData14.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData14.grabExcessHorizontalSpace = true;
		gridData14.grabExcessVerticalSpace = true;
		gridData14.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		GridData gridData9 = new GridData();
		gridData9.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData9.grabExcessVerticalSpace = true;
		gridData9.grabExcessHorizontalSpace = true;
		gridData9.horizontalSpan = 2;
		gridData9.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		group = new Group(this, SWT.NONE);
		group.setLayout(new GridLayout());
		group.setLayoutData(gridData9);
		group.setText("Stages");
		grid = new AgileGrid(group, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWTX.MARK_SELECTION_HEADERS);
		grid.setLayoutAdvisor(new ClassifyLayoutAdvisor(grid));
		grid.setCellRendererProvider(new ClassifyCellRendererProvider(grid));
		grid.setCellEditorProvider(new ClassifyCellEditorProvider(grid));
		grid.setLayoutData(gridData14);
		grid.setContentProvider(new ClassifyContentProvider());

		Display display = grid.getDisplay(); // Prep grid header cursors
		Image rowCursor = SWTX.loadImageResource(display, "/icons/row_resize_win32.gif");
		Rectangle rowBounds = rowCursor.getBounds();
		Point rowSize = new Point(rowBounds.width / 2, rowBounds.height / 2);
		grid.setDefaultRowResizeCursor(new Cursor(display, rowCursor.getImageData(), rowSize.x, rowSize.y));
		rowCursor.dispose();

		Image colCursor = SWTX.loadImageResource(display, "/icons/column_resize_win32.gif");
		Rectangle colBounds = colCursor.getBounds();
		Point colSize = new Point(colBounds.width / 2, colBounds.height / 2);
		grid.setDefaultColumnResizeCursor(new Cursor(display, colCursor.getImageData(), colSize.x, colSize.y));
		colCursor.dispose();
	}

	/**
	 * @return the textID
	 */
	public Text getDocID() {
		return textID;
	}

	/**
	 * @return the textStatus
	 */
	public Text getTextStatus() {
		return textStatus;
	}

	/**
	 * @return the navToolBar
	 */
	public NavToolBar getNavToolBar() {
		return navToolBar;
	}

	/**
	 * @return the navStateBar
	 */
	public NavStateBar getNavStateBar() {
		return navStateBar;
	}

} // @jve:decl-index=0:visual-constraint="10,10"
