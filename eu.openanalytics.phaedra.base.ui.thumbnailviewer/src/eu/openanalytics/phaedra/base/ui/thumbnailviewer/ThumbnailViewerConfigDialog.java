package eu.openanalytics.phaedra.base.ui.thumbnailviewer;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class ThumbnailViewerConfigDialog extends TitleAreaDialog {

	private ThumbnailViewer viewer;
	
	private Spinner colSpinner;
	private Spinner rowSpinner;
	private Spinner paddingSpinner;
	
	private int oldRows;
	private int oldCols;
	private int oldPadding;
	private Color oldColor;

	private ColorSelector colorSelector;
	
	public ThumbnailViewerConfigDialog(Shell parentShell, ThumbnailViewer viewer) {
		super(parentShell);
		this.viewer = viewer;
		
		this.oldRows = viewer.getThumbnail().getRows();
		this.oldCols = viewer.getThumbnail().getCols();
		this.oldPadding = viewer.getThumbnail().getPadding();
		this.oldColor = viewer.getThumbnail().getPaddingColor();
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Thumbnail Viewer Settings");
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite parentComposite = (Composite) super.createDialogArea(parent);

		Composite main = new Composite(parentComposite, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(main);
		GridLayoutFactory.fillDefaults().margins(5,5).numColumns(2).applyTo(main);
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("Force # of rows:");
		
		rowSpinner = new Spinner(main, SWT.BORDER);
		
		rowSpinner.setMinimum(0);
		rowSpinner.setMaximum(25000);
		rowSpinner.setSelection(oldRows);
		rowSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int selection = rowSpinner.getSelection();
				viewer.getThumbnail().setRows(selection);
				if (selection != 0 && colSpinner.getSelection() != 0) {
					colSpinner.setSelection(0);
				}
			}
		});
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Force # of cols:");
		
		colSpinner = new Spinner(main, SWT.BORDER);
		colSpinner.setMinimum(0);
		colSpinner.setMaximum(25000);
		colSpinner.setSelection(oldCols);
		colSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int selection = colSpinner.getSelection();
				viewer.getThumbnail().setCols(selection);
				if (selection != 0 && rowSpinner.getSelection() != 0) {
					rowSpinner.setSelection(0);
				}
			}
		});
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Padding:");
		
		paddingSpinner = new Spinner(main, SWT.BORDER);
		paddingSpinner.setMinimum(0);
		paddingSpinner.setMaximum(100);
		paddingSpinner.setSelection(oldPadding);
		paddingSpinner.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				int selection = paddingSpinner.getSelection();
				viewer.getThumbnail().setPadding(selection);
			}
		});
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Background Color:");
		
		RGB rgb = viewer.getThumbnail().getPaddingColor().getRGB();
		colorSelector = new ColorSelector(main);
		colorSelector.setColorValue(rgb);
		colorSelector.addListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				RGB rgb = colorSelector.getColorValue();
				viewer.getThumbnail().setPaddingColor(new Color(null, rgb));
			}
		});
		
		setTitle("Thumbnail Viewer Settings");
		setMessage("Change the settings of the Thumbnail Viewer.");
		
		return main;
	}
	
	@Override
	protected void cancelPressed() {
		viewer.getThumbnail().setCols(oldCols);
		viewer.getThumbnail().setRows(oldRows);
		viewer.getThumbnail().setPadding(oldPadding);
		viewer.getThumbnail().setBackground(oldColor);
		super.cancelPressed();
	}

}