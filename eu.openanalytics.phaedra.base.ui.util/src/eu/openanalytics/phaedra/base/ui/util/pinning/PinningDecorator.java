package eu.openanalytics.phaedra.base.ui.util.pinning;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.ISelectionListener;

import eu.openanalytics.phaedra.base.ui.util.view.PartDecorator;

@Deprecated
public class PinningDecorator extends PartDecorator {

	private PinningSupport pinningSupport;

	private ISelectionListener[] selectionListeners;

	public PinningDecorator(ISelectionListener... listeners) {
		pinningSupport = new PinningSupport();
		this.selectionListeners = listeners;
	}

	@Override
	public void contributeToolbar(IToolBarManager manager) {
		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				pinningSupport.createPinButton(PinningDecorator.this, parent);
			}
		};
		manager.add(contributionItem);
	}

	public void pin() {
		doPin();
		pinningSupport.setPinned(true);
	}

	public void unpin() {
		doUnpin();
		pinningSupport.setPinned(false);
	}

	public boolean isPinned() {
		return pinningSupport.isPinned();
	};

	protected void doPin() {
		if (selectionListeners != null) {
			for (ISelectionListener listener: selectionListeners) getWorkBenchPart().getSite().getPage().removeSelectionListener(listener);
		}
	}

	protected void doUnpin() {
		if (selectionListeners != null) {
			for (ISelectionListener listener: selectionListeners) getWorkBenchPart().getSite().getPage().addSelectionListener(listener);
		}
	}
}
