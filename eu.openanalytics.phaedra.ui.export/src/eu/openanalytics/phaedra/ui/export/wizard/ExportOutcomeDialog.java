package eu.openanalytics.phaedra.ui.export.wizard;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;

import eu.openanalytics.phaedra.export.core.ExportSettings;

public class ExportOutcomeDialog extends MessageDialog {
	
	private final static int OPEN_ID = 0;
	private final static int CLOSE_ID = 1;
	
	private ExportSettings settings;
	
	protected ExportOutcomeDialog(Shell parentShell, ExportSettings settings) {
		super(parentShell,"Export Complete", null,
				generateMessage(settings), MessageDialog.INFORMATION,
				new String[]{"Open","Close"}, 0);
		this.settings = settings;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case OPEN_ID:
			openFile();
			setReturnCode(OK);
			close();
			break;
		case CLOSE_ID:
			setReturnCode(OK);
			close();
			break;
		}
	}
	
	private static String generateMessage(ExportSettings settings) {
		StringBuilder msg = new StringBuilder();
		msg.append("The data was successfully exported to the following location:");
		msg.append("\n\n"+settings.destinationPath);
		return msg.toString();
	}
	
	private void openFile() {
		Program.launch(settings.destinationPath);
	}
}
