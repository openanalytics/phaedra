package eu.openanalytics.phaedra.model.plate.util;

import java.util.ArrayList;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;

public class ObjectCopyFactory {

	public static void copySettings(Plate from, Plate to, boolean copyIds, boolean copyApproval) {
		if (copyIds) to.setId(from.getId());
		to.setRows(from.getRows());
		to.setColumns(from.getColumns());
		to.setExperiment(from.getExperiment());
		to.setBarcode(from.getBarcode());
		to.setBarcodeSource(from.getBarcodeSource());
		to.setSequence(from.getSequence());
		to.setDescription(from.getDescription());
		to.setInfo(from.getInfo());
		to.setImageAvailable(from.isImageAvailable());
		to.setImagePath(from.getImagePath());
		to.setSubWellDataAvailable(from.isSubWellDataAvailable());
		to.setCalculationStatus(from.getCalculationStatus());
		to.setCalculationDate(from.getCalculationDate());
		to.setCalculationError(from.getCalculationError());
		if (copyApproval) {
			to.setValidationStatus(from.getValidationStatus());
			to.setValidationDate(from.getValidationDate());
			to.setValidationUser(from.getValidationUser());
			to.setApprovalStatus(from.getApprovalStatus());
			to.setApprovalDate(from.getApprovalDate());
			to.setApprovalUser(from.getApprovalUser());
			to.setUploadStatus(from.getUploadStatus());
			to.setUploadDate(from.getUploadDate());
			to.setUploadUser(from.getUploadUser());
		}
		
		if (from.getWells() != null && to.getWells() != null) {
			for (Well fromWell: from.getWells()) {
				Well toWell = PlateUtils.getWell(to, fromWell.getRow(), fromWell.getColumn());
				if (toWell == null) {
					toWell = new Well();
					toWell.setPlate(to);
					to.getWells().add(toWell);
				}
				copySettings(fromWell, toWell, copyIds);
			}
		}
		
		if (from.getCompounds() != null) {
			if (to.getCompounds() == null) to.setCompounds(new ArrayList<>());
			for (Compound fromCompound: from.getCompounds()) {
				Compound toCompound = PlateUtils.getCompound(to, fromCompound.getType(), fromCompound.getNumber());
				if (toCompound == null) {
					toCompound = new Compound();
					toCompound.setPlate(to);
					to.getCompounds().add(toCompound);
				}
				copySettings(fromCompound, toCompound, copyIds);
			}
		}
		
		for (Well well: to.getWells()) {
			Well fromWell = PlateUtils.getWell(from, well.getRow(), well.getColumn());
			if (fromWell == null) continue;
			Compound fromCompound = fromWell.getCompound();
			if (fromCompound == null) continue;
			Compound compound = PlateUtils.getCompound(to, fromCompound.getType(), fromCompound.getNumber());
			if (compound == null) continue;
			
			well.setCompound(compound);
			if (compound.getWells() == null) compound.setWells(new ArrayList<>());
			compound.getWells().add(well);
		}
	}
	
	private static void copySettings(Well from, Well to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setRow(from.getRow());
		to.setColumn(from.getColumn());
		to.setDescription(from.getDescription());
		to.setStatus(from.getStatus());
		to.setWellType(from.getWellType());
		to.setCompoundConcentration(from.getCompoundConcentration());
	}
	
	private static void copySettings(Compound from, Compound to, boolean copyIds) {
		if (copyIds) to.setId(from.getId());
		to.setType(from.getType());
		to.setNumber(from.getNumber());
		to.setSaltform(from.getSaltform());
		to.setDescription(from.getDescription());
		to.setValidationStatus(from.getValidationStatus());
		to.setValidationDate(from.getValidationDate());
		to.setValidationUser(from.getValidationUser());
	}
}
