package eu.openanalytics.phaedra.base.ui.charting.v2.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;


public class AWTShapeConverter {
	
	 public static MarkStyle convertShape(Shape shape, Color color, int size, float opacity){
		 MarkShape markShape = new AWTMarkShape("test", shape);
        return markShape.getStyle(color, size, opacity);
	 }
	 
	 private static class AWTMarkShape extends MarkShape {

		 private Shape awtShape;
		 
		public AWTMarkShape(String name, Shape awtShape) {
			super(name);
			this.awtShape = awtShape;
		}
		 
		public MarkStyle getStyle( Color color, int size, float opacity ) {
           return new MarkStyle( color, new Integer( size ), this, size, size + 1, opacity ) {
               protected void drawShape( Graphics g ) {
            	   Graphics2D g2 = (Graphics2D)g;
            	   g2.fill(awtShape);
            	   g2.draw(awtShape);
      	   
               }
           };
       }
	 }
}
