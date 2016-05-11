/*******************************************************************************
 * Copyright (c) 2012, 2013 Original authors and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Original authors and others - initial API and implementation
 ******************************************************************************/
package eu.openanalytics.phaedra.base.ui.nattable.columnChooser.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.columnChooser.ColumnChooserUtils;
import org.eclipse.nebula.widgets.nattable.columnChooser.ColumnEntry;
import org.eclipse.nebula.widgets.nattable.columnChooser.ColumnGroupEntry;
import org.eclipse.nebula.widgets.nattable.columnChooser.gui.AbstractColumnChooserDialog;
import org.eclipse.nebula.widgets.nattable.coordinate.PositionUtil;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel.ColumnGroup;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupUtils;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.util.ArrayUtil;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.nebula.widgets.nattable.util.ObjectUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.IColumnMatcher;
import eu.openanalytics.phaedra.base.ui.nattable.columnChooser.ISelectionTreeListener;
import eu.openanalytics.phaedra.base.ui.util.misc.StringMatcher;

public class ColumnChooserDialog extends AbstractColumnChooserDialog {

	private Tree availableTree;
	private Tree selectedTree;
	private final String selectedLabel;
	private final String availableLabel;
	private ColumnGroupModel columnGroupModel;

	private String availableFilterValue;
	private String selectedFilterValue;

	private Map<String, IColumnMatcher> columnMatchers;

	public ColumnChooserDialog(Shell parentShell, String availableLabel, String selectedLabel, Map<String, IColumnMatcher> columnMatchers) {
		super(parentShell);
		this.availableLabel = availableLabel;
		this.selectedLabel = selectedLabel;
		this.columnMatchers = columnMatchers;
	}

	@Override
	public void populateDialogArea(Composite parent) {
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		parent.setLayout(new GridLayout(4, false));

		createLabels(parent, availableLabel, selectedLabel);

		final Text availableFilter = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		availableFilter.setText("");
		availableFilter.setMessage("type filter text");
		availableFilter.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				availableFilterValue = availableFilter.getText().toLowerCase();
				fireItemsFiltered();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(availableFilter);

		// Filler.
		new Label(parent, SWT.NONE);

		final Text selectedFilter = new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
		selectedFilter.setText("");
		selectedFilter.setMessage("type filter text");
		selectedFilter.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				selectedFilterValue = selectedFilter.getText().toLowerCase();
				fireItemsFiltered();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(selectedFilter);

		// Filler.
		new Label(parent, SWT.NONE);

		availableTree = new Tree(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.Expand);
		availableTree.forceFocus();

		GridData gridData = GridDataFactory.fillDefaults().grab(true, true).create();
		availableTree.setLayoutData(gridData);
		availableTree.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				addSelected();
			}

		});

		availableTree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == ' ')
					addSelected();
			}
		});

		Composite buttonComposite = new Composite(parent, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(1, true));

		Button addButton = new Button(buttonComposite, SWT.PUSH);
		addButton.setImage(GUIHelper.getImage("arrow_right")); //$NON-NLS-1$
		gridData = GridDataFactory.fillDefaults().grab(false, true).align(SWT.CENTER, SWT.CENTER).create();
		addButton.setLayoutData(gridData);
		addButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				addSelected();
			}

		});

		Button removeButton = new Button(buttonComposite, SWT.PUSH);
		removeButton.setImage(GUIHelper.getImage("arrow_left")); //$NON-NLS-1$
		gridData = GridDataFactory.copyData(gridData);
		removeButton.setLayoutData(gridData);
		removeButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelected();
			}

		});

		if (!columnMatchers.isEmpty()) {
			Menu columnMenu = new Menu(parent.getShell(), SWT.POP_UP);
			for (Entry<String, IColumnMatcher> e : columnMatchers.entrySet()) {
				MenuItem menuItem = new MenuItem(columnMenu, SWT.PUSH);
				menuItem.setText(e.getKey());
				menuItem.addListener(SWT.Selection, event -> {
					IColumnMatcher matcher = e.getValue();
					selectMatchingItems(selectedTree, matcher);
					selectMatchingItems(availableTree, matcher);
				});
			}

			Button selectedButton = new Button(buttonComposite, SWT.PUSH);
			selectedButton.setImage(IconManager.getIconImage("funnel.png"));
			gridData = GridDataFactory.copyData(gridData);
			selectedButton.setLayoutData(gridData);
			selectedButton.addListener(SWT.Selection, e -> {
				Button button = (Button) e.widget;
				Rectangle bounds = button.getBounds();
				Point pt = buttonComposite.toDisplay(bounds.x, bounds.y + bounds.height);
				columnMenu.setLocation(pt);
				columnMenu.setVisible(true);
			});
			selectedButton.addDisposeListener(e -> {
				if (columnMenu != null && !columnMenu.isDisposed()) columnMenu.dispose();
			});
		}

		selectedTree = new Tree(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.Expand);

		gridData = GridDataFactory.fillDefaults().grab(true, true).create();
		selectedTree.setLayoutData(gridData);
		selectedTree.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				removeSelected();
			}

		});

		selectedTree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				boolean controlMask = (e.stateMask & SWT.MOD1) == SWT.MOD1;
				if (controlMask && e.keyCode == SWT.ARROW_UP) {
					moveSelectedUp();
					e.doit = false;
				} else if (controlMask && e.keyCode == SWT.ARROW_DOWN) {
					moveSelectedDown();
					e.doit = false;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

				if (e.character == ' ')
					removeSelected();
			}
		});

		selectedTree.addTreeListener(new TreeListener(){

			@Override
			public void treeCollapsed(TreeEvent event) {
				selectedTreeCollapsed(event);
			}

			@Override
			public void treeExpanded(TreeEvent event) {
				selectedTreeExpanded(event);
			}

		});

		selectedTree.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				widgetSelected(event);
			}

			@Override
			public void widgetSelected(SelectionEvent event) {
				toggleColumnGroupSelection((TreeItem) event.item);
			}

		});

		Composite upDownbuttonComposite = new Composite(parent, SWT.NONE);
		upDownbuttonComposite.setLayout(new GridLayout(1, true));

		Button topButton = new Button(upDownbuttonComposite, SWT.PUSH);
		topButton.setImage(GUIHelper.getImage("arrow_up_top")); //$NON-NLS-1$
		gridData = GridDataFactory.fillDefaults().grab(false, true).align(SWT.CENTER, SWT.CENTER).create();
		topButton.setLayoutData(gridData);
		topButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				moveSelectedToTop();
			}
		});

		Button upButton = new Button(upDownbuttonComposite, SWT.PUSH);
		upButton.setImage(GUIHelper.getImage("arrow_up")); //$NON-NLS-1$
		gridData = GridDataFactory.fillDefaults().grab(false, true).align(SWT.CENTER, SWT.CENTER).create();
		upButton.setLayoutData(gridData);
		upButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				moveSelectedUp();
			}
		});

		Button downButton = new Button(upDownbuttonComposite, SWT.PUSH);
		downButton.setImage(GUIHelper.getImage("arrow_down")); //$NON-NLS-1$
		gridData = GridDataFactory.copyData(gridData);
		downButton.setLayoutData(gridData);
		downButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				moveSelectedDown();
			}
		});

		Button bottomButton = new Button(upDownbuttonComposite, SWT.PUSH);
		bottomButton.setImage(GUIHelper.getImage("arrow_down_end")); //$NON-NLS-1$
		gridData = GridDataFactory.copyData(gridData);
		bottomButton.setLayoutData(gridData);
		bottomButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				moveSelectedToBottom();
			}
		});
	}

	protected final void fireItemsSelected(List<ColumnEntry> addedItems) {
		for (Object listener : listeners.getListeners()) {
			((ISelectionTreeListener) listener).itemsSelected(addedItems);
		}
	}

	protected final void fireItemsRemoved(List<ColumnEntry> removedItems) {
		for (Object listener : listeners.getListeners()) {
			((ISelectionTreeListener) listener).itemsRemoved(removedItems);
		}
	}

	protected final void fireItemsMoved(MoveDirectionEnum direction, List<ColumnGroupEntry> selectedColumnGroupEntries, List<ColumnEntry> selectedColumnEntries, List<List<Integer>> fromPositions, List<Integer> toPositions) {
		for (Object listener : listeners.getListeners()) {
			((ISelectionTreeListener) listener).itemsMoved(direction, selectedColumnGroupEntries, selectedColumnEntries, fromPositions, toPositions);
		}
	}

	private void fireGroupExpanded(ColumnGroupEntry columnGroupEntry) {
		for (Object listener : listeners.getListeners()) {
			((ISelectionTreeListener) listener).itemsExpanded(columnGroupEntry);
		}
	}

	private void fireGroupCollapsed(ColumnGroupEntry columnGroupEntry) {
		for (Object listener : listeners.getListeners()) {
			((ISelectionTreeListener) listener).itemsCollapsed(columnGroupEntry);
		}
	}

	private void fireItemsFiltered() {
		for (Object listener : listeners.getListeners()) {
			((ISelectionTreeListener) listener).itemsFiltered();
		}
	}


	public void populateSelectedTree(List<ColumnEntry> columnEntries, ColumnGroupModel columnGroupModel) {
		populateModel(selectedTree, columnEntries, columnGroupModel, selectedFilterValue);
	}

	public void populateAvailableTree(List<ColumnEntry> columnEntries, ColumnGroupModel columnGroupModel) {
		populateModel(availableTree, columnEntries, columnGroupModel, availableFilterValue);
	}

	/**
	 * Populates the tree.
	 *   Looks for column group and adds an extra node for the group.
	 *   The column leaves carry a {@link ColumnEntry} object as data.
	 *   The column group leaves carry a {@link ColumnGroupEntry} object as data.
	 */
	private void populateModel(Tree tree, List<ColumnEntry> columnEntries, ColumnGroupModel columnGroupModel, String filterValue) {
		this.columnGroupModel = columnGroupModel;

		try {
			// Prevents flickering.
			tree.setRedraw(false);

			// Create the matcher if needed.
			StringMatcher matcher = null;
			if (filterValue != null) matcher = new StringMatcher(filterValue);

			for (ColumnEntry columnEntry : columnEntries) {
				TreeItem treeItem;
				int columnEntryIndex = columnEntry.getIndex().intValue();

				// Create a node for the column group - if needed
				if (columnGroupModel != null && columnGroupModel.isPartOfAGroup(columnEntryIndex)) {
					ColumnGroup columnGroup = columnGroupModel.getColumnGroupByIndex(columnEntryIndex);
					String columnGroupName = columnGroup.getName();

					// Check if the column or group name matches the matcher (if any)
					if (matcher != null) {
						if (!matcher.match(columnEntry.getLabel()) && !matcher.match(columnGroupName)) {
							continue;
						}
					}

					TreeItem columnGroupTreeItem = getTreeItem(tree, columnGroupName);

					if (columnGroupTreeItem == null) {
						columnGroupTreeItem = new TreeItem(tree, SWT.NONE);
						ColumnGroupEntry columnGroupEntry = new ColumnGroupEntry(
								columnGroupName,
								columnEntry.getPosition(),
								columnEntry.getIndex(),
								columnGroup.isCollapsed());
						columnGroupTreeItem.setData(columnGroupEntry);
						columnGroupTreeItem.setText(columnGroupEntry.getLabel());
					}
					treeItem = new TreeItem(columnGroupTreeItem, SWT.NONE);
				} else {
					// Check if the column or group name matches the filter (if any)
					if (matcher != null) {
						if (!matcher.match(columnEntry.getLabel())) {
							continue;
						}
					}

					treeItem = new TreeItem(tree, SWT.NONE);
				}
				treeItem.setText(columnEntry.getLabel());
				treeItem.setData(columnEntry);
			}
		} finally {
			tree.setRedraw(true);
		}
	}

	/**
	 * If the tree contains an item with the given label return it
	 */
	private TreeItem getTreeItem(Tree tree, String label) {
		for (TreeItem treeItem : tree.getItems()) {
			if (treeItem.getText().equals(label)) {
				return treeItem;
			}
		}
		return null;
	}

	/**
	 * Get the ColumnEntries from the selected TreeItem(s)
	 * Includes nested column group entries - if the column group is selected.
	 * Does not include parent of the nested entries - since that does not denote an actual column
	 */
	private List<ColumnEntry> getColumnEntriesIncludingNested(TreeItem[] selectedTreeItems) {
		List<ColumnEntry> selectedColumnEntries = new ArrayList<ColumnEntry>();

		for (int i = 0; i < selectedTreeItems.length; i++) {
			// Column Group selected - get all children
			if (isColumnGroupLeaf(selectedTreeItems[i])) {
				TreeItem[] itemsInGroup = selectedTreeItems[i].getItems();
				for (TreeItem itemInGroup : itemsInGroup) {
					selectedColumnEntries.add((ColumnEntry) itemInGroup.getData());
				}
			} else {
				// Column
				selectedColumnEntries.add(getColumnEntryInLeaf(selectedTreeItems[i]));
			}
		}
		return selectedColumnEntries;
	}

	private List<ColumnGroupEntry> getSelectedColumnGroupEntries(TreeItem[] selectedTreeItems) {
		List<ColumnGroupEntry> selectedColumnGroups = new ArrayList<ColumnGroupEntry>();

		for (int i = 0; i < selectedTreeItems.length; i++) {
			if (isColumnGroupLeaf(selectedTreeItems[i])) {
				selectedColumnGroups.add((ColumnGroupEntry) selectedTreeItems[i].getData());
			}
		}
		return selectedColumnGroups;
	}

	private List<ColumnEntry> getSelectedColumnEntriesIncludingNested(Tree tree){
		return getColumnEntriesIncludingNested(tree.getSelection());
	}

	private List<ColumnGroupEntry> getSelectedColumnGroupEntries(Tree tree){
		return getSelectedColumnGroupEntries(tree.getSelection());
	}

	private void selectMatchingItems(Tree tree, IColumnMatcher matcher) {
		List<ColumnEntry> columnEntries = getColumnEntriesIncludingNested(tree.getItems());
		List<ColumnEntry> validEntries = new ArrayList<>();
		for (ColumnEntry entry : columnEntries) {
			if (matcher.match(entry)) validEntries.add(entry);
		}
		setSelectionIncludingNested(tree, ColumnChooserUtils.getColumnEntryIndexes(validEntries));
	}

	// Event handlers

	/**
	 * Add selected items: 'Available tree' --> 'Selected tree' Notify
	 * listeners.
	 */
	private void addSelected() {
		if (isAnyLeafSelected(availableTree)) {
			TreeItem topAvailableItem = availableTree.getTopItem();
			int topAvailableIndex = availableTree.indexOf(topAvailableItem);
			TreeItem topSelectedItem = selectedTree.getTopItem();
			int topSelectedIndex = topSelectedItem != null ? selectedTree.indexOf(topSelectedItem) : 0;

			fireItemsSelected(getSelectedColumnEntriesIncludingNested(availableTree));

			if (topAvailableIndex > -1 && topAvailableIndex < availableTree.getItemCount()) {
				availableTree.setTopItem(availableTree.getItem(topAvailableIndex));
			}
			if (topSelectedIndex > -1 && topSelectedIndex < selectedTree.getItemCount()) {
				selectedTree.setTopItem(selectedTree.getItem(topSelectedIndex));
			}
		}
	}

	/**
	 * Add selected items: 'Available tree' <-- 'Selected tree' Notify
	 * listeners.
	 */
	private void removeSelected() {
		if (isAnyLeafSelected(selectedTree)) {
			TreeItem topAvailableItem = availableTree.getTopItem();
			int topIndex = topAvailableItem == null ? -1 : availableTree.indexOf(topAvailableItem);

			TreeItem topSelectedItem = selectedTree.getTopItem();
			int topSelectedIndex = topSelectedItem == null ? - 1 : selectedTree.indexOf(topSelectedItem);

			fireItemsRemoved(getSelectedColumnEntriesIncludingNested(selectedTree));

			if (topIndex > -1 && topIndex < availableTree.getItemCount()) {
				availableTree.setTopItem(availableTree.getItem(topIndex));
			}
			if (topSelectedIndex > -1 && topSelectedIndex < selectedTree.getItemCount()) {
				selectedTree.setTopItem(selectedTree.getItem(topSelectedIndex));
			}
		}
	}

	private void selectedTreeCollapsed(TreeEvent event) {
		TreeItem item = (TreeItem) event.item;
		ColumnGroupEntry columnGroupEntry = (ColumnGroupEntry) item.getData();
		fireGroupCollapsed(columnGroupEntry);
	}

	private void selectedTreeExpanded(TreeEvent event) {
		TreeItem item = (TreeItem) event.item;
		ColumnGroupEntry columnGroupEntry = (ColumnGroupEntry) item.getData();
		fireGroupExpanded(columnGroupEntry);
	}

	private void toggleColumnGroupSelection(TreeItem treeItem) {
		if(isColumnGroupLeaf(treeItem)){
			Collection<TreeItem> selectedLeaves = ArrayUtil.asCollection(selectedTree.getSelection());
			boolean selected = selectedLeaves.contains(treeItem);
			if(selected){
				selectAllChildren(selectedTree, treeItem);
			} else {
				unSelectAllChildren(selectedTree, treeItem);
			}
		}
	}

	private void selectAllChildren(Tree tree, TreeItem treeItem) {
		Collection<TreeItem> selectedLeaves = ArrayUtil.asCollection(tree.getSelection());
		if(isColumnGroupLeaf(treeItem)){
			selectedLeaves.addAll(ArrayUtil.asCollection(treeItem.getItems()));
		}
		tree.setSelection(selectedLeaves.toArray(new TreeItem[]{}));
		tree.showSelection();
	}

	private void unSelectAllChildren(Tree tree, TreeItem treeItem) {
		Collection<TreeItem> selectedLeaves = ArrayUtil.asCollection(tree.getSelection());
		if(isColumnGroupLeaf(treeItem)){
			selectedLeaves.removeAll(ArrayUtil.asCollection(treeItem.getItems()));
		}
		tree.setSelection(selectedLeaves.toArray(new TreeItem[]{}));
		tree.showSelection();
	}

	private void moveSelectedToTop() {
		if (isAnyLeafSelected(selectedTree)) {
			if ((selectedFilterValue != null && !selectedFilterValue.isEmpty()) || !isFirstLeafSelected(selectedTree)) {
				List<ColumnEntry> selectedColumnEntries = getSelectedColumnEntriesIncludingNested(selectedTree);
				List<ColumnGroupEntry> selectedColumnGroupEntries = getSelectedColumnGroupEntries(selectedTree);

				List<Integer> allSelectedPositions = merge(selectedColumnEntries, selectedColumnGroupEntries);

				// Group continuous positions
				List<List<Integer>> postionsGroupedByContiguous = PositionUtil.getGroupedByContiguous(allSelectedPositions);
				List<Integer> toPositions = new ArrayList<Integer>();

				int shift = 0;
				for (List<Integer> groupedPositions : postionsGroupedByContiguous) {
					toPositions.add(shift);
					shift += groupedPositions.size();
				}
				fireItemsMoved(MoveDirectionEnum.UP, selectedColumnGroupEntries, selectedColumnEntries, postionsGroupedByContiguous, toPositions);
			}
		}
	}

	/**
	 * Move columns <i>up</i> in the 'Selected' Tree (Right)
	 */
	@SuppressWarnings("boxing")
	protected void moveSelectedUp() {
		if (isAnyLeafSelected(selectedTree)) {
			if(!isFirstLeafSelected(selectedTree)){
				List<ColumnEntry> selectedColumnEntries = getSelectedColumnEntriesIncludingNested(selectedTree);
				List<ColumnGroupEntry> selectedColumnGroupEntries = getSelectedColumnGroupEntries(selectedTree);

				List<Integer> allSelectedPositions = merge(selectedColumnEntries, selectedColumnGroupEntries);

				// Group continuous positions. If a column group moves, a bunch of 'from' positions move
				// to a single 'to' position
				List<List<Integer>> postionsGroupedByContiguous = PositionUtil.getGroupedByContiguous(allSelectedPositions);
				List<Integer> toPositions = new ArrayList<Integer>();

				//Set destination positions
				for (List<Integer> groupedPositions : postionsGroupedByContiguous) {
					// Do these contiguous positions contain a column group ?
					boolean columnGroupMoved = columnGroupMoved(groupedPositions, selectedColumnGroupEntries);

					//  If already at first position do not move
					int firstPositionInGroup = groupedPositions.get(0);
					if (firstPositionInGroup == 0){
						return;
					}

					// Column entry
					ColumnEntry columnEntry = getColumnEntryForPosition(selectedTree, firstPositionInGroup);
					int columnEntryIndex = columnEntry.getIndex();

					// Previous column entry
					ColumnEntry previousColumnEntry = getColumnEntryForPosition(selectedTree, firstPositionInGroup - 1);
					int previousColumnEntryIndex = previousColumnEntry.getIndex();

					if (columnGroupMoved) {
						// If the previous entry is a column group - move above it.
						if (columnGroupModel != null && columnGroupModel.isPartOfAGroup(previousColumnEntryIndex)) {
							ColumnGroup previousColumnGroup = columnGroupModel.getColumnGroupByIndex(previousColumnEntryIndex);
							toPositions.add(firstPositionInGroup - previousColumnGroup.getSize());
						} else {
							toPositions.add(firstPositionInGroup - 1);
						}
					} else {
						// If is first member of the unbreakable group, can't move up i.e. out of the group
						if (columnGroupModel != null && columnGroupModel.isPartOfAnUnbreakableGroup(columnEntryIndex) && !ColumnGroupUtils.isInTheSameGroup(columnEntryIndex, previousColumnEntryIndex, columnGroupModel)){
							return;
						}

						// If previous entry is an unbreakable column group - move above it
						if (columnGroupModel != null && columnGroupModel.isPartOfAnUnbreakableGroup(previousColumnEntryIndex) && !ColumnGroupUtils.isInTheSameGroup(columnEntryIndex, previousColumnEntryIndex, columnGroupModel)) {
							ColumnGroup previousColumnGroup = columnGroupModel.getColumnGroupByIndex(previousColumnEntryIndex);
							toPositions.add(firstPositionInGroup - previousColumnGroup.getSize());
						} else {
							toPositions.add(firstPositionInGroup - 1);
						}
					}
				}

				fireItemsMoved(MoveDirectionEnum.UP, selectedColumnGroupEntries, selectedColumnEntries, postionsGroupedByContiguous, toPositions);
			}
		}
	}

	private List<Integer> merge(List<ColumnEntry> selectedColumnEntries, List<ColumnGroupEntry> selectedColumnGroupEntries){
		//Convert to positions
		List<Integer> columnEntryPositions = ColumnChooserUtils.getColumnEntryPositions(selectedColumnEntries);
		List<Integer> columnGroupEntryPositions = ColumnGroupEntry.getColumnGroupEntryPositions(selectedColumnGroupEntries);

		//Selected columns + column groups
		Set<Integer> allSelectedPositionsSet = new HashSet<Integer>();
		allSelectedPositionsSet.addAll(columnEntryPositions);
		allSelectedPositionsSet.addAll(columnGroupEntryPositions);
		List<Integer> allSelectedPositions = new ArrayList<Integer>(allSelectedPositionsSet);
		Collections.sort(allSelectedPositions);

		return allSelectedPositions;
	}

	/**
	 * Move columns <i>down</i> in the 'Selected' Tree (Right)
	 */
	@SuppressWarnings("boxing")
	protected void moveSelectedDown() {
		if (isAnyLeafSelected(selectedTree)) {
			if (!isLastLeafSelected(selectedTree)) {
				List<ColumnEntry> selectedColumnEntries = getSelectedColumnEntriesIncludingNested(selectedTree);
				List<ColumnGroupEntry> selectedColumnGroupEntries = getSelectedColumnGroupEntries(selectedTree);

				List<Integer> allSelectedPositions = merge(selectedColumnEntries, selectedColumnGroupEntries);

				// Group continuous positions
				List<List<Integer>> postionsGroupedByContiguous = PositionUtil.getGroupedByContiguous(allSelectedPositions);
				List<Integer> toPositions = new ArrayList<Integer>();

				// Set destination positions
				for (List<Integer> groupedPositions : postionsGroupedByContiguous) {
					// Do these contiguous positions contain a column group ?
					boolean columnGroupMoved = columnGroupMoved(groupedPositions, selectedColumnGroupEntries);

					// Position of last element in list
					int lastListIndex = groupedPositions.size() - 1;
					int lastPositionInGroup = groupedPositions.get(lastListIndex);

					// Column entry
					ColumnEntry columnEntry = getColumnEntryForPosition(selectedTree, lastPositionInGroup);
					int columnEntryIndex = columnEntry.getIndex();

					// Next Column Entry
					ColumnEntry nextColumnEntry = getColumnEntryForPosition(selectedTree, lastPositionInGroup + 1);

					// Next column entry will be null the last leaf in the tree is selected
					if (nextColumnEntry == null) {
						return;
					}
					int nextColumnEntryIndex = nextColumnEntry.getIndex();

					if (columnGroupMoved) {
						// If the next entry is a column group - move past it.
						if (columnGroupModel != null && columnGroupModel.isPartOfAGroup(nextColumnEntryIndex)) {
							ColumnGroup nextColumnGroup = columnGroupModel.getColumnGroupByIndex(nextColumnEntryIndex);
							toPositions.add(lastPositionInGroup + nextColumnGroup.getSize());
						} else {
							toPositions.add(lastPositionInGroup + 1);
						}
					} else {
						// If is last member of the unbreakable group, can't move down i.e. out of the group
						if (columnGroupModel != null && columnGroupModel.isPartOfAnUnbreakableGroup(columnEntryIndex) && !ColumnGroupUtils.isInTheSameGroup(columnEntryIndex, nextColumnEntryIndex, columnGroupModel)) {
							return;
						}

						// If next entry is an unbreakable column group - move past it
						if (columnGroupModel != null && columnGroupModel.isPartOfAnUnbreakableGroup(nextColumnEntryIndex) && !ColumnGroupUtils.isInTheSameGroup(columnEntryIndex, nextColumnEntryIndex, columnGroupModel)) {
							ColumnGroup nextColumnGroup = columnGroupModel.getColumnGroupByIndex(nextColumnEntryIndex);
							toPositions.add(lastPositionInGroup + nextColumnGroup.getSize());
						} else {
							toPositions.add(lastPositionInGroup + 1);
						}
					}
				}
				fireItemsMoved(MoveDirectionEnum.DOWN, selectedColumnGroupEntries, selectedColumnEntries, postionsGroupedByContiguous, toPositions);
			}
		}
	}

	private void moveSelectedToBottom() {
		if (isAnyLeafSelected(selectedTree)) {
			if ((selectedFilterValue != null && !selectedFilterValue.isEmpty()) || !isLastLeafSelected(selectedTree)) {
				List<ColumnEntry> selectedColumnEntries = getSelectedColumnEntriesIncludingNested(selectedTree);
				List<ColumnGroupEntry> selectedColumnGroupEntries = getSelectedColumnGroupEntries(selectedTree);

				List<Integer> allSelectedPositions = merge(selectedColumnEntries, selectedColumnGroupEntries);

				// Group continuous positions
				List<List<Integer>> postionsGroupedByContiguous = PositionUtil.getGroupedByContiguous(allSelectedPositions);
				List<Integer> toPositions = new ArrayList<Integer>();

				List<List<Integer>> reversed = new ArrayList<List<Integer>>(postionsGroupedByContiguous);
				Collections.reverse(reversed);

				int totalSelItemCount = getColumnEntriesIncludingNested(selectedTree.getItems()).size();

				int shift = 0;
				for (List<Integer> groupedPositions : reversed) {
					toPositions.add(Integer.valueOf(totalSelItemCount - shift - 1));
					shift += groupedPositions.size();
				}

				fireItemsMoved(MoveDirectionEnum.DOWN, selectedColumnGroupEntries, selectedColumnEntries, reversed, toPositions);
			}
		}
	}

	private boolean columnGroupMoved(List<Integer> fromPositions, List<ColumnGroupEntry> movedColumnGroupEntries) {
		for (ColumnGroupEntry columnGroupEntry : movedColumnGroupEntries) {
			if(fromPositions.contains(columnGroupEntry.getFirstElementPosition())) return true;
		}
		return false;
	}

	/**
	 * Get the ColumnEntry in the tree with the given position
	 */
	private ColumnEntry getColumnEntryForPosition(Tree tree, int columnEntryPosition) {
		List<ColumnEntry> allColumnEntries = getColumnEntriesIncludingNested(selectedTree.getItems());

		for (ColumnEntry columnEntry : allColumnEntries) {
			if(columnEntry.getPosition().intValue() == columnEntryPosition){
				return columnEntry;
			}
		}
		return null;
	}

	// Leaf related methods

	/**
	 * Get Leaf index of the selected leaves in the tree
	 */
	protected List<Integer> getIndexesOfSelectedLeaves(Tree tree) {
		List<TreeItem> allSelectedLeaves = ArrayUtil.asList(tree.getSelection());
		List<Integer> allSelectedIndexes = new ArrayList<Integer>();

		for (TreeItem selectedLeaf : allSelectedLeaves) {
			allSelectedIndexes.add(Integer.valueOf(tree.indexOf(selectedLeaf)));
		}

		return allSelectedIndexes;
	}

	public void expandAllLeaves() {
		List<TreeItem> allLeaves = ArrayUtil.asList(selectedTree.getItems());

		for (TreeItem leaf : allLeaves) {
			if(isColumnGroupLeaf(leaf)){
				ColumnGroupEntry columnGroupEntry = (ColumnGroupEntry) leaf.getData();
				leaf.setExpanded(! columnGroupEntry.isCollapsed());
			}
		}
	}

	private boolean isColumnGroupLeaf(TreeItem treeItem) {
		if(ObjectUtils.isNotNull(treeItem)){
			return treeItem.getData() instanceof ColumnGroupEntry;
		} else {
			return false;
		}
	}

	private boolean isLastLeafSelected(Tree tree) {
		TreeItem[] selectedLeaves = tree.getSelection();
		for (int i = 0; i < selectedLeaves.length; i++) {
			if (tree.indexOf(selectedLeaves[i])+1 == tree.getItemCount()) {
				return true;
			}
		}
		return false;
	}

	private boolean isFirstLeafSelected(Tree tree) {
		TreeItem[] selectedLeaves = tree.getSelection();
		for (int i = 0; i < selectedLeaves.length; i++) {
			if (selectedTree.indexOf(selectedLeaves[i]) == 0) {
				return true;
			}
		}
		return false;
	}

	private boolean isAnyLeafSelected(Tree tree) {
		TreeItem[] selectedLeaves = tree.getSelection();
		return selectedLeaves != null && selectedLeaves.length > 0;
	}


	private ColumnEntry getColumnEntryInLeaf(TreeItem leaf) {
		if (!isColumnGroupLeaf(leaf)) {
			return (ColumnEntry) leaf.getData();
		} else {
			return null;
		}
	}

	public void removeAllLeaves() {
		selectedTree.removeAll();
		availableTree.removeAll();
	}

	// Leaf Selection

	public void setSelectionIncludingNested(List<Integer> indexes) {
		setSelectionIncludingNested(selectedTree, indexes);
	}

	public void setAvailableSelectionIncludingNested(List<Integer> indexes) {
		setSelectionIncludingNested(availableTree, indexes);
	}

	/**
	 * Marks the leaves in the tree as selected
	 * @param tree containing the leaves
	 * @param indexes index of the leaf in the tree
	 */
	protected void setSelection(Tree tree, List<Integer> indexes) {
		List<TreeItem> selectedLeaves = new ArrayList<TreeItem>();

		for (Integer leafIndex : indexes) {
			selectedLeaves.add(tree.getItem(leafIndex.intValue()));
		}
		tree.setSelection(selectedLeaves.toArray(new TreeItem[] {}));
		tree.showSelection();
	}

	/**
	 * Mark the leaves with matching column entries as selected.
	 * Also checks all the children of the column group leaves
	 * @param columnEntryIndexes index of the ColumnEntry in the leaf
	 */
	private void setSelectionIncludingNested(Tree tree, List<Integer> columnEntryIndexes) {
		Collection<TreeItem> allLeaves = ArrayUtil.asCollection(tree.getItems());
		List<TreeItem> selectedLeaves = new ArrayList<TreeItem>();

		for (TreeItem leaf : allLeaves) {
			if (!isColumnGroupLeaf(leaf)) {
				int index = getColumnEntryInLeaf(leaf).getIndex().intValue();
				if (columnEntryIndexes.contains(Integer.valueOf(index))) {
					selectedLeaves.add(leaf);
				}
			} else {
				//Check all children in column groups
				Collection<TreeItem> columnGroupLeaves = ArrayUtil.asCollection(leaf.getItems());
				for (TreeItem columnGroupLeaf : columnGroupLeaves) {
					int index = getColumnEntryInLeaf(columnGroupLeaf).getIndex().intValue();
					if (columnEntryIndexes.contains(Integer.valueOf(index))) {
						selectedLeaves.add(columnGroupLeaf);
					}
				}
			}
		}
		tree.setSelection(selectedLeaves.toArray(new TreeItem[] {}));
		setGroupsSelectionIfRequired(tree, columnEntryIndexes);
		tree.showSelection();
	}

	/**
	 * If all the leaves in a group are selected the group is also selected
	 */
	private void setGroupsSelectionIfRequired(Tree tree, List<Integer> columnEntryIndexes){
		Collection<TreeItem> allLeaves = ArrayUtil.asCollection(tree.getItems());
		Collection<TreeItem> selectedLeaves = ArrayUtil.asCollection(tree.getSelection());

		for (TreeItem leaf : allLeaves) {
			if(isColumnGroupLeaf(leaf)){
				boolean markSelected = true;
				Collection<TreeItem> nestedLeaves = ArrayUtil.asCollection(leaf.getItems());

				for (TreeItem nestedLeaf : nestedLeaves) {
					ColumnEntry columnEntry = getColumnEntryInLeaf(nestedLeaf);
					if(!columnEntryIndexes.contains(columnEntry.getIndex())){
						markSelected = false;
					}
				}
				if(markSelected){
					selectedLeaves.add(leaf);
				}
			}
		}
		tree.setSelection(selectedLeaves.toArray(new TreeItem[] {}));
	}

	protected Tree getSelectedTree() {
		return selectedTree;
	}

}
