package eu.openanalytics.phaedra.ui.silo.navigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.navigator.NavigatorContentProvider;
import eu.openanalytics.phaedra.base.ui.navigator.model.Element;
import eu.openanalytics.phaedra.base.ui.navigator.model.Group;
import eu.openanalytics.phaedra.base.ui.navigator.model.IElement;
import eu.openanalytics.phaedra.base.ui.navigator.model.IGroup;
import eu.openanalytics.phaedra.base.ui.navigator.providers.IElementProvider;
import eu.openanalytics.phaedra.model.protocol.util.GroupType;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.silo.SiloService;
import eu.openanalytics.phaedra.silo.vo.Silo;
import eu.openanalytics.phaedra.silo.vo.SiloGroup;

public class SiloProvider implements IElementProvider {

	public static final String SILO = "silo-";
	public static final String GROUP = "group-";

	protected static final String SILOS = "silos";
	protected static final String PCLASS = "pClass-";
	protected static final String WELL_SILOS = "wSilos";
	protected static final String SUBWELL_SILOS = "swSilos";

	private static final String PUBLIC_SILOS = "publicSilos";
	private static final String PRIVATE_SILOS = "privateSilos";
	private static final String EXAMPLE_SILOS = "exampleSilos";

	private Map<String, List<Silo>> tempCache = new HashMap<>();
	private Map<String, List<SiloGroup>> tempGroupCache = new HashMap<>();

	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			IElement[] elements = new IElement[1];
			elements[0] = new Group("Silos", SILOS, parent.getId());
			return elements;
		}
		if (parent.getId().equals(SILOS)) {
			// Clear cached Silos.
			tempCache.clear();

			Group mySilosGroup = new Group("My Silos", PRIVATE_SILOS, SILOS);
			mySilosGroup.setImageDescriptor(IconManager.getIconDescriptor("user.png"));
			Group publicSilosGroup = new Group("Public Silos", PUBLIC_SILOS, SILOS);
			publicSilosGroup.setImageDescriptor(IconManager.getIconDescriptor("group.png"));
			Group exampleSilosGroup = new Group("Example Silos", EXAMPLE_SILOS, SILOS);
			exampleSilosGroup.setImageDescriptor(IconManager.getIconDescriptor("user_comment.png"));
			return new IElement[] { mySilosGroup, publicSilosGroup, exampleSilosGroup };
		}
		if (parent.getId().equals(PRIVATE_SILOS)
				|| parent.getId().equals(PUBLIC_SILOS)
				|| parent.getId().equals(EXAMPLE_SILOS)) {
			return getDataTypeGroups(parent.getId());
		}
		if (parent.getId().startsWith(WELL_SILOS)) {
			return getProtocolClasses(parent, GroupType.WELL);
		}
		if (parent.getId().startsWith(SUBWELL_SILOS)) {
			return getProtocolClasses(parent, GroupType.SUBWELL);
		}

		if (parent.getId().startsWith(PCLASS)) {
			if (parent.getParentId().startsWith(WELL_SILOS)) {
				return getSilos(parent, GroupType.WELL);
			}
			if (parent.getParentId().startsWith(SUBWELL_SILOS)) {
				return getSilos(parent, GroupType.SUBWELL);
			}
		}

		if (parent.getId().startsWith(GROUP)) {
			return getSilosByGroup(parent);
		}

		return null;
	}

	private IElement[] getSilosByGroup(IGroup parent) {
		SiloGroup group = (SiloGroup) parent.getData();
		Set<Silo> silos = group.getSilos();

		ImageDescriptor icon;
		if (group.getType() == GroupType.WELL.getType()) {
			icon = IconManager.getIconDescriptor("silo_well.png");
		} else {
			icon = IconManager.getIconDescriptor("silo_subwell.png");
		}

		int index = 0;
		IElement[] elements = new IElement[silos.size()];
		for (Silo s : silos) {
			elements[index] = new Element(s.getName(), SILO + s.getId(), parent.getId(), icon);
			((Element)elements[index]).setData(s);
			index++;
		}

		return elements;
	}

	private IElement[] getSilos(IGroup parent, GroupType type) {
		String superParent = parent.getParent().getParentId();

		ImageDescriptor icon;
		if (type == GroupType.WELL) {
			icon = IconManager.getIconDescriptor("silo_well.png");
		} else {
			icon = IconManager.getIconDescriptor("silo_subwell.png");
		}

		List<Silo> allSilos = getSilos(superParent, type);
		List<Silo> silos = new ArrayList<>();

		List<SiloGroup> allSiloGroups = getSiloGroups(superParent, type);
		List<SiloGroup> siloGroups = new ArrayList<>();

		if (allSilos != null) {
			for (int i = 0; i<allSilos.size(); i++) {
				Silo s = allSilos.get(i);
				if (s.getProtocolClass().equals(parent.getData())) {
					silos.add(s);
				}
			}
		}

		if (allSiloGroups != null) {
			for (int i = 0; i<allSiloGroups.size(); i++) {
				SiloGroup s = allSiloGroups.get(i);
				if (s.getProtocolClass().equals(parent.getData())) {
					siloGroups.add(s);
				}
			}
		}

		int index = 0;
		IElement[] elements = new IElement[silos.size() + siloGroups.size()];
		for (SiloGroup g : siloGroups) {
			elements[index] = new Group(g.getName(), GROUP + g.getId(), parent.getId());
			((Group)elements[index]).setData(g);
			index++;
		}
		for (Silo s : silos) {
			elements[index] = new Element(s.getName(), SILO + s.getId(), parent.getId(), icon);
			((Element)elements[index]).setData(s);
			index++;
		}
		return elements;
	}

	private IElement[] getProtocolClasses(IGroup parent, GroupType type) {
		String superParent = parent.getParent().getId();
		Set<ProtocolClass> pClasses = new HashSet<>();

		List<Silo> silos = getSilos(superParent, type);
		if (silos != null) {
			for (int i = 0; i<silos.size(); i++) {
				Silo s = silos.get(i);
				pClasses.add(s.getProtocolClass());
			}

		}

		List<SiloGroup> siloGroups = getSiloGroups(superParent, type);
		if (silos != null) {
			for (int i = 0; i<siloGroups.size(); i++) {
				SiloGroup s = siloGroups.get(i);
				pClasses.add(s.getProtocolClass());
			}

		}

		int index = 0;
		IElement[] elements = new IElement[pClasses.size()];
		for (ProtocolClass pClass : pClasses) {
			elements[index] = new Group(pClass.getName(), PCLASS + pClass.getId(), parent.getId());
			((Group)elements[index]).setData(pClass);
			((Group)elements[index]).setImageDescriptor(IconManager.getIconDescriptor("struct.png"));
			index++;
		}
		return elements;
	}

	private List<SiloGroup> getSiloGroups(String superParent, GroupType type) {
		List<SiloGroup> siloGroups = new ArrayList<>();
		if (superParent.equals(PRIVATE_SILOS)) {
			siloGroups = tempGroupCache.get(PRIVATE_SILOS + type);
		}
		if (superParent.equals(PUBLIC_SILOS)) {
			siloGroups = tempGroupCache.get(PUBLIC_SILOS + type);
		}
		if (superParent.equals(EXAMPLE_SILOS)) {
			siloGroups = tempGroupCache.get(EXAMPLE_SILOS + type);
		}
		return siloGroups;
	}

	private List<Silo> getSilos(String superParent, GroupType type) {
		List<Silo> silos = new ArrayList<>();
		if (superParent.equals(PRIVATE_SILOS)) {
			silos = tempCache.get(PRIVATE_SILOS + type);
		}
		if (superParent.equals(PUBLIC_SILOS)) {
			silos = tempCache.get(PUBLIC_SILOS + type);
		}
		if (superParent.equals(EXAMPLE_SILOS)) {
			silos = tempCache.get(EXAMPLE_SILOS + type);
		}

		return silos;
	}

	private IElement[] getDataTypeGroups(String parentId) {
		boolean hasWell = false;
		boolean hasSubWell = false;
		if (parentId.equals(PRIVATE_SILOS)) {
			List<Silo> wellSilos = SiloService.getInstance().getPrivateSilos(GroupType.WELL);
			List<Silo> subwellSilos = SiloService.getInstance().getPrivateSilos(GroupType.SUBWELL);
			tempCache.put(PRIVATE_SILOS + GroupType.WELL, wellSilos);
			tempCache.put(PRIVATE_SILOS + GroupType.SUBWELL, subwellSilos);

			List<SiloGroup> wellSiloGroups = SiloService.getInstance().getMyPrivateSiloGroups(GroupType.WELL);
			List<SiloGroup> subwellSiloGroups = SiloService.getInstance().getMyPrivateSiloGroups(GroupType.SUBWELL);
			tempGroupCache.put(PRIVATE_SILOS + GroupType.WELL, wellSiloGroups);
			tempGroupCache.put(PRIVATE_SILOS + GroupType.SUBWELL, subwellSiloGroups);

			hasWell = !wellSilos.isEmpty() || !wellSiloGroups.isEmpty();
			hasSubWell = !subwellSilos.isEmpty() || !subwellSiloGroups.isEmpty();
		}
		if (parentId.equals(PUBLIC_SILOS)) {
			List<Silo> wellSilos = SiloService.getInstance().getPublicSilos(GroupType.WELL);
			List<Silo> subwellSilos = SiloService.getInstance().getPublicSilos(GroupType.SUBWELL);
			tempCache.put(PUBLIC_SILOS + GroupType.WELL, wellSilos);
			tempCache.put(PUBLIC_SILOS + GroupType.SUBWELL, subwellSilos);

			List<SiloGroup> wellSiloGroups = SiloService.getInstance().getPublicSiloGroups(GroupType.WELL);
			List<SiloGroup> subwellSiloGroups = SiloService.getInstance().getPublicSiloGroups(GroupType.SUBWELL);
			tempGroupCache.put(PUBLIC_SILOS + GroupType.WELL, wellSiloGroups);
			tempGroupCache.put(PUBLIC_SILOS + GroupType.SUBWELL, subwellSiloGroups);

			hasWell = !wellSilos.isEmpty() || !wellSiloGroups.isEmpty();
			hasSubWell = !subwellSilos.isEmpty() || !subwellSiloGroups.isEmpty();
		}
		if (parentId.equals(EXAMPLE_SILOS)) {
			List<Silo> wellSilos = SiloService.getInstance().getExampleSilos(GroupType.WELL);
			List<Silo> subwellSilos = SiloService.getInstance().getExampleSilos(GroupType.SUBWELL);
			tempCache.put(EXAMPLE_SILOS + GroupType.WELL, wellSilos);
			tempCache.put(EXAMPLE_SILOS + GroupType.SUBWELL, subwellSilos);

			List<SiloGroup> wellSiloGroups = SiloService.getInstance().getExampleSiloGroups(GroupType.WELL);
			List<SiloGroup> subwellSiloGroups = SiloService.getInstance().getExampleSiloGroups(GroupType.SUBWELL);
			tempGroupCache.put(EXAMPLE_SILOS + GroupType.WELL, wellSiloGroups);
			tempGroupCache.put(EXAMPLE_SILOS + GroupType.SUBWELL, subwellSiloGroups);

			hasWell = !wellSilos.isEmpty() || !wellSiloGroups.isEmpty();
			hasSubWell = !subwellSilos.isEmpty() || !subwellSiloGroups.isEmpty();
		}

		int size = 0;
		if (hasSubWell) size++;
		if (hasWell) size++;

		IElement[] elements = new IElement[size];
		if (hasSubWell) elements[--size] = new Group("Subwell", SUBWELL_SILOS, parentId);
		if (hasWell) elements[--size] = new Group("Well", WELL_SILOS, parentId);
		return elements;
	}

}