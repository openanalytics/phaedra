package eu.openanalytics.phaedra.base.ui.util.view;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface IDecoratedPart {

	public abstract void addDecorator(PartDecorator decorator);

	public abstract void removeDecorator(PartDecorator decorator);

	public abstract <E extends PartDecorator> E hasDecorator(Class<E> decoratorClass);

	public abstract void initDecorators(Composite parent, Control... ctxMenuControl);

	public abstract void dispose();

	@SuppressWarnings({ "rawtypes" })
	public abstract Object getAdapter(Class adapter);

}