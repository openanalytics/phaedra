package eu.openanalytics.phaedra.base.ui.charting.r;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.Image;

import de.walware.rj.servi.RServi;
import de.walware.rj.services.FunctionCall;
import de.walware.rj.services.utils.Graphic;
import de.walware.rj.services.utils.PngGraphic;
import eu.openanalytics.phaedra.base.r.rservi.CairoSvgGraphic;
import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.base.ui.charting.data.IDataProviderR;

public class FeatureScatterMatrixPlotter {

	public static <E> Image createPlot(IDataProviderR dataProvider, int w, int h) {
		Image image = null;
		RServi rSession = null;
		try {
			rSession = RService.getInstance().createSession();
			rSession.evalVoid("library(lattice)", null);

			// Build datasets per series (grouping).
			FunctionCall plotFun;
			createDataFrame(dataProvider, rSession);

			createVariables(dataProvider, rSession);

			// Obtain graphic
			PngGraphic pngGraphic = new PngGraphic();
			pngGraphic.setSize(w, h, Graphic.UNIT_PX);
			plotFun = createLevelPlot(rSession, "1.2");
			
			byte[] plot = pngGraphic.create(plotFun, rSession, null);
			image = new Image(null, new ByteArrayInputStream(plot));

		} catch (Exception e) {
			throw new RuntimeException("Failed to create plot", e);
		} finally {
			if (rSession != null) RService.getInstance().closeSession(rSession);
		}

		return image;
	}

	public static <E> byte[] createSVGPlot(IDataProviderR dataProvider, int w, int h) {
		byte[] plot = null;
		RServi rSession = null;
		try {
			rSession = RService.getInstance().createSession();
			rSession.evalVoid("library(Cairo)", null);

			FunctionCall plotFun;
			createDataFrame(dataProvider, rSession);

			createVariables(dataProvider, rSession);

			// Obtain graphic
			CairoSvgGraphic svgGraphic = new CairoSvgGraphic();
			svgGraphic.setSize(w, h, Graphic.UNIT_PX);
			plotFun = createLevelPlot(rSession, "0.4");

			plot = svgGraphic.create(plotFun, rSession, new NullProgressMonitor());

		} catch (Exception e) {
			throw new RuntimeException("Failed to create plot", e);
		} finally {
			if (rSession != null)
				try { rSession.close(); } catch (CoreException e) {}
		}

		return plot;
	}

	private static void createDataFrame(IDataProviderR dataProvider, RServi rSession) throws CoreException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("data <- data.frame(");
		
		for (Entry<String, List<Object>> entry : dataProvider.getDataFrame().entrySet()) {
			String key = entry.getKey();
			if (entry.getValue().size() < 1) continue;
			
			sb.append(key + ",");
			Object sample = entry.getValue().get(0);
			if (sample instanceof String)
				rSession.assignData(key, RUtils.makeStringRVector(entry.getValue().toArray(new String[entry.getValue().size()])), null);
			else if (sample instanceof Double)
				rSession.assignData(key, RUtils.makeNumericRVector(entry.getValue().toArray(new Double[entry.getValue().size()])), null);
			else if (sample instanceof Integer)
				rSession.assignData(key, RUtils.makeIntegerRVector(entry.getValue().toArray(new Integer[entry.getValue().size()])), null);
		}

		sb.deleteCharAt(sb.length()-1);
		sb.append(")");
		rSession.evalVoid(sb.toString(), null);
	}

	private static void createVariables(IDataProviderR dataProvider, RServi rSession) throws CoreException {
		StringBuilder sb = new StringBuilder();
		sb.append("formula <- ~");
		
		int size = 0;
		for (int i = 1; i <= dataProvider.getDataFrame().get("Features").size(); i++) {
			sb.append("F" + i);
			sb.append("+");
			size = i;
		}
		sb.setLength(sb.length() - 1);
		sb.append("|Welltype");
		
		rSession.evalVoid("controlColorList <- list(ALL=\"purple\",EMPTY=\"#969696\",HC=\"#00C800\",LC=\"#C80000\",SAMPLE=\"#0000C8\")", null);
		rSession.evalVoid("type <- factor(data$Welltype)", null);
		rSession.evalVoid("controlColorsVec <- as.character(factor(type, labels = unlist(controlColorList[levels(type)])))", null);
		rSession.evalVoid(sb.toString(), null);
		rSession.evalVoid("size <- " + size, null);
	}

	/**
	 * Create the function call.
	 * @param rSession The R session
	 * @param pointSize The point size (e.g. "1.2")
	 * @return
	 * @throws CoreException
	 */
	private static FunctionCall createLevelPlot(RServi rSession, String pointSize) throws CoreException {
		FunctionCall plotFun;
		plotFun = rSession.createFunctionCall("pairs");
		plotFun.add("x", "data[1:size]");
		plotFun.add("formula", "formula");
		plotFun.add("data", "data");
		plotFun.add("labels", "data$Features");
		plotFun.add("pch", "19");
		plotFun.add("col", "controlColorsVec");
		plotFun.add("cex", pointSize);
		// Legend can only be called after plot is created
		// Since this happens in the Png or SVG Graphics it's impossible to call it.
		// legend(x=0.5,y=-0.045,legend=type,hor=TRUE,xjust = 0.5,bty=\"n\",fill=unlist(controlColorList[type]))
		return plotFun;
	}

}