package eu.openanalytics.phaedra.base.ui.search.editor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import eu.openanalytics.phaedra.base.search.SearchService;
import eu.openanalytics.phaedra.base.search.model.Operator;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.Operator.OperatorType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.search.IQueryValuePanelFactory;
import eu.openanalytics.phaedra.base.ui.search.internal.QueryEditorSupportRegistry;
import eu.openanalytics.phaedra.base.ui.search.internal.QueryValuePanelFactoryRegistry;
import eu.openanalytics.phaedra.base.util.reflect.ReflectionUtils;

public class QueryFilterPanel extends Composite {
	private QueryFilter queryFilter;

	private QueryEditor queryEditor;

	private TableComboViewer typesComboViewer;
	private ComboViewer columnComboViewer;
	private ComboViewer positiveComboViewer;
	private ComboViewer operatorComboViewer;
	private Composite valuePanelContainer;
	private Composite valuePanel;
	private ImageHyperlink removeFilterLink;

	private LocalResourceManager resourceManager;

	public QueryFilterPanel(Composite parent, QueryEditor queryEditor, QueryFilter queryFilter) {
		super(parent, SWT.NULL);

		this.queryEditor = queryEditor;
		this.queryFilter = queryFilter;

		this.resourceManager = new LocalResourceManager(JFaceResources.getResources(), this);

		GridLayoutFactory.fillDefaults().numColumns(6).applyTo(this);

		TableCombo typeCombo = new TableCombo(this, SWT.BORDER | SWT.READ_ONLY);
		createTypeComboViewer(typeCombo);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(typeCombo);

		CCombo columnCombo = new CCombo(this, SWT.BORDER | SWT.READ_ONLY);
		createColumnComboViewer(columnCombo);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(170, SWT.DEFAULT).applyTo(columnCombo);

		CCombo positiveCombo = new CCombo(this, SWT.BORDER | SWT.READ_ONLY);
		createPositiveCombo(positiveCombo);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(positiveCombo);

		CCombo operatorCombo = new CCombo(this, SWT.BORDER | SWT.READ_ONLY);
		createOperatorComboViewer(operatorCombo);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(operatorCombo);

		valuePanelContainer = new Composite(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(valuePanelContainer);
		IQueryValuePanelFactory newFactory = QueryValuePanelFactoryRegistry.getInstance().getFactory(queryFilter);
		if (!newFactory.checkValue(queryFilter)) {
			newFactory.clearValue(queryFilter);
		}
		valuePanel = newFactory.createQueryValuePanel(valuePanelContainer, queryEditor, queryFilter);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(valuePanelContainer);

		createRemoveFilterLink();
		GridDataFactory.fillDefaults().align(SWT.RIGHT, SWT.CENTER).applyTo(removeFilterLink);

		// add dirty adapters
		typesComboViewer.getTableCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		columnComboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		positiveComboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		operatorComboViewer.getCCombo().addSelectionListener(queryEditor.getDirtySelectionAdapter());
		removeFilterLink.addHyperlinkListener(queryEditor.getDirtyLinkAdapter());
	}

	private void createTypeComboViewer(TableCombo combo) {
		typesComboViewer = new TableComboViewer(combo);
		typesComboViewer.setContentProvider(new ArrayContentProvider());
		typesComboViewer.setLabelProvider(new LabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getText(Object element) {
				return QueryEditorSupportRegistry.getInstance().getFactory(((Class<? extends PlatformObject>) element)).getLabel();
			}
			@SuppressWarnings("unchecked")
			@Override
			public Image getImage(Object element) {
				ImageDescriptor imageDescriptor = IconManager.getDefaultIconDescriptor((Class<? extends PlatformObject>) element);
				return imageDescriptor != null ? resourceManager.createImage(imageDescriptor) : null;
		}
		});
		typesComboViewer.setInput(SearchService.getInstance().getSupportedClasses());
		typesComboViewer.setSorter(new ViewerSorter());
		typesComboViewer.getTableCombo().setVisibleItemCount(typesComboViewer.getTableCombo().getItemCount());
		typesComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Class<? extends PlatformObject> selectedType = (Class<? extends PlatformObject>) ((IStructuredSelection) typesComboViewer.getSelection()).getFirstElement();
				if (!Objects.equal(selectedType, queryFilter.getType())) {
					queryFilter.setType(selectedType);
					updateColumnComboViewer();
				}
			}
		});
		if (queryFilter.getType() != null) {
			updateTypeComboViewer();
		}
	}

	private void createColumnComboViewer(CCombo combo) {
		columnComboViewer = new ComboViewer(combo);
		columnComboViewer.setContentProvider(new ArrayContentProvider());
		columnComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				String fieldName = ((Field) element).getName();
				return QueryEditorSupportRegistry.getInstance().getFactory(queryFilter.getType()).getLabelForField(fieldName);
			}
		});
		columnComboViewer.setComparator(new ViewerComparator());
		columnComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Field selectedField = (Field) ((IStructuredSelection) columnComboViewer.getSelection()).getFirstElement();
				if (!columnEquals(selectedField, queryFilter)) {
					IQueryValuePanelFactory oldFactory = QueryValuePanelFactoryRegistry.getInstance().getFactory(queryFilter);
					queryFilter.setColumnName(selectedField.getName());
					queryFilter.setOperatorType(Operator.OperatorType.getOperatorTypeFor(selectedField));
					if (!Operator.getOperators(queryFilter.getOperatorType()).contains(queryFilter.getOperator())) {
						queryFilter.setOperator(null);
					}
					IQueryValuePanelFactory newFactory = QueryValuePanelFactoryRegistry.getInstance().getFactory(queryFilter);
					updateOperatorColumnComboViewer();
					updateValuePanel(oldFactory, newFactory);
				}
			}
		});
		if (queryFilter.getType() != null) {
			updateColumnComboViewer();
		}
	}

	private void createPositiveCombo(CCombo combo) {
		positiveComboViewer = new ComboViewer(combo);
		positiveComboViewer.setContentProvider(new ArrayContentProvider());
		positiveComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				Boolean positive = (Boolean) element;
				return positive ? "is" : "is not";
			}
		});
		positiveComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Boolean selectedPositive = (Boolean) ((IStructuredSelection) positiveComboViewer.getSelection()).getFirstElement();
				if (!Objects.equal(selectedPositive, queryFilter.isPositive())) {
					queryFilter.setPositive(selectedPositive);
					updatePositiveCombo(selectedPositive);
				}
			}
		});
		positiveComboViewer.setInput(Arrays.asList(Boolean.TRUE, Boolean.FALSE));
		positiveComboViewer.getCCombo().select(0);
		updatePositiveCombo(queryFilter.isPositive());
	}

	private void createOperatorComboViewer(CCombo combo) {
		operatorComboViewer = new ComboViewer(combo);
		operatorComboViewer.setContentProvider(new ArrayContentProvider());
		operatorComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Operator) element).getName();
			}
		});
		operatorComboViewer.setSorter(new ViewerSorter());
		operatorComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Operator selectedOperator = (Operator) ((IStructuredSelection) operatorComboViewer.getSelection()).getFirstElement();
				if (!Objects.equal(selectedOperator, queryFilter.getOperator())) {
					IQueryValuePanelFactory oldFactory = QueryValuePanelFactoryRegistry.getInstance().getFactory(queryFilter);
					queryFilter.setOperator(selectedOperator);
					IQueryValuePanelFactory newFactory = QueryValuePanelFactoryRegistry.getInstance().getFactory(queryFilter);
					updateValuePanel(oldFactory, newFactory);
				}
			}
		});
		if (queryFilter.getOperator() != null) {
			updateOperatorColumnComboViewer();
		}
	}

	private void createRemoveFilterLink() {
		removeFilterLink = new ImageHyperlink(this, SWT.TRANSPARENT);
		removeFilterLink.setImage(IconManager.getIconImage("delete.png"));
		removeFilterLink.setToolTipText("Remove Query Filter");
		removeFilterLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				queryEditor.removeFilterPanel(QueryFilterPanel.this);
			}
		});
	}

	private boolean columnEquals(Field field, QueryFilter filter) {
		return Objects.equal(field.getName(), filter.getColumnName()) && Objects.equal(OperatorType.getOperatorTypeFor(field), filter.getOperatorType());
	}


	// update methods
	private void updateTypeComboViewer() {
		typesComboViewer.setSelection(new StructuredSelection(queryFilter.getType()));
	}

	private void updateColumnComboViewer() {
		Collection<Field> compatibleFields = ReflectionUtils.getCompatibleFields(queryFilter.getType(), false, true, true, Arrays.asList(new Class<?>[]{Number.class, Boolean.class, String.class, Date.class}));
		columnComboViewer.setInput(compatibleFields);
		columnComboViewer.getCCombo().setVisibleItemCount(compatibleFields.size());

		Predicate<Field> predicate = field -> columnEquals(field, queryFilter);

		Field field = Iterables.find(compatibleFields, predicate, null);
		if (field != null) {
			columnComboViewer.setSelection(new StructuredSelection(field));
		}
	}

	private void updatePositiveCombo(Boolean positive) {
		positiveComboViewer.setSelection(new StructuredSelection(queryFilter.isPositive()));
	}

	private void updateOperatorColumnComboViewer() {
		Set<Operator> operators = Operator.getOperators(queryFilter.getOperatorType());
		operatorComboViewer.setInput(operators);
		operatorComboViewer.getCCombo().setVisibleItemCount(operators.size());

		if (operators.contains(queryFilter.getOperator())) {
			operatorComboViewer.setSelection(new StructuredSelection(queryFilter.getOperator()));
		}
	}

	private void updateValuePanel(IQueryValuePanelFactory oldFactory, IQueryValuePanelFactory newFactory) {
		if (!newFactory.equals(oldFactory)) {
			valuePanel.dispose();
			if (!newFactory.checkValue(queryFilter)) {
				newFactory.clearValue(queryFilter);
			}
			valuePanel = newFactory.createQueryValuePanel(valuePanelContainer, queryEditor, queryFilter);
			valuePanelContainer.layout();
		}
	}

	public QueryFilter getQueryFilter() {
		return queryFilter;
	}

	public List<String> validate() {
		List<String> errorMessages = new ArrayList<>();
		if (typesComboViewer.getSelection().isEmpty()) {
			errorMessages.add("Type should be set for query filters");
		}
		if (columnComboViewer.getSelection().isEmpty()) {
			errorMessages.add("Column should be set for query filters");
		}
		if (operatorComboViewer.getSelection().isEmpty()) {
			errorMessages.add("Operator should be set for query filters");
		} else {
			errorMessages.addAll(queryFilter.getOperator().validate(queryFilter.getValue()));
		}
		return errorMessages;
	}

}
