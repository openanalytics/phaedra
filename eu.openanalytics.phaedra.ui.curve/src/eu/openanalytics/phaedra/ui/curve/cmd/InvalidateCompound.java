package eu.openanalytics.phaedra.ui.curve.cmd;

import java.util.List;

import org.eclipse.swt.widgets.Display;

import eu.openanalytics.phaedra.model.plate.vo.Compound;
import eu.openanalytics.phaedra.validation.dialog.InvalidateCompoundsDialog;

public class InvalidateCompound extends AbstractCompoundCmd {

	@Override
	protected void execInternal(List<Compound> compounds) {
		new InvalidateCompoundsDialog(Display.getDefault().getActiveShell(), compounds).open();
	}
}
