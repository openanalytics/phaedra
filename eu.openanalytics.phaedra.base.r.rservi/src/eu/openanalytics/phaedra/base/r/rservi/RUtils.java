package eu.openanalytics.phaedra.base.r.rservi;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import de.walware.rj.data.RDataFrame;
import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;
import de.walware.rj.data.RVector;
import de.walware.rj.data.defaultImpl.RCharacterDataImpl;
import de.walware.rj.data.defaultImpl.RDataFrameImpl;
import de.walware.rj.data.defaultImpl.RIntegerDataImpl;
import de.walware.rj.data.defaultImpl.RNumericDataBImpl;
import de.walware.rj.data.defaultImpl.RVectorImpl;

public class RUtils {

	static public int[] makeMissingIndexArray(double[] array){
		List<Integer> indices = new ArrayList<Integer>();
		for (int i =0;i< array.length;i++){
			if (Double.isNaN(array[i])){
				indices.add(i);
			}
		}
		//return indices.toArray(new Integer[indices.size()]);
		int[]idx = new int[indices.size()];
		for (int i=0;i<indices.size();i++){
			idx[i]= indices.get(i);
		}
		return idx;
	}

	static public RObject makeStringRVector(String[] array) {
		return new RVectorImpl<RCharacterDataImpl>(new RCharacterDataImpl(array));
	}

	static public RObject makeNumericRVector(double[] array) {
		return new RVectorImpl<RNumericDataBImpl>(new RNumericDataBImpl(array));
	}

	static public RObject makeNumericRVector(double[] array, int[] missingIndexArray) {
		return new RVectorImpl<RNumericDataBImpl>(new RNumericDataBImpl(array, missingIndexArray));
	}
	
	static public RObject makeNumericRVector(Double[] array) {
		double[] doubleArray = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			doubleArray[i] = array[i];
		}
		return makeNumericRVector(doubleArray);
	}

	static public RObject makeIntegerRVector(Integer[] array) {
		int[] intArray = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			intArray[i] = array[i];
		}
		return new RVectorImpl<RIntegerDataImpl>(new RIntegerDataImpl(intArray));
	}
	
	static public RObject makeIntegerRVector(int[] array) {
		return new RVectorImpl<RIntegerDataImpl>(new RIntegerDataImpl(array));
	}

	static public RObject makeIntegerRVector(int[] array, int[] missingIndexArray) {
		return new RVectorImpl<RIntegerDataImpl>(new RIntegerDataImpl(array, missingIndexArray));
	}

	static public RDataFrame makeDoubleRDataFrame(String[] columnNames, double[][] array2D) {
		int colCount = columnNames.length;
		int rowCount = array2D[0].length;

		@SuppressWarnings("rawtypes")
		RVector[] vectors = new RVector[colCount];

		for (int i=0; i<colCount; i++) {
			double[] data = new double[rowCount];
			int arrayIndex = 0;

			for (double value : array2D[i]) {
				data[arrayIndex++] = value;

				vectors[i] = new RVectorImpl<RStore>(new RNumericDataBImpl(data));
			}
		}

		RObject[] robjArray = vectors;

		RDataFrame df = new RDataFrameImpl(robjArray, "data.frame", columnNames, null);
		return df;
	}
	
	static public double[][] getDouble2DArrayFromRDataFrame(RDataFrame rDataFrame) {
		double[][] doubleArray2D = new double[rDataFrame.getColumnCount()][rDataFrame.getRowCount()];
		
		for (int i = 0; i < rDataFrame.getColumnCount(); i++) {
			RStore column = rDataFrame.getColumn(i);
			for (int j = 0; j < rDataFrame.getRowCount(); j++) {
				doubleArray2D[i][j] = column.getNum(j);
			}			
		}
		
		return doubleArray2D;
	}

	static public int getIntegerFromList(RList list, String name) {
		int result = 0;
		try {
			RObject o = list.get(name);
			if (o != null) {
				result = o.getData().getInt(0);
			}
		} catch (Exception e) {
			//log.error("Failed to get an Integer from a R-List");
		}
		return result;
	}

	static public double getDoubleFromList(RList list, String name) {
		double result = Double.NaN;
		try {
			RObject o = list.get(name);
			if (o != null) {
				result = o.getData().getNum(0);
			}
		} catch (Exception e) {
			//log.error("Failed to get a Double from a R-List.");
		}
		return result;
	}

	static public double getDoubleFromList(RList list, String name, int decimals) {
		double result = getDoubleFromList(list, name);
		return roundUp(result,decimals);
	}

	static double roundUp(double val, int decimals) {
		if (Double.isNaN(val) || Double.isInfinite(val)) return val;
		BigDecimal bd = new BigDecimal(val);
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}


	static public String getStringFromList(RList list, String name) {
		String result = "";
		try {
			RObject o = list.get(name);
			if (o != null) {
				result = o.getData().getChar(0);
			}
		} catch (Exception e) {
			//log.error("Failed to get a String from a R-List.");
		}
		return result;
	}

	public static double getDoubleFromVector(RObject vector, int index) {
		if (vector == null || !(vector instanceof RVector<?>)) return Double.NaN;
		RVector<?> v = (RVector<?>)vector;
		double retVal = Double.NaN;
		try {
			retVal = v.getData().getNum(index);
		} catch (Exception e) {
			// Could be logical NA. Will return NaN.
		}
		return retVal;
	}
	
	public static void showInPopup(byte[] pngImg, int x, int y) {

		Shell shell = new Shell(Display.getCurrent());
		shell.setText("R Graphic");
		shell.setSize(x, y);
		shell.open();

		GC gc = null;
		Image image = null;
		try {
			gc = new GC(shell);
			image = new Image(gc.getDevice(), new ByteArrayInputStream(pngImg));
			gc.drawImage(image, 0, 0);
		} finally {
			if (image != null) image.dispose();
			if (gc != null) gc.dispose();
		}
	}
}
