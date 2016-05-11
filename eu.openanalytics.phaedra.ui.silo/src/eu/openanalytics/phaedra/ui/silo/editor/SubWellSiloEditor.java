package eu.openanalytics.phaedra.ui.silo.editor;

import java.io.IOException;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.DefaultComparator;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultIntegerDisplayConverter;
import org.eclipse.nebula.widgets.nattable.data.convert.DefaultLongDisplayConverter;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.base.ui.nattable.NatTableUtils;
import eu.openanalytics.phaedra.base.ui.nattable.convert.FormattedDisplayConverter;
import eu.openanalytics.phaedra.base.ui.util.tooltip.ToolTipLabelProvider;
import eu.openanalytics.phaedra.base.util.misc.EclipseLog;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.IFeature;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.SubWellFeature;
import eu.openanalytics.phaedra.model.subwell.SubWellItem;
import eu.openanalytics.phaedra.model.subwell.util.SubWellUtils;
import eu.openanalytics.phaedra.silo.SiloDataService.SiloDataType;
import eu.openanalytics.phaedra.silo.SiloException;
import eu.openanalytics.phaedra.silo.accessor.ISiloAccessor;
import eu.openanalytics.phaedra.ui.protocol.dialog.FeatureSelectionDialog;
import eu.openanalytics.phaedra.ui.silo.Activator;
import eu.openanalytics.phaedra.ui.wellimage.tooltip.SubWellToolTipLabelProvider;
import eu.openanalytics.phaedra.wellimage.ImageRenderService;

public class SubWellSiloEditor extends SiloEditor<SubWellItem, SubWellFeature> {

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
	}

	@Override
	protected ImageData getImageData(SubWellItem entity, float scale, boolean[] enabledChannels) {
		try {
			return ImageRenderService.getInstance().getSubWellImageData(entity.getWell(), entity.getIndex(), scale, enabledChannels);
		} catch (IOException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
		return null;
	}

	@Override
	protected Rectangle getImageBounds(SubWellItem entity, float scale) {
		return ImageRenderService.getInstance().getSubWellImageBounds(entity.getWell(), entity.getIndex(), scale);
	}

	@Override
	protected boolean isImageReady(SubWellItem entity, float scale, boolean[] channels) {
		return ImageRenderService.getInstance().isSubWellImageCached(entity.getWell(), entity.getIndex(), scale, channels);
	}

	@Override
	protected boolean selectFeatures(ProtocolClass pClass, List<SubWellFeature> selectedFeatures, List<String> selectedNormalizations) {
		FeatureSelectionDialog<SubWellFeature> dialog = new FeatureSelectionDialog<SubWellFeature>(
				Display.getDefault().getActiveShell(), pClass, SubWellFeature.class, selectedFeatures, selectedNormalizations, 0, Integer.MAX_VALUE);
		return (dialog.open() == Window.OK);
	}

	@Override
	protected List<SubWellItem> getSelectedEntities(ISelection selection) {
		return SubWellUtils.getSubWellItems(selection);
	}

	@Override
	protected void registerDisplayConverters(ISiloAccessor<SubWellItem> accessor, String dataGroup, IConfigRegistry configRegistry) {
		try {
			String[] columns = accessor.getColumns(dataGroup);
			for (int i = 0; i < columns.length; i++) {
				IFeature f = ProtocolUtils.getSubWellFeatureByName(columns[i], accessor.getSilo().getProtocolClass());
				if (f != null) {
					String formatString = f.getFormatString();
					FormattedDisplayConverter converter = new FormattedDisplayConverter(formatString, false);
					configRegistry.registerConfigAttribute(
							CellConfigAttributes.DISPLAY_CONVERTER
							, converter
							, DisplayMode.NORMAL
							, columns[i]
					);
					NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
							, converter, converter.getFilterComparator());
				} else {
					SiloDataType dataType = accessor.getDataType(dataGroup, i);
					switch (dataType) {
					case Double:
						FormattedDisplayConverter fpConverter = new FormattedDisplayConverter(true);
						configRegistry.registerConfigAttribute(
								CellConfigAttributes.DISPLAY_CONVERTER
								, fpConverter
								, DisplayMode.NORMAL
								, columns[i]
						);
						NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
								, fpConverter, fpConverter.getFilterComparator());
						break;
					case Float:
						FormattedDisplayConverter converter = new FormattedDisplayConverter(false);
						configRegistry.registerConfigAttribute(
								CellConfigAttributes.DISPLAY_CONVERTER
								, converter
								, DisplayMode.NORMAL
								, columns[i]
						);
						NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
								, converter, converter.getFilterComparator());
						break;
					case Integer:
						DefaultIntegerDisplayConverter intConverter = new DefaultIntegerDisplayConverter();
						configRegistry.registerConfigAttribute(
								CellConfigAttributes.DISPLAY_CONVERTER
								, intConverter
								, DisplayMode.NORMAL
								, columns[i]
						);
						NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
								, intConverter, new DefaultComparator());
						break;
					case Long:
						DefaultLongDisplayConverter longConverter = new DefaultLongDisplayConverter();
						configRegistry.registerConfigAttribute(
								CellConfigAttributes.DISPLAY_CONVERTER
								, longConverter
								, DisplayMode.NORMAL
								, columns[i]
						);
						NatTableUtils.applyAdvancedFilter(configRegistry, i + 1
								, longConverter, new DefaultComparator());
						break;
					case String:
					case None:
					default:
						break;
					}
				}
			}
		} catch (SiloException e) {
			EclipseLog.error(e.getMessage(), e, Activator.getDefault());
		}
	}

	@Override
	protected ToolTipLabelProvider createToolTipLabelProvider() {
		return new SubWellToolTipLabelProvider();
	}

}