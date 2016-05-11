package eu.openanalytics.phaedra.ui.protocol.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import eu.openanalytics.phaedra.calculation.annotation.IAnnotationCreator;
import eu.openanalytics.phaedra.datacapture.DataCaptureService;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.dialog.CreateAnnotationsDialog;

public class UIAnnotationCreator implements IAnnotationCreator {

	@Override
	public int getPriority() {
		// Only applies if Phaedra is running in UI mode
		if (DataCaptureService.getInstance().isServerEnabled()) return 0;
		return 10;
	}

	@Override
	public void create(ProtocolClass pClass, Map<String, Set<String>> annotationsAndValues, Map<String, Boolean> annotationsNumeric) {
		List<String> names = new ArrayList<>(annotationsAndValues.keySet());
		List<Boolean> numeric = new ArrayList<>();
		Map<String, List<String>> values = new HashMap<>();
		for (int i = 0; i < names.size(); i++) {
			numeric.add(annotationsNumeric.get(names.get(i)));
		}
		for (String annotation: annotationsAndValues.keySet()) {
			List<String> v = new ArrayList<>(annotationsAndValues.get(annotation));
			v.sort(null);
			values.put(annotation, v);
		}
		Display.getDefault().syncExec(() -> {
			Shell shell = Display.getDefault().getActiveShell();
			if (shell == null) shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			CreateAnnotationsDialog dialog = new CreateAnnotationsDialog(shell, pClass, names, numeric, values);
			dialog.open();
		});
	}

}
