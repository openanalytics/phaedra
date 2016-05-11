package eu.openanalytics.phaedra.base.util.convert;

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFPrintPage;

public class PDFToImageConverter {

	public static Image convert(byte[] pdfBytes) throws IOException {
		return convert(pdfBytes, 0, 0, 0);
	}

	public static Image convert(byte[] pdfBytes, int width, int height) throws IOException {
		return convert(pdfBytes, 0, width, height);
	}

	public static Image convert(byte[] pdfBytes, int page, int width, int height) throws IOException {
		java.awt.Image awtImage = convertToAwt(pdfBytes, page, width, height);
		return AWTImageConverter.convert(Display.getDefault(), awtImage);
	}

	/*
	 * Same methods but return a java.awt.Image instead of
	 * org.eclipse.swt.graphics.Image.
	 */

	public static java.awt.Image convertToAwt(byte[] pdfBytes) throws IOException {
		return convertToAwt(pdfBytes, 0, 0, 0);
	}

	public static java.awt.Image convertToAwt(byte[] pdfBytes, int width, int height) throws IOException {
		return convertToAwt(pdfBytes, 0, width, height);
	}

	public static java.awt.Image convertToAwt(byte[] pdfBytes, int page, int width, int height) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(pdfBytes);
		PDFFile pdfFile = new PDFFile(buffer);
		PDFPage pdfPage = null;
		synchronized (PDFPage.class) {
			// Synced as a workaround for Java8 error "LCMS error 13: Couldn't link the profiles"
			pdfPage = pdfFile.getPage(page, true);
		}
		java.awt.Rectangle rect = new java.awt.Rectangle(0,0,
				(int)pdfPage.getBBox().getWidth(),
				(int)pdfPage.getBBox().getHeight());
		if (width == 0) width = rect.width;
		if (height == 0) height = rect.height;
		java.awt.Image result = pdfPage.getImage(width,height,rect,null,true,true);
		return result;
	}

	public static byte[] convertToSVG(byte[] pdfBytes, int width, int height) throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap(pdfBytes);
		PDFFile pdfFile = new PDFFile(buffer);
		
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		Document document = domImpl.createDocument(null, "svg", null);
		SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);
		ctx.setEmbeddedFontsOn(true);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); 
		Writer out = new OutputStreamWriter(outputStream, "UTF-8");
		
		for(int i = 0; i < pdfFile.getNumPages(); i++) {
			SVGGraphics2D svg = new SVGGraphics2D(ctx, false);
			svg.setSVGCanvasSize(new Dimension(width, height));
			PDFPrintPage page = new PDFPrintPage(pdfFile);
			PageFormat format = new PageFormat();
			format.setOrientation(1);
			Paper paper = new Paper();
			paper.setSize(width, height);
			paper.setImageableArea(0, 0, width, height);
			format.setPaper(paper);
			page.print(svg, format, i);
			svg.stream(out);
		}
		
		return outputStream.toByteArray();
	}

}