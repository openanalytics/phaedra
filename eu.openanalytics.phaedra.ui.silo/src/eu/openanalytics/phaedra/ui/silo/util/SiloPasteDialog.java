package eu.openanalytics.phaedra.ui.silo.util;

import static eu.openanalytics.phaedra.ui.silo.util.SiloPasteSettings.WellType.ALL;
import static eu.openanalytics.phaedra.ui.silo.util.SiloPasteSettings.WellType.CONTROL;
import static eu.openanalytics.phaedra.ui.silo.util.SiloPasteSettings.WellType.SAMPLE;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.ui.silo.util.SiloPasteSettings.WellType;

public class SiloPasteDialog extends TitleAreaDialog {

	private SiloPasteSettings settings;

	public SiloPasteDialog(Shell parentShell, SiloPasteSettings settings) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.MODELESS | SWT.RESIZE);
		setBlockOnOpen(true);
		
		this.settings = settings;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Settings");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Silo Paste Settings");
		setMessage("Configure which data should be pasted.");
		
		Composite area = (Composite) super.createDialogArea(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);

		Group grp = new Group(container, SWT.NONE);
		grp.setText("Valid Well Types");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grp);
		GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(grp);

		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.widget instanceof Button) {
					Button wellType = (Button) e.widget;
					Object o = wellType.getData();
					if (o instanceof WellType) {
						settings.setSupportedWellType((WellType) o);
					}
				}
			}
		};

		Button wellType = new Button(grp, SWT.RADIO);
		wellType.setText("All Well Types");
		wellType.setData(ALL);
		wellType.addSelectionListener(listener);
		wellType.setSelection(settings.getSupportedWellType() == ALL);

		wellType = new Button(grp, SWT.RADIO);
		wellType.setText("Control Well Types");
		wellType.setData(CONTROL);
		wellType.addSelectionListener(listener);
		wellType.setSelection(settings.getSupportedWellType() == CONTROL);

		wellType = new Button(grp, SWT.RADIO);
		wellType.setText("Sample Well Types");
		wellType.setData(SAMPLE);
		wellType.addSelectionListener(listener);
		wellType.setSelection(settings.getSupportedWellType() == SAMPLE);

		grp = new Group(container, SWT.NONE);
		grp.setText("Other");
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grp);
		GridLayoutFactory.fillDefaults().margins(5, 5).numColumns(2).applyTo(grp);

		Label lbl = new Label(grp, SWT.NONE);
		lbl.setText("% of data to include:");

		final Spinner subset = new Spinner(grp, SWT.BORDER);
		subset.setValues(settings.getSubsetPct() != null ? settings.getSubsetPct().intValue() : 100, 0, 100, 0, 1, 10);
		subset.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Double subsetProc = null;
				if (subset.getSelection() != 100) {
					subsetProc = (double) (subset.getSelection()) / 100;
				}
				settings.setSubsetPct(subsetProc);
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
		
		// Placeholder.
		new Label(grp, SWT.NONE);
		
		Button newGroup = new Button(grp, SWT.CHECK);
		newGroup.setText("Paste as New Group");
		newGroup.setSelection(settings.isNewGroup());
		newGroup.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (e.widget instanceof Button) {
					Button newGrp = (Button) e.widget;
					settings.setNewGroup(newGrp.getSelection());
				}
			}
		});
		
		return area;
	}

}