package eu.openanalytics.phaedra.base.ui.util.misc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class SearchBar extends Composite {

	private Combo nameCombo;
	private Text valueTxt;
	private Button searchBtn;
	private Button clearBtn;
	
	private boolean showNameCombo;
	private ISearchHandler searchHandler;

	private final static Image FILTER_ICON = IconManager.getIconImage("funnel.png");
	private final static Image FILTER_REMOVE_ICON = IconManager.getIconImage("funnel_delete.png");
	
	private static final int FILTER_TIMEOUT = 500;
	private Job filterJob;
	

	public SearchBar(Composite parent, int style) {
		this(parent, style, true);
	}
	
	public SearchBar(Composite parent, int style, boolean showNameCombo) {
		super(parent, style);

		GridLayoutFactory.fillDefaults().numColumns(4).spacing(5, 5).applyTo(this);
		
		this.showNameCombo = showNameCombo;
		if (showNameCombo) {
			nameCombo = new Combo(this, SWT.READ_ONLY);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(nameCombo);
		}
		
		valueTxt = new Text(this, SWT.BORDER);
		valueTxt.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				filterJob.cancel();
				filterJob.schedule(FILTER_TIMEOUT);				
			}
		});
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(150, 16).applyTo(valueTxt);
		
		searchBtn = new Button(this, SWT.PUSH);
		searchBtn.setToolTipText("Apply this Filter");
		searchBtn.setImage(FILTER_ICON);
		searchBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (searchHandler != null) {
					String name = SearchBar.this.showNameCombo ? nameCombo.getText() : "";
					String value = valueTxt.getText();
					searchHandler.doSearch(name, value);
				}
			}
		});
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(30, SWT.DEFAULT).applyTo(searchBtn);
		
		clearBtn = new Button(this, SWT.PUSH);
		clearBtn.setToolTipText("Clear this Filter");
		clearBtn.setImage(FILTER_REMOVE_ICON);
		clearBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (searchHandler != null) {
					valueTxt.setText("");
					searchHandler.doClear();
				}
			}
		});
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).hint(30, SWT.DEFAULT).applyTo(clearBtn);
		
		this.filterJob = new Job("filtering") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (searchHandler != null) {
							String name = SearchBar.this.showNameCombo ? nameCombo.getText() : "";
							String value = valueTxt.getText();
							searchHandler.doSearch(name, value);
						}		
					}
				});
				return Status.OK_STATUS;
			}
		};
	}

	public void setNames(String[] names) {
		if (!showNameCombo) return;
		
		nameCombo.setItems(names);
		if (names.length > 0) {
			nameCombo.select(0);
		}
	}

	public void setCurrentName(String name) {
		for (int i=0; i<nameCombo.getItems().length; i++) {
			if (nameCombo.getItem(i).equalsIgnoreCase(name)) {
				nameCombo.select(i);
				break;
			}
		}
	}
	
	public void setSearchHandler(ISearchHandler handler) {
		searchHandler = handler;
	}
	
	public static interface ISearchHandler {
		public void doSearch(String name, String value);

		public void doClear();
	}
		
}
