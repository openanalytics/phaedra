package eu.openanalytics.phaedra.base.ui.search.editor;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.time.StopWatch;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.tablecombo.TableCombo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.eclipselink.persistence.core.PatchedEclipselinkService;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.search.SearchService;
import eu.openanalytics.phaedra.base.search.model.QueryException;
import eu.openanalytics.phaedra.base.search.model.QueryFilter;
import eu.openanalytics.phaedra.base.search.model.QueryModel;
import eu.openanalytics.phaedra.base.search.model.QueryOrdering;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.editor.EditorFactory;
import eu.openanalytics.phaedra.base.ui.editor.VOEditorInput;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.nattable.NatTableBuilder;
import eu.openanalytics.phaedra.base.ui.nattable.misc.IRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.NullRichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.misc.RichColumnAccessor;
import eu.openanalytics.phaedra.base.ui.nattable.selection.NatTableSelectionProvider;
import eu.openanalytics.phaedra.base.ui.nattable.selection.SelectionTransformer;
import eu.openanalytics.phaedra.base.ui.search.Activator;
import eu.openanalytics.phaedra.base.ui.search.IQueryEditorSupport;
import eu.openanalytics.phaedra.base.ui.search.internal.QueryEditorSupportRegistry;
import eu.openanalytics.phaedra.base.ui.util.misc.FormEditorUtils;
import eu.openanalytics.phaedra.base.ui.util.pinning.ConfigurableStructuredSelection;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.base.util.misc.SelectionProviderIntermediate;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class QueryEditor extends EditorPart {

	private QueryModel queryModel;
	private QueryModel originalModel;

	private FormToolkit formToolkit;
	private Text queryNameTxt;
	private Text queryDescriptionTxt;
	private Text queryOwnerTxt;
	private Button publicBtn;
	private Button exampleBtn;
	private TableComboViewer resultTypeComboViewer;
	private NatTable table;
	private IRichColumnAccessor<PlatformObject> columnAccessor;
	private NatTableSelectionProvider<PlatformObject> natSelectionProvider;
	private EventList<PlatformObject> eventList;
	private boolean isGroupBy;
	private Button maxResultsBtn;
	private Text maxResultsTxt;
	private ImageHyperlink addQueryFilterButton;
	private ImageHyperlink addQueryOrderingButton;

	private LocalResourceManager resourceManager;
	private ISelectionListener selectionListener;
	private MenuManager menuMgr;
	private SelectionProviderIntermediate selectionProvider;
	private SelectionAdapter dirtySelectionAdapter;
	private KeyAdapter dirtyKeyAdapter;
	private HyperlinkAdapter dirtyLinkAdapter;

	private ScrolledForm form;
	private Composite parent;
	private Composite resultPanelComposite;

	private List<QueryFilterPanel> filterPanels = new LinkedList<>();
	private List<QueryOrderingPanel> orderingPanels = new LinkedList<>();

	private Action executeAction;
	private Action saveAction;
	private Action saveAsAction;
	private Action deleteAction;

	private Section queryResultsSection;

	private InputDialog saveAsDialog;

	private boolean dirty;
	private Class<?> previousResultType;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		originalModel = SelectionUtils.getFirstAsClass(((VOEditorInput)input).getValueObjects(), QueryModel.class);
		// Create a working copy that can be safely discarded if the user doesn't want to save.
		queryModel = originalModel.getCopy();
		queryModel.setName(originalModel.getName());
		queryModel.setOwner(originalModel.getOwner());
		queryModel.setDate(originalModel.getDate());

		setInput(input);
		setSite(site);
		setPartName(input.getName());
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;

		this.formToolkit = FormEditorUtils.createToolkit();
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(parent);

		createSaveAction();
		createSaveAsAction();
		createDeleteAction();
		createExecuteAction();
		createFormToolbar();

		createDirtyAdapters();

		// section 1: general query settings

		Section section = FormEditorUtils.createSection("Query Settings", form.getBody(), formToolkit);
		Composite container = FormEditorUtils.createComposite(4, section, formToolkit);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(section);

		resourceManager = new LocalResourceManager(JFaceResources.getResources(), container);

		Label queryNameLbl = FormEditorUtils.createLabel("Name:", container, formToolkit);
		GridDataFactory.fillDefaults().applyTo(queryNameLbl);

		queryNameTxt = formToolkit.createText(container, queryModel.getName(), SWT.SINGLE | SWT.WRAP);
		queryNameTxt.addModifyListener(e -> {
			queryModel.setName(queryNameTxt.getText());
			form.setText(queryModel.getName());
			setPartName(queryModel.getName());
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(queryNameTxt);

		Label ownerLbl = FormEditorUtils.createLabel("Owner:", container, formToolkit);
		GridDataFactory.fillDefaults().applyTo(ownerLbl);

		queryOwnerTxt = formToolkit.createText(container, queryModel.getOwner(), SWT.READ_ONLY);
		GridDataFactory.fillDefaults().hint(130, SWT.DEFAULT).applyTo(queryOwnerTxt);

		Label queryDescriptionLbl = FormEditorUtils.createLabel("Description:", container, formToolkit);
		GridDataFactory.fillDefaults().applyTo(queryDescriptionLbl);

		queryDescriptionTxt = formToolkit.createText(container, queryModel.getDescription(), SWT.SINGLE | SWT.WRAP);
		queryDescriptionTxt.addModifyListener(e -> queryModel.setDescription(queryDescriptionTxt.getText()));
		GridDataFactory.fillDefaults().hint(25, SWT.DEFAULT).applyTo(queryDescriptionTxt);

		maxResultsBtn = formToolkit.createButton(container, "Max results:", SWT.CHECK);
		maxResultsBtn.setSelection(queryModel.isMaxResultsSet());
		maxResultsBtn.addListener(SWT.Selection, e -> {
			queryModel.setMaxResultsSet(maxResultsBtn.getSelection());
			maxResultsTxt.setEnabled(queryModel.isMaxResultsSet());
		});
		GridDataFactory.fillDefaults().applyTo(maxResultsBtn);

		maxResultsTxt = formToolkit.createText(container, "" + queryModel.getMaxResults());
		maxResultsTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		maxResultsTxt.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					queryModel.setMaxResults(Integer.parseInt(maxResultsTxt.getText()));
				} catch (Exception ex) {
				}
			}
		});
		maxResultsTxt.addListener(SWT.FocusOut, e -> maxResultsTxt.setText("" + queryModel.getMaxResults()));
		GridDataFactory.fillDefaults().applyTo(maxResultsTxt);

		Label resultTypeLbl = FormEditorUtils.createLabel("Result Type:", container, formToolkit);
		GridDataFactory.fillDefaults().applyTo(resultTypeLbl);

		TableCombo resultTypeCombo = new TableCombo(container, SWT.BORDER | SWT.READ_ONLY);
		createResultTypeComboViewer(resultTypeCombo);
		GridDataFactory.fillDefaults().applyTo(resultTypeCombo);

		publicBtn = formToolkit.createButton(container, "Public", SWT.CHECK);
		publicBtn.setSelection(queryModel.isPublicQuery());
		publicBtn.addListener(SWT.Selection, e -> queryModel.setPublicQuery(publicBtn.getSelection()));
		GridDataFactory.fillDefaults().applyTo(publicBtn);

		exampleBtn = formToolkit.createButton(container, "Example", SWT.CHECK);
		exampleBtn.setSelection(queryModel.isExample());
		exampleBtn.addListener(SWT.Selection, e -> queryModel.setExample(exampleBtn.getSelection()));
		GridDataFactory.fillDefaults().applyTo(exampleBtn);

		// Section 2: query filters

		section = FormEditorUtils.createSection("Query Filters", form.getBody(), formToolkit);
		container = FormEditorUtils.createComposite(1, section, formToolkit);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(section);

		final Composite filterToolbar = formToolkit.createComposite(section, SWT.TRANSPARENT);
		filterToolbar.setLayout(new GridLayout(2, false));
		section.setTextClient(filterToolbar);
		formToolkit.paintBordersFor(filterToolbar);

		fillFilterToolbar(container, filterToolbar);

		for (QueryFilter queryFilter : queryModel.getQueryFilters()) {
			addFilterPanel(queryFilter, container);
		}

		// Section 3: query orderings

		section = FormEditorUtils.createSection("Query Orderings", form.getBody(), formToolkit);
		container = FormEditorUtils.createComposite(1, section, formToolkit);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(section);

		final Composite orderingToolbar = formToolkit.createComposite(section, SWT.TRANSPARENT);
		orderingToolbar.setLayout(new GridLayout(2, false));
		section.setTextClient(orderingToolbar);
		formToolkit.paintBordersFor(orderingToolbar);

		fillOrderingToolbar(container, orderingToolbar);

		for (QueryOrdering queryOrdering : queryModel.getQueryOrderings()) {
			addOrderingPanel(queryOrdering, container);
		}
		updateMoveButtonStates();

		// Section 4: query results

		queryResultsSection = FormEditorUtils.createSection("Query Results", form.getBody(), formToolkit);
		resultPanelComposite = FormEditorUtils.createComposite(1, queryResultsSection, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(queryResultsSection);

		final Composite resultToolbar = formToolkit.createComposite(queryResultsSection, SWT.TRANSPARENT);
		resultToolbar.setLayout(new GridLayout(2, false));
		queryResultsSection.setTextClient(resultToolbar);
		formToolkit.paintBordersFor(resultToolbar);

		fillResultToolbar(resultToolbar);

		menuMgr = createContextMenu();
		selectionProvider = new SelectionProviderIntermediate();
		getSite().setSelectionProvider(selectionProvider);
		getSite().registerContextMenu(menuMgr, selectionProvider);

		eventList = GlazedLists.eventListOf();
		columnAccessor = new NullRichColumnAccessor<PlatformObject>();
		createTable();

		// create Save As dialog
		this.saveAsDialog = new InputDialog(Display.getDefault().getActiveShell(), "Save Query As", "Please enter a name for the query", "Copy of " + queryModel.getName(), null);

		// add dirty adapters

		queryNameTxt.addKeyListener(dirtyKeyAdapter);
		queryDescriptionTxt.addKeyListener(dirtyKeyAdapter);
		resultTypeComboViewer.getTableCombo().addSelectionListener(dirtySelectionAdapter);
		maxResultsBtn.addSelectionListener(dirtySelectionAdapter);
		maxResultsTxt.addKeyListener(dirtyKeyAdapter);
		publicBtn.addSelectionListener(dirtySelectionAdapter);
		exampleBtn.addSelectionListener(dirtySelectionAdapter);
		addQueryFilterButton.addHyperlinkListener(dirtyLinkAdapter);
		addQueryOrderingButton.addHyperlinkListener(dirtyLinkAdapter);

		createSelectionListener();

		form.getParent().layout(true);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewQueryEditor");
	}

	private void createSelectionListener() {
		selectionListener = (part, selection) -> {
			if (part == QueryEditor.this || table == null || previousResultType == null) return;

			natSelectionProvider.setSelection(selection);
		};
		getSite().getPage().addSelectionListener(selectionListener);
	}

	private void createFormToolbar() {
		form = FormEditorUtils.createScrolledForm(queryModel.getName(), 1, parent, formToolkit);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(form);
		formToolkit.decorateFormHeading(form.getForm());
		form.getForm().setToolBarVerticalAlignment(SWT.TOP);
		form.getToolBarManager().add(saveAction);
		form.getToolBarManager().add(saveAsAction);
		form.getToolBarManager().add(deleteAction);
		form.getToolBarManager().add(executeAction);
		form.getToolBarManager().update(true);
	}

	private void createTable() {
		NatTableBuilder<PlatformObject> builder = new NatTableBuilder<>(columnAccessor, eventList);
		table = builder
				.addSelectionProvider(new SelectionTransformer<PlatformObject>(PlatformObject.class))
				.addCustomCellPainters(columnAccessor.getCustomCellPainters())
				.addConfiguration(columnAccessor.getCustomConfiguration())
				.addColumnDialogMatchers(columnAccessor.getColumnDialogMatchers())
				.resizeColumns(columnAccessor.getColumnWidths())
				.makeGroupByable(isGroupBy)
				.build(resultPanelComposite, true, menuMgr);
		table.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		table.setVisible(false);
		GridDataFactory.fillDefaults().grab(true,true).applyTo(table);
		
		natSelectionProvider = builder.getSelectionProvider();
		natSelectionProvider.setSelectionConfiguration(ConfigurableStructuredSelection.NO_PARENT);
		selectionProvider.setSelectionProviderDelegate(natSelectionProvider);
	}

	@SuppressWarnings("unchecked")
	private void showResults(final List<PlatformObject> results) {
		Display.getDefault().syncExec(() -> {
			if (!Objects.equals(previousResultType, queryModel.getType())) {
				// Fix: DNDSupport only disposes dragSource when the part is closed.
				Object ds = table.getData("DragSource");
				if (ds != null) ((DragSource)ds).dispose();
				// Get rid of the previous table.
				table.dispose();

				IQueryEditorSupport factory = QueryEditorSupportRegistry.getInstance().getFactory(queryModel.getType());
				columnAccessor = (RichColumnAccessor<PlatformObject>) factory.getColumnAccessor();
				
				createTable();
				factory.customize(table);
				table.setVisible(true);
				
				// TODO: Add drag support.
				//DNDSupport.addDragSupport(table, QueryEditor.this);
			}
			previousResultType = queryModel.getType();
			try {
				eventList.getReadWriteLock().readLock().lock();
				eventList.clear();
				eventList.addAll(results);
			} finally {
				eventList.getReadWriteLock().readLock().unlock();
			}
			queryResultsSection.setExpanded(true);
		});
	}

	private void showStatistic(final int resultCount, final long queryExecutionTime, final long resultSetTime) {
		Display.getDefault().asyncExec(() -> {
			if (queryModel.isMaxResultsSet() && resultCount == QueryModel.getDefaultMaxResults()) {
				String message = String.format("Maximum number of results (%d) reached, queried in %d ms", queryModel.getMaxResults(), queryExecutionTime);
				form.getMessageManager().addMessage("warningMessage", message, null, IMessageProvider.WARNING);
			} else {
				String message = String.format("Queried %d results in %d ms", resultCount, queryExecutionTime);
				form.getMessageManager().addMessage("infoMessage", message, null, IMessageProvider.INFORMATION);
			}
		});
	}

	private void showErrorMessage(final String errorMessage) {
		Display.getDefault().asyncExec(() -> form.getMessageManager().addMessage("errorMessage", errorMessage, null, IMessageProvider.ERROR));
	}

	private void createResultTypeComboViewer(TableCombo combo) {
		resultTypeComboViewer = new TableComboViewer(combo);
		resultTypeComboViewer.setContentProvider(new ArrayContentProvider());
		resultTypeComboViewer.setLabelProvider(new LabelProvider() {
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
		resultTypeComboViewer.setInput(SearchService.getInstance().getSupportedClasses());
		resultTypeComboViewer.setComparator(new ViewerComparator());
		resultTypeComboViewer.getTableCombo().setVisibleItemCount(resultTypeComboViewer.getTableCombo().getItemCount());
		if (queryModel.getType() != null) {
			resultTypeComboViewer.setSelection(new StructuredSelection(queryModel.getType()));
		}
		resultTypeComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				previousResultType = queryModel.getType();
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				Class<? extends PlatformObject> selectedResultType = (Class<? extends PlatformObject>) selection.getFirstElement();
				queryModel.setType(selectedResultType);
				for (QueryOrderingPanel orderingPanel : orderingPanels) {
					orderingPanel.updateOrderingColumns();
				}
			}
		});
	}

	protected List<QueryOrderingPanel> getOrderingPanels() {
		return orderingPanels;
	}

	private void fillFilterToolbar(final Composite filterContainer, Composite toolbar) {
		addQueryFilterButton = formToolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		addQueryFilterButton.setImage(IconManager.getIconImage("add.png"));
		addQueryFilterButton.setToolTipText("Add Query Filter");
		addQueryFilterButton.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				addFilterPanel(new QueryFilter(), filterContainer);
			}
		});
	}

	private void fillOrderingToolbar(final Composite orderingContainer, Composite toolbar) {
		addQueryOrderingButton = formToolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		addQueryOrderingButton.setImage(IconManager.getIconImage("add.png"));
		addQueryOrderingButton.setToolTipText("Add Query Ordering");
		addQueryOrderingButton.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				addOrderingPanel(new QueryOrdering(), orderingContainer);
			}
		});
	}

	private void fillResultToolbar(Composite toolbar) {
		ImageHyperlink isGroupByButton = formToolkit.createImageHyperlink(toolbar, SWT.TRANSPARENT);
		isGroupByButton.setImage(IconManager.getIconImage("table_groupby.png"));
		isGroupByButton.setToolTipText("Set Table Row Grouping");
		isGroupByButton.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				isGroupBy = !isGroupBy;

				isGroupByButton.setImage(IconManager.getIconImage(isGroupBy ? "table.png" : "table_groupby.png"));
				isGroupByButton.setToolTipText(isGroupBy ? "Disable Table Row Grouping" : "Set Table Row Grouping");

				// Fix: DNDSupport only disposes dragSource when the part is closed.
				Object ds = table.getData("DragSource");
				if (ds != null) ((DragSource)ds).dispose();
				table.dispose();

				createTable();

				queryResultsSection.setExpanded(true);
			}
		});
	}

	private MenuManager createContextMenu() {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> fillContextMenu(manager));
		return menuMgr;
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(getResultTypeMenuName()));
	}

	private String getResultTypeMenuName() {
		return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, queryModel.getType().getSimpleName() + "Menu");
	}

	private void addFilterPanel(QueryFilter queryFilter, Composite filterPanelContainer) {
		QueryFilterPanel filterPanel = new QueryFilterPanel(filterPanelContainer, this, queryFilter);
		filterPanels.add(filterPanel);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(filterPanel);

		if (queryFilter.getId() == 0) {
			if (!queryModel.getQueryFilters().contains(queryFilter)) queryModel.addQueryFilter(queryFilter);
			form.getForm().layout();
		}
	}

	protected void removeFilterPanel(QueryFilterPanel filterPanel) {
		queryModel.removeQueryFilter(filterPanel.getQueryFilter());

		filterPanels.remove(filterPanel);
		filterPanel.dispose();
		form.getForm().layout();
	}

	private void addOrderingPanel(QueryOrdering queryOrdering, Composite orderingContainer) {
		QueryOrderingPanel orderingPanel = new QueryOrderingPanel(orderingContainer, this, queryOrdering);
		orderingPanels.add(orderingPanel);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(orderingPanel);

		if (queryOrdering.getId() == 0L) {
			if (!queryModel.getQueryOrderings().contains(queryOrdering)) queryModel.addQueryOrdering(queryOrdering);
			updateMoveButtonStates();
			form.getForm().layout();
		}
	}

	protected void removeOrderingPanel(QueryOrderingPanel orderingPanel) {
		queryModel.removeQueryOrdering(orderingPanel.getQueryOrdering());

		orderingPanels.remove(orderingPanel);
		orderingPanel.updateOrderingColumns();
		updateMoveButtonStates();
		orderingPanel.dispose();
		form.getForm().layout();
	}

	private void updateMoveButtonStates() {
		for (QueryOrderingPanel orderingPanel : orderingPanels) {
			orderingPanel.getMoveUpLink().setEnabled(orderingPanels.indexOf(orderingPanel) > 0);
			orderingPanel.getMoveDownLink().setEnabled(orderingPanels.indexOf(orderingPanel) < orderingPanels.size() - 1);
		}
	}

	protected void moveUpQueryPanel(QueryOrderingPanel orderingPanel) {
		int index = QueryOrdering.indexOfQueryOrdering(queryModel.getQueryOrderings(), orderingPanel.getQueryOrdering().getColumnName());

		if (queryModel.canMoveUpQueryOrdering(index)) {
			queryModel.moveUpQueryOrdering(orderingPanel.getQueryOrdering().getColumnName());
			orderingPanel.moveAbove(orderingPanels.get(index - 1));
			Collections.swap(orderingPanels, index, index - 1);
			orderingPanel.getParent().layout();
			updateMoveButtonStates();
		}
	}

	protected void moveDownQueryPanel(QueryOrderingPanel orderingPanel) {
		int index = QueryOrdering.indexOfQueryOrdering(queryModel.getQueryOrderings(), orderingPanel.getQueryOrdering().getColumnName());

		if (queryModel.canMoveDownQueryOrdering(index)) {
			queryModel.moveDownQueryOrdering(orderingPanel.getQueryOrdering().getColumnName());
			orderingPanel.moveBelow(orderingPanels.get(index + 1));
			Collections.swap(orderingPanels, index, index + 1);
			orderingPanel.getParent().layout();
			updateMoveButtonStates();
		}
	}

	public FormToolkit getFormToolkit() {
		return formToolkit;
	}

	@Override
	public void setFocus() {
		queryNameTxt.setFocus();
	}

	public QueryModel getQueryModel() {
		return queryModel;
	}

	private List<String> validate() {
		List<String> errorMessages = new ArrayList<>();
		if (Strings.isNullOrEmpty(queryModel.getName())) {
			errorMessages.add("Query name should be set");
		}
		if (queryModel.getType() == null) {
			errorMessages.add("Query result type should be set");
		}
		for (QueryFilterPanel queryFilterPanel : filterPanels) {
			errorMessages.addAll(queryFilterPanel.validate());
		}
		for (QueryOrderingPanel queryOrderingPanel : orderingPanels) {
			errorMessages.addAll(queryOrderingPanel.validate());
		}
		return errorMessages;
	}


	/*
	 * Save-related methods
	 * ********************
	 */

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public void markDirty() {
		this.dirty = isOwned();
		saveAction.setEnabled(isOwned());
		firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean isUpdate = isPersistent();

		if (Strings.isNullOrEmpty(queryModel.getName())) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", "Query name should be set.");
			return;
		}
		if (queryModel.getType() == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", "Query result type should be set.");
			return;
		}
		if (!isUpdate && SearchService.getInstance().getSimilarQueryCount(queryModel.getName()) > 0) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", "There is already a saved query with the same name. Please rename first.");
			return;
		}
		// it is possible that another user makes a query public with a name that you used for a private query. in this case you are not allowed to make your query public before changing its name.
		// the opposite is impossible
		if (isUpdate && originalModel.isPublicQuery() != queryModel.isPublicQuery() && SearchService.getInstance().getSimilarQueryCount(queryModel.getName()) > 1) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", "There is already a public saved query with the same name. Please rename first.");
			return;
		}

		try {
			originalModel.merge(queryModel);
			SearchService.getInstance().saveQuery(originalModel);

			dirty = false;
			Display.getDefault().asyncExec(() -> {
				saveAction.setEnabled(false);
				deleteAction.setEnabled(true);
				firePropertyChange(IEditorPart.PROP_DIRTY);
			});
			ModelEventService.getInstance().fireEvent(new ModelEvent(originalModel, isUpdate ? ModelEventType.ObjectChanged : ModelEventType.ObjectCreated, 0));
		} catch (ConstraintViolationException e) {
			StringBuilder errorMessages = new StringBuilder();
			for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
				errorMessages.append(violation.getMessage() + " for field \"" + violation.getPropertyPath() + "\".\n");
			}
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", errorMessages.toString());
		} finally {
			// Regardless of save outcome, make sure originalModel is back in clean state.
			if (isPersistent()) SearchService.getInstance().refreshQuery(originalModel);
		}
	}

	@Override
	public void doSaveAs() {
		// Save the working copy 'queryModel' as a new persistent model.
		queryModel.setName(saveAsDialog.getValue());

		if (Strings.isNullOrEmpty(queryModel.getName())) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", "Query name should be set.");
			return;
		}
		if (queryModel.getType() == null) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", "Query result type should be set.");
			return;
		}
		if (SearchService.getInstance().getSimilarQueryCount(queryModel.getName()) > 0) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save as", "There is already a saved query with the same name.");
			return;
		}

		try {
			// Reset owner.
			queryModel.setOwner(null);
			SearchService.getInstance().saveQuery(queryModel);
			EditorFactory.getInstance().openEditor(queryModel);

			Display.getDefault().asyncExec(() -> getEditorSite().getPage().closeEditor(QueryEditor.this, false));
			ModelEventService.getInstance().fireEvent(new ModelEvent(queryModel, ModelEventType.ObjectCreated, 0));
		} catch (ConstraintViolationException e) {
			StringBuilder errorMessages = new StringBuilder();
			for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
				errorMessages.append(violation.getMessage() + " for field \"" + violation.getPropertyPath() + "\".\n");
			}
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Save", errorMessages.toString());
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return isPersistent();
	}

	@Override
	public boolean isSaveOnCloseNeeded() {
		return super.isSaveOnCloseNeeded() && isPersistent();
	}

	public boolean isPersistent() {
		return originalModel.getId() != 0;
	}

	public boolean isOwned() {
		return originalModel.getOwner() == null || Objects.equals(originalModel.getOwner(), SecurityService.getInstance().getCurrentUserName());
	}

	private void doDelete() {
		SearchService.getInstance().deleteQuery(originalModel);
		Display.getDefault().asyncExec(() -> getEditorSite().getPage().closeEditor(QueryEditor.this, false));
		ModelEventService.getInstance().fireEvent(new ModelEvent(originalModel, ModelEventType.ObjectRemoved, 0));
	}

	private void createSaveAction() {
		this.saveAction = new Action("Save", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				boolean save = MessageDialog.openQuestion(
						Display.getDefault().getActiveShell(),
						"Save Query",
						"Would you like to save the query?");
				if (!save) return;

				doSave(null);
			}
		};
		saveAction.setToolTipText("Save (Ctrl+S)");
		saveAction.setImageDescriptor(IconManager.getIconDescriptor("disk.png"));
		saveAction.setEnabled(isDirty() && isOwned());
	}

	private void createSaveAsAction() {
		this.saveAsAction = new Action("Save As", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				if (saveAsDialog.open() == Window.OK) {
					doSaveAs();
				}
			}
		};
		saveAsAction.setToolTipText("Save As");
		saveAsAction.setImageDescriptor(IconManager.getIconDescriptor("disk_as.png"));
	}

	private void createDeleteAction() {
		this.deleteAction = new Action("Delete query", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				boolean save = MessageDialog.openQuestion(
						Display.getDefault().getActiveShell(),
						"Delete Query",
						"Would you like to delete the query?");
				if (!save) return;

				doDelete();
			}
		};
		deleteAction.setToolTipText("Delete");
		deleteAction.setImageDescriptor(IconManager.getIconDescriptor("delete.png"));
		deleteAction.setEnabled(isPersistent() && isOwned());
	}

	private void createExecuteAction() {
		this.executeAction = new Action("Execute query", Action.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				// Clear result table
				try {
					eventList.getReadWriteLock().readLock().lock();
					eventList.clear();
				} finally {
					eventList.getReadWriteLock().readLock().unlock();
				}

				// Validate
				form.getMessageManager().removeAllMessages();
				List<String> errorMessages = validate();
				for (int i = 0; i < errorMessages.size(); i++) {
					form.getMessageManager().addMessage("errorMessage "+ i, errorMessages.get(i), null, IMessageProvider.ERROR);
				}
				form.getToolBarManager().update(true);

				// When no errors found execute query
				if (errorMessages.isEmpty()) {
					ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell()) {
						@Override
						protected void cancelPressed() {
							try {
								PatchedEclipselinkService.getInstance().cancelCurrentStatement();
								super.cancelPressed();
							} catch (Exception e) {
								// Failed to cancel: statement is in a non-interruptible state.
							}
						}
					};
					dialog.setBlockOnOpen(false);
					try {
						dialog.run(true, true, new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								try {
									List<PlatformObject> results;
									StopWatch stopWatch1 = new StopWatch();
									StopWatch stopWatch2 = new StopWatch();

									EclipseLog.info("Executing query: " + queryModel, Activator.getDefault());

									monitor.beginTask("Executing Query", IProgressMonitor.UNKNOWN);
									stopWatch1.start();
									results = SearchService.getInstance().search(queryModel);
									stopWatch1.stop();

									monitor.subTask("Showing results");
									stopWatch2.start();
									showResults(results);
									stopWatch2.stop();

									showStatistic(results.size(), stopWatch1.getTime(), stopWatch2.getTime());
								} catch (QueryException e) {
									showErrorMessage(e.getMessage());
								} catch (Exception e) {
									showErrorMessage("Query did not complete");
									EclipseLog.error("Failed to execute query", e, Activator.getDefault());
								} finally {
									monitor.done();
								}
							}
						});
					} catch (InvocationTargetException | InterruptedException e) {
						EclipseLog.error("Exception while running query", e, Activator.getDefault());
					}
				}
			}
		};
		executeAction.setToolTipText("Execute");
		executeAction.setImageDescriptor(IconManager.getIconDescriptor("control_play.png"));
	}

	private void createDirtyAdapters() {
		dirtySelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				markDirty();
			}
		};

		dirtyKeyAdapter = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CTRL || e.keyCode == SWT.ALT || e.keyCode == SWT.SHIFT) return;
				if (e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_RIGHT) return;
				markDirty();
			}
		};

		dirtyLinkAdapter = new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				markDirty();
			}
		};
	}

	public SelectionAdapter getDirtySelectionAdapter() {
		return dirtySelectionAdapter;
	}

	public KeyAdapter getDirtyKeyAdapter() {
		return dirtyKeyAdapter;
	}

	public HyperlinkAdapter getDirtyLinkAdapter() {
		return dirtyLinkAdapter;
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

}