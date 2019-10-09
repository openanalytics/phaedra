package eu.openanalytics.phaedra.base.scripting.jep.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.lsmp.djep.vectorJep.VectorJep;
import org.lsmp.djep.vectorJep.function.ElementDivide;
import org.lsmp.djep.vectorJep.function.ElementMultiply;
import org.lsmp.djep.vectorJep.function.Length;
import org.nfunk.jep.JEP;
import org.nfunk.jep.function.Comparative;

import eu.openanalytics.phaedra.base.scripting.jep.JEPFunction;
import eu.openanalytics.phaedra.base.scripting.jep.JEPScriptEngine;
import eu.openanalytics.phaedra.base.scripting.jep.functions.CostumComparative;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Count;
import eu.openanalytics.phaedra.base.scripting.jep.functions.CstOp;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Drop;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Filter;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Glog;
import eu.openanalytics.phaedra.base.scripting.jep.functions.If;
import eu.openanalytics.phaedra.base.scripting.jep.functions.IsNaN;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Logicle;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Mad;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Max;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Mean;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Med;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Min;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Offset;
import eu.openanalytics.phaedra.base.scripting.jep.functions.OffsetRandom;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Shift;
import eu.openanalytics.phaedra.base.scripting.jep.functions.StDev;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Sum;
import eu.openanalytics.phaedra.base.scripting.jep.functions.Var;

public class JEPParser {

	public static JEP parse(String expression, Object dataObject) throws ScriptException {
		Map<String, Object> context = new HashMap<>();
		context.put(JEPScriptEngine.CONTEXT_DATA_OBJECT, dataObject);
		return parse(expression, context);
	}
	
	public static JEP parse(String expression, Map<String, Object> context) throws ScriptException {
		return parseExpression(expression, context);
	}

	/*
	 * Non-public
	 * **********
	 */

	private static JEP parseExpression(String expression, Map<String, Object> context) throws ScriptException {

		JEP jep = createJEP();

		JEPExpression jepExpression = new JEPExpression();
		jepExpression.setExpression(expression);
		jepExpression.setJep(jep);

		List<IScanner> scanners = loadScanners();
		for (IScanner scanner: scanners) {
			VarReference[] refs = scanner.scan(jepExpression, context);
			if (refs == null) continue;
			for (VarReference ref: refs) {
				ref.execute(jepExpression);
			}
		}

		jep.parseExpression(jepExpression.getExpression());
		if (jep.hasError()) {
			throw new ScriptException("JEP evaluation failed: " + jep.getErrorInfo());
		}

		return jep;
	}

	private static List<IScanner> loadScanners() {
		List<IScanner> scanners = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IScanner.EXT_PT_ID);
		for (IConfigurationElement el : config) {
			try {
				Object o = el.createExecutableExtension(IScanner.ATTR_CLASS);
				if (o instanceof IScanner) {
					IScanner scanner = (IScanner)o;
					scanners.add(scanner);
				}
			} catch (CoreException e) {
				// Invalid extension.
			}
		}
		return scanners;
	}

	private static JEP createJEP() {
		VectorJep jep = new VectorJep();
		jep.addStandardConstants();
		jep.addStandardFunctions();
		jep.addComplex();
		jep.addFunction(JEPFunction.logicle.getFunctionName(), new Logicle());
		jep.addFunction(JEPFunction.glog.getFunctionName(), new Glog());
		jep.addFunction(JEPFunction.stdev.getFunctionName(), new StDev());
		jep.addFunction(JEPFunction.var.getFunctionName(), new Var());
		jep.addFunction(JEPFunction.med.getFunctionName(), new Med());
		jep.addFunction(JEPFunction.mean.getFunctionName(), new Mean());
		jep.addFunction(JEPFunction.mad.getFunctionName(), new Mad());
		jep.addFunction(JEPFunction.min.getFunctionName(), new Min());
		jep.addFunction(JEPFunction.max.getFunctionName(), new Max());
		jep.addFunction(JEPFunction.sum.getFunctionName(), new Sum());
		jep.addFunction(JEPFunction.count.getFunctionName(), new Count());
		jep.addFunction(JEPFunction.iff.getFunctionName(), new If());
		jep.addFunction(JEPFunction.filter.getFunctionName(), new Filter());
		jep.addFunction(JEPFunction.shift.getFunctionName(), new Shift());
		jep.addFunction(JEPFunction.offset.getFunctionName(), new Offset());
		jep.addFunction(JEPFunction.offsetrnd.getFunctionName(), new OffsetRandom());
		jep.addFunction(JEPFunction.drop.getFunctionName(), new Drop());
		jep.addFunction(JEPFunction.cstop.getFunctionName(), new CstOp());
		jep.addFunction(JEPFunction.is_nan.getFunctionName(), new IsNaN());
		jep.addFunction("length", new Length());
		jep.setAllowAssignment(true);
		jep.setAllowUndeclared(true);
		jep.setImplicitMul(true);
		jep.setElementMultiply(true);
		//Add custom comperative to jep this allows comparison between vectors and single numbers
		jep.getOperatorSet().getGT().setPFMC(new CostumComparative(Comparative.GT));
		jep.getOperatorSet().getLT().setPFMC(new CostumComparative(Comparative.LT));
		jep.getOperatorSet().getEQ().setPFMC(new CostumComparative(Comparative.EQ));
		jep.getOperatorSet().getLE().setPFMC(new CostumComparative(Comparative.LE));
		jep.getOperatorSet().getGE().setPFMC(new CostumComparative(Comparative.GE));
		jep.getOperatorSet().getNE().setPFMC(new CostumComparative(Comparative.NE));
		jep.getOperatorSet().getMultiply().setPFMC(new ElementMultiply());
		jep.getOperatorSet().getDivide().setPFMC(new ElementDivide());
		return jep;
	}

}