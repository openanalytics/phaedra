package eu.openanalytics.phaedra.ui.protocol.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import eu.openanalytics.phaedra.base.ui.util.filter.FilterMatcher;
import eu.openanalytics.phaedra.base.ui.util.table.FilteredCheckboxTable;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.calculation.annotation.AnnotationService;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.Activator;

/**
 * A dialog for creating multiple annotation features.
 * 
 * TODO Allow user to clear/reset the Values column.
 */
public class CreateAnnotationsDialog extends TitleAreaDialog {

	private CheckboxTableViewer annotationsTableViewer;
	private boolean[] selectedAnnotations;
	
	private ProtocolClass pClass;
	private List<String> names;
	private List<Boolean> numeric;
	private Map<String, List<String>> values;
	
	public CreateAnnotationsDialog(Shell parentShell, ProtocolClass pClass, List<String> names, List<Boolean> numeric, Map<String, List<String>> values) {
		super(parentShell);
		this.pClass = pClass;
		this.names = names;
		this.numeric = numeric;
		this.values = values;
		
		this.selectedAnnotations = new boolean[names.size()];
		Arrays.fill(selectedAnnotations, true);
		
		if (this.values == null) this.values = new HashMap<>();
		for (String name: names) {
			List<String> v = this.values.get(name);
			if (v == null) this.values.put(name, new ArrayList<>());
		}
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Create New Annotations");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);
		GridDataFactory.fillDefaults().applyTo(container);
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText("Protocol Class:");
		
		lbl = new Label(container, SWT.NONE);
		lbl.setText(pClass.getName());
		GridDataFactory.fillDefaults().grab(true,false).align(SWT.FILL, SWT.CENTER).applyTo(lbl);
		
		FilteredCheckboxTable table = new FilteredCheckboxTable(container, SWT.FULL_SELECTION | SWT.BORDER, new FilterMatcher(), true);
		annotationsTableViewer = table.getCheckboxViewer();
		annotationsTableViewer.getTable().setHeaderVisible(true);
		annotationsTableViewer.getTable().setLinesVisible(true);
		annotationsTableViewer.setContentProvider(new ArrayContentProvider());
		annotationsTableViewer.setCheckStateProvider(new ICheckStateProvider() {
			@Override
			public boolean isChecked(Object element) { return selectedAnnotations[names.indexOf(element)]; };
			@Override
			public boolean isGrayed(Object element) { return false; };
		});
		annotationsTableViewer.addCheckStateListener(e -> {
			selectedAnnotations[names.indexOf(e.getElement())] = e.getChecked();
		});
		createTableColumns();
		annotationsTableViewer.setInput(names);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, true).hint(SWT.DEFAULT, 150).applyTo(table);
		
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonContainer);
		
		Button btnCheckAll = new Button(buttonContainer, SWT.PUSH);
		btnCheckAll.setText("Check All");
		btnCheckAll.addListener(SWT.Selection, e -> toggleSelection(n -> true));
		
		Button btnCheckNone = new Button(buttonContainer, SWT.PUSH);
		btnCheckNone.setText("Uncheck All");
		btnCheckNone.addListener(SWT.Selection, e -> toggleSelection(n -> false));
		
		setTitle("Create New Annotations");
		setMessage("Select the annotations that should be defined in the protocol class."
				+ "\nIf the Values column is empty, any value is allowed for that annotation.");
		
		return container;
	}
	
	@Override
	protected void okPressed() {
		try {
			for (int i=0; i < names.size(); i++) {
				if (!selectedAnnotations[i]) continue;
				String name = names.get(i);
				List<FeatureClass> classes = new ArrayList<>();
				for (String value: values.get(name)) {
					classes.add(AnnotationService.getInstance().createValueClass(value));
				}
				AnnotationService.getInstance().createAnnotation(name, numeric.get(i), pClass, classes);
			}
			super.okPressed();	
		} catch (Exception e) {
			IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Error Creating Annotations", "An error occurred while creating the annotations", status);
		}
	}
	
	private void createTableColumns() {
		TableViewerColumn tvc = createColumn("Create?", 60);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}
		});
		
		tvc = createColumn("Name", 150);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return (String) element;
			}
		});
		
		tvc = createColumn("Values", 250);
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				String ann = (String) element;
				return StringUtils.createSeparatedString(values.get(ann), e -> e.toString(), ",");
			}
		});
	}
	
	private TableViewerColumn createColumn(String title, int width) {
		TableViewerColumn viewerColumn = new TableViewerColumn(annotationsTableViewer, SWT.NONE);
		TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(width);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}
	
	private void toggleSelection(Predicate<String> filter) {
		for (String name: names) {
			selectedAnnotations[names.indexOf(name)] = filter.test(name);
		}
		annotationsTableViewer.refresh();
	}
}