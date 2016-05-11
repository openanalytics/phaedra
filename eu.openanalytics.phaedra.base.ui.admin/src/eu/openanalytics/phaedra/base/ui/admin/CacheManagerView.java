package eu.openanalytics.phaedra.base.ui.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichLabelProvider;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class CacheManagerView extends ViewPart {

	private int refreshDelay = 5000;
	
	private RichTableViewer tableViewer;
	
	private Label heapSizeLbl;
	private ProgressBar heapSizeBar;
	
	private Label diskSizeLbl;
	private ProgressBar diskSizeBar;
	
	private Button clearSelectedBtn;
	private Button clearAllBtn;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);
		
		tableViewer = new RichTableViewer(parent, SWT.BORDER);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.applyColumnConfig(configureColumns());
		GridDataFactory.fillDefaults().grab(true, true).span(2,1).applyTo(tableViewer.getControl());
		
		Composite statusCmp = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(statusCmp);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5,5).applyTo(statusCmp);
		
		new Label(statusCmp, SWT.NONE).setText("Total Memory Size:");
		heapSizeLbl = new Label(statusCmp, SWT.NONE);
		heapSizeBar = new ProgressBar(statusCmp, SWT.SMOOTH | SWT.HORIZONTAL);
		heapSizeBar.setState(SWT.PAUSED);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(heapSizeBar);
		
		new Label(statusCmp, SWT.NONE).setText("Total Disk Size:");
		diskSizeLbl = new Label(statusCmp, SWT.NONE);
		diskSizeBar = new ProgressBar(statusCmp, SWT.HORIZONTAL);
		diskSizeBar.setState(SWT.PAUSED);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(diskSizeBar);
		
		Composite buttonCmp = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(buttonCmp);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(buttonCmp);

		clearSelectedBtn = new Button(buttonCmp, SWT.PUSH);
		clearSelectedBtn.setText("Clear Selected");
		clearSelectedBtn.addListener(SWT.Selection, e -> clearSelected());
		
		clearAllBtn = new Button(buttonCmp, SWT.PUSH);
		clearAllBtn.setText("Clear All");
		clearAllBtn.addListener(SWT.Selection, e -> clearAll());
		
		refreshStats();
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}
	
	private void clearSelected() {
		List<ICache> cachesToClear = SelectionUtils.getObjects(tableViewer.getSelection(), ICache.class);
		if (cachesToClear.isEmpty()) return;
		
		boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
				"Clear Selected", "Are you sure you want to clear " + cachesToClear.size() + " cache(s)?");
		if (confirmed) {
			for (ICache cache: cachesToClear) cache.clear();
			refreshStats();
		}
	}
	
	private void clearAll() {
		boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
				"Clear All", "Are you sure you want to clear all caches?");
		if (confirmed) {
			ICache[] caches = (ICache[]) tableViewer.getInput();
			for (ICache cache: caches) cache.clear();
			refreshStats();
		}
	}
	
	private void refreshStats() {
		if (tableViewer.getTable() == null || tableViewer.getTable().isDisposed()) return;
		
		String[] cacheNames = CacheService.getInstance().getAllCaches();
		Arrays.sort(cacheNames);
		ICache[] caches = new ICache[cacheNames.length];
		for (int i = 0; i < caches.length; i++) {
			caches[i] = CacheService.getInstance().getCache(cacheNames[i]);
		}
		tableViewer.setInput(caches);
		
		long totalHeapSize = Arrays.stream(caches).mapToLong(c -> c.getHeapSize()).sum() / (1024*1024);
		long totalDiskSize = Arrays.stream(caches).mapToLong(c -> c.getDiskSize()).sum() / (1024*1024);
		long maxHeapSize = CacheService.getInstance().getMaxHeapSizeBytes() / (1024*1024);
		long maxDiskSize = CacheService.getInstance().getMaxDiskSizeBytes() / (1024*1024);
		heapSizeLbl.setText(totalHeapSize + " / " + maxHeapSize + " MB");
		diskSizeLbl.setText(totalDiskSize + " / " + maxDiskSize + " MB");
		
		double heapPct = ((double)totalHeapSize)/maxHeapSize;
		heapSizeBar.setSelection((int)(heapPct*100));
		if (maxDiskSize > 0) {
			double diskPct = ((double)totalDiskSize)/maxDiskSize;
			diskSizeBar.setSelection((int)(diskPct*100));
		}
		heapSizeBar.getParent().layout();
		
		Display.getCurrent().timerExec(refreshDelay, this::refreshStats);
	}
	
	private ColumnConfiguration[] configureColumns() {
		List<ColumnConfiguration> configs = new ArrayList<>();
		ColumnConfiguration config;

		config = ColumnConfigFactory.create("Cache", "getName", ColumnDataType.String, 200);
		configs.add(config);

		config = ColumnConfigFactory.create("# Keys", ColumnDataType.Numeric, 100);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				ICache cache = (ICache) element;
				return String.valueOf(cache.getKeys().size());
			}
		});
		config.setSorter(new Comparator<ICache>() {
			@Override
			public int compare(ICache o1, ICache o2) {
				return o1.getKeys().size() - o2.getKeys().size();
			}
		});
		configs.add(config);
		
		config = ColumnConfigFactory.create("Memory Size (MB)", ColumnDataType.Numeric, 100);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				ICache cache = (ICache) element;
				return NumberUtils.round(cache.getHeapSize()/(1024.0*1024.0), 2);
			}
		});
		config.setSorter(new Comparator<ICache>() {
			@Override
			public int compare(ICache o1, ICache o2) {
				return (int)(o1.getHeapSize() - o2.getHeapSize());
			}
		});
		configs.add(config);
		
		config = ColumnConfigFactory.create("Disk Size (MB)", ColumnDataType.Numeric, 100);
		config.setLabelProvider(new RichLabelProvider(config) {
			@Override
			public String getText(Object element) {
				ICache cache = (ICache) element;
				return NumberUtils.round(cache.getDiskSize()/(1024.0*1024.0), 2);
			}
		});
		config.setSorter(new Comparator<ICache>() {
			@Override
			public int compare(ICache o1, ICache o2) {
				return (int)(o1.getDiskSize() - o2.getDiskSize());
			}
		});
		configs.add(config);
		
		return configs.toArray(new ColumnConfiguration[configs.size()]);
	}
}
