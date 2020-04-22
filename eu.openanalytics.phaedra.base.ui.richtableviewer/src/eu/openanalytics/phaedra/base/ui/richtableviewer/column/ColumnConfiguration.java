package eu.openanalytics.phaedra.base.ui.richtableviewer.column;

import java.util.Comparator;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;

import eu.openanalytics.phaedra.base.datatype.DataType;
import eu.openanalytics.phaedra.base.datatype.description.DataDescription;


public class ColumnConfiguration {

	private String key;
	private String name;
	private String tooltip;
	private ImageDescriptor image;
	
	private boolean movable = true;
	private boolean hidden;
	private int width;
	private float aspectRatio;
	
	private DataDescription dataDescription;
	private DataType dataType;
	
	private Map<String, Object> customData;
	
	private CellLabelProvider labelProvider;
	private Comparator<?> sortComparator;
	private int sortDirection;
	
	private ColumnEditingConfiguration editingConfig;
	
	
	public ColumnConfiguration() {
	}
	
	public ColumnConfiguration(String key, String name) {
		this.key = key;
		this.name = name;
		this.tooltip = name;
		this.width = 100;
		this.aspectRatio = 1;
	}
	
	public ColumnConfiguration(ColumnConfiguration config) {
		load(config);
	}
	
	
	public void load(ColumnConfiguration config) {
		this.key = config.key;
		this.name = config.name;
		this.tooltip = config.tooltip;
		this.image = config.image;
		this.movable = config.movable;
		this.hidden = config.hidden;
		this.width = config.width;
		this.aspectRatio = config.aspectRatio;
		this.dataDescription = config.dataDescription;
		this.dataType = config.dataType;
		this.customData = config.customData;
		this.labelProvider = config.labelProvider;
		this.sortComparator = config.sortComparator;
		this.sortDirection = config.sortDirection;
		this.editingConfig = config.editingConfig;
	}
	
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public boolean isCustom() {
		return key.startsWith(CustomColumnSupport.CUSTOM_KEY_PREFIX);
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
	
	public boolean isMovable() {
		return movable;
	}
	public void setMovable(boolean movable) {
		this.movable = movable;
	}
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
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
	
	public Map<String, Object> getCustomData() {
		return customData;
	}
	public void setCustomData(Map<String, Object> data) {
		this.customData = data;
	}
	
	public DataDescription getDataDescription() {
		return this.dataDescription;
	}
	public void setDataDescription(final DataDescription dataDescription) {
		this.dataDescription = dataDescription;
		this.dataType= (dataDescription != null) ? dataDescription.getDataType() : null;
	}
	public DataType getDataType() {
		return dataType;
	}
	public void setDataType(DataType dataType) {
		this.dataType = dataType;
	}
	
	public CellLabelProvider getLabelProvider() {
		return labelProvider;
	}
	public void setLabelProvider(CellLabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}
	
	public Comparator<?> getSortComparator() {
		return sortComparator;
	}
	public void setSortComparator(Comparator<?> sorter) {
		this.sortComparator = sorter;
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
