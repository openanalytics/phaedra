package eu.openanalytics.phaedra.ui.subwell.wellimage;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.misc.PlotShape;
import eu.openanalytics.phaedra.base.ui.util.tooltip.AdvancedToolTip;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.ClassificationService;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.FeatureClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.model.subwell.SubWellService;
import eu.openanalytics.phaedra.model.subwell.geometry.CalculatorFactory;
import eu.openanalytics.phaedra.model.subwell.util.SubWellDataChangeListener;
import eu.openanalytics.phaedra.ui.subwell.Activator;
import eu.openanalytics.phaedra.ui.subwell.SubWellClassificationSupport;
import eu.openanalytics.phaedra.ui.subwell.preferences.Prefs;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.SubWellToolTipLabelProvider;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas;

public class SubwellOverlay extends JP2KOverlay {

	private static final String SHOW_SELECTION = "SHOW_SELECTION";
	private static final String SHOW_CLASSIFICATION = "SHOW_CLASSIFICATION";
	private static final String CENTER_ON_ITEM = "CENTER_ON_ITEM";
	private static final String CURRENT_SELECTION = "CURRENT_SELECTION";

	public final static int SELECT_MODE = 1;
	public final static int ZOOM_MODE = 2;

	private Well currentWell;
	private boolean entitiesLoaded;
	private Point[] entityPositions;
	private List<Integer> currentSelection;

	private SubWellClassificationSupport classificationSupport;
	private ClassificationFilter classificationFilter;

	private SubwellSelectionListener selectionListener;
	private SubwellSelectionProvider selectionProvider;
	private IPropertyChangeListener preferenceListener;
	private IModelEventListener modelEventListener;

	private Cursor handCursor;
	private boolean ctrlPressed;
	private int currentMouseMode;
	private boolean dragging;
	private Path dragArea;

	private PathData lastDrawnPath;

	private boolean centerOnItem = false;
	private boolean showClassification = true;
	private boolean showSelection = true;

	private ToolItem centerToggleBtn;
	private ToolItem classificationToggleButton;
	private ToolItem selectionToggleButton;
	private ToolItem tooltipToggleButton;

	private SubwellTooltip tooltip;

	public SubwellOverlay() {
		super();
		handCursor = new Cursor(Display.getDefault(), SWT.CURSOR_HAND);

		currentSelection = new ArrayList<>();

		selectionListener = new SubwellSelectionListener();
		selectionProvider = new SubwellSelectionProvider();

		classificationSupport = new SubWellClassificationSupport();
		classificationFilter = new ClassificationFilter();

		SelectionUtils.triggerActiveSelection(classificationSupport);
		SelectionUtils.triggerActiveSelection(selectionListener);

		preferenceListener = e -> {
			if (e.getProperty().startsWith("CLASSIFICATION")) getCanvas().redraw();
		};
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(preferenceListener);

		modelEventListener = new SubWellDataChangeListener() {
			@Override
			protected void handle(List<Well> affectedWells) {
				if (affectedWells.contains(currentWell)) {
					loadEntityPositions();
					Display.getDefault().asyncExec(() -> getCanvas().redraw());
				}
			}
		};
		ModelEventService.getInstance().addEventListener(modelEventListener);
	}

	@Override
	public void render(GC gc) {
		if (currentWell == null) return;
		int defaultAntialias = gc.getAntialias();
		int size = Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SYMBOL_SIZE);

		gc.setAntialias(SWT.ON);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
		gc.setLineWidth(Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SYMBOL_LINE_WIDTH));
		gc.setAlpha(Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SYMBOL_OPACITY));

		SubWellFeature feature = classificationFilter.getSelectedFeature();
		if (showClassification) drawClassificationSymbols(gc, size, feature);
		if (showSelection) drawSelection(gc, size, feature);

		if (dragging) {
			gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GREEN));
			gc.setAlpha(50);
			gc.fillPath(dragArea);
			gc.setLineStyle(SWT.LINE_SOLID);
			gc.setAlpha(255);
			gc.drawPath(dragArea);
		}

		gc.setAntialias(defaultAntialias);
		gc.setAlpha(255);
	}

	@Override
	public MouseListener getMouseListener() {
		return new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button != 1) return;
				if (!(currentMouseMode == SELECT_MODE)) {
					dragArea = null;
					return;
				}
				dragging = true;
				dragArea = new Path(null);
				dragArea.moveTo(e.x, e.y);
			}
			@Override
			public void mouseUp(MouseEvent e) {
				if (e.button != 1) return;
				dragging = false;
				if (!(currentMouseMode == SELECT_MODE)) return;
				boolean areaSelect = ( dragArea != null && dragArea.getPathData().points.length >4);
				if (!ctrlPressed) currentSelection.clear();
				GC gc = new GC(getCanvas());
				for (int i=0; i<getEntityCount(); i++) {
					if (areaSelect) {
						Point pos = translate(getEntityPosition(i));
						if (dragArea.contains(pos.x, pos.y,gc, false)) currentSelection.add(i);
					} else {
						if (isNear(e.x, e.y, i)) {
							if (currentSelection.contains(i)) currentSelection.remove((Integer)i);
							else currentSelection.add(i);
							getCanvas().redraw();
							break;
						}
					}
				}

				// Convert and save the last drawn path.
				PathData pathData = dragArea.getPathData();
				Point[] points = SWTUtils.getPoints(pathData);
				for (int i=0; i<points.length; i++) {
					points[i] = getImageCoords(points[i]);
				}
				Path p = SWTUtils.createPath(points);
				lastDrawnPath = p.getPathData();
				p.dispose();

				dragArea = null;
				gc.dispose();
				getCanvas().redraw();
				selectionProvider.fireSelection();
			}
		};
	}

	@Override
	public MouseMoveListener getMouseMoveListener() {
		return new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (!(currentMouseMode == SELECT_MODE)) return;
				if (getCanvas() == null) return;

				if (dragging) {
					dragArea.lineTo(e.x, e.y);
					getCanvas().redraw();
				} else {
					boolean nearPoint = false;
					for (int i=0; i<getEntityCount(); i++) {
						if (isNear(e.x, e.y, i)) {
							nearPoint = true;
							break;
						}
					}
					getCanvas().setCursor(nearPoint ? handCursor : null);
				}
			}
		};
	}

	@Override
	public KeyListener getKeyListener() {
		return new KeyListener() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.CTRL)
					ctrlPressed = false;
			}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CTRL)
					ctrlPressed = true;
			}
		};
	}

	@Override
	public ISelectionListener[] getSelectionListeners() {
		return new ISelectionListener[] { selectionListener, classificationSupport };
	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return selectionProvider;
	}

	@Override
	public void setCurrentMouseMode(int currentMouseMode) {
		this.currentMouseMode = currentMouseMode;
	}

	@Override
	public void dispose() {
		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceListener);
		ModelEventService.getInstance().removeEventListener(modelEventListener);
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb.getActiveWorkbenchWindow() != null && wb.getActiveWorkbenchWindow().getActivePage() != null) {
			if (selectionListener != null) wb.getActiveWorkbenchWindow().getActivePage().removeSelectionListener(selectionListener);
			if (classificationSupport != null) wb.getActiveWorkbenchWindow().getActivePage().removeSelectionListener(classificationSupport);
		}
		handCursor.dispose();
	}

	@Override
	public void createButtons(ToolBar parent) {

		centerToggleBtn = new ToolItem(parent, SWT.CHECK);
		centerToggleBtn.setImage(IconManager.getIconImage("arrow_in.png"));
		centerToggleBtn.setToolTipText("Auto-center on selected items");
		centerToggleBtn.setSelection(centerOnItem);
		centerToggleBtn.addListener(SWT.Selection, e -> {
			centerOnItem = !centerOnItem;
			if (centerOnItem) SelectionUtils.triggerActiveSelection(selectionListener);
		});

		classificationToggleButton = new ToolItem(parent, SWT.CHECK);
		classificationToggleButton.setImage(IconManager.getIconImage("symbols.png"));
		classificationToggleButton.setToolTipText("Toggle classification icons on/off");
		classificationToggleButton.setSelection(showClassification);
		classificationToggleButton.addListener(SWT.Selection, e -> {
			showClassification = !showClassification;
			classificationToggleButton.setSelection(showClassification);
			getCanvas().redraw();
		});

		selectionToggleButton = new ToolItem(parent, SWT.CHECK);
		selectionToggleButton.setImage(IconManager.getIconImage("wand.png"));
		selectionToggleButton.setToolTipText("Toggle selection icons on/off");
		selectionToggleButton.setSelection(showSelection);
		selectionToggleButton.addListener(SWT.Selection, e -> {
			showSelection = !showSelection;
			selectionToggleButton.setSelection(showSelection);
			getCanvas().redraw();
		});

		tooltipToggleButton = new ToolItem(parent, SWT.CHECK);
		tooltipToggleButton.setImage(IconManager.getIconImage("image.png"));
		tooltipToggleButton.setToolTipText("Toggle image tooltips");
		tooltipToggleButton.setSelection(tooltip != null);
		tooltipToggleButton.addListener(SWT.Selection, e -> {
			if (tooltipToggleButton.getSelection()) {
				if (tooltip != null) tooltip.activate();
				else tooltip = new SubwellTooltip(getCanvas());
			} else {
				if (tooltip != null) tooltip.deactivate();
			}
		});

		classificationSupport.createToolbarButton(parent);
		classificationFilter.createToolbarButton(parent, getCanvas());
	}

	@Override
	public void createContextMenu(IMenuManager manager) {
		classificationSupport.createContextMenuItem(manager);
	}

	// Return a settings map for reporting
	@Override
	public Map<String, Object> createSettingsMap() {
		Map<String, Object> settingsMap = new HashMap<>();
		settingsMap.put(SHOW_SELECTION, showSelection);
		settingsMap.put(SHOW_CLASSIFICATION, showClassification);
		settingsMap.put(CENTER_ON_ITEM, centerOnItem);
		settingsMap.put(CURRENT_SELECTION, currentSelection);
		settingsMap.put("CLASSIFICATION_FILTER", classificationFilter.createSettingsMap());
		return settingsMap;
	}

	// Set a settings map for reporting
	@SuppressWarnings("unchecked")
	@Override
	public void applySettingsMap(Map<String, Object> settingsMap) {
		if (settingsMap == null) return;
		classificationFilter.filter(currentSelection, currentWell);
		for (Entry<String, Object> setting : settingsMap.entrySet()) {
			if (setting.getKey().equals(SHOW_SELECTION)) showSelection = (boolean) setting.getValue();
			if (setting.getKey().equals(SHOW_CLASSIFICATION)) showClassification = (boolean) setting.getValue();
			if (setting.getKey().equals(CENTER_ON_ITEM)) centerOnItem = (boolean) setting.getValue();
			if (setting.getKey().equals(CURRENT_SELECTION)) currentSelection = (List<Integer>) setting.getValue();
			if (setting.getKey().equals("CLASSIFICATION_FILTER")) classificationFilter.applySettings((Map<String, Object>) setting.getValue());
		}
		if (centerToggleBtn != null) centerToggleBtn.setSelection(centerOnItem);
		if (classificationToggleButton != null) classificationToggleButton.setSelection(showClassification);
		if (selectionToggleButton != null) selectionToggleButton.setSelection(showSelection);
	}

	public PathData getLastDrawnPath() {
		return lastDrawnPath;
	}

	public ClassificationFilter getClassificationFilter() {
		return classificationFilter;
	}

	private int getEntityRadius(int entityIndex) {
		// Use a fixed overlay for the selection and classification symbols.
		int radius = (int) (8 * getScale());
		if (radius < 4) radius = 4;
		return radius;
	}

	private boolean isNear(int x, int y, int entityIndex) {
		Point pos = translate(getEntityPosition(entityIndex));
		int radius = getEntityRadius(entityIndex);
		return (x > pos.x - radius && x < pos.x + radius && y > pos.y - radius && y < pos.y + radius);
	}

	private void drawSelection(GC gc, int size, SubWellFeature feature) {
		if (currentSelection == null || currentSelection.isEmpty()) return;

		gc.setAlpha(Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SELECTION_OPACITY));
		RGB rgb = PreferenceConverter.getColor(Activator.getDefault().getPreferenceStore(), Prefs.CLASSIFICATION_SELECTION_LINE_COLOR);
		Color selectionBorderColor = new Color(gc.getDevice(), rgb);
		boolean outerSelection = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.CLASSIFICATION_SELECTION_LINE_OUTER);
		boolean useClassShape = Activator.getDefault().getPreferenceStore().getBoolean(Prefs.CLASSIFICATION_SELECTION_SHAPE);

		gc.setLineStyle(Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SELECTION_LINE_STYLE));
		gc.setForeground(selectionBorderColor);
		gc.setLineWidth(Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SELECTION_LINE_WIDTH));

		for (int entityIndex: currentSelection) {
			PlotShape plot = PlotShape.Rectangle;
			Point pos = translate(getEntityPosition(entityIndex));
			int radius = getEntityRadius(entityIndex);

			if (feature != null) {
				FeatureClass fClass = ClassificationService.getInstance().getHighestClass(currentWell, entityIndex, feature);
				if (useClassShape && fClass != null) {
					String shape = fClass.getSymbol();
					plot = PlotShape.valueOf(shape);
				}
			}

			double distance = Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SELECTION_LINE_DISTANCE);
			if (!outerSelection) distance = -distance- Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SYMBOL_LINE_WIDTH);
			plot.drawShape(gc, pos.x, pos.y,
					((float)((radius + size) + Activator.getDefault().getPreferenceStore().getInt(Prefs.CLASSIFICATION_SYMBOL_LINE_WIDTH) + distance )), false);
		}
		selectionBorderColor.dispose();
	}

	private void drawClassificationSymbols(GC gc, int size, SubWellFeature feature) {

		List<Integer> itemsToDraw = classificationFilter.filter(currentSelection, currentWell);
		if (itemsToDraw.isEmpty()) return;

		SubWellFeature rejectionFeature = null;
		FeatureClass rejectionClass = null;
		if (classificationFilter.isShowRejected()) {
			rejectionFeature = classificationFilter.getRejectionFeature();
			rejectionClass = ClassificationService.getInstance().findRejectionClass(rejectionFeature);
		}

		for (int entityIndex : itemsToDraw) {
			PlotShape plot;

			Point pos = translate(getEntityPosition(entityIndex));
			int radius = getEntityRadius(entityIndex);
			FeatureClass fClassToDraw = null;

			if (feature != null) {
				// Find the first feature that has a classification.
				if (rejectionFeature != null) {
					FeatureClass fRejectedClass = ClassificationService.getInstance().getHighestClass(currentWell, entityIndex, rejectionFeature);
					if (fRejectedClass != null && fRejectedClass == rejectionClass) fClassToDraw = fRejectedClass;
				}
				if (fClassToDraw == null) {
					fClassToDraw = ClassificationService.getInstance().getHighestClass(currentWell, entityIndex, feature);
				}
				int c = 0;

				if (fClassToDraw != null) c = fClassToDraw.getRgbColor();

				int red = (c / 256) / 256;
				int green = (c / 256) % 256;
				int blue = c % 256;

				if (c != 0) {
					Color fgColor = new Color(gc.getDevice(), red, green, blue);
					setGCColor(gc, fgColor);
					fgColor.dispose();
				} else {
					setGCColor(gc, gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
				}

				plot = getShape(fClassToDraw);
			} else {
				setGCColor(gc, gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
				plot = PlotShape.Rectangle;
			}
			plot.drawShape(gc, pos.x, pos.y, radius + size, Activator.getDefault().getPreferenceStore().getBoolean((Prefs.CLASSIFICATION_SYMBOL_FILL)));
		}
	}

	private void setGCColor(GC gc, Color fgColor) {
		gc.setForeground(fgColor);
		gc.setBackground(fgColor);
	}

	private PlotShape getShape(FeatureClass fClassToDraw) {
		PlotShape plot;
		if (fClassToDraw != null) {
			plot = PlotShape.valueOf(fClassToDraw.getSymbol());
		} else {
			plot = PlotShape.QuestionMark;
		}
		return plot;
	}

	private int getEntityCount() {
		if (!entitiesLoaded) loadEntityPositions();
		return entityPositions.length;
	}

	private Point getEntityPosition(int entityIndex) {
		if (!entitiesLoaded) loadEntityPositions();
		return entityPositions[entityIndex];
	}

	private void clearEntityPositions() {
		entitiesLoaded = false;
		entityPositions = null;
	}

	private void loadEntityPositions() {
		entityPositions = new Point[0];
		entitiesLoaded = true;
		if (currentWell == null) return;

		SubWellFeature f = SubWellService.getInstance().getSampleFeature(currentWell);
		int entityCount = 0;
		Object testData = SubWellService.getInstance().getData(currentWell, f);
		if (testData != null) entityCount = (testData instanceof float[]) ? ((float[])testData).length : ((String[])testData).length;
		entityPositions = new Point[entityCount];

		for (int i=0; i<entityCount; i++) {
			int[] position = CalculatorFactory.getInstance().calculateCenter(currentWell, i);
			if (position == null) entityPositions[i] = new Point(0, 0);
			else entityPositions[i] = new Point(position[0], position[1]);
		}
	}

	private class SubwellSelectionListener implements ISelectionListener {

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			Well well = SelectionUtils.getFirstObject(selection, Well.class);
			if (well != null && !well.equals(currentWell)) {
				currentWell = well;
				clearEntityPositions();
				currentSelection.clear();
				if (getCanvas() != null) getCanvas().redraw();
			}

			List<SubWellSelection> subWellSelections = SelectionUtils.getObjects(selection, SubWellSelection.class);
			if (subWellSelections != null && !subWellSelections.isEmpty()) {
				currentSelection.clear();
				for (SubWellSelection sel: subWellSelections) {
					if (currentWell == null) {
						currentWell = sel.getWell();
						clearEntityPositions();
					}

					if (currentWell != null && currentWell.equals(sel.getWell())) {
						BitSet indices = sel.getIndices();
						for (int i = indices.nextSetBit(0); i >= 0; i = indices.nextSetBit(i+1)) {
							currentSelection.add(i);
						}
					}

					if (centerOnItem) {
						if (getEntityCount() > 0 && sel.getIndices() != null && sel.getIndices().cardinality() > 0) {
							Integer[] items = currentSelection.toArray(new Integer[currentSelection.size()]);
							if (getEntityCount() > items[0]) {
								Point loc = getEntityPosition(items[0]);
								JP2KImageCanvas c = (JP2KImageCanvas)getCanvas();
								Rectangle area = c.getClientArea();
								int x = loc.x - (int)(0.5*area.width/c.getCurrentScale());
								int y = loc.y - (int)(0.5*area.height/c.getCurrentScale());
								c.changeImageOffset(x, y);
							}
						}
					}
					if (getCanvas() != null) getCanvas().redraw();
				}
			}
		}

	}

	private class SubwellSelectionProvider extends EventManager implements ISelectionProvider {

		protected void fireSelection() {
			SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());
			for (Object listener : getListeners()) {
				((ISelectionChangedListener) listener).selectionChanged(event);
			}
		}

		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			addListenerObject(listener);
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			removeListenerObject(listener);
		}

		@Override
		public ISelection getSelection() {
			if (currentWell == null) return StructuredSelection.EMPTY;
			BitSet indices = new BitSet();
			for (int i=0; i< currentSelection.size(); i++) {
				indices.set(currentSelection.get(i));
			}
			SubWellSelection selection = new SubWellSelection(currentWell, indices);
			return new StructuredSelection(selection);
		}

		@Override
		public void setSelection(ISelection selection) {
			// Not supported.
		}
	}

	private class SubwellTooltip extends AdvancedToolTip {

		public SubwellTooltip(Canvas canvas) {
			super(canvas, ToolTip.NO_RECREATE, false);
			setPopupDelay(500);
			setShift(new Point(-3, -3));

			setDataConverter(o -> new SubWellItem(currentWell, (Integer) o));
			setLabelProvider(new SubWellToolTipLabelProvider());
		}

		@Override
		public Object getData(Event event) {
			for (int i=0; i<getEntityCount(); i++) {
				if (isNear(event.x, event.y, i)) {
					return i;
				}
			}
			return null;
		}

	}

}