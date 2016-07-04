package eu.openanalytics.phaedra.ui.cellprofiler.widget;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import eu.openanalytics.phaedra.base.ui.icons.IconManager;
import eu.openanalytics.phaedra.model.protocol.ProtocolService;
import eu.openanalytics.phaedra.model.protocol.vo.ImageChannel;

public class ChannelComposer extends Composite {

	private Composite channelRowArea;
	private List<ChannelRow> channelRows;
	private Path imageFolder;
	
	public ChannelComposer(Composite parent) {
		super(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(this);
		
		channelRowArea = new Composite(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(channelRowArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(channelRowArea);
		
		Composite buttonArea = new Composite(this, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonArea);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonArea);
		
		Button addBtn = new Button(buttonArea, SWT.PUSH);
		addBtn.addListener(SWT.Selection, e -> addChannel(createChannel()));
		addBtn.setText("Add");
		addBtn.setImage(IconManager.getIconImage("add.png"));
		
		Button clearBtn = new Button(buttonArea, SWT.PUSH);
		clearBtn.addListener(SWT.Selection, e -> clearChannels());
		clearBtn.setText("Clear");
		clearBtn.setImage(IconManager.getIconImage("delete.png"));
		
		channelRows = new ArrayList<>();
	}
	
	public Composite getChannelRowArea() {
		return channelRowArea;
	}
	
	public List<ImageChannel> getImageChannels() {
		List<ImageChannel> channels = new ArrayList<>();
		for (ChannelRow row: channelRows) channels.add(row.getChannel());
		channels.sort((c1, c2) -> c1.getSequence() - c2.getSequence());
		return channels;
	}
	
	public Path getImageFolder() {
		return imageFolder;
	}
	
	public void setImageFolder(Path imageFolder) {
		this.imageFolder = imageFolder;
	}
	
	public void addChannel(ImageChannel channel) {
		channel.setSequence(channelRows.size() + 1);
		ChannelRow row = new ChannelRow(this, channel);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(row);
		channelRows.add(row);
		channelRowArea.layout();
	}
	
	public void removeChannel(ImageChannel channel) {
		ChannelRow row = getRow(channel.getSequence());
		
		// Remove from order list and update channel sequences
		int index = channelRows.indexOf(row);
		channelRows.remove(index);
		for (int i=index; i<channelRows.size(); i++) {
			ImageChannel c = channelRows.get(i).getChannel();
			c.setSequence(c.getSequence() - 1);
			channelRows.get(i).refresh();
		}
		row.dispose();
		channelRowArea.layout();
	}
	
	public void clearChannels() {
		for (ChannelRow row: channelRows) row.dispose();
		channelRows.clear();
		channelRowArea.layout();
	}
	
	public void moveChannelUp(ImageChannel channel) {
		int newSequence = channel.getSequence() - 1;
		if (newSequence == 0) return;
		
		ChannelRow row = getRow(channel.getSequence());
		ChannelRow prevRow = getRow(newSequence);
		
		row.getChannel().setSequence(newSequence);
		prevRow.getChannel().setSequence(newSequence + 1);
		
		row.refresh();
		prevRow.refresh();
		
		row.moveAbove(prevRow);
		channelRowArea.layout();
	}
	
	public void moveChannelDown(ImageChannel channel) {
		int newSequence = channel.getSequence() + 1;
		if (newSequence > channelRows.size()) return;
		
		ChannelRow row = getRow(channel.getSequence());
		ChannelRow nextRow = getRow(newSequence);
		
		row.getChannel().setSequence(newSequence);
		nextRow.getChannel().setSequence(newSequence - 1);
		
		row.refresh();
		nextRow.refresh();
		
		nextRow.moveAbove(row);
		channelRowArea.layout();
	}
	
	private ImageChannel createChannel() {
		ImageChannel channel = ProtocolService.getInstance().createChannel(null);
		channel.setSequence(1);
		return channel;
	}
	
	private ChannelRow getRow(int seq) {
		return channelRows.stream().filter(r -> r.getChannel().getSequence() == seq).findAny().orElse(null);
	}
}
