package eu.openanalytics.phaedra.base.ui.util.misc;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Path;
import org.jfree.util.ShapeUtilities;

public enum PlotShape {
	Ellipse("Ellipse"){
		@Override
		public Shape getShape(float size){
			Shape ellipse = new Ellipse2D.Float(-size, -size, size*2, size*2);
			return ellipse;
		}
	},
	Rectangle("Rectangle"){
		@Override
		public Shape getShape(float size){
			Shape rectangle = new Rectangle2D.Float(-size, -size, size*2, size*2);
			return rectangle;
		}
	},
	TriangleUp("Triangle Up") {
		@Override
		public Shape getShape(float size){
			Shape triangle = ShapeUtilities.createUpTriangle(size);
			return triangle;
		}

	},
	TriangleRight("Triangle Right") {
		@Override
		public Shape getShape(float size){
			Shape triangle = ShapeUtilities.createUpTriangle(size);
			triangle = ShapeUtilities.rotateShape(triangle, Math.toRadians(90), 0, 0);
			return triangle;
		}

	},
	TriangleLeft("Triangle Left") {
		@Override
		public Shape getShape(float size){
			Shape triangle = ShapeUtilities.createUpTriangle(size);
			triangle = ShapeUtilities.rotateShape(triangle, Math.toRadians(270), 0, 0);
			return triangle;
		}

	},
	TriangleDown("Triangle Down") {
		@Override
		public Shape getShape(float size){
			Shape triangleDown = ShapeUtilities.createDownTriangle(size);
			return triangleDown;
		}
	},
	Diamond("Diamond") {
		@Override
		public Shape getShape(float size){
			Shape diamond = ShapeUtilities.createDiamond(size);
			return diamond;
		}
	},
	Cross("Cross") {
		@Override
		public Shape getShape(float size){
			Shape cross = ShapeUtilities.createRegularCross(size, size/3.5f);
			return cross;
		}
	},
	DiagonalCross("Diagonal Cross") {
		@Override
		public Shape getShape(float size){
			Shape cross = ShapeUtilities.createRegularCross(size, size/3);
			cross = ShapeUtilities.rotateShape(cross, Math.toRadians(45), 0, 0);
			return cross;
		}
	},
	Star("Star") {
		@Override
		public Shape getShape(float size){
			Font font = new Font("Wingdings", Font.BOLD, (int)(size*3));
			FontRenderContext frc = new FontRenderContext(null, false, false);
			GlyphVector gv = font.createGlyphVector(frc, new int[]{144});
			return gv.getOutline(-size * 1.5f, size);
		}
	},
	ProhibitorySign("Prohibitory Sign") {
		@Override
		public Shape getShape(float size){
			Font font = new Font("Webdings", Font.BOLD, (int)(size*3));
			FontRenderContext frc = new FontRenderContext(null, false, false);
			GlyphVector gv = font.createGlyphVector(frc, new int[]{91});
			return gv.getOutline(-size*1.5f, size);
		}
	},
	QuestionMark("Question Mark") {
		@Override
		public Shape getShape(float size){
			Font font = new Font("SansSerif", Font.BOLD, (int)(size*3));
			FontRenderContext frc = new FontRenderContext(null, false, false);
			GlyphVector gv = font.createGlyphVector(frc, "?");
			return gv.getOutline(-size, size);
		}
	},
	CheckMark("Check Mark") {
		@Override
		public Shape getShape(float size){
			Font font = new Font("Webdings", Font.BOLD, (int)(size*3));
			FontRenderContext frc = new FontRenderContext(null, false, false);
			GlyphVector gv = font.createGlyphVector(frc, new int[]{68});
			return gv.getOutline(-size*1.5f, size/2);
		}
	};

	private String label;
	private Shape shape;

	private PlotShape(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public abstract Shape getShape(float size);

	public void drawShape(GC gc, int x, int y, float pixelSize) {
		drawShape(gc, x, y, pixelSize, false);
	}

	public void drawShape(GC gc, int x, int y, float pixelSize, boolean fill) {
		shape = getShape(pixelSize);

		int type;
		float[] coords = new float[6];
		Path path = new Path(gc.getDevice());
		PathIterator pit = shape.getPathIterator(null);
		while (!pit.isDone()) {
			type = pit.currentSegment(coords);
			switch (type) {
			case (PathIterator.SEG_MOVETO):
				path.moveTo(coords[0]+x, coords[1]+y);
			break;
			case (PathIterator.SEG_LINETO):
				path.lineTo(coords[0]+x, coords[1]+y);
			break;
			case (PathIterator.SEG_QUADTO):
				path.quadTo(coords[0]+x, coords[1]+y, coords[2]+x, coords[3]+y);
			break;
			case (PathIterator.SEG_CUBICTO):
				path.cubicTo(coords[0]+x, coords[1]+y, coords[2]+x,
						coords[3]+y, coords[4]+x, coords[5]+y);
			break;
			case (PathIterator.SEG_CLOSE):
				path.close();
			break;
			default:
				break;
			}
			pit.next();
		}

		if (fill) gc.fillPath(path);
		gc.drawPath(path);
		path.dispose();
	}
}
