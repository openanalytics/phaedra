package eu.openanalytics.phaedra.base.ui.trafficlight;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.trafficlight.notification.TrafficNotificationWindow;
import eu.openanalytics.phaedra.base.util.CollectionUtils;

public class StatusLineManager extends ContributionItem {

	private Label[] statusLabels;
	private IStatusChecker[] checkers;
	private TrafficStatus[] currentStatus;
	private Map<String, Image> statusIcons;

	private TrafficNotificationWindow currentNotification;

	public StatusLineManager(IStatusChecker[] checkers) {
		this.statusLabels = new Label[checkers.length];
		this.currentStatus = new TrafficStatus[checkers.length];
		this.checkers = checkers;
		this.statusIcons = new HashMap<>();
	}

	@Override
	public void fill(Composite parent) {
		for (int i = 0; i < statusLabels.length; i++) {
			final int index = i;
			statusLabels[index] = new Label(parent, SWT.NONE);
			statusLabels[index].addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					popNotification(checkers[index], currentStatus[index]);
				}
			});
			// Initialize with a blank status.
			updateStatus(checkers[index], currentStatus[index]);
		}
	}

	@Override
	public void dispose() {
		if (currentNotification != null) currentNotification.close();
		super.dispose();
	}

	public void updateStatus(IStatusChecker checker, TrafficStatus status) {
		int i = CollectionUtils.find(checkers, checker);
		if (statusLabels[i] == null || statusLabels[i].isDisposed()) return;

		if (status == null) status = new TrafficStatus(TrafficStatus.UNKNOWN, "Not tested");
		currentStatus[i] = status;

		statusLabels[i].setImage(createLabelIcon(checker, status));
		statusLabels[i].setToolTipText(checker.getName() + ": " + status.getCodeLabel());

		if (currentNotification != null && currentNotification.isOpen()
				&& currentNotification.getChecker() == checker) currentNotification.setStatus(status);
	}

	private Image createLabelIcon(IStatusChecker checker, TrafficStatus status) {
		String key = checker.getIconLetter() + "#" + status.getIconPath();
		if (!statusIcons.containsKey(key)) {
			Image icon = IconManager.getIconImage(status.getIconPath());
			// Create a copy.
			Image labelledIcon = new Image(icon.getDevice(), icon.getImageData());
			// Draw the label on the copy.
			GC gc = null;
			Font font = new Font(null, "Arial", 8, SWT.BOLD);
			try {
				gc = new GC(labelledIcon);
				gc.setFont(font);
				gc.drawText("" + checker.getIconLetter(), 5, 2, true);
			} finally {
				if (font != null) font.dispose();
				if (gc != null) gc.dispose();
			}
			statusIcons.put(key, labelledIcon);
		}
		return statusIcons.get(key);
	}

	private void popNotification(IStatusChecker checker, TrafficStatus status) {
		if (currentNotification != null) {
			currentNotification.close();
		}
		currentNotification = new TrafficNotificationWindow(Display.getDefault(), checker);
		currentNotification.open();
		currentNotification.setStatus(status);
	}
}
