package eu.openanalytics.phaedra.base.ui.util.split;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;

public class SplitCompositeFactory {

	private static SplitCompositeFactory instance;
	
	private int mode = SplitComposite.MODE_H_1_2;
	
	private SplitCompositeFactory() {
		// Private constructor
	}
	
	public static SplitCompositeFactory getInstance() {
		if (instance == null) instance = new SplitCompositeFactory();
		return instance;
	}

	public SplitCompositeFactory prepare(IMemento memento, int defaultMode) {
		if (memento != null) {
			Integer savedMode = memento.getInteger("splitcomp_mode");
			if (savedMode != null) mode = savedMode;
			else mode = defaultMode;
		} else {
			mode = defaultMode;
		}
		return this;
	}
	
	public SplitComposite create(Composite parent) {
		return create(parent, false);
	}
	
	public SplitComposite create(Composite parent, boolean border) {
		return new SplitComposite(parent, mode, border);
	}
}
