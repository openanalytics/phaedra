package eu.openanalytics.phaedra.ui.subwell.wellimage.edit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.imaging.overlay.ImageOverlayFactory;
import eu.openanalytics.phaedra.base.imaging.overlay.JP2KOverlay;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.subwell.SubWellSelection;
import eu.openanalytics.phaedra.ui.subwell.wellimage.SubwellOverlay;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas;
import eu.openanalytics.phaedra.ui.wellimage.util.JP2KImageCanvas.JP2KImageCanvasListener;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class WellImageEditor extends EditorPart {

	private Composite thumbnailColumn;
	private TableViewer thumbnailTableViewer;

	private Composite imageComposite;
	private ImageControlPanel imageControlPanel;
	private JP2KImageCanvas imageCanvas;

	private Composite paletteComposite;
	private IPaletteTool[] palettes;
	private IPaletteTool activePalette;

	private Plate plate;
	private boolean isDirty;

	private JP2KOverlay[] overlays;

	public WellImageEditor() {
		// Do nothing.
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName("ROI Editor: " + input.getName());

		plate = ((WellImageEditorInput)input).getPlate();
		isDirty = false;
	}

	@Override
	public void createPartControl(Composite parent) {

		GridLayoutFactory.fillDefaults().numColumns(4).spacing(0,0).applyTo(parent);

		/*
		 * Left column: thumbnails.
		 * ************************
		 */

		thumbnailColumn = new Composite(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(false, true).hint(185, SWT.DEFAULT).applyTo(thumbnailColumn);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0,0).applyTo(thumbnailColumn);

		thumbnailTableViewer = new TableViewer(thumbnailColumn, SWT.VIRTUAL);
		thumbnailTableViewer.setContentProvider(new ArrayContentProvider());
		thumbnailTableViewer.setLabelProvider(new OwnerDrawLabelProvider() {
			@Override
			protected void measure(Event event, Object element) {
				event.setBounds(new Rectangle(event.x, event.y, 150 + 3, 150 + 4));
			}

			@Override
			protected void paint(Event event, Object element) {
				Image img = null;
				try {
					img = getThumbnail((Well) element);
					if (img == null) return;
					if (img != null) event.gc.drawImage(img, event.x, event.y+2);
				} catch (IOException e) {
					// Do nothing.
				} finally {
					img.dispose();
				}
			}
		});
		thumbnailTableViewer.addSelectionChangedListener(event -> {
			Well well = SelectionUtils.getFirstObject(event.getSelection(), Well.class);
			selectWell(well);
			for (IPaletteTool palette: palettes) palette.selectionChanged(event);
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(thumbnailTableViewer.getControl());

		List<Well> wells = new ArrayList<>(plate.getWells());
		Collections.sort(wells, PlateUtils.WELL_NR_SORTER);
		thumbnailTableViewer.setInput(wells);

		/*
		 * Middle column: image.
		 * *********************
		 */

		imageComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(imageComposite);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0,0).applyTo(imageComposite);

		imageControlPanel = new ImageControlPanel(imageComposite, SWT.BORDER, false, false);
		imageControlPanel.addImageControlListener(new ImageControlListener() {
			@Override
			public void componentToggled(int component, boolean state) {
				imageCanvas.changeChannels(imageControlPanel.getButtonStates());
				thumbnailTableViewer.getTable().redraw();
			}
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(imageControlPanel);

		Button toggleClassificationBtn = new Button(imageControlPanel, SWT.CHECK);
		toggleClassificationBtn.setText("Show classification symbols");

		imageCanvas = new JP2KImageCanvas(imageComposite, SWT.NONE);
		JP2KImageCanvasListener imageCanvasListener = new JP2KImageCanvasListener() {
			@Override
			public void onFileChange() {
				imageControlPanel.setImage(imageCanvas.getCurrentWell());
			}
			@Override
			public void onOffsetChange(int x, int y) {
				for (JP2KOverlay overlay: overlays) overlay.setOffset(new Point(x,y));
			}
			@Override
			public void onScaleChange(float newScale) {
				for (JP2KOverlay overlay: overlays) overlay.setScale(newScale);
			}
		};
		imageCanvas.addListener(imageCanvasListener);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(imageCanvas);

		// Initialize the overlays.
		List<JP2KOverlay> overlaysToAdd = new ArrayList<>();
		SubwellOverlay subwellOverlay = (SubwellOverlay)ImageOverlayFactory.create(imageCanvas, "subwell.overlay");
		subwellOverlay.getSelectionProvider().addSelectionChangedListener((event) -> {
			if (activePalette == null) return;
			PathData region = subwellOverlay.getLastDrawnPath();
			activePalette.add(region, event.getSelection());
		});
		overlaysToAdd.add(subwellOverlay);

		toggleClassificationBtn.addListener(SWT.Selection, e -> {
			subwellOverlay.getClassificationFilter().setShowAllClasses(toggleClassificationBtn.getSelection());
			imageCanvas.redraw();
		});

		// Initialize palettes and corresponding overlays.
		initializePalettes(overlaysToAdd);
		overlays = overlaysToAdd.toArray(new JP2KOverlay[overlaysToAdd.size()]);
		imageCanvas.setOverlays(overlays);

		Sash sash = new Sash (parent, SWT.VERTICAL);
		sash.addListener(SWT.Selection, (event) -> {
			int limit = 300;
			int newPaletteWidth = Math.max(parent.getBounds().width - event.x - sash.getBounds().width, limit);
			GridDataFactory.fillDefaults().grab(false, true).hint(newPaletteWidth, SWT.DEFAULT).applyTo(paletteComposite);
			parent.layout();
		});
		GridDataFactory.fillDefaults().grab(false, true).applyTo(sash);

		/*
		 * Right column: palette.
		 * **********************
		 */

		paletteComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).hint(300, SWT.DEFAULT).applyTo(paletteComposite);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5,5).applyTo(paletteComposite);

		TabFolder tabFolder = new TabFolder(paletteComposite, SWT.TOP);
		tabFolder.addListener(SWT.Selection, e -> {
			int selectedPaletteIndex = CollectionUtils.find(tabFolder.getItems(), e.item);
			if (selectedPaletteIndex == -1) return;
			activePalette = palettes[selectedPaletteIndex];
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tabFolder);

		for (IPaletteTool palette: palettes) {
			TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
			tabItem.setText(palette.getLabel());
			Composite paletteContainer = new Composite(tabFolder, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(1).margins(5,5).applyTo(paletteContainer);
			palette.createUI(paletteContainer);
			tabItem.setControl(paletteContainer);
		}
	}

	@Override
	public void setFocus() {
		imageCanvas.setFocus();
	}

	@Override
	public void dispose() {
		imageCanvas.dispose();
		imageControlPanel.dispose();
		for (IPaletteTool palette: palettes) palette.dispose();
		super.dispose();
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		for (IPaletteTool palette: palettes) palette.finish();
	}

	@Override
	public void doSaveAs() {
		throw new UnsupportedOperationException();
	}

	/*
	 * Non-public
	 * **********
	 */

	private void initializePalettes(List<JP2KOverlay> overlaysToAdd) {
		palettes = new IPaletteTool[2];
		palettes[0] = new DrawRegionPaletteTool();
		palettes[1] = new DrawCellsPaletteTool();

		IPaletteToolHost host = new IPaletteToolHost() {
			@Override
			public JP2KImageCanvas getCanvas() {
				return imageCanvas;
			}

			@Override
			public IValueObject getInputObject() {
				return plate;
			}

			@Override
			public boolean isDirty() {
				return isDirty;
			}

			@Override
			public void setDirty(boolean dirty) {
				if (isDirty == dirty) return;
				isDirty = dirty;
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}

			@Override
			public void toggleDrawMode(boolean enabled) {
				for (JP2KOverlay overlay: overlays) {
					overlay.setCurrentMouseMode(enabled ? SubwellOverlay.SELECT_MODE : SubwellOverlay.ZOOM_MODE);
				}
				if (enabled) {
					selectCells(new BitSet());
				}
				imageCanvas.setDraggingEnabled(!enabled);
			}
		};

		for (IPaletteTool palette: palettes) {
			// Configure the palette's overlays (e.g. RegionOverlay)
			JP2KOverlay[] overlays = palette.configureOverlays(host);
			if (overlays != null) for (JP2KOverlay o: overlays) overlaysToAdd.add(o);

			// Restore the palette's state, if any
			PaletteStateHelper.restoreState(palette);
		}

		imageCanvas.getParent().addListener(SWT.Dispose, e -> {
			for (IPaletteTool palette: palettes) {
				PaletteStateHelper.saveState(palette);
			}
		});
	}

	private void selectWell(Well well) {
		StructuredSelection selection = new StructuredSelection(well);
		for (JP2KOverlay overlay: overlays) {
			ISelectionListener[] listeners = overlay.getSelectionListeners();
			if (listeners == null) continue;
			for (ISelectionListener l: listeners) {
				l.selectionChanged(this, selection);
			}
		}
		imageCanvas.loadImage(well);
	}

	private void selectCells(BitSet cells) {
		SubWellSelection subwellSelection = new SubWellSelection(imageCanvas.getCurrentWell(), new BitSet());
		ISelection selection = new StructuredSelection(subwellSelection);
		for (JP2KOverlay overlay: overlays) {
			ISelectionListener[] listeners = overlay.getSelectionListeners();
			if (listeners == null) continue;
			for (ISelectionListener l: listeners) {
				l.selectionChanged(WellImageEditor.this, selection);
			}
		}
	}

	private Image getThumbnail(Well well) throws IOException {
		ImageData imageData = ImageRenderService.getInstance().getWellImageData(well
				, 150, 150, imageControlPanel.getButtonStates());
		Image img = null;
		if (imageData != null) {
			img = new Image(null, imageData);
			addWellLabel(img, imageData, well);
		}
		return img;
	}

	private void addWellLabel(Image image, ImageData imageData, Well well) {
		int x = 0;
		int y = 0;
		for (int i=0; i<imageData.width*imageData.height; i++) {
			x = i % imageData.width;
			y = i / imageData.height;
			if (imageData.getAlpha(x, y) > 0) break;
		}
		GC gc = new GC(image);
		gc.drawText(PlateUtils.getWellCoordinate(well), x+3, y+3);
		gc.dispose();
	}
}
