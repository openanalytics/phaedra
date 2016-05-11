package eu.openanalytics.phaedra.base.ui.volumerenderer.internal;

import java.nio.ByteBuffer;
import java.util.List;

import javax.media.opengl.GL;

import org.eclipse.swt.graphics.ImageData;

import com.sun.opengl.util.BufferUtil;

import eu.openanalytics.phaedra.base.ui.volumerenderer.VolumeDataItem;
import eu.openanalytics.phaedra.base.ui.volumerenderer.VolumeDataModel;
import eu.openanalytics.phaedra.base.ui.volumerenderer.internal.VolumeLoader.DIRECTION;

public class VolumeRenderer implements IDataModelRenderer {
	
	private VolumeDataModel model;
	private VolumeLoader loader;
	private ByteBuffer[] buffers;
	
	private int[] texIds;
	private boolean texAssigned;

	private boolean useXY = true;
	private boolean reverseOrder = false;

	public VolumeRenderer(IDataModel model) {
		setDataModel(model);
	}
	
	@Override
	public void setDataModel(IDataModel model) {
		this.model = (VolumeDataModel)model;
	}

	@Override
	public void init(GL gl, SpatialConfig cfg) {
		loadImage(model.getImage());
	}
	
	protected void loadImage(ImageData[][] image) {
		loader = new VolumeLoader();
		loader.load(image);
		buffers = new ByteBuffer[2];
		
		loader.setDirection(DIRECTION.XY);
		buffers[0] = BufferUtil.newByteBuffer(loader.getSize());
		for (int index=0;index<loader.getDepth();index++) {
			byte[] slice = loader.get2D(index);
			buffers[0].put(slice);
		}
		buffers[0].rewind();
		
		loader.setDirection(DIRECTION.YZ);
		buffers[1] = BufferUtil.newByteBuffer(loader.getSize());
		for (int index=0;index<loader.getDepth();index++) {
			byte[] slice = loader.get2D(index);
			buffers[1].put(slice);
		}
		buffers[1].rewind();
	}
	
	public void updateRotation(int x, int y, int z) {
		//System.out.println(""+x+","+y+","+z);
		if (z > 50) {
			if (x < -50) reverseOrder = true; 
			else reverseOrder = false;
			useXY = (x > -50 && x < 50);
		} else if (z < -50) {
			if (x > 50) reverseOrder = true; 
			else reverseOrder = false;
			useXY = (x > -50 && x < 50);
		} else {
			if (x > -50 && x < 50 && z > -50 && z < 50) {
				reverseOrder = y > 50 || x < -50;
				useXY = (y > -50 && y < 50);
			} else if (x < -50 || x > 50) {
				useXY = (z > -50 && z < 50);
				reverseOrder = useXY;
			} else {
				if (x > -50 && x < 50) useXY = true;
				if (y > -50 && y < 50) useXY = true;
				reverseOrder = y > 50 || x < -50;
				if (x < -50 && z < 0) reverseOrder = false;
			}
		}
	}
	
	public void enableOverlay(boolean show) {
		// Loop thru buffers looking for alpha 254 -> these are overlay pixels
		for (ByteBuffer buffer: buffers) {
			buffer.rewind();
			while (buffer.hasRemaining()) {
				int start = buffer.position();
				buffer.position(start+3);
				byte a = buffer.get();
				if (a == (byte)254) {
					buffer.position(start);
					if (show) {
						buffer.put((byte)255);
						buffer.put((byte)0);
						buffer.put((byte)0);
					} else {
						buffer.put((byte)255);
						buffer.put((byte)255);
						buffer.put((byte)255);
					}
					buffer.put((byte)254);
				}
			}
			buffer.rewind();
		}
		texAssigned = false;
	}
	
	/*
	 * ***********************
	 * Render methods (OpenGL)
	 * ***********************
	 */
	
	@Override
	public void render(GL gl, SpatialConfig cfg) {
		renderVolume(gl, cfg);
		renderSelection(gl, cfg);
	}
	
	public void renderPoints(GL gl, SpatialConfig cfg) {
		if (loader == null) return;
		
		loader.setDirection(DIRECTION.XY);
		float offsetX = 0-loader.getWidth()/2;
		float offsetY = 0-loader.getHeight()/2;
		float offsetZ = 0-loader.getDepth()/2;
		
		List<VolumeDataItem> items = model.getItems();
		gl.glColor4f(0,0,0,1);
		gl.glPointParameterf(GL.GL_POINT_SIZE_MIN, 1f);
		gl.glPointParameterf(GL.GL_POINT_SIZE_MAX, 100f);
		for (VolumeDataItem item: items) {
			gl.glPointSize(item.getSize());
			float[] c = item.getCoordinates();
			gl.glLoadName(item.getLabel());
			gl.glBegin(GL.GL_POINTS);
			gl.glVertex3f(
					offsetX+(loader.getWidth()-c[1]),
					offsetY+c[0],
					offsetZ+c[2]);
			gl.glEnd();
		}
	}
	
	protected void renderSelection(GL gl, SpatialConfig cfg) {
		if (loader == null) return;
		
		loader.setDirection(DIRECTION.XY);
		float offsetX = 0-loader.getWidth()/2;
		float offsetY = 0-loader.getHeight()/2;
		float offsetZ = 0-loader.getDepth()/2;
		
		List<VolumeDataItem> items = model.getItems();
		gl.glColor4f(0f,0f,1f,1f);
		gl.glPointParameterf(GL.GL_POINT_SIZE_MIN, 1f);
		gl.glPointParameterf(GL.GL_POINT_SIZE_MAX, 100f);
		for (VolumeDataItem item: items) {
			if (item.isSelected()) {
				float[] c = item.getCoordinates();
				float cogX = offsetX+(loader.getWidth()-c[1]);
				float cogY = offsetY+c[0];
				float cogZ = offsetZ+c[2];
				float size = item.getSize();
				float rad = size/200;
				if (rad < 5) rad = 5;
				
				gl.glLineWidth(1f);
				gl.glBegin(GL.GL_LINES);
				gl.glVertex3f(cogX-rad,cogY-rad,cogZ-rad);
				gl.glVertex3f(cogX+rad,cogY-rad,cogZ-rad);
				gl.glVertex3f(cogX-rad,cogY-rad,cogZ-rad);
				gl.glVertex3f(cogX-rad,cogY+rad,cogZ-rad);
				gl.glVertex3f(cogX+rad,cogY-rad,cogZ-rad);
				gl.glVertex3f(cogX+rad,cogY+rad,cogZ-rad);
				gl.glVertex3f(cogX-rad,cogY+rad,cogZ-rad);
				gl.glVertex3f(cogX+rad,cogY+rad,cogZ-rad);
				
				gl.glVertex3f(cogX-rad,cogY-rad,cogZ+rad);
				gl.glVertex3f(cogX+rad,cogY-rad,cogZ+rad);
				gl.glVertex3f(cogX-rad,cogY-rad,cogZ+rad);
				gl.glVertex3f(cogX-rad,cogY+rad,cogZ+rad);
				gl.glVertex3f(cogX+rad,cogY-rad,cogZ+rad);
				gl.glVertex3f(cogX+rad,cogY+rad,cogZ+rad);
				gl.glVertex3f(cogX-rad,cogY+rad,cogZ+rad);
				gl.glVertex3f(cogX+rad,cogY+rad,cogZ+rad);
				
				gl.glVertex3f(cogX-rad,cogY-rad,cogZ-rad);
				gl.glVertex3f(cogX-rad,cogY-rad,cogZ+rad);
				gl.glVertex3f(cogX+rad,cogY-rad,cogZ-rad);
				gl.glVertex3f(cogX+rad,cogY-rad,cogZ+rad);
				gl.glVertex3f(cogX-rad,cogY+rad,cogZ-rad);
				gl.glVertex3f(cogX-rad,cogY+rad,cogZ+rad);
				gl.glVertex3f(cogX+rad,cogY+rad,cogZ-rad);
				gl.glVertex3f(cogX+rad,cogY+rad,cogZ+rad);
				gl.glEnd();
				
//				gl.glLineWidth(3f);
//				gl.glBegin(GL.GL_LINE_LOOP);
//				gl.glVertex3f(cogX-rad,cogY-rad,cogZ);
//				gl.glVertex3f(cogX+rad,cogY-rad,cogZ);
//				gl.glVertex3f(cogX+rad,cogY+rad,cogZ);
//				gl.glVertex3f(cogX-rad,cogY+rad,cogZ);
//				gl.glEnd();
			}
		}
	}
	
	protected void renderVolume(GL gl, SpatialConfig cfg) {
		if (loader == null) return;
		
		gl.glColor4f(0,0,0,0);
		gl.glEnable( GL.GL_TEXTURE_3D );
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glTexEnvf( GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, 
				GL.GL_REPLACE);

		if (!texAssigned) {
			if (texIds != null) {
				// Delete existing textures.
				gl.glDeleteTextures(texIds.length, texIds, 0);
			}
			texIds = new int[buffers.length];
			gl.glGenTextures(buffers.length, texIds, 0 );

			int internalFormat = GL.GL_RGBA;
			int format = GL.GL_RGBA;
			int type = GL.GL_UNSIGNED_BYTE;
			loader.setDirection(DIRECTION.XY);
			gl.glBindTexture(GL.GL_TEXTURE_3D, texIds[0]);
			gl.glTexImage3D(GL.GL_TEXTURE_3D, 0, internalFormat,
					loader.getWidth(),loader.getHeight(),loader.getDepth(), 0, format, type, buffers[0]);
			loader.setDirection(DIRECTION.YZ);
			gl.glBindTexture(GL.GL_TEXTURE_3D, texIds[1]);
			gl.glTexImage3D(GL.GL_TEXTURE_3D, 0, internalFormat,
					loader.getWidth(),loader.getHeight(),loader.getDepth(), 0, format, type, buffers[1]);
			texAssigned = true;
		}
		
//		int error = gl.glGetError();
//		System.out.println(error);

		gl.glTexParameteri( GL.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_LINEAR );
		gl.glTexParameteri( GL.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_LINEAR );
		gl.glTexParameterf( GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, 
				GL.GL_CLAMP );
		gl.glTexParameterf( GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, 
				GL.GL_CLAMP );
		gl.glTexParameterf( GL.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_R, 
				GL.GL_CLAMP );

		loader.setDirection(DIRECTION.XY);
		float offsetX = 0-loader.getWidth()/2;
		float offsetY = 0-loader.getHeight()/2;
		float offsetZ = 0-loader.getDepth()/2;
		
		if (useXY) {
			gl.glBindTexture( GL.GL_TEXTURE_3D, texIds[0]);
			gl.glBegin( GL.GL_QUADS );
			for (int slice=0; slice<loader.getDepth(); slice++) {
				float vertexZ = slice;
				if (reverseOrder) {
					vertexZ = loader.getDepth() - 1 - slice;
				}
				float texCoordZ = vertexZ/loader.getDepth();
				
				gl.glTexCoord3f(0f, 0f, texCoordZ);
				gl.glVertex3f(offsetX, offsetY, offsetZ+vertexZ);
				gl.glTexCoord3f(0f, 1f, texCoordZ);
				gl.glVertex3f(offsetX, offsetY+loader.getHeight(), offsetZ+vertexZ);
				gl.glTexCoord3f(1f, 1f, texCoordZ);
				gl.glVertex3f(offsetX+loader.getWidth(), offsetY+loader.getHeight(), offsetZ+vertexZ);
				gl.glTexCoord3f(1f, 0f, texCoordZ);
				gl.glVertex3f(offsetX+loader.getWidth(), offsetY, offsetZ+vertexZ);
			}
			gl.glEnd();
		} else {
			gl.glBindTexture( GL.GL_TEXTURE_3D, texIds[1]);
			gl.glBegin( GL.GL_QUADS );
			for (int slice=0; slice<loader.getWidth(); slice++) {
				float vertexX = slice;
				if (reverseOrder) {
					vertexX = loader.getWidth() - 1 - slice;
				}
				float texCoordZ = vertexX/loader.getWidth();
				
				gl.glTexCoord3f(0f, 0f, texCoordZ);
				gl.glVertex3f(offsetX+vertexX, offsetY, offsetZ);
				gl.glTexCoord3f(0f, 1f, texCoordZ);
				gl.glVertex3f(offsetX+vertexX, offsetY, offsetZ+loader.getDepth());
				gl.glTexCoord3f(1f, 1f, texCoordZ);
				gl.glVertex3f(offsetX+vertexX, offsetY+loader.getHeight(), offsetZ+loader.getDepth());
				gl.glTexCoord3f(1f, 0f, texCoordZ);
				gl.glVertex3f(offsetX+vertexX, offsetY+loader.getHeight(), offsetZ);
			}
			gl.glEnd();
		}
		
		gl.glDisable(GL.GL_TEXTURE_3D);
		gl.glEnable(GL.GL_DEPTH_TEST);

	}
}
