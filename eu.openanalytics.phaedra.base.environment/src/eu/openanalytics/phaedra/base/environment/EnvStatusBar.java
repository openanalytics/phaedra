package eu.openanalytics.phaedra.base.environment;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class EnvStatusBar extends ContributionItem {

	private Label statusLabel;
	
	@Override
	public void fill(Composite parent) {
		
		String userName = SecurityService.getInstance().getCurrentUserName();
		String envName = Screening.getEnvironment().getName();
		String statusLbl = userName + " @ " + envName;
		
		Color bgColor = new Color(null, new RGB(157, 167, 195));
		
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(5,0).applyTo(comp);
		comp.setBackground(bgColor);
		
		Label imgLabel = new Label(comp, SWT.NONE);
		imgLabel.setImage(IconManager.getIconImage("user_master.png"));
		GridDataFactory.fillDefaults().grab(false, true).applyTo(imgLabel);
		
		statusLabel = new Label(comp, SWT.NONE);
		statusLabel.setText(statusLbl);
		statusLabel.setBackground(bgColor);
		GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.CENTER).applyTo(statusLabel);
		
		GC gc = new GC(statusLabel);
		int textWidth = gc.textExtent(statusLbl).x;
		gc.dispose();
		
		StatusLineLayoutData layoutData = new StatusLineLayoutData();
		layoutData.widthHint = textWidth + 30;
		layoutData.heightHint = parent.getSize().y;
		comp.setLayoutData(layoutData);
	}
}
