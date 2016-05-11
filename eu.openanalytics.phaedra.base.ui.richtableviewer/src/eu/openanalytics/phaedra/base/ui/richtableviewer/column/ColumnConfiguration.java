package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.Comparator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;

public class ColumnConfiguration {

	private String key;
	private String name;
	private String tooltip;
	private ImageDescriptor image;
	private int width;
	private float aspectRatio;
	private String formatString;
	private ColumnDataType dataType;
	private String userMode;
	private boolean hidden;
	private boolean movable = true;
	private int sortDirection;
	
	private CellLabelProvider labelProvider;
	private Comparator<?> sorter;
	private ColumnEditingConfiguration editingConfig;
	
	public ColumnConfiguration() {
		// Default constructor.
	}
	
	public ColumnConfiguration(String key, String name) {
		this.key = key;
		this.name = name;
		this.tooltip = name;
		this.width = 100;
		this.aspectRatio = 1;
		this.dataType = ColumnDataType.String;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTooltip() {
		return tooltip;
	}
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
	public ImageDescriptor getImage() {
		return image;
	}
	public void setImage(ImageDescriptor image) {
		this.image = image;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public float getAspectRatio() {
		return aspectRatio;
	}
	public void setAspectRatio(float aspectRatio) {
		this.aspectRatio = aspectRatio;
	}
	public String getFormatString() {
		return formatString;
	}
	public void setFormatString(String formatString) {
		this.formatString = formatString;
	}
	public ColumnDataType getDataType() {
		return dataType;
	}
	public void setDataType(ColumnDataType dataType) {
		this.dataType = dataType;
	}
	public String getUserMode() {
		return userMode;
	}
	public void setUserMode(String userMode) {
		this.userMode = userMode;
	}
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	public CellLabelProvider getLabelProvider() {
		return labelProvider;
	}
	public void setLabelProvider(CellLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}
	public Comparator<?> getSorter() {
		return sorter;
	}
	public void setSorter(Comparator<?> sorter) {
		this.sorter = sorter;
	}
	public boolean isMovable() {
		return movable;
	}
	public void setMovable(boolean movable) {
		this.movable = movable;
	}
	public int getSortDirection() {
		return sortDirection;
	}
	public void setSortDirection(int sortDirection) {
		this.sortDirection = sortDirection;
	}
	public ColumnEditingConfiguration getEditingConfig() {
		return editingConfig;
	}
	public void setEditingConfig(ColumnEditingConfiguration editingConfig) {
		this.editingConfig = editingConfig;
	}
}
