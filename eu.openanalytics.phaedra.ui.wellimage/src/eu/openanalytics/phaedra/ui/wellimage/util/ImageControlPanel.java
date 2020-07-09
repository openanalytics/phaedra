package eu.openanalytics.phaedra.ui.wellimage.util;

import java.util.List;

import org.eclipse.core.commands.common.EventManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import eu.openanalytics.phaedra.base.imaging.jp2k.comp.IComponentType;
import eu.openanalytics.phaedra.base.ui.util.misc.CustomToggleButton;
import eu.openanalytics.phaedra.model.plate.util.PlateUtils;
import eu.openanalytics.phaedra.model.plate.vo.Plate;
import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;
import eu.openanalytics.phaedra.model.protocol.vo.ProtocolClass;
import eu.openanalytics.phaedra.ui.protocol.ProtocolUIService;
import eu.openanalytics.phaedra.ui.protocol.event.IUIEventListener;
import eu.openanalytics.phaedra.ui.protocol.event.UIEvent.EventType;
import eu.openanalytics.phaedra.wellimage.component.ComponentTypeFactory;

public class ImageControlPanel extends Composite {

	public final static int[] SCALE_RATIOS_PART1 = {4, 2, 1, 1, 1, 1, 1,  1,  1,  1};
	public final static int[] SCALE_RATIOS_PART2 = {1, 1, 1, 2, 4, 8, 16, 32, 64, 128};

	private Composite buttonContainer;
	private CustomToggleButton[] componentBtns;
	private Combo scaleCombo;

	private ImageControlListenerManager listenerManager;
	private IUIEventListener imageSettingsListener;
	
	private ProtocolClass currentPClass;
	private boolean scaleEnabled;
	
	private boolean[] buttonStates;
	private float currentScale;

	public ImageControlPanel(Composite parent, int style, boolean scale, boolean deprecatedZoom) {
		super(parent, style);

		this.scaleEnabled = scale;

		GridLayoutFactory.fillDefaults().numColumns(7).applyTo(this);

		buttonContainer = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().hint(0, 22).applyTo(buttonContainer);

		if (scaleEnabled) {
			Label lbl = new Label(this, SWT.NONE);
			lbl.setText("Scale:");
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(lbl);

			scaleCombo = new Combo(this, SWT.READ_ONLY);
			scaleCombo.setItems(new String[]{
					SCALE_RATIOS_PART1[0]+":"+SCALE_RATIOS_PART2[0],
					SCALE_RATIOS_PART1[1]+":"+SCALE_RATIOS_PART2[1],
					SCALE_RATIOS_PART1[2]+":"+SCALE_RATIOS_PART2[2],
					SCALE_RATIOS_PART1[3]+":"+SCALE_RATIOS_PART2[3],
					SCALE_RATIOS_PART1[4]+":"+SCALE_RATIOS_PART2[4],
					SCALE_RATIOS_PART1[5]+":"+SCALE_RATIOS_PART2[5],
					SCALE_RATIOS_PART1[6]+":"+SCALE_RATIOS_PART2[6],
					SCALE_RATIOS_PART1[7]+":"+SCALE_RATIOS_PART2[7],
					SCALE_RATIOS_PART1[8]+":"+SCALE_RATIOS_PART2[8],
					SCALE_RATIOS_PART1[9]+":"+SCALE_RATIOS_PART2[9]}
					);
			scaleCombo.select(2);
			scaleCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					changeScale(true);
				}
			});
			GridDataFactory.fillDefaults().applyTo(scaleCombo);
		}

		listenerManager = new ImageControlListenerManager();

		imageSettingsListener = event -> {
			if (event.type == EventType.ImageSettingsChanged && currentPClass != null) {
				boolean[] buttonState = getButtonStates();
				createButtons();
				setButtonStates(buttonState);
				buttonContainer.layout();
			}
		};
		ProtocolUIService.getInstance().addUIEventListener(imageSettingsListener);
		addListener(SWT.Dispose, e -> {
			ProtocolUIService.getInstance().removeUIEventListener(imageSettingsListener);
		});
		
		buttonStates = new boolean[0];
		currentScale = 1f;
	}

	@Override
	public boolean setFocus() {
		return buttonContainer.setFocus();
	}

	public void setImage(Well well) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(well);
		setImage(pClass);
	}

	public void setImage(Plate plate) {
		ProtocolClass pClass = PlateUtils.getProtocolClass(plate);
		setImage(pClass);
	}

	public void setImage(ProtocolClass pClass) {
		if (pClass == null) return;

		if (currentPClass == null || !pClass.equals(currentPClass)) {
			currentPClass = pClass;
			createButtons();
		}

		currentPClass = pClass;

		GridDataFactory.fillDefaults().grab(false, false).hint(35*componentBtns.length, 22).applyTo(buttonContainer);
		this.layout();
		buttonContainer.layout();
	}

	public boolean isDisabled(int component) {
		if (buttonStates.length <= component) return false;
		return !buttonStates[component];
	}

	public boolean[] getButtonStates() {
		return buttonStates;
	}

	public void setButtonStates(boolean[] buttonStates) {
		for (int i = 0; i < this.buttonStates.length && i < buttonStates.length; i++) {
			if (this.buttonStates[i] == buttonStates[i]) continue;
			else toggleComponent(i);
		}
	}

	public String[] getChannelNames() {
		String[] channelNames = new String[currentPClass.getImageSettings().getImageChannels().size()];
		int i = 0;
		for (ImageChannel cfg : currentPClass.getImageSettings().getImageChannels())
			channelNames[i++] = cfg == null ? "Unknown channel" : cfg.getName();

		return channelNames;
	}

	public float getCurrentScale() {
		return currentScale;
	}

	public void setCurrentScale(float currentScale) {
		setCurrentScale(currentScale, true);
	}

	public void setCurrentScale(float currentScale, boolean fireEvent) {
		for (int i = 0; i < SCALE_RATIOS_PART1.length && i < SCALE_RATIOS_PART2.length; i++) {
			if (Math.abs(((float)SCALE_RATIOS_PART1[i] / SCALE_RATIOS_PART2[i]) - currentScale) < 0.001) {
				if (i < scaleCombo.getItemCount()) {
					scaleCombo.select(i);
					changeScale(fireEvent);
				}
			}
		}
	}

	public MouseWheelListener getMouseWheelListener() {
		return new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent e) {
				scrollScale(e.count);
			}
		};
	}

	public KeyListener getKeyListener() {
		return new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				// 48 = 0; 57 = 9;
				if (e.keyCode >= 49 && e.keyCode <= 57) {
					int nr = e.keyCode - 49;
					if (nr < getButtonStates().length) {
						toggleComponent(nr);
					}
				}
				if (e.keyCode >= SWT.KEYPAD_1 && e.keyCode <= SWT.KEYPAD_9) {
					int nr = e.keyCode - SWT.KEYPAD_1;
					if (nr < getButtonStates().length) {
						toggleComponent(nr);
					}
				}
			}
		};
	}

	public void addImageControlListener(ImageControlListener listener) {
		listenerManager.addListener(listener);
	}

	public void removeImageControlListener(ImageControlListener listener) {
		listenerManager.removeListener(listener);
	}

	public static class ImageControlListener {
		
		public void componentToggled(int component, boolean state) {
			// Default: do nothing.
		}
		
		public void scaleChanged(float ratio) {
			// Default: do nothing.
		}
	}

	/*
	 * **********
	 * Non-public
	 * **********
	 */

	private void createButtons() {
		if (currentPClass == null) return;

		if (componentBtns != null) {
			for (CustomToggleButton btn: componentBtns) {
				btn.dispose();
			}
		}

		int components = currentPClass.getImageSettings().getImageChannels().size();
		componentBtns = new CustomToggleButton[components];
		buttonStates = new boolean[components];

		for (int i=0; i<components; i++) {
			final int componentNr = i;

			buttonStates[i] = currentPClass.getImageSettings().getImageChannels().get(i).isShowInWellView();

			ImageChannel cfg = getChannel(componentNr);
			String channelName = cfg == null ? "Unknown channel" : cfg.getName();

			componentBtns[i] = new CustomToggleButton(buttonContainer, SWT.NONE);
			componentBtns[i].setText(""+(i+1));
			componentBtns[i].setToolTipText("Toggle component '" + channelName + "'");
			componentBtns[i].setImage(createButtonImage(componentNr));
			componentBtns[i].addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					toggleComponent(componentNr);
				}
			});
			GridDataFactory.fillDefaults().hint(20,20).applyTo(componentBtns[i]);
			if (!buttonStates[i]) componentBtns[i].toggle();
		}

		GridLayoutFactory.fillDefaults().numColumns(components).applyTo(buttonContainer);
	}

	private void scrollScale(int scrollCount) {
		int index = scaleCombo.getSelectionIndex();
		if (scrollCount > 0) {
			index--;
		} else {
			index++;
		}
		if (index >= 0 && index < scaleCombo.getItemCount()) {
			scaleCombo.select(index);
			changeScale(true);
		}
	}

	private void changeScale(boolean fireEvent) {
		int index = scaleCombo.getSelectionIndex();
		currentScale = (float)SCALE_RATIOS_PART1[index]/SCALE_RATIOS_PART2[index];
		if (fireEvent) listenerManager.fireScaleEvent(currentScale);
	}

	private void toggleComponent(int nr) {
		boolean state = componentBtns[nr].toggle();
		buttonStates[nr] = state;
		listenerManager.fireToggleEvent(nr, state);
	}

	private Image createButtonImage(int component) {
		ImageChannel cfg = getChannel(component);
		if (cfg != null) {
			IComponentType type = ComponentTypeFactory.getInstance().getComponent(cfg);
			return type.createIcon(Display.getCurrent());
		}
		return null;
	}

	private ImageChannel getChannel(int component) {
		List<ImageChannel> knownChannels = currentPClass.getImageSettings().getImageChannels();
		if (knownChannels != null && knownChannels.size() > component) {
			return knownChannels.get(component);
		}
		return null;
	}

	private static class ImageControlListenerManager extends EventManager {

		public void addListener(ImageControlListener listener) {
			addListenerObject(listener);
		}

		public void removeListener(ImageControlListener listener) {
			removeListenerObject(listener);
		}

		public void fireToggleEvent(int component, boolean state) {
			for (Object listener: getListeners()) {
				((ImageControlListener)listener).componentToggled(component, state);
			}
		}

		public void fireScaleEvent(float ratio) {
			for (Object listener: getListeners()) {
				((ImageControlListener)listener).scaleChanged(ratio);
			}
		}
	}
	
}
