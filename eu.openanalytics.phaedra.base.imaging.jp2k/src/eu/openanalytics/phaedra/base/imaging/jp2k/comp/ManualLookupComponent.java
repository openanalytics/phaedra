package eu.openanalytics.phaedra.base.imaging.jp2k.comp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColorCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.util.misc.ColorUtils;
import eu.openanalytics.phaedra.base.util.misc.ImageUtils;
import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;

public class ManualLookupComponent extends BaseComponentType {

	private int[] lutTable;

	@Override
	public String getName() {
		return "Manual Lookup Overlay";
	}

	@Override
	public int getId() {
		return 2;
	}

	@Override
	public String getDescription() {
		return "A lookup overlay where the pixel values are mapped onto a custom table of colors.";
	}

	@Override
	public void loadConfig(Map<String, String> config) {
		lutTable = new int[256];
		for (String key: config.keySet()) {
			if (!NumberUtils.isNumeric(key)) continue;
			int index = Integer.parseInt(key);
			int value = loadColor(config, key, 0);
			if (index >= 0 && index < lutTable.length) lutTable[index] = value;
		}
	}
	
	@Override
	public void saveConfig(Map<String, String> config) {
		for (int i=0; i<lutTable.length; i++) {
			// Black is the default, do not save it.
			if (lutTable[i] == 0) continue;
			saveColor(config, ""+i, lutTable[i]);
		}
	}
	
	@Override
	public void blend(ImageData source, ImageData target, int... params) {
		int[] sourcePixels = new int[target.width*target.height];
		source.getPixels(0, 0, sourcePixels.length, sourcePixels, 0);
		
		int[] targetPixels = new int[target.width*target.height];
		target.getPixels(0, 0, targetPixels.length, targetPixels, 0);
		
		int alpha = params[5];
		for (int i=0; i<sourcePixels.length; i++) {
			int overlayValue = sourcePixels[i];
			// Skip background.
			if (overlayValue == 0) continue;
			// Skip transparent pixels.
			if (source.alphaData != null && source.alphaData[i] != (byte)255) continue;

			// Scaling 16bit down to 8bit usually results in labels betweeen 0 and 1.
			// So instead, chop off the highest 8 bits.
			if (source.depth == 16) overlayValue = overlayValue & 0xFF;
			else overlayValue = ImageUtils.to8bit(overlayValue, source.depth);
			
			int color = lutTable[overlayValue];
			if (alpha == 255) targetPixels[i] = color;
			else targetPixels[i] = ImageUtils.blend(color, targetPixels[i], alpha);
		}
		target.setPixels(0, 0, targetPixels.length, targetPixels, 0);
	}

	@Override
	public Image createIcon(Device device) {
		Image img = new Image(device, 20, 20);
		GC gc = new GC(img);

		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, 20, 20);
		
		int sampleColors = 5;
		int heightPerSample = img.getBounds().height/sampleColors;
		for (int i=0; i<sampleColors; i++) {
			int offset = i*heightPerSample;
			int r = (lutTable[i] >> 16) & 0xFF;
			int g = (lutTable[i] >> 8) & 0xFF;
			int b = (lutTable[i]) & 0xFF;
			
			// Skip black.
			if (lutTable[i] == 0) continue;
			
			Color color = new Color(device,r,g,b);
			gc.setBackground(color);
			gc.fillRectangle(0, offset, 20, heightPerSample);
			color.dispose();
		}

		gc.dispose();

		return img;
	}

	@Override
	public void createConfigArea(final Composite parent, final Map<String, String> config, final ISelectionChangedListener changeListener) {
		Button configBtn = new Button(parent, SWT.PUSH);
		configBtn.setText("Configure...");
		configBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				new ConfigDialog(parent.getShell(), config, changeListener).open();
			}
		});
	}
	
	private class ConfigDialog extends TitleAreaDialog {

		private TableViewer tableViewer;
		
		private ISelectionChangedListener changeListener;
		private Map<String,String> config;
		private Map<String,String> configWorkingCopy;
		
		public ConfigDialog(Shell parentShell, Map<String,String> config, ISelectionChangedListener changeListener) {
			super(parentShell);
			this.changeListener = changeListener;
			this.config = config;
			this.configWorkingCopy = new HashMap<>(config);
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			Composite parentContainer = (Composite) super.createDialogArea(parent);
			
			Composite container = new Composite(parentContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).margins(5,5).applyTo(container);
			
			tableViewer = new TableViewer(container, SWT.FULL_SELECTION | SWT.BORDER);
			tableViewer.setContentProvider(new ArrayContentProvider());
			tableViewer.getTable().setHeaderVisible(true);
			tableViewer.getTable().setLinesVisible(true);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getControl());
			
			// Dummy column for Windows bug related to images in columns.
			TableViewerColumn col = new TableViewerColumn(tableViewer, SWT.NONE);
			col.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {}
			});
			
			col = new TableViewerColumn(tableViewer, SWT.NONE);
			col.getColumn().setWidth(50);
			col.getColumn().setText("Index");
			col.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					Integer index = (Integer)cell.getElement();
					cell.setText(""+index);
				}
			});
			
			col = new TableViewerColumn(tableViewer, SWT.NONE);
			col.getColumn().setWidth(200);
			col.getColumn().setText("Color");
			col.setEditingSupport(new ColorEditingSupport(tableViewer, configWorkingCopy));
			col.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public Image getImage(Object element) {
					String indexString = element.toString();
					RGB rgb = loadColorRGB(configWorkingCopy, indexString, 0);
					int w = 60;
					int h = 15;
					Image img = new Image(null, w, h);
					GC gc = new GC(img);
					Color color = new Color(null, rgb);
					gc.setBackground(color);
					gc.fillRectangle(0,0,w,h);
					color.dispose();
					gc.dispose();
					return img;
				}
				@Override
				public String getText(Object element) {
					String indexString = element.toString();
					RGB rgb = loadColorRGB(configWorkingCopy, indexString, 0);
					return "#" + Integer.toHexString(ColorUtils.rgbToHex(rgb)).toUpperCase();
				}
			});
			
			Composite buttonContainer = new Composite(container, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(buttonContainer);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(buttonContainer);
			
			Button addBtn = new Button(buttonContainer, SWT.PUSH);
			addBtn.setImage(IconManager.getIconImage("add.png"));
			addBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					List<Integer> input = generateTableInput();
					String newNr = "1";
					if (!input.isEmpty()) newNr = "" + (input.get(input.size()-1) + 1);
					configWorkingCopy.put(newNr, ColorUtils.rgbToStringHex(new RGB(0,0,0)));
					tableViewer.setInput(generateTableInput());
				}
			});
			
			Button removeBtn = new Button(buttonContainer, SWT.PUSH);
			removeBtn.setImage(IconManager.getIconImage("delete.png"));
			removeBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Integer selected = SelectionUtils.getFirstObject(tableViewer.getSelection(), Integer.class);
					if (selected != null) {
						configWorkingCopy.remove("" + selected);
						tableViewer.setInput(generateTableInput());
					}
				}
			});

			tableViewer.setInput(generateTableInput());
			
			setTitle("Configure Image Channel");
			setMessage("You can configure the settings of the image channel below.");
			
			return parentContainer;
		}
		
		private List<Integer> generateTableInput() {
			List<Integer> input = new ArrayList<>();
			for (String key: configWorkingCopy.keySet()) {
				try {
					input.add(Integer.parseInt(key));
				} catch (NumberFormatException e) {
					// Invalid setting: continue.
				}
			}
			Collections.sort(input);
			return input;
		}
		
		@Override
		protected void okPressed() {
			config.clear();
			config.putAll(configWorkingCopy);
			if (changeListener != null) changeListener.selectionChanged(null);
			super.okPressed();
		}
		
		@Override
		public boolean close() {
			return super.close();
		}
	}
	
	private static class ColorEditingSupport extends EditingSupport {

		private final TableViewer viewer;
		private Map<String,String> config;
		
		public ColorEditingSupport(TableViewer viewer, Map<String,String> config) {
			super(viewer);
			this.viewer = viewer;
			this.config = config;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}
		
		@Override
		protected CellEditor getCellEditor(Object element) {
			return new ColorCellEditor(((TableViewer)getViewer()).getTable());
		}

		@Override
		protected Object getValue(Object element) {
			String indexString = element.toString();
			return loadColorRGB(config, indexString, 0);
		}

		@Override
		protected void setValue(Object element, Object value) {
			String indexString = element.toString();
			saveColor(config, indexString, ColorUtils.rgbToHex((RGB)value));
			viewer.update(element, null);
		}
	}
}
