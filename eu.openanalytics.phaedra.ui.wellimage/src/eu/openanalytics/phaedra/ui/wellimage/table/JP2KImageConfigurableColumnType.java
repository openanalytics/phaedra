package eu.openanalytics.phaedra.ui.wellimage.table;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;

import eu.openanalytics.phaedra.base.ui.richtableviewer.RichTableViewer;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnDataType;
import eu.openanalytics.phaedra.base.ui.richtableviewer.column.IConfigurableColumnType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel;
import eu.openanalytics.phaedra.ui.wellimage.util.ImageControlPanel.ImageControlListener;

public class JP2KImageConfigurableColumnType implements IConfigurableColumnType {

	private List<TableColumn> validColumns;

	private Combo columnCombo;

	private JP2KImageLabelProvider imageProvider;
	private ProtocolClass pClass;

	private ImageControlPanel imgControlPanel;

	@Override
	public void fillConfigArea(Composite parent, RichTableViewer tableViewer) {
		this.validColumns = new ArrayList<>();

		TableColumn[] columns = tableViewer.getTable().getColumns();
		for (TableColumn column : columns) {
			Object data = column.getData();
			if (data instanceof ColumnConfiguration) {
				ColumnConfiguration conf = (ColumnConfiguration) data;
				if (conf.getDataType() == getColumnDataType()) {
					validColumns.add(column);
				}
			}
		}

		String[] colNames = new String[validColumns.size()];
		int i = 0;
		for (TableColumn column : validColumns) {
			colNames[i++] = column.getText();
		}

		Label label = new Label(parent, SWT.NONE);
		label.setText("Image column to configure:");

		columnCombo = new Combo(parent, SWT.READ_ONLY);
		columnCombo.setItems(colNames);
		columnCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int sel = columnCombo.getSelectionIndex();
				selectColumn(sel);
			}
		});

		// Automatically select the first column.
		if (colNames.length > 0) {
			columnCombo.select(0);
			selectColumn(0);
		}

		imgControlPanel = new ImageControlPanel(parent, SWT.NONE, imageProvider.hasScale(), false);
		imgControlPanel.addImageControlListener(new ImageControlListener() {
			@Override
			public void componentToggled(int component, boolean state) {
				imageProvider.setSettings(imgControlPanel.getButtonStates());
			}
			@Override
			public void scaleChanged(float ratio) {
				imageProvider.setSettings(imgControlPanel.getCurrentScale());
			}
		});
		GridDataFactory.fillDefaults().span(2, 1).applyTo(imgControlPanel);
		loadSettings();
	}

	@Override
	public ColumnDataType getColumnDataType() {
		return ColumnDataType.JP2K_IMAGE;
	}

	@Override
	public String getName() {
		return "Image Settings";
	}

	private void selectColumn(int columnIndex) {
		TableColumn column = validColumns.get(columnIndex);
		Object o = column.getData();
		if (o instanceof ColumnConfiguration) {
			ColumnConfiguration config = (ColumnConfiguration) o;
			CellLabelProvider labelProvider = config.getLabelProvider();
			if (labelProvider instanceof JP2KImageLabelProvider) {
				imageProvider = (JP2KImageLabelProvider) labelProvider;
				loadSettings();
			}
		}
	}

	private void loadSettings() {
		if (imgControlPanel != null) {
			pClass = imageProvider.getProtocolClass();
			imgControlPanel.setImage(pClass);
			imgControlPanel.setButtonStates(imageProvider.getDefaultChannels());
		}
	}

}