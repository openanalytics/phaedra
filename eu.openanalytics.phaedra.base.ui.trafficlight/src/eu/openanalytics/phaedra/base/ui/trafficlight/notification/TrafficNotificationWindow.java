package eu.openanalytics.phaedra.base.ui.trafficlight.notification;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.trafficlight.IStatusChecker;
import eu.openanalytics.phaedra.base.ui.trafficlight.StatusManager;
import eu.openanalytics.phaedra.base.ui.trafficlight.TrafficStatus;

public class TrafficNotificationWindow extends AbstractNotificationWindow {

	private IStatusChecker checker;
	
	private Label checkerNameLbl;
	private Label statusIconLbl;
	private Label statusLbl;
	private Label messageLbl;
	private Label detailedLbl;
	private Label descriptionLbl;
	private Button pollBtn;
	
	public TrafficNotificationWindow(Display display, IStatusChecker checker) {
		super(display);
		this.checker = checker;
	}

	@Override
	protected void createContentArea(Composite parent) {

		FormToolkit toolkit = new FormToolkit(Display.getCurrent());
		
		GridLayoutFactory.fillDefaults().applyTo(parent);
		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		GridLayoutFactory.fillDefaults().margins(10,10).numColumns(2).applyTo(container);
		
		toolkit.adapt(container);
		toolkit.paintBordersFor(container);
		
		checkerNameLbl = toolkit.createLabel(container, checker.getName(), SWT.NONE);
		GridDataFactory.fillDefaults().span(2,1).applyTo(checkerNameLbl);
		
		Label lbl = toolkit.createLabel(container, "", SWT.SEPARATOR | SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().span(2,1).grab(true,false).applyTo(lbl);
		
		toolkit.createLabel(container, "Status:", SWT.NONE);
		
		Composite c = toolkit.createComposite(container);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT, 25).grab(true,false).applyTo(c);
		GridLayoutFactory.fillDefaults().numColumns(3).margins(5,0).applyTo(c);
		statusIconLbl = toolkit.createLabel(c, "", SWT.NONE);
		GridDataFactory.fillDefaults().hint(25,25).applyTo(statusIconLbl);
		statusLbl = toolkit.createLabel(c, "", SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(statusLbl);
		pollBtn = toolkit.createButton(c, "", SWT.PUSH);
		pollBtn.setToolTipText("Test Again");
		pollBtn.setImage(IconManager.getIconImage("control_play.png"));
		pollBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				StatusManager.getInstance().forcePoll(checker.getClass());
			}
		});
		
		lbl = toolkit.createLabel(container, "Message:", SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		messageLbl = toolkit.createLabel(container, "", SWT.WRAP);
		GridDataFactory.fillDefaults().hint(200, 40).applyTo(messageLbl);
	
		lbl = toolkit.createLabel(container, "Description:", SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		descriptionLbl = toolkit.createLabel(container, checker.getDescription(), SWT.WRAP);
		GridDataFactory.fillDefaults().hint(200, 60).align(SWT.BEGINNING, SWT.CENTER).applyTo(descriptionLbl);
		
		lbl = toolkit.createLabel(container, "Latest test:", SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(lbl);
		
		detailedLbl = toolkit.createLabel(container, "", SWT.WRAP);
		GridDataFactory.fillDefaults().hint(200, 80).applyTo(detailedLbl);
	
	}

	public void setStatus(TrafficStatus status) {
		statusIconLbl.setImage(IconManager.getIconImage(status.getIconPath()));
		statusLbl.setText(status.getCodeLabel());
		messageLbl.setText(status.getMessage());
		detailedLbl.setText(status.getDetailedMessage());
	}

	public IStatusChecker getChecker() {
		return checker;
	}
}
