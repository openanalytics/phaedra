package eu.openanalytics.phaedra.base.cache.prefs;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.cache.Activator;
import eu.openanalytics.phaedra.base.cache.CacheService;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {

	private static final int MIN_HEAP_SIZE = (int)(CacheService.CACHE_HEAP_PCT_MIN * 100);
	private static final int MAX_HEAP_SIZE = (int)(CacheService.CACHE_HEAP_PCT_MAX * 100);
	
	private Spinner maxHeapSizeTxt;
	private Label heapSizeLbl;
	private Text defaultTTLTxt;
	private Text defaultTTITxt;
	private Button diskCachingBtn;
	private Text maxDiskSizeTxt;
	private Text diskBufferSizeTxt;
	
	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);
		
		Group heapGroup = new Group(container, SWT.NONE);
		heapGroup.setText("In-Memory");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(heapGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(heapGroup);
		
		new Label(heapGroup, SWT.NONE).setText("Max size (% of heap):");
		maxHeapSizeTxt = new Spinner(heapGroup, SWT.BORDER);
		maxHeapSizeTxt.setValues(75, MIN_HEAP_SIZE, MAX_HEAP_SIZE, 0, 1, 10);
		maxHeapSizeTxt.addListener(SWT.Selection, e -> calculateHeapSize());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(maxHeapSizeTxt);
		
		new Label(heapGroup, SWT.NONE);
		heapSizeLbl = new Label(heapGroup, SWT.NONE);
		
		new Label(heapGroup, SWT.NONE).setText("Default TTL (seconds):");
		defaultTTLTxt = new Text(heapGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(defaultTTLTxt);
		
		new Label(heapGroup, SWT.NONE).setText("Default TTI (seconds):");
		defaultTTITxt = new Text(heapGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(defaultTTITxt);
		
		Group diskGroup = new Group(container, SWT.NONE);
		diskGroup.setText("Disk");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(diskGroup);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(diskGroup);
		
		diskCachingBtn = new Button(diskGroup, SWT.CHECK);
		diskCachingBtn.setText("Enable disk caching");
		diskCachingBtn.addListener(SWT.Selection, e -> toggleDiskCaching());
		GridDataFactory.fillDefaults().span(2,1).applyTo(diskCachingBtn);
		
		new Label(diskGroup, SWT.NONE).setText("Max size (MB):");
		maxDiskSizeTxt = new Text(diskGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(maxDiskSizeTxt);
		
		new Label(diskGroup, SWT.NONE).setText("Buffer size (MB):");
		diskBufferSizeTxt = new Text(diskGroup, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(diskBufferSizeTxt);
		
		new Label(container, SWT.NONE).setText("Note: a restart is required to apply changes.");
		
		initValues();
		return container;
	}

	private void initValues() {
		maxHeapSizeTxt.setSelection(Prefs.getInt(Prefs.CACHE_HEAP_PCT));
		defaultTTLTxt.setText(String.valueOf(Prefs.getInt(Prefs.DEFAULT_CACHE_TTL)));
		defaultTTITxt.setText(String.valueOf(Prefs.getInt(Prefs.DEFAULT_CACHE_TTI)));
		diskCachingBtn.setSelection(Prefs.getBoolean(Prefs.DEFAULT_CACHE_USE_DISK));
		maxDiskSizeTxt.setText(String.valueOf(Prefs.getInt(Prefs.CACHE_DISK_SIZE)));
		diskBufferSizeTxt.setText(String.valueOf(Prefs.getInt(Prefs.CACHE_DISK_BUFFER_SIZE)));
		toggleDiskCaching();
		calculateHeapSize();
	}
	
	private void toggleDiskCaching() {
		boolean enabled = diskCachingBtn.getSelection();
		maxDiskSizeTxt.setEnabled(enabled);
		diskBufferSizeTxt.setEnabled(enabled);
	}
	
	private void calculateHeapSize() {
		double pct = maxHeapSizeTxt.getSelection() / 100.0;
		long maxHeap = Runtime.getRuntime().maxMemory();
		long maxCache = (long)(maxHeap * pct);
		maxHeap = maxHeap / (1024*1024);
		maxCache = maxCache / (1024*1024);
		heapSizeLbl.setText(maxCache + " / " + maxHeap + " MB");
	}
	
	@Override
	protected void performDefaults() {
		IPreferenceStore store = getPreferenceStore();
		maxHeapSizeTxt.setSelection(store.getDefaultInt(Prefs.CACHE_HEAP_PCT));
		defaultTTLTxt.setText(String.valueOf(store.getDefaultInt(Prefs.DEFAULT_CACHE_TTL)));
		defaultTTITxt.setText(String.valueOf(store.getDefaultInt(Prefs.DEFAULT_CACHE_TTI)));
		diskCachingBtn.setSelection(store.getDefaultBoolean(Prefs.DEFAULT_CACHE_USE_DISK));
		maxDiskSizeTxt.setText(String.valueOf(store.getDefaultInt(Prefs.CACHE_DISK_SIZE)));
		diskBufferSizeTxt.setText(String.valueOf(store.getDefaultInt(Prefs.CACHE_DISK_BUFFER_SIZE)));
		toggleDiskCaching();
		calculateHeapSize();
		super.performDefaults();
	}
	
	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		store.setValue(Prefs.CACHE_HEAP_PCT, Integer.parseInt(maxHeapSizeTxt.getText()));
		store.setValue(Prefs.DEFAULT_CACHE_TTL, Integer.parseInt(defaultTTLTxt.getText()));
		store.setValue(Prefs.DEFAULT_CACHE_TTI, Integer.parseInt(defaultTTITxt.getText()));
		store.setValue(Prefs.DEFAULT_CACHE_USE_DISK, diskCachingBtn.getSelection());
		store.setValue(Prefs.CACHE_DISK_SIZE, Integer.parseInt(maxDiskSizeTxt.getText()));
		store.setValue(Prefs.CACHE_DISK_BUFFER_SIZE, Integer.parseInt(diskBufferSizeTxt.getText()));
		return true;
	}
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

}
