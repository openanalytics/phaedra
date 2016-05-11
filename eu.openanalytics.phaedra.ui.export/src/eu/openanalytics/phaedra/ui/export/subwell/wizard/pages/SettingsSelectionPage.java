package eu.openanalytics.phaedra.ui.export.subwell.wizard.pages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.export.core.subwell.ExportSettings;
import eu.openanalytics.phaedra.export.core.subwell.IExportWriter;

public class SettingsSelectionPage extends WizardPage {

	public static final String PAGE_NAME = "Export Settings";

	private ExportSettings settings;

	public SettingsSelectionPage(ExportSettings settings) {
		super(PAGE_NAME);
		setTitle(PAGE_NAME);
		setDescription("Set the Export Settings");

		this.settings = settings;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);

		Group grp = new Group(container, SWT.NONE);
		grp.setText("Select an Export Mode");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grp);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(grp);
		
		Button singlePage = new Button(grp, SWT.RADIO);
		singlePage.setText("Single Page");
		singlePage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateMode(IExportWriter.MODE_ONE_PAGE);
			}
		});
		singlePage.setSelection(settings.getExportMode() == IExportWriter.MODE_ONE_PAGE);

		Button pagePerWell = new Button(grp, SWT.RADIO);
		pagePerWell.setText("Page Per Well");
		pagePerWell.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateMode(IExportWriter.MODE_PAGE_PER_WELL);
			}
		});
		pagePerWell.setSelection(settings.getExportMode() == IExportWriter.MODE_PAGE_PER_WELL);

		grp = new Group(container, SWT.NONE);
		grp.setText("Exported Well Types");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grp);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(grp);

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.widget instanceof Button) {
					Button wellType = (Button) e.widget;
					Object o = wellType.getData();
					if (o instanceof Integer) {
						settings.setIncludedWellTypes((int) o);
					}
				}
			}
		};
		
		Button wellType = new Button(grp, SWT.RADIO);
		wellType.setText("All Well Types");
		wellType.setData(IExportWriter.WELLTYPE_ALL);
		wellType.addSelectionListener(listener);
		wellType.setSelection(settings.getIncludedWellTypes() == IExportWriter.WELLTYPE_ALL);
		
		wellType = new Button(grp, SWT.RADIO);
		wellType.setText("Control Well Types");
		wellType.setData(IExportWriter.WELLTYPE_CONTROL);
		wellType.addSelectionListener(listener);
		wellType.setSelection(settings.getIncludedWellTypes() == IExportWriter.WELLTYPE_CONTROL);
		
		wellType = new Button(grp, SWT.RADIO);
		wellType.setText("Sample Well Types");
		wellType.setData(IExportWriter.WELLTYPE_SAMPLE);
		wellType.addSelectionListener(listener);
		wellType.setSelection(settings.getIncludedWellTypes() == IExportWriter.WELLTYPE_SAMPLE);
		
		grp = new Group(container, SWT.NONE);
		grp.setText("Other");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grp);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(grp);
		
		Label lbl = new Label(grp, SWT.NONE);
		lbl.setText("% of data to include:");
		
		final Spinner subset = new Spinner(grp, SWT.BORDER);
		subset.setValues(settings.getSubsetProc() != null ? settings.getSubsetProc().intValue() : 100, 0, 100, 0, 1, 10);
		subset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Double subsetProc = null;
				if (subset.getSelection() != 100) {
					subsetProc = (double) (subset.getSelection()) / 100;
				}
				settings.setSubsetProc(subsetProc);
			}
		});
		
		Button includeRejected = new Button(grp, SWT.CHECK);
		includeRejected.setText("Include Rejected");
		includeRejected.setSelection(settings.isIncludeRejected());
		includeRejected.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.widget instanceof Button) {
					Button incRej = (Button) e.widget;
					settings.setIncludeRejected(incRej.getSelection());
				}
			}
		});
		
		checkPageComplete();
		setControl(container);
	}

	private void updateMode(int mode) {
		settings.setExportMode(mode);
		IWizardPage nextPage = getNextPage();
		if (nextPage instanceof FileLocationPage) {
			if (mode == IExportWriter.MODE_PAGE_PER_WELL) {
				((FileLocationPage) nextPage).updateFileSelectorExtensions(new String[] { "*.xlsx" });
			} else {
				((FileLocationPage) nextPage).updateFileSelectorExtensions(new String[] { "*.xlsx", "*.csv", "*.h5" });
			}
		}
	}
	
	private void checkPageComplete() {
		setPageComplete(settings.getExportMode() > 0);
	}

}