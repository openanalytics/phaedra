package eu.openanalytics.phaedra.ui.wellimage.table;

import org.eclipse.swt.graphics.ImageData;

import eu.openanalytics.phaedra.base.ui.richtableviewer.column.ColumnConfiguration;
import eu.openanalytics.phaedra.base.ui.richtableviewer.util.ImageLabelProvider;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;

public abstract class JP2KImageLabelProvider extends ImageLabelProvider {

	public JP2KImageLabelProvider(ColumnConfiguration config) {
		super(config);
	}

	public JP2KImageLabelProvider(ColumnConfiguration config, boolean autoAdjustSize) {
		super(config, autoAdjustSize);
	}

	@Override
	public void setSettings(Object settings) {
		super.setSettings(settings);
		settingsChanged(settings);
	}

	public boolean hasScale() {
		return false;
	}

	public abstract boolean[] getDefaultChannels();

	protected abstract void settingsChanged(Object settings);

	protected abstract ProtocolClass getProtocolClass();

	@Override
	protected abstract ImageData getImageData(Object element, Object settings);

}