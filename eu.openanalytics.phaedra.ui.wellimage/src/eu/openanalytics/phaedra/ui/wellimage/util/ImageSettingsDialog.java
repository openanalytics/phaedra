package eu.openanalytics.phaedra.ui.wellimage.util;

import java.util.Arrays;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;

public class ImageSettingsDialog extends TitleAreaDialog {

	private ImageControlPanel imgControlPanel;
	private ImageControlListener listener;

	private ProtocolClass pClass;
	private float scale;
	private boolean[] channels;

	private boolean showScale;

	public ImageSettingsDialog(Shell parentShell, ProtocolClass pClass
			, float scale, boolean[] channels, ImageControlListener listener) {

		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.MODELESS | SWT.RESIZE);
		setBlockOnOpen(true);

		this.pClass = pClass;
		this.scale = scale;
		this.channels = Arrays.copyOf(channels, channels.length);
		this.listener = listener;
		this.showScale = true;
	}

	public ImageSettingsDialog(Shell parentShell, ProtocolClass pClass
			, boolean[] channels, ImageControlListener listener) {

		this(parentShell, pClass, 1f, channels, listener);
		this.showScale = false;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Adjust Image Settings");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Adjust Image Settings");
		setMessage("Adjust the Image Settings");
		Composite area = (Composite) super.createDialogArea(parent);
		Composite comp = new Composite(area, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(comp);
		GridLayoutFactory.fillDefaults().margins(10, 10).applyTo(comp);

		imgControlPanel = new ImageControlPanel(comp, SWT.NONE, showScale, false);
		imgControlPanel.setImage(pClass);
		if (showScale && scale > 0f) imgControlPanel.setCurrentScale(scale);
		if (channels != null) imgControlPanel.setButtonStates(channels);
		imgControlPanel.addImageControlListener(listener);

		return comp;
	}

	@Override
	protected void cancelPressed() {
		if (showScale) imgControlPanel.setCurrentScale(scale);
		imgControlPanel.setButtonStates(channels);
		super.cancelPressed();
	}

}
