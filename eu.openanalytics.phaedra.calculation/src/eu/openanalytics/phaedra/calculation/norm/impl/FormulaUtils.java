package eu.openanalytics.phaedra.calculation.norm.impl;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.openanalytics.phaedra.calculation.norm.INormalizer;


public class FormulaUtils {
	
	private static class Formula {
		
		private INormalizer normalizer;
		
		private String ltx;
		private String alt;
		
		public Formula(INormalizer normalizer, String code) {
			this.normalizer = normalizer;
			
			code = "NORMALIZED_VALUE = " + code;
			this.ltx = toLtx(code);
			this.alt = toAlt(code);
		}
		
		private String toLtx(String code) {
			String s = code;
			s = s.replace("NORMALIZED_VALUE", "\\var{normalized}_{ij}");
			s = s.replace("X_VALUE", "x_{ij}");
//			s = s.replace("Mean", "\\mu");
//			s = s.replace("Median", "\\tilde{x}");
//			s = s.replace("SD", "\\sigma");
			s = s.replace("Mean", "\\stat{Mean}");
			s = s.replace("Median", "\\stat{Median}");
			s = s.replace("SD", "\\stat{SD}");
			s = s.replace("MAD", "\\stat{MAD}");
			s = s.replace("(X_S)", "_{\\welltype{S}}");
			s = s.replace("(X_S+LC)", "_{\\welltype{S \\cup LC}}");
			s = s.replace("(X_HC)", "_{\\welltype{HC}}");
			s = s.replace("(X_LC)", "_{\\welltype{LC}}");
			s = s.replace("�", "\\cdot");
			return s;
		}
		
		private String toAlt(String code) {
			String s = code;
			s = s.replace("NORMALIZED_VALUE", "normalized");
			s = s.replace("X_VALUE", "x");
			s = s.replace("\\frac{", "(");
			s = s.replace("}{", "/");
			s = s.replace("}", ")");
			return s;
		}
		
	}
	
	private static String frac(String num, String den) {
		return "\\frac{" + num + "}{" + den + "}";
	}
	
	private static List<Formula> NORM_METHODS = Arrays.asList(
			
			new Formula(new PctEffectNormalizer(),
					frac("X_VALUE - Median(X_LC)",  "Median(X_HC) - Median(X_LC)") +    " � 100" ),
			new Formula(new MinusPctEffectNormalizer(),
					"- " +
					frac("X_VALUE - Median(X_LC)",  "Median(X_HC) - Median(X_LC)") +    " � 100" ),
			new Formula(new PctEffectMinus100Normalizer(),
					frac("X_VALUE - Median(X_LC)",  "Median(X_HC) - Median(X_LC)") +    " � 100 " +
					"- 100"),
			new Formula(new PctEffectInverseNormalizer(),
					"100 - " +
					frac("X_VALUE - Median(X_LC)",  "Median(X_HC) - Median(X_LC)") +    " � 100" ),
			
			new Formula(new PctCtlNormalizer(),
					frac("X_VALUE",                 "Median(X_HC)") +       " � 100" ),
			new Formula(new PctHighCtl0Normalizer(),
					frac("Median(X_HC) - X_VALUE",  "Median(X_HC)") +       " � 100" ),
			new Formula(new PctLowCtlNormalizer(),
					frac("X_VALUE - Median(X_LC)",  "Median(X_LC)") +       " � 100" ),
			new Formula(new PctLowCtl0Normalizer(),
					frac("X_VALUE - Median(X_LC)",  "Median(X_LC)") +       " � 100" ),
			new Formula(new PctLowCtl100Normalizer(),
					frac("X_VALUE",                 "Median(X_LC)") +       " � 100" ),
			new Formula(new PctInvLowCtl0Normalizer(),
					"- " +
					frac("X_VALUE - Median(X_LC)",  "Median(X_LC)") +       " � 100" ),
			new Formula(new PctCtlMinNormalizer(),
					frac("X_VALUE - Median(X_LC)",  "Median(X_HC) - Median(X_LC)") +    " � 100" ),
			
			new Formula(new SigmaHighNormalizer(),
					frac("X_VALUE - Median(X_HC)",  "SD(X_HC)")),
			new Formula(new SigmaLowNormalizer(),
					frac("X_VALUE - Median(X_LC)",  "SD(X_LC)")),
			
			new Formula(new PctInhPosMeanNormalizer(),
					frac("X_VALUE - Mean(X_LC)",    "Mean(X_HC) - Mean(X_LC)") +        " � 100" ),
			new Formula(new PctInhPosMedianNormalizer(),
					frac("X_VALUE - Median(X_LC)",  "Median(X_HC) - Median(X_LC)") +    " � 100" ),
			new Formula(new PctInhNegMeanNormalizer(),
					"100 - " +
					frac("X_VALUE - Mean(X_LC)",    "Mean(X_HC) - Mean(X_LC)") +        " � 100" ),
			new Formula(new PctInhNegMedianNormalizer(),
					"100 - " +
					frac("X_VALUE - Median(X_LC)",  "Median(X_HC) - Median(X_LC)") +    " � 100" ),
			
			new Formula(new ZScoreSamplesNormalizer(),
					frac("X_VALUE - Mean(X_S+LC)",  "SD(X_S+LC)") ), 
			new Formula(new ZScoreLowNormalizer(),
					frac("X_VALUE - Mean(X_LC)",    "SD(X_LC)") ),
			new Formula(new ZScoreRobSamplesLowNormalizer(),
					frac("X_VALUE - Median(X_S+LC)", "1.4826 � MAD(X_S+LC)") ),
			new Formula(new ZScoreRobLowNormalizer(),
					frac("X_VALUE - Median(X_LC)",  "1.4826 � MAD(X_LC)") ),
			new Formula(new ZScoreRobSamplesNormalizer(),
					frac("X_VALUE - Median(X_S)", "1.4826 � MAD(X_S)") )
			);
	
	public static String getBaseFileName(INormalizer normalizer) {
		return normalizer.getClass().getSimpleName();
	}
	
	private static String LTX_TEMPLATE = "\\documentclass[preview]{standalone}\n" +
			"\\usepackage[T1]{fontenc}\n" +
			"\\usepackage{amsmath}\n" +
			
			"\\newcommand{\\var}[1]{\\mathit{#1}}\n" +
			"\\newcommand{\\stat}[1]{\\mathit{#1}}\n" +
			"\\newcommand{\\welltype}[1]{\\mathit{#1}}\n" +
			
//			"\\usepackage{empheq}\\fboxsep=0.01pt\n" +
//			"\\usepackage{xcolor}\n" +
			
			"\\pagestyle{empty}\n" +
			"\\begin{document}\n" +
//			"$\\displaystyle FORMULA $\n" +
			"\\begin{equation*}\nFORMULA\n\\end{equation*}\n" +
//			"\\begin{empheq}[box=\\colorbox{white!0!}]{equation*}\nFORMULA\\end{empheq}" +
			"\\end{document}\n";
	
	private static double[] LTX_VIEWBOX_TWEAK = { 0, -1, 2, 2.5 };
	
	
	/**
	 * Note, when setting up the Java process to launch this tool:
	 * - all files are created in the working directory
	 * - all environment variables required for latex should be set
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		generateSvg();
		generateHtmlTable("preview", "");
		generateHtmlTable("doc", "../../Images/formulas/");
	}
	
	private static void generateSvg() throws IOException, InterruptedException {
		ProcessBuilder pbLatex = new ProcessBuilder(
				"latex", "FILENAME");
		int pbLatexFileIdx = pbLatex.command().indexOf("FILENAME");
//		pbLatex.directory(directory.toFile());
		pbLatex.redirectOutput(Redirect.INHERIT);
		pbLatex.redirectError(Redirect.INHERIT);
		
		ProcessBuilder pbToSvg = new ProcessBuilder(
				"dvisvgm", "--scale=1.33", "--no-fonts", "--exact-bbox", "--optimize", "FILENAME");
//		pbLatex.directory(directory.toFile());
		pbLatex.command();
		int pbToSvgFileIdx = pbToSvg.command().indexOf("FILENAME");
		pbLatex.redirectOutput(Redirect.INHERIT);
		pbLatex.redirectError(Redirect.INHERIT);
		
		for (Formula formula : NORM_METHODS) {
			String baseName = getBaseFileName(formula.normalizer);
			Path svgPath = Paths.get(baseName + ".svg");
			System.out.println("\n### " + formula.normalizer.getId() + " " + baseName);
			System.out.flush();
			
			String doc = LTX_TEMPLATE.replace("FORMULA", formula.ltx);
			Files.write(Paths.get(baseName + ".tex"), doc.getBytes(StandardCharsets.ISO_8859_1));
			Files.deleteIfExists(svgPath);
			
			{	pbLatex.command().set(pbLatexFileIdx, baseName + ".tex");
				Process process = pbLatex.start();
				int exit = process.waitFor();
				if (exit != 0) {
					throw new IOException("ExitCode= " + exit);
				}
			}
			{	pbToSvg.command().set(pbToSvgFileIdx, baseName + ".dvi");
				Process process = pbToSvg.start();
				int exit = process.waitFor();
				if (exit != 0) {
					throw new IOException("ExitCode= " + exit);
				}
			}
			
			System.out.flush();
			System.err.flush();
			
			System.setProperty("line.separator", "\n");
			List<String> lines = Files.readAllLines(svgPath, StandardCharsets.UTF_8);
			Matcher matcher = Pattern.compile("\\<svg.* width='([0-9.]+)pt' height='([0-9.]+)pt' viewBox='([\\+\\-0-9. ]+)'\\>").matcher("");
			int tweaked = 0;
			for (ListIterator<String> iter = lines.listIterator(); iter.hasNext();) {
				String line = iter.next();
				if (matcher.reset(line).matches()) {
					double width = Double.parseDouble(matcher.group(1));
					double height = Double.parseDouble(matcher.group(2));
					String[] viewBox = matcher.group(3).split(" ");
					for (int i = 0; i < viewBox.length; i++) {
						double v = Double.parseDouble(viewBox[i]);
						v += LTX_VIEWBOX_TWEAK[i];
						viewBox[i] = Double.toString(v);
					}
					width += LTX_VIEWBOX_TWEAK[2];
					height += LTX_VIEWBOX_TWEAK[3];
					line = new StringBuilder().append(line, 0, matcher.start(1)).append(width)
							.append(line, matcher.end(1), matcher.start(2)).append(height)
							.append(line, matcher.end(2), matcher.start(3)).append(String.join(" ", viewBox))
							.append(line, matcher.end(3), line.length()).toString();
					iter.set(line);
					tweaked |= 1;
					break;
				}
			}
			if (tweaked != 1) {
				System.err.println("Tweak failed: " + Integer.toBinaryString(tweaked));
			}
			Files.write(svgPath, lines, StandardCharsets.UTF_8);
		}
	}
	
	private static final String CSS =
			"table.mltable {\n" +
				"border-collapse: collapse;\n" +
				"border: 1px solid;\n" +
				"padding: 0px;\n" +
			"}\n" +
			"table.mltable th, table.mltable td {\n" +
				"padding: 5px;\n" +
				"text-align: left;\n" +
			"}\n" +
			"table.mltable tr.rowgroup-first th, table.mltable tr.rowgroup-first td {\n" +
				"border-top: 1px solid;\n" +
			"}\n";
	
	private static void generateHtmlTable(String name, String rel) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html><head>");
		sb.append("<style>\n").append(CSS).append("</style>");
		sb.append("</head><body>\n");
		
		sb.append("<table class=\"mltable\">\n");
		for (Formula formula : NORM_METHODS) {
			String svgFileName = rel + getBaseFileName(formula.normalizer) + ".svg";
			sb.append("<tr class=\"rowgroup-first\">")
					.append("<th>").append(formula.normalizer.getId()).append("</th>")
					.append("<td>").append((formula.normalizer.getDescription().startsWith("value =")) ? "" : formula.normalizer.getDescription()).append("</td>")
				.append("</tr>\n<tr>")
					.append("<td colspan=\"2\"><img")
//							.append("style=\"\"")
							.append(" src=\"").append(svgFileName).append("\"")
							.append(" alt=\"").append(formula.alt).append("\"/></td>")
				.append("</tr>\n");
		}
		sb.append("</table>\n");
		
		sb.append("</body></html>");
		
		Files.write(Paths.get(name + ".html"), sb.toString().getBytes(StandardCharsets.ISO_8859_1));
	}
	
}
