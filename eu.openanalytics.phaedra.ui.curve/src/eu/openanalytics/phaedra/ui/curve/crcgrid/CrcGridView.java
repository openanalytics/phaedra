package eu.openanalytics.phaedra.ui.curve.crcgrid;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;

import eu.openanalytics.phaedra.base.event.IModelEventListener;
import eu.openanalytics.phaedra.base.event.ModelEvent;
import eu.openanalytics.phaedra.base.event.ModelEventService;
import eu.openanalytics.phaedra.base.event.ModelEventType;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.base.util.convert.PDFToImageConverter;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.curve.CurveService;
import eu.openanalytics.phaedra.model.curve.vo.Curve;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.curve.cmd.EditCurve;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;


public class CrcGridView extends DecoratedView {

	private Composite imageContainer;
	private Label infoLbl;
	private Label[] curveImages;
	private Cursor handCursor;

	private Compound currentCompound;
	private Feature currentFeature;
	private int currentImageSize;
	private int currentFeatureCount;

	private ISelectionListener selectionListener;
	private IModelEventListener eventListener;
	private IUIEventListener uiEventListener;

	private final static int[] IMAGE_SIZES = {50,100,150,200,250};

	@Override
	public void createPartControl(Composite parent) {

		currentImageSize = 150;
		handCursor = new Cursor(Display.getDefault(), SWT.CURSOR_HAND);

		GridLayoutFactory.fillDefaults().margins(5,5).applyTo(parent);

		infoLbl = new Label(parent, SWT.NONE);
		infoLbl.setText("No compound selected");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(infoLbl);

		ScrolledComposite scrollingContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrollingContainer.getHorizontalBar().setIncrement(10);
		scrollingContainer.getVerticalBar().setIncrement(10);
		scrollingContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				recalcSize();
			}
		});
		GridDataFactory.fillDefaults().grab(true, true).applyTo(scrollingContainer);

		imageContainer = new Composite(scrollingContainer, SWT.NONE);
		scrollingContainer.setContent(imageContainer);

		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {

				if (part == CrcGridView.this) return;

				Compound compound = SelectionUtils.getFirstObject(selection, Compound.class);
				if (compound != null && !compound.equals(currentCompound)) {
					setCompound(compound);
				}

				Feature feature = SelectionUtils.getFirstObject(selection, Feature.class);
				setFeature(feature);
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		eventListener = new IModelEventListener() {
			@Override
			public void handleEvent(ModelEvent event) {
				if (event.type == ModelEventType.CurveFit || event.type == ModelEventType.CurveFitFailed) {
					Curve curve = (Curve)event.source;
					if (curve != null && curve.getCompounds().contains(currentCompound)) {
						// Trigger a refresh of the current compound.
						Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								setCompound(currentCompound);
							}
						});
					}
				}
			}
		};
		ModelEventService.getInstance().addEventListener(eventListener);

		uiEventListener = new IUIEventListener() {
			@Override
			public void handle(UIEvent event) {
				if (event.type == EventType.FeatureSelectionChanged) {
					Feature f = ProtocolUIService.getInstance().getCurrentFeature();
					setFeature(f);
				}
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(uiEventListener);

		addDecorator(new SelectionHandlingDecorator(selectionListener));
		initDecorators(parent);

		// Try to get an initial selection from the page.
		SelectionUtils.triggerActiveSelection(selectionListener);

		// Link specific help view based on the Context ID
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.eclipse.datatools.connectivity.ui.viewCRCGrid");
	}

	private void setFeature(Feature f) {
		if (currentCompound == null) return;
		ProtocolClass pc = PlateUtils.getProtocolClass(currentCompound.getPlate());
		if (f == null || f.equals(currentFeature) || !f.getProtocolClass().equals(pc)) return;

		currentFeature = f;
		for (Label lbl: curveImages) {
			lbl.redraw();
		}
		if (f != null && !f.equals(ProtocolUIService.getInstance().getCurrentFeature())) {
			ProtocolUIService.getInstance().setCurrentFeature(f);
		}
	}

	private void setCompound(Compound c) {

		if (curveImages != null) for (Label lbl: curveImages) {
			if (lbl == null || lbl.isDisposed()) continue;
			if (lbl.getImage() != null && !lbl.getImage().isDisposed()) lbl.getImage().dispose();
			lbl.dispose();
		}

		currentCompound = c;

		if (currentFeature != null && currentCompound != null
				&& !currentFeature.getProtocolClass().equals(PlateUtils.getProtocolClass(currentCompound.getPlate()))) {
			currentFeature = null;
		}

		if (c == null) {
			infoLbl.setText("No compound selected");
		} else {
			infoLbl.setText("Now showing: [" + c.getType() + c.getNumber() + "]");

			List<Feature> features = PlateUtils.getFeatures(currentCompound.getPlate());
			features = CollectionUtils.findAll(features, ProtocolUtils.FEATURES_WITH_CURVES);
			currentFeatureCount = features.size();

			curveImages = new Label[currentFeatureCount];
			for (int i=0; i<currentFeatureCount; i++) {
				final Feature f = features.get(i);
				curveImages[i] = new Label(imageContainer, SWT.BORDER);
				curveImages[i].setCursor(handCursor);
				curveImages[i].addMouseListener(new MouseAdapter() {
					@Override
					public void mouseUp(MouseEvent e) {
						setFeature(f);
					}
				});
				curveImages[i].addPaintListener(new PaintListener() {
					@Override
					public void paintControl(PaintEvent e) {
						if (f == currentFeature) {
							e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_DARK_BLUE));
							e.gc.setLineWidth(5);
							e.gc.drawRectangle(0, 0, currentImageSize-1, currentImageSize-1);
						}
					}
				});

				Curve curve = getCurrentCurve(f);
				if (curve == null || curve.getPlot() == null) continue;
				try {
					Image img = PDFToImageConverter.convert(curve.getPlot(), currentImageSize, currentImageSize);
					curveImages[i].setImage(img);
				} catch (IOException e) {
					e.printStackTrace();
				}

				createContextMenu(curveImages[i], f);
			}
			recalcSize();
		}

	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		ModelEventService.getInstance().removeEventListener(eventListener);
		ProtocolUIService.getInstance().removeUIEventListener(uiEventListener);
		handCursor.dispose();
	}

	@Override
	public void setFocus() {
		infoLbl.setFocus();
	}

	@Override
	protected void fillToolbar() {
		IToolBarManager mngr = getViewSite().getActionBars().getToolBarManager();
		ContributionItem item = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				ToolItem dropdown = DropdownToolItemFactory.createDropdown(parent);
				dropdown.setImage(IconManager.getIconImage("select.png"));
				dropdown.setToolTipText("Plot Size");

				for (int i = 0; i < IMAGE_SIZES.length; i++) {
					final int size = IMAGE_SIZES[i];
					String name = size + "x" + size;
					MenuItem item = DropdownToolItemFactory.createChild(dropdown, name, SWT.RADIO);
					item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event) {
							currentImageSize = size;
							if (currentCompound != null) setCompound(currentCompound);
						}
					});
					if (currentImageSize == size) item.setSelection(true);
				}
			}
		};
		mngr.add(item);

		super.fillToolbar();
	}

	/*
	 * Non-public
	 * **********
	 */

	private Curve getCurrentCurve(Feature f) {
		if (currentCompound == null) return null;
		//TODO Support grouping curves
		Well w = currentCompound.getWells().get(0);
		return CurveService.getInstance().getCurve(w, f);
	}
	
	private void recalcSize() {
		int spacing = 5;
		int imgSize = currentImageSize + (2*spacing);
		int imgPerRow = imageContainer.getParent().getSize().x / imgSize;
		if (imgPerRow < 1) imgPerRow = 1;
		int rows = currentFeatureCount/imgPerRow;
		if (currentFeatureCount%imgPerRow > 0) rows++;
		imageContainer.setSize(imgSize*imgPerRow, imgSize*rows);
		GridLayoutFactory.fillDefaults().numColumns(imgPerRow).applyTo(imageContainer);
		imageContainer.layout();
	}

	private void createContextMenu(Label lbl, final Feature f) {
		MenuManager menuMgr = new MenuManager("#Popup");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager, f);
			}
		});

		Menu contextMenu = menuMgr.createContextMenu(lbl);
		lbl.setMenu(contextMenu);
	}

	private void fillContextMenu(IMenuManager manager, final Feature f) {
		ContributionItem item = new ContributionItem() {
			@Override
			public void fill(Menu menu, int index) {
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
				menuItem.setText("Edit Curve");
				menuItem.setImage(IconManager.getIconImage("pencil.png"));
				menuItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event event) {
						if (currentCompound != null) {
							executeCommand(EditCurve.class.getName(), getCurrentCurve(f));
						}
					}
				});
			}
		};
		manager.add(item);
		manager.update(true);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void executeCommand(String id, Object data) {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		IHandlerService handlerService = (IHandlerService)part.getSite().getService(IHandlerService.class);
		Event cmdEvent = new Event();
		cmdEvent.data = data;
		try {
			handlerService.executeCommand(id, cmdEvent);
		} catch (Exception ex) {
			throw new RuntimeException("Command failed", ex);
		}
	}
}
