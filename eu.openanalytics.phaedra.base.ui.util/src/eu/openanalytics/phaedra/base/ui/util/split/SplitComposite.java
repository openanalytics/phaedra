package eu.openanalytics.phaedra.base.ui.util.split;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IMemento;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class SplitComposite extends SashForm {

	public final static int MODE_H_1_2 = 1;
	public final static int MODE_H_2_1 = 2;
	public final static int MODE_V_1_2 = 3;
	public final static int MODE_V_2_1 = 4;
	public final static int MODE_1_ONLY = 5;
	public final static int MODE_2_ONLY = 6;

	private static Image[] modeIcons = {
		IconManager.getIconImage("split/h_1_2.png"),
		IconManager.getIconImage("split/h_2_1.png"),
		IconManager.getIconImage("split/v_1_2.png"),
		IconManager.getIconImage("split/v_2_1.png"),
		IconManager.getIconImage("split/1_only.png"),
		IconManager.getIconImage("split/2_only.png")
	};

	private static String[] modeTexts = {
		"Arrange horizontally 1 - 2",
		"Arrange horizontally 2 - 1",
		"Arrange vertically 1 - 2",
		"Arrange vertically 2 - 1",
		"Only 1",
		"Only 2"
	};

	private int currentMode;
	private Control firstChild;
	private Control secondChild;
	private ToolItem selectModeBtn;
	private ListenerList listenerList;

	public SplitComposite(Composite parent, int mode, boolean border) {
		super(parent, getStyle(mode, border));
		currentMode = mode;
		listenerList = new ListenerList();
		setSashWidth(2);
//		setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				// Clear references to prevent leak.
				firstChild = null;
				secondChild = null;
				selectModeBtn = null;
				listenerList.clear();
			}
		});
	}

	public void save(IMemento memento) {
		if (memento != null) {
			memento.putInteger("splitcomp_mode", currentMode);
		}
	}

	public ContributionItem createModeButton() {
		ContributionItem modeButton = new ContributionItem() {
			@Override
			public void fill(final ToolBar parent, int index) {

				final Menu dropdownMenu = new Menu(parent.getShell());

				// Create the toolbar dropdown button.
				selectModeBtn = new ToolItem(parent, SWT.DROP_DOWN);

				// Listen to clicks on the dropdown button.
				selectModeBtn.addListener (SWT.Selection, new Listener () {
					@Override
					public void handleEvent (Event event) {
						if (event.detail == SWT.ARROW) {
							Rectangle rect = selectModeBtn.getBounds();
							Point pt = new Point(rect.x, rect.y + rect.height);
							pt = parent.toDisplay(pt);
							dropdownMenu.setLocation(pt.x, pt.y);
							dropdownMenu.setVisible(true);
						} else {
							if (currentMode == MODE_2_ONLY) currentMode = 1;
							else currentMode++;

							for (int mode = 1; mode <= 6; mode++) {
								if (currentMode == mode) {
									dropdownMenu.getItem(mode-1).setSelection(true);
								} else {
									dropdownMenu.getItem(mode-1).setSelection(false);
								}
							}

							setMode(currentMode);
						}
					}
				});

				// Listen to clicks on the dropdown submenu.
				SelectionListener menuItemListener = new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						MenuItem item = (MenuItem)e.widget;
						int mode = (Integer)item.getData();
						setMode(mode);
					}
				};

				// Create the submenu.
				for (int mode = 1; mode <= 6; mode++) {
					MenuItem menuItem = new MenuItem(dropdownMenu, SWT.RADIO);
					menuItem.setText(modeTexts[mode-1]);
					menuItem.setData(mode);
					menuItem.setImage(modeIcons[mode-1]);
					menuItem.addSelectionListener(menuItemListener);

					if (mode == currentMode) menuItem.setSelection(true);
				}

				// Make sure the initial mode is set.
				setMode(currentMode);
			}
		};

		return modeButton;
	}

	public int getMode(){
		return currentMode;
	}

	private static int getStyle(int mode, boolean border) {
		int style = ((mode == MODE_H_1_2 || mode == MODE_H_2_1) ? SWT.HORIZONTAL : SWT.VERTICAL);
		if (border) style = style | SWT.BORDER;
		return style;
	}

	public void setMode(int mode) {
		currentMode = mode;

		if (firstChild == null || secondChild == null) {
			firstChild = getChildren()[0];
			secondChild = getChildren()[1];
		}

		switch(currentMode) {
		case MODE_H_1_2:
			setOrientation(SWT.HORIZONTAL);
			firstChild.moveAbove(secondChild);
			firstChild.setVisible(true);
			secondChild.setVisible(true);
			break;
		case MODE_H_2_1:
			setOrientation(SWT.HORIZONTAL);
			secondChild.moveAbove(firstChild);
			firstChild.setVisible(true);
			secondChild.setVisible(true);
			break;
		case MODE_V_1_2:
			setOrientation(SWT.VERTICAL);
			firstChild.moveAbove(secondChild);
			firstChild.setVisible(true);
			secondChild.setVisible(true);
			break;
		case MODE_V_2_1:
			setOrientation(SWT.VERTICAL);
			secondChild.moveAbove(firstChild);
			firstChild.setVisible(true);
			secondChild.setVisible(true);
			break;
		case MODE_1_ONLY:
			firstChild.setVisible(true);
			secondChild.setVisible(false);
			break;
		case MODE_2_ONLY:
			firstChild.setVisible(false);
			secondChild.setVisible(true);
			break;
		}

		if (selectModeBtn != null) {
			selectModeBtn.setToolTipText(modeTexts[currentMode-1]);
			selectModeBtn.setImage(modeIcons[currentMode-1]);
		}
		fireSplitModeChangedEvent();
		layout();
	}

	/**
	 * Get the visibility for given child.
	 * @param child Should be 1 or 2.
	 * @return
	 */
	public boolean isVisible(int child) {
		switch(currentMode) {
		case MODE_1_ONLY:
			return child < 2;
		case MODE_2_ONLY:
			return child > 1;
		default:
			return true;
		}
	}

	public void addSplitModeChangedListener(ISplitCompositeListener listener){
		listenerList.add(listener);
	}

	public void removeSplitModeChangedListener(ISplitCompositeListener listener){
		listenerList.remove(listener);
	}

	private void fireSplitModeChangedEvent(){
		Object[] listeners = listenerList.getListeners();
		for(int i = 0; i < listeners.length; i += 2){
			if(listeners[i] instanceof ISplitCompositeListener){
				((ISplitCompositeListener)listeners[i]).splitModeChanged(currentMode);
			}
		}
	}
}
