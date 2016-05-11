package eu.openanalytics.phaedra.base.ui.nattable.command;

import org.eclipse.nebula.widgets.nattable.command.AbstractContextFreeCommand;

public class TreeColumnExpandCollapseCommand extends AbstractContextFreeCommand {

	private final int groupByDepth;

	public TreeColumnExpandCollapseCommand(int parentIndex) {
		this.groupByDepth = parentIndex;
	}

	protected TreeColumnExpandCollapseCommand(TreeColumnExpandCollapseCommand command) {
		this.groupByDepth = command.groupByDepth;
	}

	public int getGroupByDepth() {
		return this.groupByDepth;
	}

}
