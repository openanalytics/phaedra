package eu.openanalytics.phaedra.ui.partsettings.decorator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.base.ui.util.toolitem.DropdownToolItemFactory;
import eu.openanalytics.phaedra.base.ui.util.view.PartDecorator;
import eu.openanalytics.phaedra.base.util.misc.Properties;
import eu.openanalytics.phaedra.base.util.misc.SelectionUtils;
import eu.openanalytics.phaedra.model.protocol.vo.Protocol;
import eu.openanalytics.phaedra.ui.partsettings.dialog.PartSettingsDialog;
import eu.openanalytics.phaedra.ui.partsettings.service.PartSettingsService;
import eu.openanalytics.phaedra.ui.partsettings.utils.PartSettingsUtils;
import eu.openanalytics.phaedra.ui.partsettings.vo.PartSettings;
import eu.openanalytics.phaedra.ui.user.SelectUserDialog;

public class SettingsDecorator extends PartDecorator {

	public static final String IS_PART_SETTINGS_TOOLITEM = "IS_PART_SETTINGS_TOOLITEM";

	// Currently still called "Saved views" to match Phaedra documentation.
	private static final String LOAD_SETTINGS = "Show available views";
	private static final String SAVE_SETTINGS = "Save view";
	private static final String SAVE_SETTINGS_AS = "Save as new view";
	private static final String SHARE_SETTINGS = "Share view";
	private static final String EDIT_SETTINGS = "Rename view";
	private static final String REMOVE_SETTINGS = "Remove view";

	private Supplier<Protocol> protocolSupplier;
	private Supplier<Properties> propertySaver;
	private Consumer<Properties> propertyLoader;

	private Optional<PartSettings> currentSettings;

	private MenuItem editMenuItem;
	private MenuItem shareMenuItem;
	private MenuItem removeMenuItem;

	public SettingsDecorator(Supplier<Properties> propertySaver, Consumer<Properties> propertyLoader) {
		this(null, propertySaver, propertyLoader);
	}

	public SettingsDecorator(Supplier<Protocol> protocolSupplier, Supplier<Properties> propertySaver
			, Consumer<Properties> propertyLoader) {

		if (protocolSupplier == null) protocolSupplier = getDefaultProtocolSupplier();

		this.protocolSupplier = protocolSupplier;
		this.propertySaver = propertySaver;
		this.propertyLoader = propertyLoader;
		this.currentSettings = Optional.empty();
	}

	@Override
	public void contributeToolbar(IToolBarManager manager) {
		manager.add(new ContributionItem() {
			@Override
			public void fill(ToolBar parent, int index) {
				fillToolBar(parent);
			}
			@Override
			public boolean isDynamic() {
				return true;
			}
		});
		super.contributeToolbar(manager);
	}

	public Optional<PartSettings> getCurrentSettings() {
		return currentSettings;
	}

	public void loadPartSettings(Optional<PartSettings> settings) {
		if (settings.isPresent()) {
			PartSettings partSettings = settings.get();
			Properties properties = partSettings.getProperties();
			propertyLoader.accept(properties);
		}
		setCurrentPartSettings(settings);
	}

	public PartSettings savePartSettings(String name) {
		Protocol protocol = protocolSupplier.get();
		String className = getWorkBenchPart().getClass().getName();
		Properties properties = propertySaver.get();
		PartSettings settings = PartSettingsService.getInstance().createPartSettings(protocol, className, properties);
		settings.setName(name);
		PartSettingsService.getInstance().updatePartSettings(settings);
		return settings;
	}
	
	/*
	 * Non-public
	 * **********
	 */
	
	private void fillToolBar(ToolBar parent) {
		if (parent.getItemCount() != 0) new ToolItem(parent, SWT.SEPARATOR);

		ToolItem toolItem = createSavedPartToolItem(parent);

		MenuItem item;
		item = DropdownToolItemFactory.createChild(toolItem, LOAD_SETTINGS, SWT.CASCADE);
		createLoadSettingsMenu(item);

		item = DropdownToolItemFactory.createChild(toolItem, SAVE_SETTINGS, SWT.PUSH);
		item.addListener(SWT.Selection, e -> saveSettings());
		item = DropdownToolItemFactory.createChild(toolItem, SAVE_SETTINGS_AS, SWT.PUSH);
		item.addListener(SWT.Selection, e -> saveSettingsAs());
		
		shareMenuItem = DropdownToolItemFactory.createChild(toolItem, SHARE_SETTINGS, SWT.PUSH);
		shareMenuItem.addListener(SWT.Selection, e -> shareSettings());
		editMenuItem = DropdownToolItemFactory.createChild(toolItem, EDIT_SETTINGS, SWT.PUSH);
		editMenuItem.addListener(SWT.Selection, e -> editSettings());
		removeMenuItem = DropdownToolItemFactory.createChild(toolItem, REMOVE_SETTINGS, SWT.PUSH);
		removeMenuItem.addListener(SWT.Selection, e -> removeSettings());

		refreshMenuItems();
	}

	private ToolItem createSavedPartToolItem(ToolBar parent) {
		ToolItem item = DropdownToolItemFactory.createDropdown(parent);
		item.setImage(IconManager.getIconImage("report.png"));
		item.setToolTipText("Settings");
		item.setData(IS_PART_SETTINGS_TOOLITEM, true);
		return item;
	}

	private void createLoadSettingsMenu(MenuItem item) {
		Menu loadMenu = new Menu(item);
		item.setMenu(loadMenu);

		loadMenu.addListener(SWT.Show, e -> {
			Arrays.stream(loadMenu.getItems()).forEach(oldItem -> oldItem.dispose());

			List<PartSettings> partSettings = getAvailablePartSettings();
			fillLoadMenu(loadMenu, partSettings);

			List<PartSettings> templateSettings = getAvailableTemplates();
			fillLoadMenu(loadMenu, templateSettings);

			if (loadMenu.getItemCount() == 0) {
				MenuItem settingsItem = new MenuItem(loadMenu, SWT.RADIO);
				settingsItem.setText("No Settings Available");
				settingsItem.setEnabled(false);
			}
		});
	}

	private void refreshMenuItems() {
		this.shareMenuItem.setEnabled(currentSettings.isPresent());
		this.editMenuItem.setEnabled(currentSettings.isPresent());
		this.removeMenuItem.setEnabled(currentSettings.isPresent());
	}
	
	private void fillLoadMenu(Menu loadMenu, List<PartSettings> partSettings) {
		if (loadMenu.getItemCount() != 0 && !partSettings.isEmpty()) new MenuItem(loadMenu, SWT.SEPARATOR);

		MenuItem settingsItem;
		for (PartSettings settings : partSettings) {
			settingsItem = new MenuItem(loadMenu, SWT.RADIO);
			settingsItem.setText(settings.getDisplayName());
			settingsItem.setSelection(currentSettings.isPresent() && currentSettings.get().getId() == settings.getId());
			settingsItem.addListener(SWT.Selection, event -> {
				if (!((MenuItem) event.widget).getSelection()) return;
				loadPartSettings(Optional.of(settings));
			});
		}
	}

	private void saveSettings() {
		if (currentSettings.isPresent()) {
			PartSettings partSettings = currentSettings.get();
			boolean confirm = MessageDialog.openQuestion(null, SAVE_SETTINGS
				, "Are you sure you want to overwrite " + partSettings.getName() + "?");

			if (confirm) {
				partSettings.setProperties(propertySaver.get());
				PartSettingsService.getInstance().updatePartSettings(partSettings);
			}
		} else {
			saveSettingsAs();
		}
	}

	private void saveSettingsAs() {
		Protocol protocol = protocolSupplier.get();
		String className = getWorkBenchPart().getClass().getName();
		Properties properties = propertySaver.get();
		PartSettings settings = PartSettingsService.getInstance().createPartSettings(protocol, className, properties);
		updateSettings(settings);
	}

	private void shareSettings() {
		if (!currentSettings.isPresent()) return;
		SelectUserDialog dialog = new SelectUserDialog(Display.getDefault().getActiveShell(), "Select a user to share the view with:");
		if (dialog.open() == Window.OK) {
			String username = dialog.getSelectedUser();
			PartSettingsService.getInstance().shareSettings(currentSettings.get(), username);
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), "View shared",
					"The view '" + currentSettings.get().getName() + "' has been shared with " + username + ".");
		}
	}
	
	private void editSettings() {
		if (currentSettings.isPresent()) updateSettings(currentSettings.get());
	}

	private void removeSettings() {
		if (currentSettings.isPresent()) {
			PartSettings partSettings = currentSettings.get();
			boolean confirm = MessageDialog.openConfirm(null, REMOVE_SETTINGS
				, "Are you sure you want to remove " + partSettings.getName() + "?");

			if (confirm) {
				PartSettingsService.getInstance().deletePartSettings(partSettings);
				setCurrentPartSettings(Optional.empty());
			}
		}
	}

	private void updateSettings(PartSettings partSettings) {
		String title = partSettings.getId() < 1 ? SAVE_SETTINGS_AS : EDIT_SETTINGS;
		PartSettingsDialog dialog = new PartSettingsDialog(null, title, partSettings);
		if (dialog.open() == Window.OK) {
			PartSettingsService.getInstance().updatePartSettings(partSettings);
			setCurrentPartSettings(Optional.of(partSettings));
		}
	}

	private void setCurrentPartSettings(Optional<PartSettings> currentSettings) {
		this.currentSettings = currentSettings;
		refreshMenuItems();
	}
	
	private List<PartSettings> getAvailablePartSettings() {
		Protocol protocol = protocolSupplier.get();
		String className = getWorkBenchPart().getClass().getName();
		List<PartSettings> partSettings = PartSettingsService.getInstance().getPartSettings(protocol, className);
		Collections.sort(partSettings, PartSettingsUtils.NAME_SORTER);
		return partSettings;
	}

	private List<PartSettings> getAvailableTemplates() {
		Protocol protocol = protocolSupplier.get();
		String className = getWorkBenchPart().getClass().getName();
		List<PartSettings> partTemplates = PartSettingsService.getInstance().getPartSettingsTemplates(protocol, className);
		Collections.sort(partTemplates, PartSettingsUtils.NAME_SORTER);
		return partTemplates;
	}

	private Supplier<Protocol> getDefaultProtocolSupplier() {
		return () -> {
			ISelection selection = getWorkBenchPart().getSite().getSelectionProvider().getSelection();
			Protocol protocol = SelectionUtils.getFirstObject(selection, Protocol.class);
			return protocol;
		};
	}
}
