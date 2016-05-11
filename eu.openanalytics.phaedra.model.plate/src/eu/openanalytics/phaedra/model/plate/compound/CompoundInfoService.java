package eu.openanalytics.phaedra.model.plate.compound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

import eu.openanalytics.phaedra.base.cache.CacheKey;
import eu.openanalytics.phaedra.base.cache.CacheService;
import eu.openanalytics.phaedra.base.cache.ICache;
import eu.openanalytics.phaedra.base.security.SecurityService;
import eu.openanalytics.phaedra.base.security.model.Permissions;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;

public class CompoundInfoService {

	private static CompoundInfoService instance = new CompoundInfoService();
	
	private static final CompoundInfo BLANK_INFO = new CompoundInfo();
	
	private List<ICompoundInfoProvider> providers;
	
	private CompoundInfoService() {
		providers = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ICompoundInfoProvider.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				ICompoundInfoProvider provider = (ICompoundInfoProvider) el.createExecutableExtension(ICompoundInfoProvider.ATTR_CLASS);
				providers.add(provider);
			} catch (CoreException e) {
				// Ignore invalid extensions.
			}
		}
	}
	
	public static CompoundInfoService getInstance() {
		return instance;
	}
	
	public CompoundInfo getInfo(Compound compound) {
		Plate plate = compound.getPlate();
		SecurityService.getInstance().checkWithException(Permissions.PLATE_OPEN, plate);
		
		// First check the cache.
		ICache compoundInfoCache = CacheService.getInstance().getDefaultCache();
		CompoundInfo info = (CompoundInfo) compoundInfoCache.get(getCacheKey(compound));
		if (info != null) return info;

		for (Compound c: plate.getCompounds()) compoundInfoCache.put(getCacheKey(c), BLANK_INFO);
		
		Map<String, List<Compound>> compoundsByType = PlateService.streamableList(plate.getCompounds())
				.stream().collect(Collectors.groupingBy(Compound::getType));
		List<String> typesToQuery = new ArrayList<>(compoundsByType.keySet());
		for (ICompoundInfoProvider provider: providers) {
			if (typesToQuery.isEmpty()) break;
			List<String> supportedTypes = provider.getSupportedCompoundTypes()
					.stream().filter(t -> typesToQuery.contains(t)).collect(Collectors.toList());
			if (supportedTypes.isEmpty()) continue;
			
			for (String type: supportedTypes) {
				List<Compound> compounds = compoundsByType.get(type);
				List<CompoundInfo> infos = provider.getInfo(compounds);
				for (int i = 0; i < compounds.size(); i++) {
					compoundInfoCache.put(getCacheKey(compounds.get(i)), infos.get(i));
				}
			}
			
			typesToQuery.removeAll(supportedTypes);
		}

		info = (CompoundInfo) compoundInfoCache.get(getCacheKey(compound));
		if (info != null) return info;
		else return BLANK_INFO;
	}
	
	private CacheKey getCacheKey(Compound compound) {
		return CacheKey.create("CompoundInfo", compound.getType(), compound.getNumber());
	}
}
