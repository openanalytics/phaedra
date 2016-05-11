package eu.openanalytics.phaedra.base.ui.charting.v2.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;

import eu.openanalytics.phaedra.base.util.convert.AWTImageConverter;
import uk.ac.starlink.ttools.plot.Shader;

public class ShaderIconCreator {

	private final Shader shader_;
	private final boolean horizontal_;
	private final int width_;
	private final int height_;
	private final int xpad_;
	private final int ypad_;
	private final float[] baseRgba_;

	/**
	 * Constructor.
	 *
	 * @param  shader   shader
	 * @param  horizontal   true for a horizontal bar, false for vertical
	 * @param  baseColor   the base colour modified by the shader
	 * @param  width    total width of the icon
	 * @param  height   total height of the icon
	 * @param  xpad     internal padding in the X direction
	 * @param  ypad     internal padding in the Y direction
	 */
	public ShaderIconCreator( Shader shader, boolean horizontal, Color baseColor,
			int width, int height, int xpad, int ypad ) {
		shader_ = shader;
		horizontal_ = horizontal;
		width_ = width;
		height_ = height;
		xpad_ = xpad;
		ypad_ = ypad;
		baseRgba_ = baseColor.getRGBComponents(null);
	}

	public int getIconWidth() {
		return width_;
	}

	public int getIconHeight() {
		return height_;
	}

	public Image paintIcon(Device device, int x, int y ) {
		BufferedImage bi = new BufferedImage(width_, height_, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();

		Color origColor = g.getColor();
		int npix = horizontal_ ? width_ - 2 * xpad_
				: height_ - 2 * ypad_;
		float np1 = 1f / ( npix - 1 );
		int xlo = x + xpad_;
		int xhi = x + width_ - xpad_;
		int ylo = y + ypad_;
		int yhi = y + height_ - ypad_;
		for ( int ipix = 0; ipix < npix; ipix++ ) {
			g.setColor( getColor( ipix * np1 ) );
			if ( horizontal_ ) {
				g.fillRect( xlo + ipix - 1, ylo, 1, yhi - ylo );
			}
			else {
				g.fillRect( xlo, ylo + ipix - 1, xhi - xlo, 1 );
			}
		}
		g.setColor( origColor );
		return AWTImageConverter.convert(null, bi);
	}

	/**
	 * Returns the colour corresponding to a given parameter value for
	 * this icon's shader.
	 *
	 * @param  value  parameter value
	 * @return colour
	 */
	 private Color getColor( float value ) {
		 float[] rgba = baseRgba_.clone();
		 shader_.adjustRgba( rgba, value );
		 return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
	 }

}
