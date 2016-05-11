package eu.openanalytics.phaedra.base.ui.util.copy;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CopyItems;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.CutItems;
import eu.openanalytics.phaedra.base.ui.util.copy.cmd.PasteItems;
import eu.openanalytics.phaedra.base.ui.util.view.PartDecorator;

public class CopyPasteSelectionDecorator extends PartDecorator {

	public static final int COPY = 2;
	public static final int PASTE = 4;
	public static final int CUT = 8;

	private int style;

	public static long cutTime;
	public static CopyPasteSelectionDecorator cutDecorator;

	public CopyPasteSelectionDecorator(int style) {
		this.style = style;
	}

	@Override
	public void contributeContextMenu(IMenuManager manager) {
		if (COPY == (style & COPY)) {
			CommandContributionItemParameter param = new CommandContributionItemParameter(
					getWorkBenchPart().getSite()
					, null
					, CopyItems.class.getName()
					, SWT.PUSH
			);
			param.icon = IconManager.getIconDescriptor("page_copy.png");
			CommandContributionItem item = new CommandContributionItem(param);
			manager.add(item);
		}
		if (CUT == (style & CUT)) {
			CommandContributionItemParameter param = new CommandContributionItemParameter(
					getWorkBenchPart().getSite()
					, null
					, CutItems.class.getName()
					, SWT.PUSH
			);
			param.icon = IconManager.getIconDescriptor("cut.png");
			CommandContributionItem item = new CommandContributionItem(param);
			manager.add(item);
		}
		if (PASTE == (style & PASTE)) {
			CommandContributionItemParameter param = new CommandContributionItemParameter(
					getWorkBenchPart().getSite()
					, null
					, PasteItems.class.getName()
					, SWT.PUSH
			);
			param.icon = IconManager.getIconDescriptor("page_paste.png");
			CommandContributionItem item = new CommandContributionItem(param);
			manager.add(item);
		}

		// Make sure the optional key bindings work.
		manager.updateAll(true);
	}

	@Override
	public void onDispose() {
		if (cutDecorator == this) {
			cutDecorator = null;
		}
		super.onDispose();
	}

	public void cutAction(ISelection selection) {
		// Do nothing.
	}

	public void pasteAction(ISelection selection) {
		// Do nothing.
	}

}