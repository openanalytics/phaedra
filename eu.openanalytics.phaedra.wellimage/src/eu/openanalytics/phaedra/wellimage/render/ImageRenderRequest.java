package eu.openanalytics.phaedra.wellimage.render;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import eu.openanalytics.phaedra.model.plate.vo.Well;
import eu.openanalytics.phaedra.model.protocol.vo.ImageSettings;

public class ImageRenderRequest {

	public Well well;
	
	public float scale;
	public Point size;
	
	public Rectangle region;
	
	public boolean[] components;
	
	public ImageSettings customSettings;
	
	public boolean applyBlend;
	public boolean applyGamma;
	
	public static class Builder {
		
		private Well well;
		
		private float scale = 1.0f;
		private Point size;
		
		private Rectangle region;
		
		private boolean[] components;
		
		private ImageSettings customSettings;
		
		private boolean applyBlend = true;
		private boolean applyGamma = true;
		
		public Builder withWell(Well well) {
			this.well = well;
			return this;
		}
		
		public Builder withScale(float scale) {
			this.scale = scale;
			return this;
		}
		
		public Builder withSize(Point size) {
			this.size = size;
			return this;
		}
		
		public Builder withRegion(Rectangle region) {
			this.region = region;
			return this;
		}
		
		public Builder withComponents(boolean[] components) {
			this.components = components;
			return this;
		}
		
		public Builder withCustomSettings(ImageSettings customSettings) {
			this.customSettings = customSettings;
			return this;
		}
		
		public Builder withApplyBlend(boolean applyBlend) {
			this.applyBlend = applyBlend;
			return this;
		}
		
		public Builder withApplyGamma(boolean applyGamma) {
			this.applyGamma = applyGamma;
			return this;
		}
		
		public ImageRenderRequest build() {
			ImageRenderRequest req = new ImageRenderRequest();
			req.well = this.well;
			req.scale = this.scale;
			req.size = this.size;
			req.region = this.region;
			req.components = this.components;
			req.customSettings = this.customSettings;
			req.applyBlend = this.applyBlend;
			req.applyGamma = this.applyGamma;
			return req;
		}
	}
}
