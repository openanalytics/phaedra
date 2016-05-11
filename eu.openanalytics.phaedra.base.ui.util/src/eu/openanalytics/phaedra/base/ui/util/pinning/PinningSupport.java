package eu.openanalytics.phaedra.base.ui.util.pinning;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

@Deprecated
public class PinningSupport {

	private boolean pinned;

	public PinningSupport() {
		pinned = false;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}

	public boolean isPinned() {
		return pinned;
	}

	public void createPinButton(final IPinnableView view, ToolBar parent) {
		final ToolItem pinBtn = new ToolItem(parent, SWT.CHECK);
		pinBtn.setImage(IconManager.getIconImage("pin.png"));
		pinBtn.setToolTipText(view.isPinned() ? "Click to unpin" : "Click to pin");
		pinBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (view.isPinned()) view.unpin();
				else view.pin();
				pinBtn.setToolTipText(view.isPinned() ? "Click to unpin" : "Click to pin");
			}
		});
	}

	public void createPinButton(final PinningDecorator pinningDecorator, ToolBar parent) {
		final ToolItem pinBtn = new ToolItem(parent, SWT.CHECK);
		pinBtn.setImage(IconManager.getIconImage("pin.png"));
		pinBtn.setToolTipText(pinningDecorator.isPinned() ? "Click to unpin" : "Click to pin");
		pinBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (pinningDecorator.isPinned()) pinningDecorator.unpin();
				else pinningDecorator.pin();
				pinBtn.setToolTipText(pinningDecorator.isPinned() ? "Click to unpin" : "Click to pin");
			}
		});
	}
}
