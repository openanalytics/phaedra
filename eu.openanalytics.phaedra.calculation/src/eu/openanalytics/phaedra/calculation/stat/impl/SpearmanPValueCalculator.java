package eu.openanalytics.phaedra.calculation.stat.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.statet.rj.data.RList;
import org.eclipse.statet.rj.servi.RServi;

import eu.openanalytics.phaedra.base.r.rservi.RService;
import eu.openanalytics.phaedra.base.r.rservi.RUtils;
import eu.openanalytics.phaedra.calculation.stat.StatUtils;
import eu.openanalytics.phaedra.calculation.stat.ctx.IStatContext;

public class SpearmanPValueCalculator extends BaseStatCalculator {

	@Override
	public double calculate(IStatContext context) {
		if (context.getDataSets() < 4)
			return Double.NaN;

		double[] plate1Data = context.getData(0);
		double[] plate2Data = context.getData(3);

		if (ArrayUtils.isNotEmpty(plate1Data) && ArrayUtils.isNotEmpty(plate2Data)) {
			RServi rServi = null;
			try {
				rServi = RService.getInstance().createSession();

				rServi.assignData("p1", RUtils.makeNumericRVector(plate1Data), null);
				rServi.assignData("p2", RUtils.makeNumericRVector(plate2Data), null);

				// To calculate spearman:
				RList spearmanResult = (RList) rServi
						.evalData("cor.test(p1, p2, alternative = 'two.sided', method = 'spearman')", null);
				double spearmanPValue = spearmanResult.get("p.value").getData().getNum(0);

				return StatUtils.round(spearmanPValue, 2);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return Double.NaN;
			} finally {
				if (rServi != null) {
					RService.getInstance().closeSession(rServi);
				}
			}
		}
		return Double.NaN;
	}
}
