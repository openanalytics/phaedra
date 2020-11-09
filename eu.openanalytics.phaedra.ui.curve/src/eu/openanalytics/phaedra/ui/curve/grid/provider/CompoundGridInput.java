package eu.openanalytics.phaedra.ui.curve.grid.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import eu.openanalytics.phaedra.base.db.IValueObject;
import eu.openanalytics.phaedra.base.environment.Screening;
import eu.openanalytics.phaedra.base.util.CollectionUtils;
import eu.openanalytics.phaedra.calculation.CalculationService;
import eu.openanalytics.phaedra.model.curve.CurveFitService;
import eu.openanalytics.phaedra.model.curve.util.CurveGrouping;
import eu.openanalytics.phaedra.model.curve.util.CurveUtils;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Feature;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.curve.CompoundWithGrouping;
import eu.openanalytics.phaedra.ui.curve.MultiploCompound;
import eu.openanalytics.phaedra.validation.ValidationService.CompoundValidationStatus;
import eu.openanalytics.phaedra.validation.ValidationService.PlateValidationStatus;


public class CompoundGridInput {
	
	/**
	 * List with the CompoundWithGrouping of a compound (simple or multiplo)
	 */
	private static interface ICompoundItem {
		
		public Compound getCompound();
		
		public ArrayList<CompoundWithGrouping> getGridCompounds();
		
		public boolean includesV(Compound candidate);
		public boolean includesM(MultiploCompound candidate);
		
	}
	
	private static class SingleCompoundItem implements ICompoundItem {
		
		private final Compound compound;
		private final ArrayList<CompoundWithGrouping> gridCompounds = new ArrayList<>(0);
		
		SingleCompoundItem(Compound compound) {
			this.compound = compound;
		}
		
		@Override
		public Compound getCompound() {
			return compound;
		}
		
		@Override
		public ArrayList<CompoundWithGrouping> getGridCompounds() {
			return gridCompounds;
		}
		
		@Override
		public boolean includesV(Compound candidate) {
			return compound.equals(candidate);
		}
		
		@Override
		public boolean includesM(MultiploCompound candidate) {
			return false;
		}
		
	}
	
	private static class MultiploCompoundItem implements ICompoundItem {
		
		private final MultiploCompound compound;
		private final ArrayList<CompoundWithGrouping> gridCompounds = new ArrayList<>(0);
		
		MultiploCompoundItem(List<Compound> compounds, Compound firstCompound) {
			this.compound = new MultiploCompound(firstCompound, compounds);
		}
		
		@Override
		public MultiploCompound getCompound() {
			return compound;
		}
		
		@Override
		public ArrayList<CompoundWithGrouping> getGridCompounds() {
			return gridCompounds;
		}
		
		@Override
		public boolean includesV(Compound candidate) {
			return compound.getCompounds().contains(candidate);
		}
		
		@Override
		public boolean includesM(MultiploCompound candidate) {
			return compound.getCompounds().equals(candidate.getCompounds());
		}
		
	}
	
	private static class Key {
		
		private final long experimentId;
		private final String type;
		private final String number;
		
		public Key(Compound compound) {
			this.experimentId = compound.getPlate().getExperiment().getId();
			this.type = compound.getType();
			this.number = compound.getNumber();
		}
		
		@Override
		public int hashCode() {
			int hash = 1;
			hash = 31 * hash + (int)(experimentId ^ (experimentId >>> 32));
			hash = 31 * hash + number.hashCode();
			hash = 31 * hash + type.hashCode();
			return hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			Key other = (Key)obj;
			return (experimentId == other.experimentId
					&& type.equals(other.type)
					&& number.equals(other.number));
		}
		
		@Override
		public String toString() {
			return String.format("%1$s: %2$s %3$s", experimentId, type, number);
		}
		
	}
	
	
	public static MultiploCompound getMultiploCompound(Compound compound) {
		if (compound instanceof CompoundWithGrouping) {
			compound = ((CompoundWithGrouping) compound).getDelegate();
		}
		return (compound instanceof MultiploCompound) ? (MultiploCompound)compound : null;
	}
	
	
	private Protocol protocol;
	private List<Feature> features;
	
	private List<CompoundWithGrouping> compounds;
	
	private Map<Key, List<ICompoundItem>> compoundItems;
	
	
	public CompoundGridInput(List<IValueObject> valueObjects) {
		this.compounds = new ArrayList<>();
		this.compoundItems = new HashMap<>();
		
		init(valueObjects);
	}
	
	
	private boolean containsV(Compound compound) {
		List<ICompoundItem> items = compoundItems.get(new Key(compound));
		if (items == null || items.isEmpty()) return false;
		for (ICompoundItem item : items) {
			if (item.includesV(compound)) {
				return true;
			}
		}
		return false;
	}
	
	private void addItem(ICompoundItem item) {
		Key key = new Key(item.getCompound());
		List<ICompoundItem> items = compoundItems.get(key);
		if (items == null) {
			items = new ArrayList<>(1);
			compoundItems.put(key, items);
		}
		items.add(item);
	}
	
	protected void init(List<IValueObject> valueObjects) {
		if (valueObjects.isEmpty()) {
			features = new ArrayList<>();
			return;
		}
		
		protocol = ((Compound)valueObjects.get(0)).getPlate().getExperiment().getProtocol();
		features = CollectionUtils.findAll(ProtocolUtils.getFeatures(protocol), CurveUtils.FEATURES_WITH_CURVES);
		
		for (IValueObject vo: valueObjects) {
			Compound compound = (Compound) vo;
			ICompoundItem item;
			
			// PHA-861: Let approvers view also the invalidated data
			String ignoreInvalidatedPlateCheck = Screening.getEnvironment().getConfig().getValue("ignore.invalidated.plate.check");
			boolean isIgnoreInvalidatedPlateCheck = StringUtils.isNotBlank(ignoreInvalidatedPlateCheck) ? Boolean.parseBoolean(ignoreInvalidatedPlateCheck) : false;
			
			List<Compound> multiploCompounds = CalculationService.getInstance().getMultiploCompounds(compound);
			if (multiploCompounds.size() > 1) {
				// If this is a multiplo compound, skip it if there is already a multiplo variant in the list.
				if (!containsV(compound)) {
					Compound firstCompound = multiploCompounds.stream()
							.filter(c -> isIgnoreInvalidatedPlateCheck || !CompoundValidationStatus.INVALIDATED.matches(c))
							.filter(c -> isIgnoreInvalidatedPlateCheck || !PlateValidationStatus.INVALIDATED.matches(c.getPlate()))
							.findFirst().orElse(multiploCompounds.get(0));
					item = new MultiploCompoundItem(multiploCompounds, firstCompound);
				} else {
					continue;
				}
			} else {
				item = new SingleCompoundItem(compound);
			}
			
			// If there is grouping, add the compound once for each grouping.
			Set<CurveGrouping> groupings = new HashSet<>();
			for (Feature f: features) {
				for (CurveGrouping cg: CurveFitService.getInstance().getGroupings(item.getCompound(), f)) groupings.add(cg);
			}
			
			ArrayList<CompoundWithGrouping> gridCompounds = item.getGridCompounds();
			gridCompounds.ensureCapacity(groupings.size());
			for (CurveGrouping grouping: groupings) {
				CompoundWithGrouping gridCompound = new CompoundWithGrouping(item.getCompound(), grouping);
				gridCompounds.add(gridCompound);
				compounds.add(gridCompound);
			}
			addItem(item);
		}
	}
	
	
	public boolean isEmpty() {
		return (protocol == null);
	}
	
	public Protocol getProtocol() {
		return protocol;
	}
	
	public List<Feature> getFeatures() {
		return features;
	}
	
	/**
	 * Returns a list with all grid compounds (row elements).
	 * @return a list with all compounds
	 */
	public List<CompoundWithGrouping> getGridCompounds() {
		return this.compounds;
	}
	
	/**
	 * Returns a list with the grid compounds (row elements) for the specified compound.
	 * @return a list with the compounds (empty, if the compound is not part of this input)
	 */
	public List<CompoundWithGrouping> getGridCompounds(Compound compound) {
		List<ICompoundItem> items = compoundItems.get(new Key(compound));
		if (items == null || items.isEmpty()) return Collections.emptyList();
		
		if (compound instanceof CompoundWithGrouping) {
			CompoundWithGrouping groupingCompound = (CompoundWithGrouping)compound;
			ICompoundItem item = getItem(groupingCompound.getDelegate());
			if (item != null) {
				for (CompoundWithGrouping gridCompound : item.getGridCompounds()) {
					if (gridCompound.getGrouping().equals(groupingCompound.getGrouping())) {
						return Collections.singletonList(gridCompound);
					}
				}
			}
			return Collections.emptyList();
		}
		else {
			ICompoundItem item = getItem(compound);
			if (item != null) {
				return item.getGridCompounds();
			}
			return Collections.emptyList();
		}
	}
	
	private ICompoundItem getItem(Compound compound) {
		List<ICompoundItem> items = compoundItems.get(new Key(compound));
		if (items != null) {
			if (compound instanceof MultiploCompound) {
				MultiploCompound multiploCompound = (MultiploCompound)compound;
				for (ICompoundItem item : items) {
					if (item.includesM(multiploCompound)) {
						return item;
					}
				}
			} else {
				for (ICompoundItem item : items) {
					if (item.includesV(compound)) {
						return item;
					}
				}
			}
		}
		return null;
	}
	
}
