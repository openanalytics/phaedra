package eu.openanalytics.phaedra.base.ui.util.copy;

import org.eclipse.core.expressions.PropertyTester;

import eu.openanalytics.phaedra.base.ui.util.view.IDecoratedPart;

public class CopyableTester extends PropertyTester {

	private static final String COPYABLE = "copyable";

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (COPYABLE.equals(property)) {
			if (receiver instanceof ICopyable) return true;
			else if (receiver instanceof IDecoratedPart) {
				IDecoratedPart view = (IDecoratedPart) receiver;
				CopyableDecorator decorator = view.hasDecorator(CopyableDecorator.class);
				if (decorator != null) return true;
			}
		}
		return false;
	}
}