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
	
	private ICache cache;
	
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
		cache = CacheService.getInstance().createCache("CompoundInfo");
	}
	
	public static CompoundInfoService getInstance() {
		return instance;
	}
	
	public String getSaltform(Compound compound) {
		Plate plate = compound.getPlate();
		SecurityService.getInstance().checkWithException(Permissions.PLATE_OPEN, plate);
		
		// First check the cache.
		CacheKey key = getSaltformCacheKey(compound);
		if (cache.contains(key)) return (String) cache.get(key);

		// Not cached: query whole plate via a provider
		for (Compound c: plate.getCompounds()) cache.put(getSaltformCacheKey(c), null);
		
		Map<String, List<Compound>> compoundsByType = PlateService.streamableList(plate.getCompounds())
				.stream().collect(Collectors.groupingBy(Compound::getType));
		
		for (String compoundType: compoundsByType.keySet()) {
			ICompoundInfoProvider provider = getProvider(compoundType, ICompoundInfoProvider.CAP_SALTFORM);
			if (provider == null) continue;
			
			List<Compound> compounds = compoundsByType.get(compoundType);
			List<String> saltforms = provider.getSaltforms(compounds);
			for (int i = 0; i < compounds.size(); i++) {
				cache.put(getSaltformCacheKey(compounds.get(i)), saltforms.get(i));
			}
		}

		return (String) cache.get(key);
	}
	
	public CompoundInfo getInfo(Compound compound) {
		Plate plate = compound.getPlate();
		SecurityService.getInstance().checkWithException(Permissions.PLATE_OPEN, plate);
		
		// First check the cache.
		CacheKey key = getInfoCacheKey(compound);
		if (cache.contains(key)) {
			CompoundInfo info = (CompoundInfo) cache.get(key);
			if (info == null) info = BLANK_INFO;
			return info;
		}

		// Not cached: query whole plate via a provider
		for (Compound c: plate.getCompounds()) cache.put(getInfoCacheKey(c), BLANK_INFO);
		
		Map<String, List<Compound>> compoundsByType = PlateService.streamableList(plate.getCompounds())
				.stream().collect(Collectors.groupingBy(Compound::getType));
		
		for (String compoundType: compoundsByType.keySet()) {
			ICompoundInfoProvider provider = getProvider(compoundType, ICompoundInfoProvider.CAP_FULL_INFO);
			if (provider == null) continue;
			
			List<Compound> compounds = compoundsByType.get(compoundType);
			List<CompoundInfo> infos = provider.getInfo(compounds);
			for (int i = 0; i < compounds.size(); i++) {
				cache.put(getInfoCacheKey(compounds.get(i)), infos.get(i));
			}
		}

		CompoundInfo info = (CompoundInfo) cache.get(key);
		if (info == null) info = BLANK_INFO;
		return info;
	}
	
	private ICompoundInfoProvider getProvider(String compoundType, int capability) {
		for (ICompoundInfoProvider provider: providers) {
			if (provider.getSupportedCompoundTypes(capability).contains(compoundType)) return provider;
		}
		return null;
	}
	
	private CacheKey getSaltformCacheKey(Compound compound) {
		return CacheKey.create("CompoundSaltform", compound.getType(), compound.getNumber());
	}
	
	private CacheKey getInfoCacheKey(Compound compound) {
		return CacheKey.create("CompoundInfo", compound.getType(), compound.getNumber());
	}
}
