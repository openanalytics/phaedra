package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.imaging.overlay.ImageOverlayFactory;
import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.subwell.Activator;
import eu.openanalytics.phaedra.ui.wellimage.overlay.RegionOverlay;

public abstract class AbstractPaletteTool implements IPaletteTool {

	private IPaletteToolHost host;
	private RegionOverlay regionOverlay;
	
	private Well currentWell;
	private Map<Well, List<AbstractDrawnObject>> drawnObjects;
	
	private Button undoBtn;
	private Button resetBtn;
	private Button finishBtn;
	
	public abstract String getLabel();
	
	@Override
	public void restoreState(IMemento memento) {
		// Default: do nothing
	}
	
	@Override
	public void saveState(IMemento memento) {
		// Default: do nothing
	}
	
	@Override
	public JP2KOverlay[] configureOverlays(IPaletteToolHost host) {
		this.host = host;
		this.drawnObjects = new HashMap<Well, List<AbstractDrawnObject>>();
		this.regionOverlay = (RegionOverlay)ImageOverlayFactory.create(host.getCanvas(), "region.overlay");
		
		loadInitial(host.getInputObject());
		
		return new JP2KOverlay[] {regionOverlay};
	}
	
	@Override
	public void start() {
		host.toggleDrawMode(true);
	}
	
	@Override
	public void add(PathData path, ISelection selection) {
		if (currentWell == null) throw new IllegalStateException("Cannot add object: no well selected");
		
		AbstractDrawnObject object = generateObject(path, selection);
		
		List<AbstractDrawnObject> objects = getCurrentWellDrawnObjects();
		objects.add(object);
		regionOverlay.addRegion(object.path, object.color);
		
		host.setDirty(true);
		host.getCanvas().redraw();
	}
	
	@Override
	public void update() {
		host.getCanvas().redraw();
	}
	
	@Override
	public void cancel() {
		host.toggleDrawMode(false);
	}

	@Override
	public void finish() {
		// First, remove unmodified wells from the map
		for (Well well: drawnObjects.keySet().toArray(new Well[drawnObjects.size()])) {
			if (drawnObjects.get(well) == null || drawnObjects.get(well).isEmpty()) drawnObjects.remove(well);
		}
		if (drawnObjects.isEmpty()) return;
		
		boolean confirmed = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Confirm Save",
				"Do you want to save the changes made to " + getDrawnObjects().size() + " well(s)?");
		if (!confirmed) return;
		
		// Closing the image provider during save will result in much faster JP2K update.
		getHost().getCanvas().closeImageProvider();
		try {
			Shell shell = Display.getCurrent().getActiveShell();
			try {
				new ProgressMonitorDialog(shell).run(true, false,
					(monitor) -> {
						try {
							doSave(monitor);
						} catch (IOException e) {
							throw new InvocationTargetException(e);
						}
				});
			} catch (Exception e) {
				IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to save image ROIs", e);
				Activator.getDefault().getLog().log(status);
				ErrorDialog.openError(shell, "Error while saving", "Failed to save changes", status);
			}
		} finally {
			try { getHost().getCanvas().openImageProvider(); } catch (IOException e) {}
		}
		
		host.toggleDrawMode(false);
		host.setDirty(false);
		reset();
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Well well = SelectionUtils.getFirstObject(event.getSelection(), Well.class);
		if (currentWell == null || !currentWell.equals(well)) {
			currentWell = well;
			// Update the region overlay
			regionOverlay.clearRegions();
			List<AbstractDrawnObject> objects = getCurrentWellDrawnObjects();
			for (AbstractDrawnObject object: objects) {
				regionOverlay.addRegion(object.path, object.color);
			}
		}
	}
	
	@Override
	public void dispose() {
		// Default: do nothing.
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	protected void loadInitial(IValueObject inputObject) {
		// Default: do nothing.
	}
	
	protected abstract AbstractDrawnObject generateObject(PathData path, ISelection selection);
	
	protected abstract void doSave(IProgressMonitor monitor) throws IOException;
	
	protected IPaletteToolHost getHost() {
		return host;
	}
	
	protected Well getCurrentWell() {
		return currentWell;
	}
	
	protected Map<Well, List<AbstractDrawnObject>> getDrawnObjects() {
		return drawnObjects;
	}
	
	protected List<AbstractDrawnObject> getCurrentWellDrawnObjects() {
		if (currentWell == null) return null;
		List<AbstractDrawnObject> objects = drawnObjects.get(currentWell);
		if (objects == null) {
			objects = new ArrayList<>();
			drawnObjects.put(currentWell, objects);
		}
		return objects;
	}
	
	protected void createButtons(Composite container) {
		undoBtn = new Button(container, SWT.PUSH);
		undoBtn.setImage(IconManager.getIconImage("arrow_rotate_anticlockwise.png"));
		undoBtn.setText("Undo");
		undoBtn.addListener(SWT.Selection, e -> undo());
		GridDataFactory.fillDefaults().span(2,1).grab(true, false).applyTo(undoBtn);
		
		resetBtn = new Button(container, SWT.PUSH);
		resetBtn.setImage(IconManager.getIconImage("bin.png"));
		resetBtn.setText("Reset");
		resetBtn.addListener(SWT.Selection, e -> reset());
		GridDataFactory.fillDefaults().span(2,1).grab(true, false).applyTo(resetBtn);
		
		finishBtn = new Button(container, SWT.PUSH);
		finishBtn.setImage(IconManager.getIconImage("disk.png"));
		finishBtn.setText("Save");
		finishBtn.addListener(SWT.Selection, e -> finish());
		GridDataFactory.fillDefaults().span(2,1).grab(true, false).applyTo(finishBtn);
	}
	
	protected void undo() {
		if (currentWell == null) return;
		List<AbstractDrawnObject> objects = drawnObjects.get(currentWell);
		if (objects == null || objects.isEmpty()) return;
		AbstractDrawnObject object = objects.remove(objects.size()-1);
		regionOverlay.removeRegion(object.path);
		update();
	}
	
	protected void reset() {
		if (drawnObjects.isEmpty()) return;

		if (host.isDirty()) {
			boolean confirmed = MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Discard changes?",
					"Are you sure you want to discard all changes for all wells?");
			if (!confirmed) return;
		}
		
		drawnObjects.clear();
		regionOverlay.clearRegions();
		
		update();
	}
	
	protected abstract static class AbstractDrawnObject {
		public PathData path;
		public RGB color;
	}
}
