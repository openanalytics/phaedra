package eu.openanalytics.phaedra.ui.plate.calculation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.calculation.formula.FormulaService;
import eu.openanalytics.phaedra.calculation.formula.model.FormulaRuleset;
import eu.openanalytics.phaedra.calculation.formula.model.RulesetType;
import eu.openanalytics.phaedra.calculation.outlier.OutlierDetectionService;
import eu.openanalytics.phaedra.model.plate.PlateService;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.util.ProtocolUtils;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.Activator;
import eu.openanalytics.phaedra.validation.ValidationJobHelper;
import eu.openanalytics.phaedra.validation.ValidationService.Action;

public class OutlierDetectionHandler extends AbstractHandler {
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		List<Plate> plates = SelectionUtils.getObjects(selection, Plate.class);
		if (plates.isEmpty()) plates = SelectionUtils.getObjects(selection, Experiment.class).stream()
				.flatMap(e -> PlateService.getInstance().getPlates(e).stream())
				.collect(Collectors.toList());
		if (!plates.isEmpty()) execute(plates);
		return null;
	}
	
	public static boolean execute(List<Plate> plates) {
		if (plates == null || plates.isEmpty()) return false;
		
		ProtocolClass pClass = ProtocolUtils.getProtocolClass(plates.get(0));
		List<FormulaRuleset> rulesets = FormulaService.getInstance()
				.getRulesetsForProtocolClass(pClass.getId(), RulesetType.OutlierDetection.getCode())
				.values().stream().collect(Collectors.toList());
		
		List<Well> outlierWells = plates.stream()
				.flatMap(p -> PlateService.streamableList(p.getWells()).stream())
				.filter(w -> {
					int wellNr = PlateUtils.getWellNr(w);
					for (FormulaRuleset rs: rulesets) {
						boolean[] outliers = OutlierDetectionService.getInstance().getOutliers(w.getPlate(), rs.getFeature());
						if (outliers[wellNr - 1]) return true;
					}
					return false;
				}).collect(Collectors.toList());
		if (outlierWells.isEmpty()) return false;
		
		Map<Object, Long> outliersPerPlate = outlierWells
				.stream()
				.collect(Collectors.groupingBy(w -> w.getPlate().getBarcode(), Collectors.counting()));
		
		OutlierDetectionDialog dialog = new OutlierDetectionDialog(Display.getCurrent().getActiveShell(), plates, outlierWells) {
			@Override
			protected void okPressed() {
				try {
					//PHA-653: UR-016: HTS Outlier Detection improvements
					String remark = "Rejected by outlier detection rules";
					StringBuilder message = new StringBuilder();
					message.append(String.format("Are you sure you want to auto-reject %d wells with the reason %s? %s", outlierWells.size(), remark, "\n"));
					message.append("\n");
					for (Object plateBC : outliersPerPlate.keySet()) {
						message.append(String.format("For plate %s auto-reject %d wells %s", plateBC, outliersPerPlate.get(plateBC), "\n"));
					}
					
					boolean confirmed = MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Reject Detected Outliers", message.toString());
					if (confirmed) ValidationJobHelper.doInJob(Action.REJECT_OUTLIER_WELL, remark, outlierWells);
					
					super.okPressed();
				}
				catch (Exception e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to perform outlier detection", e),
							StatusManager.SHOW | StatusManager.LOG | StatusManager.BLOCK);
				}
			}
		};
		return (dialog.open() == Dialog.OK);
	}
}
