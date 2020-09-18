package eu.openanalytics.phaedra.link.platedef.template.link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.base.util.misc.NumberUtils;
import eu.openanalytics.phaedra.base.util.misc.StringUtils;
import eu.openanalytics.phaedra.calculation.annotation.AnnotationService;
import eu.openanalytics.phaedra.link.platedef.PlateDefinitionService;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkException;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettings;
import eu.openanalytics.phaedra.link.platedef.link.PlateLinkSettingsDialog;
import eu.openanalytics.phaedra.link.platedef.source.AbstractDefinitionSource;
import eu.openanalytics.phaedra.link.platedef.template.PlateTemplate;
import eu.openanalytics.phaedra.link.platedef.template.WellTemplate;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.model.protocol.vo.WellType;

public class TemplateLinkSource extends AbstractDefinitionSource {

	@Override
	public boolean requiresSettings() {
		return true;
	}
	
	@Override
	public boolean isAvailable() {
		return true;
	}
	
	@Override
	public PlateLinkSettingsDialog createSettingsDialog(Shell parentShell, List<Plate> plates) {
		return new TemplateLinkSettingsDialog(parentShell, plates);
	}
	
	@Override
	public PlateLinkSettings getDefaultSettings(Plate plate) {
		// Take the protocol class default template, unmodified.
		PlateLinkSettings settings = new PlateLinkSettings();
		ProtocolClass pClass = plate.getExperiment().getProtocol().getProtocolClass();
		String id = pClass.getDefaultTemplate();
		try {
			if (PlateDefinitionService.getInstance().getTemplateManager().exists(id)) {
				PlateTemplate template = PlateDefinitionService.getInstance().getTemplateManager().getTemplate(id);
				settings.getSettings().put("template", template);
			} else {
				throw new RuntimeException("Template not found: " + id);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return settings;
	}

	@Override
	public String test(PlateLinkSettings settings) throws PlateLinkException {
		try {
			applyTemplate(settings, true);
		} catch (Exception e) {
			throw new PlateLinkException("Test failed: " + e.getMessage(), e);
		}
		return "Test successful";
	}
	
	@Override
	protected String doLink(PlateLinkSettings settings) throws PlateLinkException {
		try {
			applyTemplate(settings, false);
		} catch (Exception e) {
			throw new PlateLinkException("Link failed: " + e.getMessage(), e);
		}
		return "Linked";
	}
	
	private void applyTemplate(PlateLinkSettings settings, boolean test) throws IOException, PlateLinkException {

		PlateTemplate template = (PlateTemplate)settings.getSettings().get("template");
		if (template == null) throw new PlateLinkException("No template provided");
		
		if (test) return;
		
		Plate plate = settings.getPlate();
		
		Function<Well,WellTemplate> templateGetter = well -> {
			int wellNr = NumberUtils.getWellNr(well.getRow(), well.getColumn(), template.getColumns());
			WellTemplate wellTemplate = template.getWells().get(wellNr);
			return wellTemplate;
		};
		
		// First (before modifying the plate in any way), check if any new welltypes should be created.
		List<WellType> knownTypes = new ArrayList<>(ProtocolService.getInstance().getWellTypes());
		for (Well well: plate.getWells()) {
			WellTemplate wellTemplate = templateGetter.apply(well);
			if (wellTemplate == null || wellTemplate.isSkip()) continue;
			
			String type = wellTemplate.getWellType();
			if (type == null || type.isEmpty()) continue;
			
			WellType typeMatch = knownTypes.stream().filter(t -> t.getCode().equals(type)).findAny().orElse(null);
			if (typeMatch == null) {
				typeMatch = ProtocolService.getInstance().createWellType(type);
				knownTypes.add(typeMatch);
			}
		}
		
		AnnotationService.getInstance().applyAnnotations(plate,
				well -> templateGetter.apply(well).getAnnotations().keySet(),
				(well, ann) -> templateGetter.apply(well).getAnnotations().get(ann)
		);
		
		String plateInfo = plate.getInfo();
		if (plateInfo == null) {
			plateInfo = "Linked with " + template.getId();
		} else if (plateInfo.contains("Linked with")) {
			int index = plateInfo.indexOf("Linked with");
			if (plateInfo.startsWith("Linked with")) {
				plateInfo = "Linked with " + template.getId();
			} else {
				plateInfo = plateInfo.substring(0,index)
					+ "Linked with " + template.getId();
			}
		} else {
			plateInfo += ", Linked with " + template.getId();
		}
		plateInfo = StringUtils.trim(plateInfo, 100);
		plate.setInfo(plateInfo);
		
		List<Compound> compounds = new ArrayList<>(plate.getCompounds());
		boolean compoundsModified = false;
		
		for (Well well: plate.getWells()) {
			WellTemplate wellTemplate = templateGetter.apply(well);
			
			if (wellTemplate == null || wellTemplate.isSkip()) continue;
			
			String wellType = wellTemplate.getWellType();
			if (wellType == null || wellType.isEmpty()) {
				wellType = WellType.EMPTY;
			}
			well.setWellType(wellType);
			
			double conc = 0;
			String concString = wellTemplate.getConcentration();
			if (NumberUtils.isDouble(concString)) {
				conc = Double.parseDouble(concString);
			}
			well.setCompoundConcentration(conc);

			String compType = wellTemplate.getCompoundType();
			String compNr = wellTemplate.getCompoundNumber();
			
			Compound existingComp = well.getCompound();
			Compound newComp = null;
			if (compType != null && !compType.isEmpty() && compNr != null && !compNr.isEmpty()) {
				newComp = compounds.stream()
						.filter(c -> compType.equals(c.getType()) && compNr.equals(c.getNumber())).findAny()
						.orElseGet(() -> PlateService.getInstance().createCompound(plate, compType, compNr));
			}
			
			if (newComp != existingComp) {
				if (existingComp != null) existingComp.getWells().remove(well);
				if (newComp != null) {
					compounds.add(newComp);
					newComp.getWells().add(well);
				}
				well.setCompound(newComp);
				compoundsModified = true;
			}
		}
		
		if (compoundsModified) PlateService.getInstance().saveCompounds(plate);
		PlateService.getInstance().updatePlate(plate);
	}
}
