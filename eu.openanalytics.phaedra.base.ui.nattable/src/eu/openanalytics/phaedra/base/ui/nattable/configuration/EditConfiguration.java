package eu.openanalytics.phaedra.base.ui.nattable.configuration;

import org.eclipse.nebula.widgets.nattable.config.AbstractLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.config.DefaultEditableRule;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.IEditableRule;
import org.eclipse.nebula.widgets.nattable.data.validate.DefaultDataValidator;
import org.eclipse.nebula.widgets.nattable.data.validate.IDataValidator;
import org.eclipse.nebula.widgets.nattable.edit.EditConfigAttributes;
import org.eclipse.nebula.widgets.nattable.edit.action.KeyEditAction;
import org.eclipse.nebula.widgets.nattable.edit.action.MouseEditAction;
import org.eclipse.nebula.widgets.nattable.edit.command.EditCellCommandHandler;
import org.eclipse.nebula.widgets.nattable.edit.config.DialogErrorHandling;
import org.eclipse.nebula.widgets.nattable.edit.editor.TextCellEditor;
import org.eclipse.nebula.widgets.nattable.edit.event.InlineCellEditEventHandler;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayer;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.CellEditorMouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.KeyEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.swt.SWT;

public class EditConfiguration extends AbstractLayerConfiguration<AbstractLayer> {

	private IDataValidator dataValidator;
	private IEditableRule editableRule;

	public EditConfiguration() {
		this(new DefaultDataValidator(), IEditableRule.ALWAYS_EDITABLE);
	}
	
	public EditConfiguration(IDataValidator dataValidator, IEditableRule editableRule) {
		this.dataValidator = dataValidator;
		this.editableRule = editableRule;
	}
	
	@Override
	public void configureTypedLayer(AbstractLayer layer) {
		layer.registerCommandHandler(new EditCellCommandHandler());
		layer.registerEventHandler(new InlineCellEditEventHandler(layer));
	}
	
	@Override
	public void configureRegistry(IConfigRegistry configRegistry) {
		// Make Cells editable.
		configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITABLE_RULE, editableRule);
		configRegistry.registerConfigAttribute(EditConfigAttributes.CELL_EDITOR, new TextCellEditor());
		
		configRegistry.registerConfigAttribute(EditConfigAttributes.DATA_VALIDATOR, dataValidator);
		
		// Dialogs for handling Conversion and Validation errors.
		configRegistry.registerConfigAttribute(EditConfigAttributes.CONVERSION_ERROR_HANDLER, new DialogErrorHandling(), DisplayMode.EDIT);
		configRegistry.registerConfigAttribute(EditConfigAttributes.VALIDATION_ERROR_HANDLER, new DialogErrorHandling(), DisplayMode.EDIT);
	}
	
	@Override
	public void configureUiBindings(UiBindingRegistry uiBindingRegistry) {
		// Do not register bindings if there is no real editing support
		// (this allows other configurations to register bindings such as double click)
		if (editableRule instanceof DefaultEditableRule) return;
		
		uiBindingRegistry.registerKeyBinding(new KeyEventMatcher(SWT.NONE, SWT.F2), new KeyEditAction());
		uiBindingRegistry.registerDoubleClickBinding(
				new CellEditorMouseEventMatcher(GridRegion.BODY, MouseEventMatcher.LEFT_BUTTON),
				new MouseEditAction());
	}
	
}