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

public class SiloProvider implements IElementProvider {

	public static final String GROUP_ID_SILOS = "silos";
	public static final String GROUP_ID_WELL_SILOS = "wellSilos";
	public static final String GROUP_ID_SW_SILOS = "swSilos";
	public static final String GROUP_ID_PUBLIC_SILOS = "publicSilos";
	public static final String GROUP_ID_PRIVATE_SILOS = "privateSilos";
	
	public static final String GROUP_PREFIX_PCLASS = "pClass-";

	public static final String ELEMENT_PREFIX_SILO = "silo-";

	private Map<String, List<Silo>> tempCache = new HashMap<>();

	@Override
	public IElement[] getChildren(IGroup parent) {
		if (parent == NavigatorContentProvider.ROOT_GROUP) {
			IElement[] elements = new IElement[1];
			elements[0] = new Group("Silos", GROUP_ID_SILOS, parent.getId());
			return elements;
		} else if (parent.getId().equals(GROUP_ID_SILOS)) {
			// Clear cached Silos.
			tempCache.clear();

			Group mySilosGroup = new Group("My Silos", GROUP_ID_PRIVATE_SILOS, GROUP_ID_SILOS);
			mySilosGroup.setImageDescriptor(IconManager.getIconDescriptor("user.png"));
			Group publicSilosGroup = new Group("Public Silos", GROUP_ID_PUBLIC_SILOS, GROUP_ID_SILOS);
			publicSilosGroup.setImageDescriptor(IconManager.getIconDescriptor("group.png"));
			return new IElement[] { mySilosGroup, publicSilosGroup };
		} else if (parent.getId().equals(GROUP_ID_PRIVATE_SILOS) || parent.getId().equals(GROUP_ID_PUBLIC_SILOS)) {
			return getDataTypeGroups(parent.getId());
		} else if (parent.getId().equals(GROUP_ID_WELL_SILOS)) {
			return getProtocolClasses(parent, GroupType.WELL);
		} else if (parent.getId().equals(GROUP_ID_SW_SILOS)) {
			return getProtocolClasses(parent, GroupType.SUBWELL);
		} else if (parent.getId().startsWith(GROUP_PREFIX_PCLASS)) {
			if (parent.getParentId().equals(GROUP_ID_WELL_SILOS)) {
				return getSilos(parent, GroupType.WELL);
			} else if (parent.getParentId().equals(GROUP_ID_SW_SILOS)) {
				return getSilos(parent, GroupType.SUBWELL);
			}
		}

		return null;
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

		if (allSilos != null) {
			for (int i = 0; i<allSilos.size(); i++) {
				Silo s = allSilos.get(i);
				if (s.getProtocolClass().equals(parent.getData())) {
					silos.add(s);
				}
			}
		}

		int index = 0;
		IElement[] elements = new IElement[silos.size()];
		for (Silo s : silos) {
			elements[index] = new Element(s.getName(), ELEMENT_PREFIX_SILO + s.getId(), parent.getId(), icon);
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

		int index = 0;
		IElement[] elements = new IElement[pClasses.size()];
		for (ProtocolClass pClass : pClasses) {
			elements[index] = new Group(pClass.getName(), GROUP_PREFIX_PCLASS + pClass.getId(), parent.getId());
			((Group)elements[index]).setData(pClass);
			((Group)elements[index]).setImageDescriptor(IconManager.getIconDescriptor("struct.png"));
			index++;
		}
		return elements;
	}

	private List<Silo> getSilos(String superParent, GroupType type) {
		List<Silo> silos = new ArrayList<>();
		if (superParent.equals(GROUP_ID_PRIVATE_SILOS)) {
			silos = tempCache.get(GROUP_ID_PRIVATE_SILOS + type);
		}
		if (superParent.equals(GROUP_ID_PUBLIC_SILOS)) {
			silos = tempCache.get(GROUP_ID_PUBLIC_SILOS + type);
		}
		return silos;
	}

	private IElement[] getDataTypeGroups(String parentId) {
		boolean hasWell = false;
		boolean hasSubWell = false;
		
		if (parentId.equals(GROUP_ID_PRIVATE_SILOS)) {
			List<Silo> wellSilos = SiloService.getInstance().getPrivateSilos(GroupType.WELL);
			List<Silo> subwellSilos = SiloService.getInstance().getPrivateSilos(GroupType.SUBWELL);
			tempCache.put(GROUP_ID_PRIVATE_SILOS + GroupType.WELL, wellSilos);
			tempCache.put(GROUP_ID_PRIVATE_SILOS + GroupType.SUBWELL, subwellSilos);

			hasWell = !wellSilos.isEmpty();
			hasSubWell = !subwellSilos.isEmpty();
		} else if (parentId.equals(GROUP_ID_PUBLIC_SILOS)) {
			List<Silo> wellSilos = SiloService.getInstance().getPublicSilos(GroupType.WELL);
			List<Silo> subwellSilos = SiloService.getInstance().getPublicSilos(GroupType.SUBWELL);
			tempCache.put(GROUP_ID_PUBLIC_SILOS + GroupType.WELL, wellSilos);
			tempCache.put(GROUP_ID_PUBLIC_SILOS + GroupType.SUBWELL, subwellSilos);

			hasWell = !wellSilos.isEmpty();
			hasSubWell = !subwellSilos.isEmpty();
		}

		int size = 0;
		if (hasSubWell) size++;
		if (hasWell) size++;

		IElement[] elements = new IElement[size];
		if (hasSubWell) elements[--size] = new Group("Subwell", GROUP_ID_SW_SILOS, parentId);
		if (hasWell) elements[--size] = new Group("Well", GROUP_ID_WELL_SILOS, parentId);
		return elements;
	}

}