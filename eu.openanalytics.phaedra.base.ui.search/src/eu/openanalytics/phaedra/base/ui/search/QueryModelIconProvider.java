package eu.openanalytics.phaedra.base.ui.search;

import org.eclipse.jface.resource.ImageDescriptor;

import eu.openanalytics.phaedra.base.search.model.QueryModel;
import eu.openanalytics.phaedra.base.ui.icons.AbstractIconProvider;
import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class QueryModelIconProvider extends AbstractIconProvider<QueryModel> {
	
	@Override
	public Class<QueryModel> getType() {
		return QueryModel.class;
	}

	@Override
	public ImageDescriptor getDefaultImageDescriptor() {
		return IconManager.getIconDescriptor("find.png");
	}

	@Override
	public ImageDescriptor getCreateImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getDeleteImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getUpdateImageDescriptor() {
		return null;
	}

}
