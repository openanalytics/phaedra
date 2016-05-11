package eu.openanalytics.phaedra.base.ui.util.pinning;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISelectionListener;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.view.PartDecorator;

public class SelectionHandlingDecorator extends PartDecorator {

	private SelectionHandlingMode currentMode;
	private List<SelectionHandlingMode> allowedModes;

	private ISelectionListener[] selectionListeners;
	private ISelectionListener[] highlightListeners;

	private SelectionHandlingDecorator(SelectionHandlingMode currentMode) {
		this.currentMode = currentMode;
		this.allowedModes = new ArrayList<>();
	}

	/**
	 * @param selectionListeners The selectionListener(s)
	 * @param highlightListeners The highlightListener(s)
	 * @param selectionIsHighlight True if selection mode should also highlight. False otherwise.
	 */
	public SelectionHandlingDecorator(ISelectionListener[] selectionListeners, ISelectionListener[] highlightListeners, boolean selectionIsHighlight) {
		this(selectionIsHighlight ? SelectionHandlingMode.SEL_HILITE : SelectionHandlingMode.SEL);

		this.selectionListeners = selectionListeners;
		this.highlightListeners =  highlightListeners;

		allowedModes.add(selectionIsHighlight ? SelectionHandlingMode.SEL_HILITE : SelectionHandlingMode.SEL);
		allowedModes.add(SelectionHandlingMode.HILITE);
		allowedModes.add(SelectionHandlingMode.NONE);
	}

	/**
	 * Note: Normal mode does not highlight.
	 * @param selectionListeners The selectionListener(s)
	 * @param highlightListeners The highlightListener(s)
	 */
	public SelectionHandlingDecorator(ISelectionListener selectionListener, ISelectionListener highlightListener) {
		this(new ISelectionListener[] { selectionListener }, new ISelectionListener[] { highlightListener}, false);
	}

	/**
	 * @param selectionListeners The selectionListener(s)
	 * @param highlightListeners The highlightListener(s)
	 * @param selectionIsHighlight True if normal mode should also highlight. False otherwise.
	 */
	public SelectionHandlingDecorator(ISelectionListener selectionListener, ISelectionListener highlightListener, boolean selectionIsHighlight) {
		this(new ISelectionListener[] { selectionListener }, new ISelectionListener[] { highlightListener }, selectionIsHighlight);
	}

	public SelectionHandlingDecorator(ISelectionListener[] selectionListeners) {
		this(SelectionHandlingMode.SEL_HILITE);

		this.selectionListeners = selectionListeners;

		allowedModes.add(SelectionHandlingMode.SEL_HILITE);
		allowedModes.add(SelectionHandlingMode.NONE);
	}

	public SelectionHandlingDecorator(ISelectionListener selectionListener) {
		this(new ISelectionListener[] { selectionListener });
	}

	public SelectionHandlingMode getMode() {
		return currentMode;
	}

	@Override
	public void onCreate(Composite parent) {
		super.onCreate(parent);
		handleModeChange(currentMode);
	}

	@Override
	public void contributeToolbar(IToolBarManager manager) {
		ContributionItem contributionItem = new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				final ToolItem toggleBtn = new ToolItem(parent, SWT.PUSH);
				toggleBtn.setImage(IconManager.getIconImage(currentMode.getIcon()));
				toggleBtn.setToolTipText("Current mode: " + currentMode.getName());
				toggleBtn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						currentMode = getNextMode(currentMode);

						toggleBtn.setImage(IconManager.getIconImage(currentMode.getIcon()));
						toggleBtn.setToolTipText("Current mode: " + currentMode.getName());

						handleModeChange(currentMode);
					}
				});
			}
		};
		manager.add(contributionItem);
	}

	protected void handleModeChange(SelectionHandlingMode newMode) {
		// Remove previous listeners.
		removeListeners(selectionListeners);
		removeListeners(highlightListeners);

		// Add the new ones if applicable.
		switch (newMode) {
		case SEL_HILITE:
			addListeners(selectionListeners);
			addListeners(highlightListeners);
			break;
		case SEL:
			addListeners(selectionListeners);
			break;
		case HILITE:
			addListeners(highlightListeners);
			break;
		case NONE:
			break;
		default:
			break;
		}
	}

	private void addListeners(ISelectionListener[] listeners) {
		if (listeners != null) {
			for (ISelectionListener listener: listeners) {
				getWorkBenchPart().getSite().getPage().addSelectionListener(listener);
			}
		}
	}

	private void removeListeners(ISelectionListener[] listeners) {
		if (listeners != null) {
			for (ISelectionListener listener: listeners) {
				getWorkBenchPart().getSite().getPage().removeSelectionListener(listener);
			}
		}
	}

	private SelectionHandlingMode getNextMode(SelectionHandlingMode mode) {
		int index = allowedModes.indexOf(mode) + 1;
		if (index >= allowedModes.size()) index = 0;

		return allowedModes.get(index);
	}

}
