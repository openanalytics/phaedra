package eu.openanalytics.phaedra.ui.protocol.dialog;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.util.FeatureGroupManager;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class ManageGroupsDialog extends TitleAreaDialog {

	private ProtocolClass pClass;
	private GroupType type;
	private FeatureGroup noGroup;
	private List<FeatureGroup> featureGroups;

	private List<IFeature> features;

	private TableViewer tableViewer;

	private Map<Integer, FeatureGroup> oldFeatureGroups;

	// Convenience Constructor.
	public ManageGroupsDialog(Shell parentShell, ProtocolClass pClass, GroupType type) {
		this(parentShell, pClass, type, null);
	}

	public ManageGroupsDialog(Shell parentShell, ProtocolClass pClass, GroupType type, List<IFeature> features) {
		super(parentShell);
		this.pClass = pClass;
		this.type = type;
		this.featureGroups = ProtocolService.getInstance().getCustomFeatureGroups(pClass, type);
		this.oldFeatureGroups = new HashMap<>();

		for (FeatureGroup fg : featureGroups) {
			FeatureGroup fgOld = ProtocolService.getInstance().createFeatureGroup(pClass, type, "New group");
			fgOld.setId(fg.getId());
			fgOld.setName(fg.getName());
			fgOld.setDescription(fg.getDescription());
			oldFeatureGroups.put(System.identityHashCode(fg), fgOld);
		}

		Collections.sort(featureGroups, ProtocolUtils.FEATURE_GROUP_NAME_SORTER);
		// If a feature is given, it can be set using this dialog. To be able to select no group, we add a 'None' Feature group here.
		this.features = features;
		if (features != null) {
			noGroup = ProtocolService.getInstance().getFeatureGroup(pClass, type, FeatureGroupManager.FEATURE_GROUP_NONE);
			featureGroups.add(0, noGroup);
		}
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Manage Groups");
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		// Container of the whole dialog box
		Composite area = new Composite((Composite)super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(5,5).spacing(0,0).applyTo(area);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(area);

		// Container of the main part of the dialog (Input)
		Composite main = new Composite(area, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(main);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(main);

		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Groups:");
		GridDataFactory.fillDefaults().span(2, 1).applyTo(lbl);

		tableViewer = new TableViewer(main, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
		Table table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		// Assign group on Double Click. PHAEDRA-2759
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});

		createColumns();

		tableViewer.setInput(featureGroups);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

		Composite crudArea = new Composite(main, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(crudArea);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(crudArea);

		Button btn = new Button(crudArea, SWT.PUSH);
		btn.setImage(IconManager.getIconImage("add.png"));
		btn.setToolTipText("Add a new group");
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FeatureGroup fg = ProtocolService.getInstance().createFeatureGroup(pClass, type, "New group");
				featureGroups.add(fg);
				tableViewer.refresh();
				tableViewer.reveal(fg);
				tableViewer.setSelection(new StructuredSelection(fg));
			}
		});

		btn = new Button(crudArea, SWT.NONE);
		btn.setImage(IconManager.getIconImage("delete.png"));
		btn.setToolTipText("Remove the selected group");
		btn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FeatureGroup fg = getSelection();
				if (fg != null) {
					removeFeatureGroup(fg);
				}
			}
		});

		setTitle("Manage Groups");
		setMessage("Manage the groups for the " + pClass.getName() + " Protocol Class.");

		return area;
	}

	@Override
	protected void okPressed() {
		featureGroups.remove(noGroup);
		for (int i = 0; i < pClass.getFeatureGroups().size(); i++) {
			if (pClass.getFeatureGroups().get(i).getType() == type.getType()) {
				pClass.getFeatureGroups().remove(i--);
			}
		}
		pClass.getFeatureGroups().addAll(featureGroups);
		if (features != null && !features.isEmpty()) {
			FeatureGroup fg = getSelection();
			if (fg != null) {
				if (fg == noGroup) {
					fg = null;
				}
				for (IFeature o : features) {
					if (o instanceof Feature) {
						((Feature) o).setFeatureGroup(fg);
					}
					if (o instanceof SubWellFeature) {
						((SubWellFeature) o).setFeatureGroup(fg);
					}
				}
			}
		}
		super.okPressed();
	}

	@Override
	protected void cancelPressed() {
		for (FeatureGroup fg : featureGroups) {
			if ("No Group".equals(fg.getName())) continue;
			FeatureGroup oldFg = oldFeatureGroups.get(System.identityHashCode(fg));
			if (oldFg != null) {
				fg.setName(oldFg.getName());
				fg.setDescription(oldFg.getDescription());
			}
		}
		super.cancelPressed();
	}

	private void removeFeatureGroup(FeatureGroup fg) {
		boolean hasFeature = false;
		for (Feature f: pClass.getFeatures()) {
			if (f.getFeatureGroup() == fg) hasFeature = true;
		}
		for (SubWellFeature f: pClass.getSubWellFeatures()) {
			if (f.getFeatureGroup() == fg) hasFeature = true;
		}
		if (hasFeature) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Cannot delete Feature Group",
					"Cannot delete this Feature Group, because it still contains features. Ungroup or remove the features first.");
		} else {
			featureGroups.remove(fg);
			tableViewer.refresh();
		}
	}

	private void createColumns() {
		TableViewerColumn tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Name");
		tvc.getColumn().setWidth(200);
		tvc.getColumn().setToolTipText("Name for the group.");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				FeatureGroup fg = (FeatureGroup) element;
				return fg.getName();
			}
		});
		tvc.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				FeatureGroup fg = (FeatureGroup) element;
				String valueToSet = String.valueOf(value);
				fg.setName(valueToSet);
				getViewer().update(element, null);
			}
			@Override
			protected Object getValue(Object element) {
				FeatureGroup fg = (FeatureGroup) element;
				return fg.getName();
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(((TableViewer)getViewer()).getTable());
			}
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});

		tvc = new TableViewerColumn(tableViewer, SWT.NONE);
		tvc.getColumn().setText("Description");
		tvc.getColumn().setWidth(250);
		tvc.getColumn().setToolTipText("Description for the group.");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				FeatureGroup fg = (FeatureGroup) element;
				return fg.getDescription();
			}
		});
		tvc.setEditingSupport(new EditingSupport(tableViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				FeatureGroup fg = (FeatureGroup) element;
				String valueToSet = String.valueOf(value);
				fg.setDescription(valueToSet);
				getViewer().update(element, null);
			}
			@Override
			protected Object getValue(Object element) {
				FeatureGroup fg = (FeatureGroup) element;
				if (fg.getDescription() != null) {
					return fg.getDescription();
				} else {
					return "";
				}
			}
			@Override
			protected CellEditor getCellEditor(Object element) {
				return new TextCellEditor(((TableViewer)getViewer()).getTable());
			}
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
	}

	private FeatureGroup getSelection() {
		ISelection sel = tableViewer.getSelection();
		if (sel instanceof StructuredSelection) {
			Object o = ((StructuredSelection) sel).getFirstElement();
			if (o instanceof FeatureGroup) {
				return (FeatureGroup) o;
			}
		}
		return null;
	}

}