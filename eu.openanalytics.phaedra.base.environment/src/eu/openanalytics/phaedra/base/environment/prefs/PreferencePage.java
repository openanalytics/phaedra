package eu.openanalytics.phaedra.base.environment.prefs;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import eu.openanalytics.phaedra.base.environment.Activator;
import eu.openanalytics.phaedra.base.util.process.ProcessUtils;

public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {

	private Button useAllPhysCoresBtn;
	private Button useAllLogCoresBtn;
	private Spinner threadPoolSize;
	private Spinner rPoolsize;
	private Button useParallelSubWellLoading;

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		createMTComponents(createGroup(comp, "Multithreading"));
		return comp;
	}

	private void createMTComponents(Composite parent) {
        IPreferenceStore store = getPreferenceStore();

        boolean useAllPhysCores = store.getBoolean(Prefs.USE_ALL_PHYS_CORES);
        boolean useAllLogCores = store.getBoolean(Prefs.USE_ALL_LOG_CORES);

        useAllPhysCoresBtn = new Button(parent, SWT.RADIO);
        useAllPhysCoresBtn.setText("Use all physical CPU cores:");
        useAllPhysCoresBtn.setSelection(useAllPhysCores);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(useAllPhysCoresBtn);

        Label label = new Label(parent, NONE);
		label.setText("" + ProcessUtils.getPhysicalCores());

        useAllLogCoresBtn = new Button(parent, SWT.RADIO);
        useAllLogCoresBtn.setText("Use all logical CPU cores:");
		useAllLogCoresBtn.setSelection(useAllLogCores);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(useAllLogCoresBtn);

        label = new Label(parent, NONE);
		label.setText("" + ProcessUtils.getLogicalCores());

        Button customPoolSize = new Button(parent, SWT.RADIO);
        customPoolSize.setText("Use a custom threadpool size:");
        customPoolSize.setSelection(!useAllPhysCores && !useAllLogCores);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(customPoolSize);

		threadPoolSize = new Spinner(parent, SWT.BORDER);
		threadPoolSize.setMinimum(1);
		threadPoolSize.setMaximum(Short.MAX_VALUE);
		threadPoolSize.setDigits(0);
		threadPoolSize.setIncrement(1);
		threadPoolSize.setPageIncrement(2);
		threadPoolSize.setSelection(store.getInt(Prefs.THREAD_POOL_SIZE));
		threadPoolSize.setEnabled(!useAllPhysCores && !useAllLogCores);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(threadPoolSize);

        label = new Label(parent, NONE);
		label.setText("R pool size (requires restart):");
        GridDataFactory.fillDefaults().grab(false, false).applyTo(label);

        rPoolsize = new Spinner(parent, SWT.BORDER);
        rPoolsize.setMinimum(1);
        rPoolsize.setMaximum(Short.MAX_VALUE);
        rPoolsize.setDigits(0);
        rPoolsize.setIncrement(1);
        rPoolsize.setPageIncrement(2);
		rPoolsize.setSelection(store.getInt(Prefs.R_POOL_SIZE));
        GridDataFactory.fillDefaults().grab(true, false).applyTo(rPoolsize);

        useParallelSubWellLoading = new Button(parent, SWT.CHECK);
        useParallelSubWellLoading.setText("Load subwell data in parallel");
        useParallelSubWellLoading.setSelection(store.getBoolean(Prefs.USE_PARALLEL_SUBWELL_LOADING));
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(useParallelSubWellLoading);

        SelectionListener selectionListener = new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		threadPoolSize.setEnabled(!useAllPhysCoresBtn.getSelection() && !useAllLogCoresBtn.getSelection());
        	}
		};
		useAllPhysCoresBtn.addSelectionListener(selectionListener);
		useAllLogCoresBtn.addSelectionListener(selectionListener);
		customPoolSize.addSelectionListener(selectionListener);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

    private static Group createGroup(Composite parent, String title) {
    	Group group = new Group(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults()
        	.equalWidth(true)
	        .numColumns(2)
	        .margins(5, 5)
	        .applyTo(group);
    	group.setText(title);
        GridDataFactory.fillDefaults().grab(true, false).span(2,1).applyTo(group);
    	return group;
    }

	@Override
    protected void performDefaults() {
        IPreferenceStore store = getPreferenceStore();
        useAllPhysCoresBtn.setSelection(store.getDefaultBoolean(Prefs.USE_ALL_PHYS_CORES));
        useAllLogCoresBtn.setSelection(store.getDefaultBoolean(Prefs.USE_ALL_LOG_CORES));
        threadPoolSize.setSelection(store.getDefaultInt(Prefs.THREAD_POOL_SIZE));
        rPoolsize.setSelection(store.getDefaultInt(Prefs.R_POOL_SIZE));
        useParallelSubWellLoading.setSelection(store.getDefaultBoolean(Prefs.USE_PARALLEL_SUBWELL_LOADING));
    }

	@Override
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		store.setValue(Prefs.USE_ALL_PHYS_CORES, useAllPhysCoresBtn.getSelection());
		store.setValue(Prefs.USE_ALL_LOG_CORES, useAllLogCoresBtn.getSelection());
		store.setValue(Prefs.THREAD_POOL_SIZE, threadPoolSize.getSelection());
		store.setValue(Prefs.R_POOL_SIZE, rPoolsize.getSelection());
		store.setValue(Prefs.USE_PARALLEL_SUBWELL_LOADING, useParallelSubWellLoading.getSelection());
		return true;
	}
}