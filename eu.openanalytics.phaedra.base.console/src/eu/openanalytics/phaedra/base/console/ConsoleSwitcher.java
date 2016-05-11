package eu.openanalytics.phaedra.base.console;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import eu.openanalytics.phaedra.base.util.CollectionUtils;

public class ConsoleSwitcher extends ContributionItem {

	private ToolItem switchButton;
	
	@Override
	public void fill(ToolBar parent, int index) {
		switchButton = new ToolItem(parent, SWT.PUSH);
		switchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggle();
			};
		});
		toggle();
	}
	
	private void toggle() {
		InteractiveConsole[] consoles = ConsoleManager.getInstance().getConsoles();
		InteractiveConsole current = (InteractiveConsole)switchButton.getData("console");
		if (current == null) {
			current = consoles[0];
		} else {
			int index = CollectionUtils.find(consoles, current) + 1;
			if (index == consoles.length) index = 0;
			current = consoles[index];
		}
		
		IConsoleManager consoleMgr = ConsolePlugin.getDefault().getConsoleManager();
		for (IConsole c: consoleMgr.getConsoles()) {
			if (c.getName().equals(current.getName())) consoleMgr.showConsoleView(c);
		}
		
		Image prevImage = switchButton.getImage();
		switchButton.setImage(current.getIcon().createImage());
		if (prevImage != null) prevImage.dispose();
		
		switchButton.setToolTipText(current.getName() + " (click to toggle)");
		switchButton.setData("console", current);
	}
}
