package eu.openanalytics.phaedra.ui.plate.util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class HeaderBar extends Composite {

	private Label iconLbl;
	private Label textLbl;
	private Label statusLbl;

	private Color bgColor;
	private Color bgColorSelected;

	public HeaderBar(Composite parent, int style) {
		super(parent, style);

		parent.addDisposeListener(e -> {
			bgColor.dispose();
			bgColorSelected.dispose();
		});

		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(this);

		iconLbl = new Label(this, SWT.NONE);
		textLbl = new Label(this, SWT.NONE);
		statusLbl = new Label(this, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).grab(true, false).applyTo(textLbl);

		bgColor = new Color(null, 200,200,200);
		bgColorSelected = new Color(null, 150,150,225);
		setBackground(bgColor);
		iconLbl.setBackground(bgColor);
		textLbl.setBackground(bgColor);
		statusLbl.setBackground(bgColor);
	}

	public void setSelected(boolean selected) {
		Color c = selected ? bgColorSelected : bgColor;
		setBackground(c);
		iconLbl.setBackground(c);
		textLbl.setBackground(c);
		statusLbl.setBackground(c);
	}

	public void setText(String text) {
		textLbl.setText(text);
	}

	public void setIcon(Image image) {
		iconLbl.setImage(image);
	}

	public void setStatus(Image image) {
		statusLbl.setImage(image);
	}

}
