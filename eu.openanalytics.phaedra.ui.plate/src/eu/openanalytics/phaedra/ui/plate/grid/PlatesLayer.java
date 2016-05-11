package eu.openanalytics.phaedra.ui.plate.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.openanalytics.phaedra.base.ui.gridviewer.layer.BaseGridLayer;
import eu.openanalytics.phaedra.base.ui.gridviewer.layer.GridState;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.ui.plate.Activator;
import eu.openanalytics.phaedra.ui.plate.preferences.Prefs;

public abstract class PlatesLayer extends BaseGridLayer {

	private List<Plate> plates;

	protected boolean multiPlate;

	@Override
	public String getId() {
		if (multiPlate)
			return super.getId() + "MultiPlate";
		return super.getId();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setInput(Object newInput) {
		plates = new ArrayList<>();
		if (newInput instanceof Plate) {
			plates.add((Plate) newInput);
		} else if (newInput instanceof List) {
			List<?> list = (List<?>) newInput;
			if (!list.isEmpty() && list.get(0) instanceof Plate) {
				plates.addAll((List<Plate>) newInput);
			} else if (!list.isEmpty() && list.get(0) instanceof Well) {
				List<Well> wells = (List<Well>) newInput;
				Set<Plate> uniquePlates = new HashSet<>();
				for (Well w : wells) {
					uniquePlates.add(w.getPlate());
				}
				plates.addAll(uniquePlates);
			}
		}
		super.setInput(newInput);
	}

	protected boolean hasPlates() {
		return (plates != null && !plates.isEmpty());
	}

	protected List<Plate> getPlates() {
		return plates;
	}

	protected Plate getPlate() {
		return plates.get(0);
	}

	@Override
	protected void initialize() {
		// Make sure to clean up resources from previous initializations.
		dispose();
		// Do not initialize if new input is null.
		multiPlate = getLayerSupport().getId().contains("hca.multiplewell.grid");
		if (hasPlates()) doInitialize();
	}

	protected abstract void doInitialize();
	
	@Override
	public boolean isDefaultEnabled() {
		Boolean defaultEnabled = GridState.getBooleanValue(GridState.ALL_PROTOCOLS, getId(), GridState.DEFAULT_ENABLED);
		if (defaultEnabled != null) {
			return defaultEnabled;
		} else {
			return Activator.getDefault().getPreferenceStore().getBoolean(Prefs.SHOW_DEFAULT + getClass());
		}
	}

	@SuppressWarnings("unchecked")
	protected static List<Well> getWells(Object o) {
		if (o instanceof Well) {
			return Arrays.asList((Well) o);
		} else if (o instanceof Plate) {
			return ((Plate) o).getWells();
		} else if (o instanceof List) {
			List<?> list = (List<?>) o;
			if (!list.isEmpty()) {
				Object firstItem = list.get(0);
				if (firstItem instanceof Well) {
					return (List<Well>) o;
				} else if (firstItem instanceof Plate) {
					List<Plate> plates = (List<Plate>) list;
					List<Well> wells = new ArrayList<>();
					for (Plate p : plates) {
						wells.addAll(p.getWells());
					}
					return wells;
				}
			}
		}

		return new ArrayList<>();
	}

	protected int[] calculatePadding(int width, int height, int border) {
		// Calculate a padding that scales with size and would be equal to border if size was 100x100.
		return new int[] { (int) (width*border/100), (int) (height*border/100) };
	}
}