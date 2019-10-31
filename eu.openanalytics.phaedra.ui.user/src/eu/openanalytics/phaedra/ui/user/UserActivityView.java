package eu.openanalytics.phaedra.ui.user;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ColumnConfigFactory;
import eu.openanalytics.phaedra.base.util.misc.VersionUtils;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import eu.openanalytics.phaedra.model.user.UserService;
import eu.openanalytics.phaedra.model.user.util.UserActivity;
import eu.openanalytics.phaedra.model.user.util.UserActivitySorter;
import eu.openanalytics.phaedra.model.user.vo.UserSession;

public class UserActivityView extends ViewPart {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	private RichTableViewer tableViewer;
	
	@Override
	public void createPartControl(Composite parent) {
		tableViewer = new RichTableViewer(parent, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		List<ColumnConfiguration> configs = new ArrayList<>();
		configs.add(ColumnConfigFactory.create("User", o -> ((UserActivity) o).getUserName(), ColumnDataType.String, 200));
		configs.add(ColumnConfigFactory.create("Version", o -> ((UserActivity) o).getLatestSessionVersion(), ColumnDataType.String, 150));
		configs.add(ColumnConfigFactory.create("# Logins", o -> "" + ((UserActivity) o).getLoginCount(), ColumnDataType.Numeric, 75));
		configs.add(ColumnConfigFactory.create("Last Login", o -> DATE_FORMAT.format(((UserActivity) o).getLatestSessionDate()), ColumnDataType.Date, 200));
		tableViewer.applyColumnConfig(configs);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new ActivityLabelProvider());
		GridDataFactory.fillDefaults().grab(true,true).applyTo(tableViewer.getControl());

		createToolBar();
		loadActivity();
	}

	@Override
	public void setFocus() {
		tableViewer.getControl().setFocus();
	}

	private void loadActivity() {
		JobUtils.runUserJob(monitor -> {
			final List<UserActivity> activities = UserService.getInstance().getAllUserActivity();
			if (monitor.isCanceled()) return;
			Collections.sort(activities, UserActivitySorter.ACTIVITY_SORTER);
			Display.getDefault().asyncExec(() -> {
				if (!tableViewer.getControl().isDisposed()) tableViewer.setInput(activities);
			});
		}, "Loading User Activity", IProgressMonitor.UNKNOWN, toString(), null);
	}

	private void createToolBar() {
		IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
		Action refreshAction = new Action("Refresh") {
			@Override
			public void run() {
				loadActivity();
			}
		};
		refreshAction.setToolTipText("Refresh");
		refreshAction.setImageDescriptor(IconManager.getIconDescriptor("refresh.png"));
		mgr.add(refreshAction);
	}

	public Styler getStyle(String version) {
		if (VersionUtils.isVersionUnknown(version)) {
			return new DefaultColorStyler(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY), null);
		} else {
			return new DefaultColorStyler(null, null);
		}
	}

	private class ActivityLabelProvider extends StyledCellLabelProvider {
		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			StyledString styledString = null;

			Image icon = null;

			if (element instanceof UserActivity) {
				UserActivity activity = (UserActivity)element;
				if (cell.getColumnIndex() == 0) {
					styledString = new StyledString(activity.getUserName().toLowerCase());
					icon = activity.isActiveToday() ? IconManager.getIconImage("status_green.png") : IconManager.getIconImage("status_grey.png");
				}
				if (cell.getColumnIndex() == 1) {
					String version = activity.getLatestSessionVersion();
					styledString = new StyledString(version, getStyle(version));
				}
				if (cell.getColumnIndex() == 2) {
					styledString = new StyledString(activity.getLoginCount() + "", StyledString.COUNTER_STYLER);
				}
				if (cell.getColumnIndex() == 3) {
					styledString = new StyledString(DATE_FORMAT.format(activity.getLatestSessionDate()) + "", StyledString.DECORATIONS_STYLER);
				}
			} else if (element instanceof UserSession) {
				UserSession session = (UserSession)element;
				if (cell.getColumnIndex() == 0) {
					icon = IconManager.getIconImage("connect.png");
					styledString = new StyledString(session.getHost().toLowerCase() + "");
				}
				if (cell.getColumnIndex() == 1) {
					String version = session.getVersion();
					styledString = new StyledString(version, getStyle(version));
					//styledString = new StyledString(session.getVersion(), StyledString.COUNTER_STYLER);
				}
				if (cell.getColumnIndex() == 2) {
					styledString = new StyledString("");
				}
				if (cell.getColumnIndex() == 3) {
					styledString = new StyledString(DATE_FORMAT.format(session.getLoginDate()) + "", StyledString.DECORATIONS_STYLER);
				}
			}

			cell.setText(styledString.getString());
			cell.setStyleRanges(styledString.getStyleRanges());
			if (icon != null) cell.setImage(icon);
		}
	}

	private class DefaultColorStyler extends Styler {

		private Color fForegroundColorName;
		private Color fBackgroundColorName;

		public DefaultColorStyler(Color foregroundColorName, Color backgroundColorName) {
			this.fForegroundColorName = foregroundColorName;
			this.fBackgroundColorName = backgroundColorName;
		}

		@Override
		public void applyStyles(TextStyle textStyle) {
			if (fForegroundColorName != null) textStyle.foreground = fForegroundColorName;
			if (fBackgroundColorName != null) textStyle.background = fBackgroundColorName;
		}
	}

}
