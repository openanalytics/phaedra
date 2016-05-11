package eu.openanalytics.phaedra.base.ui.util.pinning;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.part.ViewPart;

@Deprecated
public abstract class BasePinnableView extends ViewPart implements IPinnableView {

	private PinningSupport pinningSupport;

	public BasePinnableView() {
		super();
		pinningSupport = new PinningSupport();
	}

	@Override
	public void pin() {
		doPin();
		pinningSupport.setPinned(true);
	}

	@Override
	public void unpin() {
		doUnpin();
		pinningSupport.setPinned(false);
	}

	@Override
	public boolean isPinned() {
		return pinningSupport.isPinned();
	};

	protected void contributePinButton() {
		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				pinningSupport.createPinButton(BasePinnableView.this, parent);
			}
		};
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(contributionItem);
		tbm.update(true);
	}

	protected abstract void doPin();
	protected abstract void doUnpin();
}
