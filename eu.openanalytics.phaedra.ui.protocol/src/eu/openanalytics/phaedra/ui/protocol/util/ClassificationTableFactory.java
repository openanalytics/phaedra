package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColorCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.calculation.ClassificationService.PatternType;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;

public class ClassificationTableFactory {

	public static TableViewer createTableViewer(Composite parent, boolean editable, SelectionListener dirtyListener) {

		final Map<String, Image> imageCache = new HashMap<>();
		final Font font = new Font(null, new FontData[] { new FontData("Consolas", 8, SWT.NONE) });
		TableViewer featureClassesViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		
		// Warning!!! Changing the order of the columns here, or adding new ones requires an update to the Comparator.
		ClassificationTableComparator comparator = new ClassificationTableComparator();
		featureClassesViewer.setComparator(comparator);
		
		Table table = featureClassesViewer.getTable();
		table.setFont(font);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.addDisposeListener(e -> {
			font.dispose();
			for (Image i: imageCache.values()) i.dispose();
		});

		int sorting = 0;
		TableViewerColumn tvc = new TableViewerColumn(featureClassesViewer, SWT.NONE);
		tvc.getColumn().setText("");
		tvc.getColumn().setWidth(0);
		tvc.getColumn().setResizable(false);
		tvc.setLabelProvider(new ColumnLabelProvider());

		tvc = new TableViewerColumn(featureClassesViewer, SWT.NONE);
		tvc.getColumn().setText("Pattern");
		tvc.getColumn().setWidth(80);
		tvc.getColumn().setToolTipText("The pattern for this class");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				FeatureClass fc = (FeatureClass) element;
				return fc.getPattern();
			}
		});
		tvc.getColumn().addSelectionListener(getSelectionAdapter(featureClassesViewer, tvc.getColumn(), comparator, sorting++));
		if (editable) tvc.setEditingSupport(new PatternEditingSupport(featureClassesViewer, dirtyListener));
		
		tvc = new TableViewerColumn(featureClassesViewer, SWT.NONE);
		tvc.getColumn().setText("Type");
		tvc.getColumn().setWidth(60);
		tvc.getColumn().setToolTipText("The pattern type for this class");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return PatternType.getType((FeatureClass) element).toString();
			}
			@Override
			public String getToolTipText(Object element) {
				return PatternType.getType((FeatureClass) element).getDescription();
			}
		});
		tvc.getColumn().addSelectionListener(getSelectionAdapter(featureClassesViewer, tvc.getColumn(), comparator, sorting++));
		if (editable) tvc.setEditingSupport(new PatternTypeEditingSupport(featureClassesViewer, dirtyListener));
		
		tvc = new TableViewerColumn(featureClassesViewer, SWT.NONE);
		tvc.getColumn().setText("Color");
		tvc.getColumn().setWidth(100);
		tvc.getColumn().setToolTipText("Color for class");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public Image getImage(Object element) {
				FeatureClass fc = (FeatureClass) element;
				Image img = imageCache.get("RGB" + fc.getRgbColor());
				if (img == null) {
					int w = 60;
					int h = 15;
					img = new Image(null, w, h);
					GC gc = new GC(img);
					RGB rgbColor = ColorUtils.hexToRgb(fc.getRgbColor());
					Color color = new Color(null, rgbColor);
					gc.setBackground(color);
					gc.fillRectangle(0,0,w,h);
					color.dispose();
					gc.dispose();
					imageCache.put("RGB" + fc.getRgbColor(), img);
				}
				return img;
			}
			@Override
			public String getText(Object element) {
				return "";
			}
		});
		tvc.getColumn().addSelectionListener(getSelectionAdapter(featureClassesViewer, tvc.getColumn(), comparator, sorting++));
		if (editable) tvc.setEditingSupport(new ColorEditingSupport(featureClassesViewer, dirtyListener));

		tvc = new TableViewerColumn(featureClassesViewer, SWT.NONE);
		tvc.getColumn().setText("Label");
		tvc.getColumn().setWidth(120);
		tvc.getColumn().setToolTipText("Label for class");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				FeatureClass fc = (FeatureClass) element;
				return fc.getLabel();
			}
		});
		tvc.getColumn().addSelectionListener(getSelectionAdapter(featureClassesViewer, tvc.getColumn(), comparator, sorting++));
		if (editable) tvc.setEditingSupport(new LabelEditingSupport(featureClassesViewer, dirtyListener));

		tvc = new TableViewerColumn(featureClassesViewer, SWT.NONE);
		tvc.getColumn().setText("Symbol");
		tvc.getColumn().setWidth(75);
		tvc.getColumn().setAlignment(SWT.CENTER);
		tvc.setLabelProvider(new OwnerDrawLabelProvider() {
			@Override
			protected void paint(Event event, Object element) {
				FeatureClass fc = (FeatureClass) element;
				String shape = fc.getSymbol();
				if (shape != null && !shape.equals("")) {
					RGB rgbColor = ColorUtils.hexToRgb(fc.getRgbColor()); 
					Color color =  new Color(PlatformUI.getWorkbench().getDisplay(), rgbColor);
					GC gc = event.gc;
					gc.setAntialias(SWT.ON);
					PlotShape ps = PlotShape.valueOf(shape);
					gc.setForeground(color);
					gc.setBackground(color);
					ps.drawShape(gc, event.x+ gc.getClipping().width/2, event.y+ event.height/2, 5, true);
					color.dispose();
				}
			}

			@Override
			protected void measure(Event event, Object element) {
				// do nothing
			}
		});
		tvc.getColumn().addSelectionListener(getSelectionAdapter(featureClassesViewer, tvc.getColumn(), comparator, sorting++));
		if (editable) tvc.setEditingSupport(new SymbolEditingSupport(featureClassesViewer, dirtyListener, imageCache));

		tvc = new TableViewerColumn(featureClassesViewer, SWT.NONE);
		tvc.getColumn().setText("Description");
		tvc.getColumn().setWidth(250);
		tvc.getColumn().setToolTipText("Description for class");
		tvc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				FeatureClass fc = (FeatureClass) element;
				return fc.getDescription();
			}
		});
		tvc.getColumn().addSelectionListener(getSelectionAdapter(featureClassesViewer, tvc.getColumn(), comparator, sorting++));
		if (editable) tvc.setEditingSupport(new DescriptionEditingSupport(featureClassesViewer, dirtyListener));

		featureClassesViewer.setContentProvider(new ArrayContentProvider());
		return featureClassesViewer;
	}

	public static IElementComparer createClassComparer() {
		// This comparer checks equality on object equality. This is needed for new FeatureClasses that have id 0.
		IElementComparer comparer = new IElementComparer() {
			@Override
			public int hashCode(Object element) {
				FeatureClass clazz = (FeatureClass)element;
				if (clazz == null) return 0;
				if (clazz.getId() == 0) return ((Object)clazz).hashCode();
				return clazz.hashCode();
			}

			@Override
			public boolean equals(Object a, Object b) {
				return a == b;
			}
		};
		return comparer;
	}

	public static Link createAddClassLink(Composite parent, Listener linkListener) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("tag_blue_add.png"));

		Link link = new Link(parent, SWT.NONE);
		link.setText("<a>Add class</a>");
		if (linkListener != null) link.addListener(SWT.Selection, linkListener);
		return link;
	}

	public static Link createRemoveClassLink(Composite parent, Listener linkListener) {
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setImage(IconManager.getIconImage("tag_blue_delete.png"));

		Link link = new Link(parent, SWT.NONE);
		link.setText("<a>Remove class</a>");
		if (linkListener != null) link.addListener(SWT.Selection, linkListener);
		return link;
	}

	private static SelectionAdapter getSelectionAdapter(final TableViewer viewer, final TableColumn column
			, final ClassificationTableComparator comparator, final int index) {

		SelectionAdapter selectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				viewer.getTable().setSortDirection(dir);
				viewer.getTable().setSortColumn(column);
				viewer.refresh();
			}
		};
		return selectionAdapter;
	}

	private static abstract class BaseEditingSupport extends EditingSupport {

		private final TableViewer viewer;
		private final SelectionListener dirtyListener;

		public BaseEditingSupport(TableViewer viewer, SelectionListener dirtyListener) {
			super(viewer);
			this.viewer = viewer;
			this.dirtyListener = dirtyListener;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (dirtyListener != null) dirtyListener.widgetSelected(null);
			viewer.update(element, null);
		}
	}

	private static class PatternEditingSupport extends BaseEditingSupport {

		public PatternEditingSupport(TableViewer viewer, SelectionListener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(((TableViewer)getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			FeatureClass fc = (FeatureClass) element;
			return fc.getPattern();
		}

		@Override
		protected void setValue(Object element, Object value) {
			FeatureClass fc = (FeatureClass) element;
			String valueToSet = String.valueOf(value);
			if (valueToSet == null || valueToSet.isEmpty()) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "Invalid value", 
						"Invalid value: pattern cannot be empty");
				return;				
			} else if (PatternType.getType(fc) == PatternType.BitMask) {
				try {
					Integer.parseInt(valueToSet.replace('.', '0'), 2);
				} catch (NumberFormatException e) {
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "Invalid value", 
							"Invalid value: " + valueToSet + "\nOnly binary patterns are allowed for pattern type BitMask");
					return;
				}
			}
			fc.setPattern(valueToSet);
			super.setValue(element, valueToSet);
		}
	}
	
	private static class PatternTypeEditingSupport extends BaseEditingSupport {

		public PatternTypeEditingSupport(TableViewer viewer, SelectionListener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			TableComboCellEditor editor = new TableComboCellEditor(((TableViewer)getViewer()).getTable());
			editor.setContentProvider(new ArrayContentProvider());
			editor.setLabelProvider(new LabelProvider());
			editor.setInput(PatternType.values());
			return editor;
		}

		@Override
		protected Object getValue(Object element) {
			FeatureClass fc = (FeatureClass) element;
			return PatternType.getType(fc);
		}

		@Override
		protected void setValue(Object element, Object value) {
			FeatureClass fc = (FeatureClass) element;
			PatternType type = (PatternType) value;
			fc.setPatternType(type.getName());
			super.setValue(element, value);
		}
	}

	private static class ColorEditingSupport extends BaseEditingSupport {

		public ColorEditingSupport(TableViewer viewer, SelectionListener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new ColorCellEditor(((TableViewer)getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			FeatureClass fc = (FeatureClass) element;
			return ColorUtils.hexToRgb(fc.getRgbColor());
		}

		@Override
		protected void setValue(Object element, Object value) {
			FeatureClass fc = (FeatureClass) element;
			int colorInt = ColorUtils.rgbToHex((RGB)value);
			fc.setRgbColor(colorInt);
			super.setValue(element, value);
		}
	}

	private static class LabelEditingSupport extends BaseEditingSupport {

		public LabelEditingSupport(TableViewer viewer, SelectionListener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(((TableViewer)getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			FeatureClass fc = (FeatureClass) element;
			return fc.getLabel();
		}

		@Override
		protected void setValue(Object element, Object value) {
			FeatureClass fc = (FeatureClass) element;
			String valueToSet = String.valueOf(value);
			fc.setLabel(valueToSet);
			super.setValue(element, valueToSet);
		}
	}

	private static class SymbolEditingSupport extends BaseEditingSupport {

		private Map<String, Image> imageCache;

		public SymbolEditingSupport(TableViewer viewer, SelectionListener dirtyListener, Map<String, Image> imageCache) {
			super(viewer, dirtyListener);
			this.imageCache = imageCache;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			TableComboCellEditor editor = new TableComboCellEditor(((TableViewer)getViewer()).getTable());

			String[] shapeNames = new String[PlotShape.values().length];
			for (PlotShape shape : PlotShape.values()) {
				shapeNames[shape.ordinal()] = shape.name();
			}

			LabelProvider labelProvider = new LabelProvider() {
				public Image getImage (Object element) {
					String symbolName = (String)element;
					Image img = imageCache.get(symbolName);
					if (img == null) {
						img = new Image(null, 15, 15);
						GC gc = new GC(img);
						gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
						PlotShape.valueOf(symbolName).drawShape(gc, 7 ,7, 5);
						gc.dispose();
						imageCache.put(symbolName, img);
					}
					return img;
				}
				public String getText (Object element) {
					return (String) element;
				}
			};

			editor.setContentProvider(new ArrayContentProvider());
			editor.setLabelProvider(labelProvider);
			editor.setInput(shapeNames);

			return editor;
		}

		@Override
		protected Object getValue(Object element) {
			FeatureClass fc = (FeatureClass) element;
			return fc.getSymbol();
		}

		@Override
		protected void setValue(Object element, Object value) {
			FeatureClass fc = (FeatureClass) element;
			String valueToSet = String.valueOf(value);
			fc.setSymbol(valueToSet);
			super.setValue(element, valueToSet);
		}	
	}

	private static class DescriptionEditingSupport extends BaseEditingSupport {

		public DescriptionEditingSupport(TableViewer viewer, SelectionListener dirtyListener) {
			super(viewer, dirtyListener);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return new TextCellEditor(((TableViewer)getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			FeatureClass fc = (FeatureClass) element;
			return fc.getDescription();
		}

		@Override
		protected void setValue(Object element, Object value) {
			FeatureClass fc = (FeatureClass) element;
			String valueToSet = String.valueOf(value);
			fc.setDescription(valueToSet);
			super.setValue(element, valueToSet);
		}
	}
}
