package eu.openanalytics.phaedra.base.ui.search.editor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

import eu.openanalytics.phaedra.base.search.model.QueryOrdering;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class QueryOrderingPanel extends Composite {
	private QueryOrdering queryOrdering;

	private QueryEditor queryEditor;

	private ComboViewer columnComboViewer;
	private ComboViewer directionComboViewer;
	private Button caseSensitiveCheckbox;
	private ImageHyperlink moveUpLink;
	private ImageHyperlink moveDownLink;
	private ImageHyperlink removeFilterLink;

	private ISelectionChangedListener columnSelectionChangedListener;

	public QueryOrderingPanel(Composite parent, QueryEditor queryEditor, QueryOrdering queryOrdering) {
		super(parent, SWT.NULL);

		this.queryEditor = queryEditor;
		this.queryOrdering = queryOrdering;

		this.columnSelectionChangedListener = e -> handleColumnChange();

		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(this);

		CCombo columnCombo = new CCombo(this, SWT.BORDER | SWT.READ_ONLY);
		createColumnComboViewer(columnCombo);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(170, SWT.DEFAULT).applyTo(columnCombo);

		CCombo directionCombo = new CCombo(this, SWT.BORDER | SWT.READ_ONLY);
		createDirectionComboViewer(directionCombo);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(directionCombo);

		createCaseSensitiveCheckbox();
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(caseSensitiveCheckbox);

		createMoveUpLink();
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(moveUpLink);

		createMoveDownLink();
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(moveDownLink);

		createRemoveFilterLink();
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(removeFilterLink);

		enableColumnSelectionChangedListener();

		// add dirty adapters
		columnComboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		directionComboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		caseSensitiveCheckbox.addSelectionListener(queryEditor.getDirtySelectionAdapter());
		moveUpLink.addHyperlinkListener(queryEditor.getDirtyLinkAdapter());
		moveDownLink.addHyperlinkListener(queryEditor.getDirtyLinkAdapter());
		removeFilterLink.addHyperlinkListener(queryEditor.getDirtyLinkAdapter());
	}

	private void createColumnComboViewer(CCombo combo) {
		columnComboViewer = new ComboViewer(combo);
		columnComboViewer.setContentProvider(new ArrayContentProvider());
		columnComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Field) element).getName();
			}
		});
		columnComboViewer.setComparator(new ViewerComparator());
		updateOrderingColumns();
	}

	private void createDirectionComboViewer(CCombo combo) {
		directionComboViewer = new ComboViewer(combo);
		directionComboViewer.setContentProvider(new ArrayContentProvider());
		directionComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				Boolean ascending = (Boolean) element;
				return ascending ? "ascending" : "descending";
			}
		});
		directionComboViewer.addSelectionChangedListener(e -> handleDirectionChange());
		directionComboViewer.setInput(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
		directionComboViewer.getCCombo().select(0);
	}

	private void createCaseSensitiveCheckbox() {
		caseSensitiveCheckbox = new Button(this, SWT.CHECK);
		caseSensitiveCheckbox.setText("Case sensitive");
		caseSensitiveCheckbox.setSelection(queryOrdering.isCaseSensitive());
		caseSensitiveCheckbox.setVisible(Objects.equal(queryOrdering.getColumnType(), String.class));
		caseSensitiveCheckbox.addListener(SWT.Selection, e -> queryOrdering.setCaseSensitive(caseSensitiveCheckbox.getSelection()));
	}

	private void createMoveUpLink() {
		moveUpLink = new ImageHyperlink(this, SWT.TRANSPARENT);
		moveUpLink.setImage(IconManager.getIconImage("arrow_up.png"));
		moveUpLink.setToolTipText("Move Up");
		moveUpLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				queryEditor.moveUpQueryPanel(QueryOrderingPanel.this);
			}
		});
	}

	private void createMoveDownLink() {
		moveDownLink = new ImageHyperlink(this, SWT.TRANSPARENT);
		moveDownLink.setImage(IconManager.getIconImage("arrow_down.png"));
		moveDownLink.setToolTipText("Move Down");
		moveDownLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				queryEditor.moveDownQueryPanel(QueryOrderingPanel.this);
			}
		});
	}

	private void createRemoveFilterLink() {
		removeFilterLink = new ImageHyperlink(this, SWT.TRANSPARENT);
		removeFilterLink.setImage(IconManager.getIconImage("delete.png"));
		removeFilterLink.setToolTipText("Remove Query Ordering");
		removeFilterLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				queryEditor.removeOrderingPanel(QueryOrderingPanel.this);
			}
		});
	}

	public QueryOrdering getQueryOrdering() {
		return queryOrdering;
	}

	protected ImageHyperlink getMoveUpLink() {
		return moveUpLink;
	}

	protected ImageHyperlink getMoveDownLink() {
		return moveDownLink;
	}

	protected void updateOrderingColumns() {
		if (queryEditor.getQueryModel().getType() != null) {
			final List<String> orderingColumnNames = new ArrayList<>(QueryOrdering.getColumnNames(queryEditor.getQueryModel().getQueryOrderings()));
			List<Field> allColumns = new ArrayList<>(ReflectionUtils.getCompatibleFields(queryEditor.getQueryModel().getType(), false, true, true, Arrays.asList(new Class<?>[]{Number.class, Boolean.class, String.class, Date.class})));
			Collection<Field> columns = Collections2.filter(allColumns, new Predicate<Field>() {
				@Override
				public boolean apply(Field field) {
					return !orderingColumnNames.contains(field.getName()) || Objects.equal(queryOrdering.getColumnName(), field.getName());
				}
			});
			columnComboViewer.setInput(columns);
			columnComboViewer.getCCombo().setVisibleItemCount(columns.size());
			Field column = Iterables.find(columns, new Predicate<Field>() {
				@Override
				public boolean apply(Field field) {
					return field.getName().equals(queryOrdering.getColumnName());
				}
			}, null);
			if (column != null) {
				columnComboViewer.setSelection(new StructuredSelection(column));
			}
		}
	}

	private void enableColumnSelectionChangedListener() {
		columnComboViewer.addSelectionChangedListener(columnSelectionChangedListener);
	}

	private void disableColumnSelectionChangedListener() {
		columnComboViewer.removeSelectionChangedListener(columnSelectionChangedListener);
	}

	private void handleColumnChange() {
		Field column = (Field) ((IStructuredSelection) columnComboViewer.getSelection()).getFirstElement();

		queryOrdering.setColumnName(column.getName());
		queryOrdering.setColumnType(column.getType());

		caseSensitiveCheckbox.setVisible(Objects.equal(column.getType(), String.class));

		for (QueryOrderingPanel orderingPanel : queryEditor.getOrderingPanels()) {
			orderingPanel.disableColumnSelectionChangedListener();
			orderingPanel.updateOrderingColumns();
			orderingPanel.enableColumnSelectionChangedListener();
		}
	}

	private void handleDirectionChange() {
		Boolean selectedAscending = (Boolean) ((IStructuredSelection) directionComboViewer.getSelection()).getFirstElement();
		if (!Objects.equal(selectedAscending, queryOrdering.isAscending())) {
			queryOrdering.setAscending(selectedAscending);
		}
	}

	public List<String> validate() {
		List<String> errorMessages = new ArrayList<>();
		if (columnComboViewer.getSelection().isEmpty()) {
			errorMessages.add("Column should be set for query orderings");
		}
		return errorMessages;
	}

}
