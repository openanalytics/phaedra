package eu.openanalytics.phaedra.base.ui.volumerenderer.internal;

import javax.media.opengl.GL;

public interface IOpenGLRenderer {
	
	public void init(GL gl, SpatialConfig cfg);
	
	public void render(GL gl, SpatialConfig cfg);
}
