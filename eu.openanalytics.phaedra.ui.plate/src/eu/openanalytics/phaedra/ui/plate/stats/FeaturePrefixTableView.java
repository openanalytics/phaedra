package eu.openanalytics.phaedra.ui.plate.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.decorator.SettingsDecorator;

public class FeaturePrefixTableView extends DecoratedView {

	private RichTableViewer tableViewer;

	private FeaturePrefixCalculator calculator;

	private ISelectionListener selectionListener;

	@Override
	public void createPartControl(Composite parent) {

		calculator = new FeaturePrefixCalculator();

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);

		tableViewer = new RichTableViewer(container, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl());
		tableViewer.setContentProvider(new ArrayContentProvider());

		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				Well well = SelectionUtils.getFirstObject(selection, Well.class);
				if (well != null && !well.equals(calculator.getCurrentWell())) {
					setPartName("Feature Prefix Table: " + PlateUtils.getWellCoordinate(well));

					if (PlateUtils.isSameProtocolClass(well, calculator.getCurrentWell())) {
						calculator.setCurrentWell(well);
					} else {
						calculator.setCurrentWell(well);
						// Another protocol class: rebuild columns from scratch.
						tableViewer.applyColumnConfig(configureColumns());
					}
					tableViewer.setInput(calculator.getPrefixes());
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		addDecorator(new SettingsDecorator(this::getProtocol, this::getProperties, this::setProperties));
		addDecorator(new SelectionHandlingDecorator(selectionListener));
		addDecorator(new CopyableDecorator());
		initDecorators(parent, tableViewer.getTable());

		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "eu.openanalytics.phaedra.ui.help.viewFeaturePrefixTable");
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

	@Override
	protected void fillToolbar() {
		super.fillToolbar();

		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				final ToolItem manageGroupsButton = new ToolItem(parent, SWT.PUSH);
				manageGroupsButton.setImage(IconManager.getIconImage("table.png"));
				manageGroupsButton.setToolTipText("Manage Feature Groups");
				manageGroupsButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						ConfigureDialog dialog = new ConfigureDialog(Display.getDefault().getActiveShell(), calculator.getPrefixes());
						dialog.open();
						tableViewer.applyColumnConfig(configureColumns());
						tableViewer.refresh();
					}
				});
			}
		};
		mgr.add(contributionItem);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator());
		tableViewer.contributeConfigButton(manager);
	}

	private Protocol getProtocol() {
		return Optional.of(calculator.getCurrentWell()).map(w -> w.getAdapter(Protocol.class)).orElse(null);
	}
	
	private Properties getProperties() {
		Properties properties = new Properties();
		properties.addProperty("prefixes", calculator.getPrefixes());
		properties.addProperty("hiddenColumns", Arrays.stream(tableViewer.getCurrentColumnState()).mapToInt(c -> c.isHidden()?1:0).toArray());
		return properties;
	}
	
	@SuppressWarnings("unchecked")
	private void setProperties(Properties properties) {
		List<String> prefixes = (List<String>) properties.getProperty("prefixes");
		int[] hiddenColumns = (int[]) properties.getProperty("hiddenColumns");
		if (prefixes != null) {
			calculator.setPrefixes(prefixes);
			ColumnConfiguration[] cfg = configureColumns();
			if (hiddenColumns != null) {
				for (int i = 0; i < cfg.length; i++) {
					if (hiddenColumns[i] == 1) cfg[i].setHidden(true);
				}
			}
			tableViewer.applyColumnConfig(cfg);
			tableViewer.setInput(calculator.getPrefixes());
		}
	}

	private ColumnConfiguration[] configureColumns() {

		List<ColumnConfiguration> configs = new ArrayList<ColumnConfiguration>();

		ColumnConfiguration config = ColumnConfigFactory.create("", DataType.String, 100);
		RichLabelProvider labelProvider = new RichLabelProvider(config){
			@Override
			public String getText(Object element) {
				return element.toString();
			}
		};
		config.setLabelProvider(labelProvider);
		configs.add(config);

		if (calculator.getCurrentWell() != null) {
			List<String> suffixes = calculator.getSuffixes();

			// Create a column for each suffix
			for (final String suffix: suffixes) {
				config = calculator.getColumnState(suffix);
				if (config == null) {
					config = ColumnConfigFactory.create(suffix, DataType.String, 120);
				}
				labelProvider = new RichLabelProvider(config) {
					@Override
					public String getText(Object element) {
						return calculator.getValueFor((String)element, suffix);
					}
				};
				config.setLabelProvider(labelProvider);
				configs.add(config);
			}
		}

		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}

	private static class ConfigureDialog extends TitleAreaDialog {

		private RichTableViewer prefixTableViewer;
		private Button addPrefixBtn;
		private Button removePrefixBtn;

		private List<String> originalPrefixes;
		private List<String> modifiedPrefixes;

		public ConfigureDialog(Shell parentShell, List<String> prefixes) {
			super(parentShell);
			originalPrefixes = prefixes;
			modifiedPrefixes = new ArrayList<>(prefixes);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite parentContainer = (Composite)super.createDialogArea(parent);
			Composite container = new Composite(parentContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(container);

			prefixTableViewer = new RichTableViewer(container, SWT.BORDER | SWT.V_SCROLL);
			GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(prefixTableViewer.getControl());
			prefixTableViewer.setContentProvider(new ArrayContentProvider());
			prefixTableViewer.setInput(modifiedPrefixes);
			createPrefixColumns();

			addPrefixBtn = new Button(container, SWT.PUSH);
			addPrefixBtn.setText("Add");
			addPrefixBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					modifiedPrefixes.add("New prefix");
					prefixTableViewer.refresh();
				}
			});

			removePrefixBtn = new Button(container, SWT.PUSH);
			removePrefixBtn.setText("Remove");
			removePrefixBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					StructuredSelection sel = (StructuredSelection)prefixTableViewer.getSelection();
					Iterator<?> it = sel.iterator();
					while (it.hasNext()) {
						modifiedPrefixes.remove(it.next());
					}
					prefixTableViewer.refresh();
				}
			});

			setTitle("Configure Table");
			setMessage("Configure the groups to show in the table. Each group corresponds to one row.");

			return parentContainer;
		}

		@Override
		protected void okPressed() {
			originalPrefixes.clear();
			originalPrefixes.addAll(modifiedPrefixes);
			super.okPressed();
		}

		protected void createPrefixColumns() {

			TableViewerColumn tvc = new TableViewerColumn(prefixTableViewer, SWT.NONE);
			tvc.getColumn().setText("Prefix");
			tvc.getColumn().setWidth(250);
			tvc.setLabelProvider(new ColumnLabelProvider());

			EditingSupport editingSupport = new EditingSupport(prefixTableViewer) {

				@Override
				protected boolean canEdit(Object element) {
					return true;
				}

				@Override
				protected void setValue(Object element, Object value) {
					String oldValue = (String)element;
					String newValue = (String)value;
					int index = modifiedPrefixes.indexOf(oldValue);
					modifiedPrefixes.set(index, newValue);
					prefixTableViewer.refresh();
				}

				@Override
				protected CellEditor getCellEditor(Object element) {
					return new TextCellEditor(((TableViewer)getViewer()).getTable());
				}

				@Override
				protected Object getValue(Object element) {
					return element;
				}
			};
			tvc.setEditingSupport(editingSupport);
		}
	}
}
