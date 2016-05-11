package eu.openanalytics.phaedra.ui.plate.chart.r;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import eu.openanalytics.phaedra.base.ui.util.copy.CopyableDecorator;
import eu.openanalytics.phaedra.base.ui.util.pinning.SelectionHandlingDecorator;
import eu.openanalytics.phaedra.base.ui.util.view.DecoratedView;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.plate.vo.Experiment;

public abstract class BaseExperimentChartRView extends DecoratedView {

	protected ISelectionListener selectionListener;

	protected Label plotLbl;

	protected Experiment currentSelection;

	@Override
	public void createPartControl(Composite parent) {
		plotLbl.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				Point p = plotLbl.getParent().getSize();
				if (p.x > 0 && p.y > 0) {
					createPlot();
				}
			}
		});

		selectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part == BaseExperimentChartRView.this) return;
				Experiment experiment = SelectionUtils.getFirstObject(selection, Experiment.class);
				if (experiment != null && !experiment.equals(currentSelection)) {
					currentSelection = experiment;
					featureSelection();
					createPlot();
				}
			}
		};
		getSite().getPage().addSelectionListener(selectionListener);

		addDecorator(new SelectionHandlingDecorator(selectionListener));
		addDecorator(new CopyableDecorator());
		initDecorators(parent);

		SelectionUtils.triggerActiveSelection(selectionListener);
	}

	@Override
	public void setFocus() {
		plotLbl.getParent().setFocus();
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(selectionListener);
		super.dispose();
	}

	protected abstract void createPlot();

	protected void featureSelection() {

	}

}