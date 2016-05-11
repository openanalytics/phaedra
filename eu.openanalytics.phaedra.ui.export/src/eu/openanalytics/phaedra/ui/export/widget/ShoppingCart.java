package eu.openanalytics.phaedra.ui.export.widget;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;

public class ShoppingCart extends Composite {

	private Button addButton;
	private Button removeButton;
	private List list;
	
	private AddItemAction action;
	
	public ShoppingCart(Composite parent, int style) {
		super(parent, style);
		
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(this);

		Composite btnContainer = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false,false).applyTo(btnContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0,0).applyTo(btnContainer);
		
		addButton = new Button(btnContainer, SWT.PUSH);
		addButton.setImage(IconManager.getIconImage("add.png"));
		addButton.setToolTipText("Add");
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (action != null) {
					String name = action.getName();
					Object value = action.getValue();
					list.add(name);
					list.setData(name, value);
				}
			}
		});
		GridDataFactory.fillDefaults().applyTo(addButton);
		
		removeButton = new Button(btnContainer, SWT.PUSH);
		removeButton.setImage(IconManager.getIconImage("delete.png"));
		removeButton.setToolTipText("Remove");
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int[] indices = list.getSelectionIndices();
				list.remove(indices);
			}
		});
		GridDataFactory.fillDefaults().applyTo(removeButton);
		
		list = new List(this, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridDataFactory.fillDefaults().hint(SWT.DEFAULT,70).grab(true,false).applyTo(list);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		addButton.setEnabled(enabled);
		removeButton.setEnabled(enabled);
		list.setEnabled(enabled);
		super.setEnabled(enabled);
	}
	
	public void setAddItemAction(AddItemAction action) {
		this.action = action;
	}
	
	public String[] getItemNames() {
		return list.getItems();
	}
	
	public Object[] getItemValues() {
		String[] names = list.getItems();
		Object[] values = new Object[names.length];
		for (int i=0; i<names.length; i++) {
			values[i] = list.getData(names[i]);
		}
		return values;
	}
	
	public static abstract class AddItemAction {
		public abstract String getName();
		public abstract Object getValue();
	}
}
