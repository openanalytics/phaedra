package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.environment.prefs.PrefUtils;
import eu.openanalytics.phaedra.base.ui.util.highlighter.HighlightListener;
import eu.openanalytics.phaedra.base.ui.util.highlighter.HighlightTimer;
import eu.openanalytics.phaedra.base.ui.util.highlighter.State;
import eu.openanalytics.phaedra.base.util.misc.SWTUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;

public class Thumbnail extends Canvas implements MouseListener, MouseMoveListener, KeyListener {

	private int padding;
	private Color paddingColor;

	// User preferences
	private int rows;
	private int cols;

	private int hScrollOffset;
	private int vScrollOffset;
	private int maxX;
	private int maxY;
	private int clientAreaWidth;
	private int clientAreaHeight;

	private ThumbnailCell[] cells;
	private IThumbnailCellRenderer cellRenderer;

	// Selection
	private boolean selectionEnabled;
	private boolean dragging;
	private boolean ctrlPressed;
	private Rectangle dragArea;

	private HighlightListener highlightListener;

	private Set<ThumbnailCell> selectedCells;
	private Color selectionBoxColor;
	private Color emptyCellColor;

	private PaintListener paintListener;
	private IThumbnailSelectionListener selectionListener;

	private List<Future<?>> runningTasks;
	private final ScheduledExecutorService scheduler;
	private final Job resizeJob;

	public Thumbnail(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED | SWT.H_SCROLL | SWT.V_SCROLL);

		this.scheduler = Executors.newScheduledThreadPool(PrefUtils.getNumberOfThreads());
		this.resizeJob = new Job("Resize") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (!isDisposed()) {
					Display.getDefault().syncExec(() -> {
						Rectangle clientArea = getClientArea();
						clientAreaWidth = clientArea.width;
						clientAreaHeight = clientArea.height;
					});
					resize(true);
				}
				return Status.OK_STATUS;
			}
		};

		this.cells = new ThumbnailCell[0];
		this.selectionEnabled = true;
		this.selectedCells = new HashSet<>();
		this.selectionBoxColor = new Color(parent.getDisplay(), 100, 100, 100);
		this.emptyCellColor = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		// TODO: Move to Preferences
		this.padding = 1;
		this.paddingColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

		this.runningTasks = new ArrayList<>();

		setBackground(paddingColor);

		getHorizontalBar().addListener(SWT.Selection, e -> scrollH());
		getVerticalBar().addListener(SWT.Selection, e -> scrollV());

		highlightListener = state -> {
			if (getSelectedCells().isEmpty()) return;
			GC gc = new GC(Thumbnail.this);
			try {
				getSelectedCells().forEach(c -> {
					c.getHighlight().setState(state);
					if (isDisposed()) return;
					Rectangle bounds = c.getBounds();
					if (bounds.x < -bounds.width || bounds.x > clientAreaWidth || bounds.y < -bounds.height || bounds.y > clientAreaHeight) {
						// Do not redraw cell highlighting for cells that are not visible.
						return;
					}
					c.getHighlight().paint(gc, bounds.x, bounds.y, bounds.width, bounds.height);
				});
			} finally {
				gc.dispose();
			}
		};
		HighlightTimer.getInstance().addListener(highlightListener);

		paintListener = e -> paint(e.gc);
		addPaintListener(paintListener);
		addListener(SWT.Resize, e -> {
			resizeJob.cancel();
			resizeJob.schedule(20);
		});
		addListener(SWT.Dispose, e -> {
			// Make sure jobs from this widget are canceled.
			JobUtils.cancelJobs(resizeJob.toString());

			HighlightTimer.getInstance().removeListener(highlightListener);
			removePaintListener(paintListener);
			selectionBoxColor.dispose();
			scheduler.shutdown();
		});

		addMouseListener(this);
		addMouseMoveListener(this);
		addKeyListener(this);
	}

	public void setInput(Object[] input) {
		cells = new ThumbnailCell[input.length];
		int index = 0;
		for (Object o : input) {
			cells[index++] = new ThumbnailCell(this, o, new Rectangle(0, 0, 0, 0));
		}
		resize(true);
	}

	public IThumbnailCellRenderer getCellRenderer() {
		return cellRenderer;
	}

	public void setCellRenderer(IThumbnailCellRenderer provider) {
		this.cellRenderer = provider;
	}

	public ThumbnailCell[] getCells() {
		return cells;
	}

	public Set<ThumbnailCell> getSelectedCells() {
		return selectedCells;
	}

	public void setSelectedCells(Set<ThumbnailCell> selectedCells) {
		this.selectedCells.forEach(c -> c.getHighlight().setState(State.Off));
		if (selectedCells != null && !selectedCells.isEmpty()) {
			this.selectedCells = selectedCells;
			this.selectedCells.forEach(c -> c.getHighlight().setState(State.On1));
		} else {
			this.selectedCells.clear();
		}
		redraw();
	}

	public boolean isSelectionEnabled() {
		return selectionEnabled;
	}

	public void setSelectionEnabled(boolean selectionEnabled) {
		this.selectionEnabled = selectionEnabled;
	}

	protected int gethScrollOffset() {
		return hScrollOffset;
	}

	protected int getvScrollOffset() {
		return vScrollOffset;
	}

	protected int getCols() {
		return cols;
	}

	protected void setCols(int cols) {
		this.cols = cols;
		// Cannot have both.
		if (cols > 0) {
			this.rows = 0;
		}
		resize(false);
	}

	protected int getRows() {
		return rows;
	}

	protected void setRows(int rows) {
		this.rows = rows;
		// Cannot have both.
		if (rows > 0) {
			this.cols = 0;
		}
		resize(false);
	}

	protected int getPadding() {
		return padding;
	}

	protected void setPadding(int padding) {
		this.padding = padding;
		resize(false);
	}

	protected Color getPaddingColor() {
		return paddingColor;
	}

	protected void setPaddingColor(Color paddingColor) {
		this.paddingColor = paddingColor;
		setBackground(paddingColor);
	}

	protected void resize(boolean newSize) {
		JobUtils.runUserJob(monitor -> resize(monitor, true), "Updating thumbnail images", cells.length, resizeJob.toString(), null);
	}

	/**
	 * Calculate the new positions for each cell.
	 * When newSize is true, retrieve new bounds.
	 * @param monitor
	 *
	 * @param newSize
	 */
	private void resize(IProgressMonitor monitor, boolean newSize) {
		if (clientAreaWidth == 0 || clientAreaHeight == 0) return;
		maxX = 0;
		maxY = 0;

		Rectangle[] newBounds = null;
		if (newSize) {
			Rectangle[] newSizes = new Rectangle[cells.length];
			IntStream.range(0, cells.length).parallel().forEach(i -> {
				if (monitor.isCanceled()) return;
				newSizes[i] = cellRenderer.getImageBounds(cells[i].getData());
				monitor.worked(1);
			});
			newBounds = newSizes;

			// Would cause all the images to display as a slideshow on the first position for the first load.
			//Arrays.stream(cells).parallel().forEach(cell -> {
			//	if (monitor.isCanceled()) return;
			//	Rectangle imageBounds = cellRenderer.getImageBounds(cell.getData());
			//	cell.setBounds(imageBounds);
			//	monitor.worked(1);
			//});
		}

		if (monitor.isCanceled()) return;

		int index = 0;
		if (rows > 0) {
			// The user specified the number of rows. Use those.
			int curX = 0;
			int curY = 0;
			int row = 0;
			for (ThumbnailCell cell : cells) {
				Rectangle bounds;
				if (newBounds != null) bounds = newBounds[index++];
				else bounds = cell.getBounds();
				if (row++ >= rows) {
					curY = 0;
					curX = maxX;
					row = 1;
				}
				cell.setBounds(curX, curY, bounds.height, bounds.width);
				curY += bounds.height + padding;
				maxX = Math.max(maxX, curX + bounds.width + padding);
				maxY = Math.max(maxY, curY);
			}
		} else if (cols > 0) {
			// The user specified the number of cols. Use those.
			int curX = 0;
			int curY = 0;
			int col = 0;
			for (ThumbnailCell cell : cells) {
				Rectangle bounds;
				if (newBounds != null) bounds = newBounds[index++];
				else bounds = cell.getBounds();
				if (col++ >= cols) {
					curY = maxY;
					curX = 0;
					col = 1;
				}
				cell.setBounds(curX, curY, bounds.height, bounds.width);
				curX += bounds.width + padding;
				maxX = Math.max(maxX, curX);
				maxY = Math.max(maxY, curY + bounds.height + padding);
			}
		} else {
			// Use default.
			int curX = 0;
			int curY = 0;

			for (ThumbnailCell cell : cells) {
				Rectangle bounds;
				if (newBounds != null) bounds = newBounds[index++];
				else bounds = cell.getBounds();
				int curYNew = curY + bounds.height;
				if (curYNew > clientAreaHeight) {
					curYNew = bounds.height;
					curY = 0;
					curX = maxX;
				}
				cell.setBounds(curX, curY, bounds.height, bounds.width);
				curY = curYNew + padding;
				maxX = Math.max(maxX, curX + bounds.width + padding);
				maxY = Math.max(maxY, curYNew);
			}
		}

		Display.getDefault().syncExec(() -> {
			if (isDisposed()) return;
			updateHScroll();
			updateVScroll();
			redraw();
		});
	}

	private void paint(GC gc) {
		cancelTasks();

		if (cells != null && cellRenderer != null) {
			// Set color for the placeholder's.
			gc.setBackground(emptyCellColor);
			for (final ThumbnailCell cell : cells) {
				// Only bother with the cells that the user can currently see.
				Rectangle bounds = cell.getBounds();
				if (bounds.x > clientAreaWidth || bounds.y > clientAreaHeight) {
					continue;
				}
				if (bounds.x + bounds.width > 0 && bounds.y + bounds.height > 0) {
					// Try to paint the Cell Image.
					if (!cell.paint(gc)) {
						// Cell Image wasn't cached thus not drawn. Create a cache task.
						Future<?> submit = scheduler.submit(() -> cell.cacheAndPaint());
						runningTasks.add(submit);
					}
				}
			}
		}

		// If the user is dragging, draw the dragging rectangle.
		if (dragging) {
			gc.setLineWidth(1);
			gc.setForeground(selectionBoxColor);
			gc.drawRectangle(dragArea);
		}
	}

	/**
	 * Clear the running tasks.
	 * This is done to make sure it does not waste time on caching images which are no longer in sight (e.g. after scrolling).
	 */
	private void cancelTasks() {
		for (Future<?> task : runningTasks) {
			task.cancel(true);
		}
		runningTasks.clear();
	}

	/*
	 * *********************************
	 * Key, mouse and selection handling
	 * *********************************
	 */

	public boolean isDragging() {
		return dragging;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (!selectionEnabled) return;

		ctrlPressed = (e.keyCode == SWT.CTRL);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!selectionEnabled) return;

		if (((e.stateMask & SWT.CTRL) == SWT.CTRL) && (e.keyCode == 'a')) {
			// Select all.
			Set<ThumbnailCell> selection = new HashSet<>(cells.length);
			for (ThumbnailCell cell : cells) {
				selection.add(cell);
			}
			setSelectedCells(selection);
			handleSelection();
		}

		if (e.keyCode == SWT.CTRL) {
			ctrlPressed = false;
		}

		if (!selectedCells.isEmpty()) {
			boolean moved = (
					e.keyCode == SWT.ARROW_LEFT || e.keyCode == SWT.ARROW_RIGHT
					|| e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN);

			if (moved) {
				ThumbnailCell cell = selectedCells.iterator().next();
				int x = cell.getBounds().x;
				int y = cell.getBounds().y;

				if (e.keyCode == SWT.ARROW_LEFT) {
					x -= cell.getBounds().width / 2;
				} else if (e.keyCode == SWT.ARROW_RIGHT) {
					x += cell.getBounds().width + cell.getBounds().width / 2;
				} else if (e.keyCode == SWT.ARROW_UP) {
					y -= cell.getBounds().height / 2;
				} else if (e.keyCode == SWT.ARROW_DOWN) {
					y += cell.getBounds().height + cell.getBounds().height / 2;
				}

				cell = getCellAt(x, y);

				if (cell != null) {
					clearSelection();
					addToSelection(cell);
					handleSelection();
				}
			}
		}
	}

	@Override
	public void mouseMove(MouseEvent e) {
		if (!selectionEnabled) return;

		if (dragging) {
			dragArea.width = e.x - dragArea.x;
			dragArea.height = e.y - dragArea.y;
			redraw();
		}
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {
		// Do nothing.
	}

	@Override
	public void mouseDown(MouseEvent e) {
		if (!selectionEnabled) return;
		dragging = true;
		dragArea = new Rectangle(e.x,e.y,0,0);
	}

	@Override
	public void mouseUp(MouseEvent e) {
		if (!selectionEnabled) return;

		dragging = false;
		if (dragArea == null) return;

		if (e.button == 3 && selectedCells.size() > 1) {
			return;
		}

		if (!ctrlPressed) clearSelection();

		ThumbnailCell cell = getCellAt(e.x, e.y);
		Point p = new Point(e.x, e.y);
		boolean areaSelect = (SWTUtils.getDistance(new Point(dragArea.x, dragArea.y), p) > 4);
		if (areaSelect) {
			SWTUtils.normalize(dragArea);
			handleAreaSelection();
		} else {
			if (ctrlPressed && selectedCells.contains(cell)) {
				selectedCells.remove(cell);
				cell.getHighlight().setState(State.Off);
			} else {
				if (cell != null) addToSelection(cell);
			}
		}

		handleSelection();
		dragArea = null;
	}

	public void hookSelectionListener(IThumbnailSelectionListener listener) {
		this.selectionListener = listener;
	}

	protected ThumbnailCell getCellAt(int x, int y) {
		for (ThumbnailCell cell : cells) {
			if (cell.contains(x, y)) {
				return cell;
			}
		}
		return null;
	}

	private void addToSelection(ThumbnailCell cell) {
		selectedCells.add(cell);
	}

	private void handleSelection() {
		this.selectedCells.forEach(c -> c.getHighlight().setState(State.On1));
		redraw();
		if (selectionListener != null) {
			selectionListener.selectionChanged();
		}
	}

	private void handleAreaSelection() {
		for (ThumbnailCell cell : cells) {
			Rectangle cellRectangle = cell.getBounds();
			if (cellRectangle.intersects(dragArea)) {
				addToSelection(cell);
			}
		}
	}

	private void clearSelection() {
		// Set the State to Off for the selection that is being cleared.
		this.selectedCells.forEach(c -> c.getHighlight().setState(State.Off));
		this.selectedCells.clear();
		// Redraw so no Selection is visible.
		redraw();
	}

	private void updateHScroll() {
		if (maxX > clientAreaWidth) {
			getHorizontalBar().setEnabled(true);
			hScrollOffset = Math.min(hScrollOffset, maxX - clientAreaWidth);
			getHorizontalBar().setValues(hScrollOffset, 0, maxX, clientAreaWidth, 50, clientAreaWidth);
		} else {
			hScrollOffset = 0;
			getHorizontalBar().setEnabled(false);
		}
	}

	private void updateVScroll() {
		if (maxY > clientAreaHeight) {
			getVerticalBar().setEnabled(true);
			vScrollOffset = Math.min(vScrollOffset, maxY - clientAreaHeight);
			getVerticalBar().setValues(vScrollOffset, 0, maxY, clientAreaHeight, 50, clientAreaHeight);
		} else {
			vScrollOffset = 0;
			getVerticalBar().setEnabled(false);
		}
	}

	private void scrollH() {
		hScrollOffset = getHorizontalBar().getSelection();
		redraw();
	}
	private void scrollV() {
		vScrollOffset = getVerticalBar().getSelection();
		redraw();
	}

}