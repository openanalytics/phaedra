package eu.openanalytics.phaedra.base.ui.nattable.command;

import org.eclipse.nebula.widgets.nattable.command.AbstractLayerCommandHandler;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.layer.event.VisualRefreshEvent;

import ca.odell.glazedlists.TreeList;

public class TreeColumnExpandCollapseCommandHandler extends AbstractLayerCommandHandler<TreeColumnExpandCollapseCommand> {

	private GroupByDataLayer<?> layer;

	public TreeColumnExpandCollapseCommandHandler(GroupByDataLayer<?> layer) {
		this.layer = layer;
	}

	@Override
	public Class<TreeColumnExpandCollapseCommand> getCommandClass() {
		return TreeColumnExpandCollapseCommand.class;
	}

	@Override
	protected boolean doCommand(TreeColumnExpandCollapseCommand command) {
		int depth = command.getGroupByDepth();

		TreeList<?> treeList = layer.getTreeList();
		if (depth >= 0) {
			boolean isExpanding = isExpanding(depth, treeList);

			expandCollapseToLevel(depth, isExpanding);

			layer.fireLayerEvent(new VisualRefreshEvent(layer));
		}

		return true;
	}

	/**
	 * <p>Use the first row of given depth to see if it is expanding (currently collapsed).</p>
	 *
	 * @param depth
	 * @param treeList
	 * @return
	 */
	private boolean isExpanding(int depth, TreeList<?> treeList) {
		boolean isExpanding = true;

		for (int i = 0; i < treeList.size(); i++) {
			if (treeList.depth(i) == depth) {
				isExpanding = !treeList.isExpanded(i);
				break;
			}
		}

		return isExpanding;
	}

	private void expandCollapseToLevel(int depth, boolean isExpanding) {
        TreeList<?> treeList = layer.getTreeList();

        boolean actionPerformed = false;
        treeList.getReadWriteLock().writeLock().lock();
        try {
            // iterating directly over the TreeList is a lot faster than checking the nodes
            for (int i = (treeList.size() - 1); i >= 0; i--) {

            	boolean matchingDepth = false;
            	if (isExpanding) {
            		matchingDepth = treeList.depth(i) <= depth;
            	} else {
            		matchingDepth = treeList.depth(i) >= depth;
            	}

            	/*
            	 * Checks if the node at the given visible index has children,
            	 * is expandable and is on a level below the given level. If it
            	 * is it will be expanded otherwise skipped. This backwards
            	 * searching and expanding mechanism is necessary to ensure to
            	 * really get every expandable node in the whole tree structure.
            	 */
                if (treeList.hasChildren(i) && matchingDepth) {
                	if (treeList.isExpanded(i)) {
                		if (!isExpanding) {
                			treeList.setExpanded(i, false);
                			actionPerformed = true;
                		}
                	} else {
                		if (isExpanding) {
                			treeList.setExpanded(i, true);
                			actionPerformed = true;
                		}
                	}
                }
            }
        } finally {
            treeList.getReadWriteLock().writeLock().unlock();
        }

        // if at least one element was expanded we need to perform the step
        // again as we are only able to retrieve the visible nodes
        if (actionPerformed) {
        	expandCollapseToLevel(depth, isExpanding);
        }
    }

}