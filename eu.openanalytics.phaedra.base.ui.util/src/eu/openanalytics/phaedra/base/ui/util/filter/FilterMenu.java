package eu.openanalytics.phaedra.base.ui.util.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;

public class FilterMenu {

	private Shell shell;
	private Composite composite;
	private Text filterText;
	private TreeViewer treeViewer;

	private List<FilterMenuSelectionListener> filterMenuSelectionListeners;

	public FilterMenu(ITreeContentProvider contentProvider, IBaseLabelProvider labelProvider, boolean changeOnFocus) {
		// While NO_TRIM looks best, it doesn't support resizable so we need the ugly border.
		shell = new Shell(Display.getCurrent().getActiveShell(), SWT.RESIZE | SWT.ON_TOP);
		Display display = shell.getDisplay();
		shell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		composite = new Composite(shell, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(1, 1).applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

		FilteredTree filteredTree = new FilteredTree(composite
				, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL
				, new NamePatternFilter(), true);

		filterText = filteredTree.getFilterControl();
		treeViewer = filteredTree.getViewer();

		Tree tree = treeViewer.getTree();
		tree.setHeaderVisible(false);
		tree.setLinesVisible(false);

		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(labelProvider);

		tree.addListener(SWT.KeyDown, e -> {
			switch (e.keyCode) {
			case SWT.ARROW_DOWN:
				if (tree.getSelection().length > 0) {
					TreeItem selItem = tree.getSelection()[0];
					TreeItem lastItem = tree.getItem(tree.getItemCount() - 1);
					if (selItem == lastItem) {
						if (!tree.getSelection()[0].getExpanded()) {
							// We're at the last unexpanded element. Focus filter.
							filterText.setFocus();
						}
					} else if (selItem.getParentItem() == lastItem) {
						TreeItem parentItem = selItem.getParentItem();
						if (selItem == parentItem.getItem(parentItem.getItemCount() - 1)) {
							// We're at the last child element of the expanded last element. Focus filter.
							filterText.setFocus();
						}
					}
				}
				break;
			case SWT.ARROW_UP:
				if (tree.getSelection().length > 0) {
					if (tree.getSelection()[0] == tree.getItem(0)) {
						// On the first item, going up should grant focus to text field
						filterText.setFocus();
					}
				}
				break;
			case SWT.ESC:
				dispose();
				break;
			}

			// TODO: Test if useful. Also capture other characters such as numbers, (, ), /, ...
			//if (e.keyCode >= 97 && e.keyCode <= 122) {
			//	filterText.setText(filterText.getText() + e.character);
			//	filterText.setFocus();
			//	filterText.setSelection(filterText.getText().length());
			//}
		});

		filterText.addListener(SWT.KeyDown, e -> {
			switch (e.keyCode) {
			case SWT.CR:
			case SWT.KEYPAD_CR:
				gotoSelectedElement(true);
				break;
			case SWT.ARROW_DOWN:
				tree.setFocus();
				tree.setSelection(tree.getItem(0));
				break;
			case SWT.ARROW_UP:
				tree.setFocus();
				TreeItem lastItem = tree.getItem(tree.getItemCount() - 1);
				if (lastItem.getExpanded()) {
					TreeItem lastChildItem = lastItem.getItem(lastItem.getItemCount() - 1);
					tree.setSelection(lastChildItem);
				} else {
					tree.setSelection(lastItem);
				}
				break;
			case SWT.ESC:
				dispose();
				break;
			}
		});
		if (changeOnFocus) treeViewer.addSelectionChangedListener(e -> gotoSelectedElement(false));
		treeViewer.addDoubleClickListener(e -> gotoSelectedElement(true));

		// Close the shell when no longer active.
		shell.addListener(SWT.Deactivate, e -> dispose());

		GridDataFactory.fillDefaults().grab(true, true).applyTo(filteredTree);

		GridLayoutFactory.fillDefaults().margins(1, 1).applyTo(shell);

		shell.setSize(new Point(250, 500));

		filterMenuSelectionListeners = new ArrayList<>();
	}

	public void dispose() {
		if (shell != null) {
			if (!shell.isDisposed()) shell.dispose();
			shell = null;
			treeViewer = null;
			composite = null;
			filterText = null;
		}
	}

	public void setInput(Object input) {
		treeViewer.setInput(input);
	}

	public void setFocus() {
		shell.setVisible(true);
		shell.setFocus();
		filterText.setFocus();
	}

	public Point computeSizeHint() {
		return shell.getSize();
		//return shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	public void setLocation(Point location) {
		Rectangle trim = shell.computeTrim(0, 0, 0, 0);
		Point textLocation = composite.getLocation();
		location.x += trim.x - textLocation.x;
		location.y += trim.y - textLocation.y;
		shell.setLocation(location);
	}

	public void addFilterMenuSelectionListener(FilterMenuSelectionListener listener) {
		filterMenuSelectionListeners.add(listener);
	}

	public void removeFilterMenuSelectionListener(FilterMenuSelectionListener listener) {
		filterMenuSelectionListeners.remove(listener);
	}

	protected Object getSelectedElement() {
		return ((IStructuredSelection) treeViewer.getSelection()).getFirstElement();
	}

	private void gotoSelectedElement(boolean close) {
		Tree tree = treeViewer.getTree();
		if (tree.getSelectionCount() < 1) return;
		if (tree.getSelection()[0].getItemCount() > 0) return;

		for (FilterMenuSelectionListener listener : filterMenuSelectionListeners) {
			listener.selected(getSelectedElement());
		}

		// Close the shell
		if (close) dispose();
	}

	public interface FilterMenuSelectionListener {
		public void selected(Object selectedElement);
	}

}
