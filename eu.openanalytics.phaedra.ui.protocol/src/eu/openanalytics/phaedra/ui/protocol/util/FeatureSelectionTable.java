package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnViewerSorter;
import eu.openanalytics.phaedra.base.ui.util.misc.StringMatcher;
import eu.openanalytics.phaedra.calculation.norm.NormalizationService;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureGroup;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;

public class FeatureSelectionTable<F extends IFeature> extends Composite {

	private List<F> allFeatures;
	private List<F> filteredFeatures;
	private List<F> selectedFeatures;
	private List<F> originalSelection;

	private Set<FeatureGroup> featureGroups;

	private List<String> allFeatureNormalizations;
	private List<String> selectedNormalizations;

	private CheckboxTableViewer configTableViewer;
	private Table configTable;

	private Menu menu;

	private SelectionListener listener;

	public FeatureSelectionTable(Composite parent, int style, ProtocolClass pClass, Class<F> featureClass, List<F> selectedFeatures) {
		this(parent, style, pClass, featureClass, selectedFeatures, null);
	}

	public FeatureSelectionTable(Composite parent, int style, ProtocolClass pClass, Class<F> featureClass, List<F> selectedFeatures, List<String> selectedNormalizations) {

		super(parent, style);

		this.selectedFeatures = selectedFeatures;
		this.originalSelection = new ArrayList<>();
		this.originalSelection.addAll(selectedFeatures);
		this.allFeatures = getFeatures(pClass, featureClass);
		this.filteredFeatures = new ArrayList<>(allFeatures);
		this.featureGroups = new HashSet<>();
		this.allFeatureNormalizations = new ArrayList<>();
		this.selectedNormalizations = selectedNormalizations;

		for (int i=0; i<allFeatures.size(); i++) {
			IFeature f = allFeatures.get(i);
			FeatureGroup fg = f.getFeatureGroup();
			if (fg != null) featureGroups.add(fg);
			String norm = NormalizationService.NORMALIZATION_NONE;
			if (f instanceof Feature) norm = ((Feature)f).getNormalization();
			allFeatureNormalizations.add(norm);
		}

		fillComposite();

		addDisposeListener(e -> {
			if (menu != null && !menu.isDisposed()) menu.dispose();
		});
	}

	/**
	 * Reset the selected Features back to the default.
	 */
	public void resetSelection() {
		clearSelectedFeatures();
		for (F f : originalSelection) toggleSelectedFeature(f, true);
	}

	/**
	 * Add a SelectionListener. Adding a new listener will overwrite the previous one.
	 * This listener will be called on each button press in this composite.
	 * Please note that the event argument can be null.
	 *
	 * It can be useful in e.g. a Wizard page to see if enough Features were selected.
	 *
	 * @param listener
	 */
	public void addSelectionListener(SelectionListener listener) {
		this.listener = listener;
	}

	@SuppressWarnings("unchecked")
	private List<F> getFeatures(ProtocolClass pClass, Class<F> featureClass) {
		List<F> features = new ArrayList<>();
		if (featureClass == Feature.class) {
			for (Feature f: pClass.getFeatures()) features.add((F)f);
		} else if (featureClass == SubWellFeature.class) {
			for (SubWellFeature f: pClass.getSubWellFeatures()) features.add((F)f);
		}
		return features;
	}

	private void fillComposite() {
		final Composite compositeColumns = new Composite(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(compositeColumns);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(compositeColumns);

		final Composite compositeLeft = new Composite(compositeColumns, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(compositeLeft);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(compositeLeft);

		final Text textSearch = new Text(compositeLeft, SWT.BORDER | SWT.SEARCH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true,false).applyTo(textSearch);

		textSearch.addModifyListener(e -> {
			filteredFeatures.clear();
			String text = textSearch.getText();
			if (text != null && !text.isEmpty()) {
				String[] split = text.split(",");
				List<StringMatcher> matchers = new ArrayList<>();
				for (String s : split) {
					if (s.startsWith(" ")) s = s.substring(1);
					if (s.endsWith(" ")) s = s.substring(0, s.length()-1);

					matchers.add(new StringMatcher(s, true, false));
				}

				for (F f : allFeatures) {
					for (StringMatcher matcher : matchers) {
						if (matcher.match(f.getName()) || matcher.match(f.getDisplayName())) {
							filteredFeatures.add(f);
							break;
						}
					}
				}
				setInput(filteredFeatures);
			} else {
				filteredFeatures.addAll(allFeatures);
				setInput(allFeatures);
			}
		});

		configTableViewer = CheckboxTableViewer.newCheckList(compositeLeft, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		configTable = configTableViewer.getTable();
		configTable.setLinesVisible(true);
		configTable.setHeaderVisible(true);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).hint(SWT.DEFAULT, 200).applyTo(configTable);

		Composite compositeRight = new Composite(compositeColumns, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(compositeRight);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(compositeRight);

		final Button buttonSearch = new Button(compositeRight, SWT.NONE);
		buttonSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonSearch.setText("Clear search");
		buttonSearch.addListener(SWT.Selection, e -> {
			textSearch.setText("");
			filteredFeatures.clear();
			filteredFeatures.addAll(allFeatures);
			setInput(allFeatures);
		});

		final Button buttonAll = new Button(compositeRight, SWT.NONE);
		buttonAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonAll.setText("Select All");
		buttonAll.setToolTipText("Select everything");
		buttonAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (F f : filteredFeatures) {
					toggleSelectedFeature(f, true);
					configTableViewer.setChecked(f, true);
				}
				if (listener != null) listener.widgetSelected(e);
			}
		});

		final Button buttonNone = new Button(compositeRight, SWT.NONE);
		buttonNone.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonNone.setText("Select None");
		buttonNone.setToolTipText("Select nothing");
		buttonNone.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (F f : filteredFeatures) {
					toggleSelectedFeature(f, false);
					configTableViewer.setChecked(f, false);
				}
				if (listener != null) listener.widgetSelected(e);
			}
		});

		final Button buttonKey = new Button(compositeRight, SWT.NONE);
		buttonKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonKey.setText("Select Key");
		buttonKey.setToolTipText("Select key features");
		buttonKey.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (F f : filteredFeatures) {
					toggleSelectedFeature(f, f.isKey());
					configTableViewer.setChecked(f, f.isKey());
				}
				if (listener != null) listener.widgetSelected(e);
			}
		});

		final Button buttonNum = new Button(compositeRight, SWT.NONE);
		buttonNum.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonNum.setText("Select Numeric");
		buttonNum.setToolTipText("Select numeric features");
		buttonNum.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (F f : filteredFeatures) {
					toggleSelectedFeature(f, f.isNumeric());
					configTableViewer.setChecked(f, f.isNumeric());
				}
				if (listener != null) listener.widgetSelected(e);
			}
		});

		final Button buttonGroups = new Button(compositeRight, SWT.NONE);
		buttonGroups.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		buttonGroups.setText("Select Group");
		buttonGroups.setToolTipText("Select features of a specific group");
		buttonGroups.addListener(SWT.Selection, e -> {
			Menu menu = getMenu(buttonGroups);
			Rectangle bounds = buttonGroups.getBounds();
			Point point = buttonGroups.toDisplay(bounds.x, bounds.height);
			menu.setLocation(point);
			menu.setVisible(true);
		});

		loadColumns();

		configTableViewer.addCheckStateListener(event -> {
			@SuppressWarnings("unchecked")
			F feature = (F) event.getElement();
			toggleSelectedFeature(feature, event.getChecked());
			if (listener != null) listener.widgetSelected(null);
		});
	}

	private void loadColumns() {
		TableViewerColumn column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setWidth(200);
		column.getColumn().setText("Name");
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IFeature) element).getName();
			}
		});
		Comparator<IFeature> sorter = (IFeature o1, IFeature o2) -> {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
		};
		new ColumnViewerSorter<IFeature>(column, sorter);

		column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setWidth(100);
		column.getColumn().setText("Alias");
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((IFeature) element).getShortName();
			}
		});
		sorter = (IFeature o1, IFeature o2) -> {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			String sn1 = o1.getShortName();
			String sn2 = o2.getShortName();
			String s1 = sn1 != null ? sn1 : "";
			String s2 = sn2 != null ? sn2 : "";
			return s1.toLowerCase().compareTo(s2.toLowerCase());
		};
		new ColumnViewerSorter<IFeature>(column, sorter);

		if (selectedNormalizations != null) {
			column = new TableViewerColumn(configTableViewer, SWT.BORDER);
			column.getColumn().setWidth(150);
			column.getColumn().setText("Normalization");
			column.getColumn().setMoveable(false);
			column.getColumn().setResizable(true);
			column.getColumn().setAlignment(SWT.LEFT);
			column.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					int index = allFeatures.indexOf(element);
					return allFeatureNormalizations.get(index);
				}
			});
			NormalizationEditingSupport editingSupport = new NormalizationEditingSupport(configTableViewer);
			column.setEditingSupport(editingSupport);
		}

		column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setWidth(150);
		column.getColumn().setText("Group");
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				FeatureGroup fg = ((IFeature) element).getFeatureGroup();
				if (fg != null) {
					return fg.getName();
				} else {
					return "";
				}
			}
		});
		sorter = (IFeature o1, IFeature o2) -> {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			FeatureGroup fg1 = o1.getFeatureGroup();
			FeatureGroup fg2 = o2.getFeatureGroup();
			String g1 = fg1 == null ? "" : fg1.getName();
			String g2 = fg2 == null ? "" : fg2.getName();
			return g1.toLowerCase().compareTo(g2.toLowerCase());
		};
		new ColumnViewerSorter<IFeature>(column, sorter);

		column = new TableViewerColumn(configTableViewer, SWT.BORDER);
		column.getColumn().setWidth(100);
		column.getColumn().setText("Current Pos.");
		column.getColumn().setMoveable(false);
		column.getColumn().setResizable(true);
		column.getColumn().setAlignment(SWT.LEFT);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IFeature f = ((IFeature) element);
				int index = originalSelection.indexOf(f) + 1;
				if (index > 0) {
					return index + "";
				} else {
					return "";
				}
			}
		});
		sorter = (IFeature o1, IFeature o2) -> {
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return -1;
			String i1 = (selectedFeatures.indexOf(o1) + 1) + "";
			String i2 = (selectedFeatures.indexOf(o2) + 1) + "";
			return i1.compareTo(i2);
		};
		new ColumnViewerSorter<IFeature>(column, sorter);

		configTableViewer.setContentProvider(new ArrayContentProvider());
		setInput(filteredFeatures);
	}

	private void setInput(List<F> features) {
		configTableViewer.setInput(features);
		for (F f : features) {
			configTableViewer.setChecked(f, selectedFeatures.contains(f));
		}
	}

	private void toggleSelectedFeature(F feature, boolean add) {
		if (add) {
			if (!selectedFeatures.contains(feature)) {
				selectedFeatures.add(feature);
				int index = allFeatures.indexOf(feature);
				if (selectedNormalizations != null) selectedNormalizations.add(allFeatureNormalizations.get(index));
			}
		} else {
			int index = selectedFeatures.indexOf(feature);
			if (index < 0) return;
			selectedFeatures.remove(index);
			if (selectedNormalizations != null) selectedNormalizations.remove(index);
		}
	}

	private void clearSelectedFeatures() {
		selectedFeatures.clear();
		if (selectedNormalizations != null) selectedNormalizations.clear();
	}

	private Menu getMenu(Button buttonGroups) {
		if (menu == null) {
			menu = new Menu(getShell(), SWT.POP_UP);
			for (final FeatureGroup fg : featureGroups) {
				MenuItem item = new MenuItem(menu, SWT.CHECK);
				item.setText(fg.getName());
				item.addListener(SWT.Selection, e -> {
					for (F f : filteredFeatures) {
						if (f.getFeatureGroup() == fg) {
							MenuItem source = (MenuItem) e.widget;
							boolean selected = source.getSelection();
							toggleSelectedFeature(f, selected);
							configTableViewer.setChecked(f, selected);
						}
					}
					if (listener != null) listener.widgetSelected(null);
				});
			}

		}

		return menu;
	}

	private class NormalizationEditingSupport extends EditingSupport {

		private ComboBoxViewerCellEditor cellEditor;

		public NormalizationEditingSupport(ColumnViewer viewer) {
			super(viewer);
			cellEditor = new ComboBoxViewerCellEditor((Composite) getViewer().getControl(), SWT.READ_ONLY);
			cellEditor.setLabelProvider(new LabelProvider());
			cellEditor.setContentProvider(new ArrayContentProvider());
			cellEditor.setInput(NormalizationService.getInstance().getNormalizations());
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return cellEditor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			int index = allFeatures.indexOf(element);
			return allFeatureNormalizations.get(index);
		}

		@Override
		protected void setValue(Object element, Object value) {
			int index = allFeatures.indexOf(element);
			allFeatureNormalizations.set(index, value.toString());
			int selectionIndex = selectedFeatures.indexOf(element);
			if (selectionIndex != -1) selectedNormalizations.set(selectionIndex, value.toString());
			configTableViewer.update(element, null);
		}
	}
}