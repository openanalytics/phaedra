package eu.openanalytics.phaedra.base.ui.charting.v2.chart.axes;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AbstractChartSettingsDialog;
import eu.openanalytics.phaedra.base.ui.charting.v2.chart.AuxiliaryChartSettings;
import eu.openanalytics.phaedra.base.ui.charting.v2.layer.AbstractChartLayer;
import eu.openanalytics.phaedra.base.ui.charting.v2.util.ShaderIconCreator;
import eu.openanalytics.phaedra.base.util.threading.JobUtils;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Shaders;

public class AuxilaryAxesSettingsDialog<ENTITY, ITEM> extends AbstractChartSettingsDialog<ENTITY, ITEM> {

	private List<AuxiliaryChartSettings> auxSettingValues;
	private Shader[] allShaders;

	private String[] jepExpressions;
	private int dimensions;

	private AuxiliaryChartSettings[] currentSettings;

	public AuxilaryAxesSettingsDialog(Shell parentShell, AbstractChartLayer<ENTITY, ITEM> layer) {
		super(parentShell, layer);

		this.auxSettingValues = new ArrayList<>();
		this.currentSettings = new AuxiliaryChartSettings[3];
		this.dimensions = getLayer().getChart().getType().getNumberOfDimensions();
		this.jepExpressions = getLayer().getDataProvider().getDataProviderSettings().getJepExpressions();

		allShaders = Shaders.getAllLutFileNames();
		for (AuxiliaryChartSettings originalSetting : getSettings().getAuxiliaryChartSettings()) {
			AuxiliaryChartSettings setting = new AuxiliaryChartSettings();
			setting.setHiCut(originalSetting.getHiCut());
			setting.setLoCut(originalSetting.getLoCut());
			setting.setShader(originalSetting.getShader());
			setting.setWeightFeature(originalSetting.getWeightFeature());
			auxSettingValues.add(setting);
		}

		getLayer().dataChanged();
	}

	@Override
	public String getTitle() {
		return "Auxiliary Axes";
	}

	@Override
	public String getTitleMessage() {
		return "Select up to 3 auxiliary axes and their shaders. \n"
				+ "Optionally, stretch the shaders by setting a lo and hi cut value.";
	}

	@Override
	public int getNumberOfColumns() {
		return 3;
	}

	@Override
	public Control embedDialogArea(final Composite comp) {
		List<Object> bufferList = new ArrayList<>();
		bufferList.add("None");
		bufferList.addAll(getLayer().getDataProvider().getFeatures());
		String[] bufferArray = bufferList.toArray(new String[bufferList.size()]);

		for (int i = 0; i < 3; i++) {
			final int index = i;
			AuxiliaryChartSettings auxSettingsTemp = null;
			if (i < getSettings().getAuxiliaryChartSettings().size()) {
				auxSettingsTemp = getSettings().getAuxiliaryChartSettings().get(i);
			}
			if (auxSettingsTemp == null) {
				auxSettingsTemp = new AuxiliaryChartSettings();
				getSettings().getAuxiliaryChartSettings().add(auxSettingsTemp);
			}
			currentSettings[index] = auxSettingsTemp;

			// final int index = i;
			Label lbl = new Label(comp, SWT.NONE);
			lbl.setText("Auxiliary axis " + (i + 1) + ":");
			final Combo comboFeature = new Combo(comp, SWT.READ_ONLY);
			final TableComboViewer comboShader = new TableComboViewer(comp, SWT.BORDER | SWT.READ_ONLY);

			lbl = new Label(comp, SWT.NONE);
			lbl.setText("Lo cut:");
			final Spinner locutSpinner = new Spinner(comp, SWT.BORDER);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(locutSpinner);
			lbl = new Label(comp, SWT.NONE);
			lbl.setText("Hi cut:");
			final Spinner hicutSpinner = new Spinner(comp, SWT.BORDER);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(hicutSpinner);

			comboFeature.setItems(bufferArray);
			comboFeature.addListener(SWT.Selection, e -> {
				String feature = comboFeature.getItem(comboFeature.getSelectionIndex());
				if ("None".equals(feature)) {
					// Feature unselected: unselect shader also.
					comboShader.setSelection(new StructuredSelection("None"));
					currentSettings[index].setShader(null);
				} else {
					// Make sure some shader is selected as well.
					Shader shader = (Shader) ((StructuredSelection) comboShader.getSelection()).getFirstElement();
					if (shader == null) {
						comboShader.setSelection(new StructuredSelection(allShaders[0]));
						currentSettings[index].setShader(allShaders[0]);
					}
				}
				JobUtils.runUserJob(monitor -> {
					if (!"None".equals(feature)) {
						getLayer().getDataProvider().setAuxilaryFeature(feature, dimensions + index, new SubProgressMonitor(monitor, 100));
						jepExpressions = getLayer().getDataProvider().getDataProviderSettings().getJepExpressions();
						currentSettings[index].setWeightFeature(feature);
					} else {
						currentSettings[index].setWeightFeature(null);
					}
					getLayer().getDataProvider().setDataBounds(null);
					getLayer().getDataProvider().getAuxiliaryFeatures().clear();
					getSettings().getAuxiliaryChartSettings().clear();
					int dim = dimensions;
					int skipped = 0;
					for (AuxiliaryChartSettings setting : currentSettings) {
						if (setting.getWeightFeature() != null && !setting.getWeightFeature().isEmpty()) {
							getSettings().getAuxiliaryChartSettings().add(setting);
							getLayer().getDataProvider().getAuxiliaryFeatures().add(setting.getWeightFeature());
							getLayer().getDataProvider().getDataProviderSettings().getJepExpressions()[dim] = jepExpressions[dim + skipped];
							dim++;
						} else {
							skipped++;
						}
					}
					getLayer().dataChanged();
				}, "Updating auxilary feature", 100, getLayer().toString(), null);
			});

			comboShader.setContentProvider(new ArrayContentProvider());
			comboShader.setInput(allShaders);
			comboShader.getTableCombo().setShowImageWithinSelection(true);
			comboShader.setLabelProvider(new LabelProvider() {
				@Override
				public Image getImage(Object element) {
					Image img = createShaderImage(comp, (Shader) element);
					return (img == null) ? super.getImage(element) : img;
				}

				@Override
				public String getText(Object element) {
					return ((Shader) element).getName();
				};
			});

			comboShader.addSelectionChangedListener(event -> {
				currentSettings[index].setShader((Shader) ((StructuredSelection) comboShader.getSelection()).getFirstElement());
				getLayer().settingsChanged();
			});

			// Preselect feature and shader.
			if (currentSettings[index].getWeightFeature() != null) {
				comboFeature.select(bufferList.indexOf(currentSettings[index].getWeightFeature()));
				if (currentSettings[index].getShader() != null) {
					comboShader.setSelection(new StructuredSelection(currentSettings[index].getShader()), true);
				}
			} else {
				comboFeature.select(0);
			}

			locutSpinner.setValues(0, 0, 999, 3, 1, 100);
			locutSpinner.setSelection((int) (currentSettings[index].getLoCut() * 1000));
			locutSpinner.addListener(SWT.Selection, e -> {
				if (locutSpinner.getSelection() >= hicutSpinner.getSelection()) {
					locutSpinner.setSelection(hicutSpinner.getSelection() - 1);
				}
				currentSettings[index].setLoCut((double) locutSpinner.getSelection() / (double) 1000);
				getLayer().dataChanged();
			});

			hicutSpinner.setValues(0, 1, 1000, 3, 1, 100);
			hicutSpinner.setSelection((int) (currentSettings[index].getHiCut() * 1000));

			hicutSpinner.addListener(SWT.Selection, e -> {
				if (locutSpinner.getSelection() >= hicutSpinner.getSelection()) {
					locutSpinner.setSelection(hicutSpinner.getSelection() - 1);
					currentSettings[index].setLoCut((double) locutSpinner.getSelection() / (double) 1000);
				}
				currentSettings[index].setHiCut((double) hicutSpinner.getSelection() / (double) 1000);
				getLayer().dataChanged();
			});
		}
		return comp;
	}

	private Image createShaderImage(Composite parent, Shader shader) {
		if (shader != null) {
			ShaderIconCreator icon = new ShaderIconCreator(shader, true, Color.BLACK, 48, 16, 4, 1);
			Image image = icon.paintIcon(parent.getDisplay(), 1, 1);
			return image;
		}
		return null;
	}

	@Override
	protected void cancelPressed() {
		// restore all values to previous settings
		getLayer().getChartSettings().setAuxiliaryChartSettings(auxSettingValues);

		List<String> auxFeatures = new ArrayList<String>();
		for (AuxiliaryChartSettings auxSetting : auxSettingValues) {
			if (auxSetting.getWeightFeature() != null) {
				auxFeatures.add(auxSetting.getWeightFeature());
			}
		}

		getLayer().getDataProvider().setAuxiliaryFeatures(auxFeatures);
		getLayer().getDataProvider().setDataBounds(null);
		getLayer().dataChanged();

		super.cancelPressed();
	}
}