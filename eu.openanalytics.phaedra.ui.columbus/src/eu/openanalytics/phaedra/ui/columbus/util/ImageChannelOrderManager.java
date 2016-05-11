package eu.openanalytics.phaedra.ui.columbus.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

public class ImageChannelOrderManager {

	private List<ChannelMapping> channelOrder;
	
	public ImageChannelOrderManager() {
		this.channelOrder = new ArrayList<>();
	}
	
	public void registerChannel(ImageChannel channel, Composite control, Button upBtn, Button downBtn, Button deleteBtn) {
		ChannelMapping mapping = new ChannelMapping(channel, control, upBtn, downBtn);
		upBtn.addListener(SWT.Selection, e -> moveUp(mapping));
		downBtn.addListener(SWT.Selection, e -> moveDown(mapping));
		deleteBtn.addListener(SWT.Selection, e -> delete(mapping));
		channelOrder.add(mapping);
		refreshButtons();
	}
	
	public void reset() {
		channelOrder.clear();
	}
	
	private void moveUp(ChannelMapping mapping) {
		int index = channelOrder.indexOf(mapping);
		if (index == 0) return;
		ChannelMapping prev = channelOrder.get(index - 1);

		if (prev.channel.getType() != mapping.channel.getType()) return;
		
		mapping.channel.setSequence(mapping.channel.getSequence() - 1);
		prev.channel.setSequence(prev.channel.getSequence() + 1);
		
		// Switch order in the list
		channelOrder.set(index, prev);
		channelOrder.set(index - 1, mapping);
		
		// Switch order in the UI
		mapping.control.moveAbove(prev.control);
		mapping.control.getParent().layout();
		
		refreshButtons();
	}
	
	private void moveDown(ChannelMapping mapping) {
		int index = channelOrder.indexOf(mapping);
		if (index == channelOrder.size() - 1) return;
		ChannelMapping next = channelOrder.get(index + 1);

		if (next.channel.getType() != mapping.channel.getType()) return;
		
		mapping.channel.setSequence(mapping.channel.getSequence() + 1);
		next.channel.setSequence(next.channel.getSequence() - 1);
		
		// Switch order in the list
		channelOrder.set(index, next);
		channelOrder.set(index + 1, mapping);
		
		// Switch order in the UI
		mapping.control.moveBelow(next.control);
		mapping.control.getParent().layout();
		
		refreshButtons();
	}

	private void delete(ChannelMapping mapping) {
		boolean confirmed = MessageDialog.openConfirm(mapping.control.getShell(), "Delete Image Channel",
				"Are you sure you want to delete the channel '" + mapping.channel.getName() + "' from the list?");
		if (!confirmed) return;
		
		mapping.channel.setSequence(-1);
		
		// Remove from order list and update channel sequences
		int index = channelOrder.indexOf(mapping);
		channelOrder.remove(index);
		for (int i=index; i<channelOrder.size(); i++) {
			channelOrder.get(i).channel.setSequence(channelOrder.get(i).channel.getSequence() - 1);
		}
		
		// Remove from UI
		Composite parent = mapping.control.getParent();
		mapping.control.dispose();
		parent.layout();
		
		refreshButtons();
	}

	private void refreshButtons() {
		if (channelOrder.isEmpty()) return;
		
		for (int i = 0; i < channelOrder.size(); i++) {
			ChannelMapping m = channelOrder.get(i);
			boolean up = true;
			boolean down = true;
			
			if (i == 0) up = false;
			else if (m.channel.getType() != channelOrder.get(i - 1).channel.getType()) up = false;

			if (i == channelOrder.size() - 1) down = false;
			else if (i + 1 < channelOrder.size() && m.channel.getType() != channelOrder.get(i + 1).channel.getType()) down = false;
			
			toggle(m.upBtn, up);
			toggle(m.downBtn, down);
		}
	}
	
	private void toggle(Button btn, boolean enabled) {
		if (btn.isEnabled() != enabled) btn.setEnabled(enabled);
	}
	
	private static class ChannelMapping {
		
		public ImageChannel channel;
		public Composite control;
		public Button upBtn;
		public Button downBtn;
		
		public ChannelMapping(ImageChannel channel, Composite control, Button upBtn, Button downBtn) {
			this.channel = channel;
			this.control = control;
			this.upBtn = upBtn;
			this.downBtn = downBtn;
		}
	}
}
