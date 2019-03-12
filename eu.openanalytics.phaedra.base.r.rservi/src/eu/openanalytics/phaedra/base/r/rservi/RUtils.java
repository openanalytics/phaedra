package eu.openanalytics.phaedra.base.r.rservi;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.eclipse.statet.rj.data.RCharacterStore;
import org.eclipse.statet.rj.data.RDataFrame;
import org.eclipse.statet.rj.data.RIntegerStore;
import org.eclipse.statet.rj.data.RList;
import org.eclipse.statet.rj.data.RNumericStore;
import org.eclipse.statet.rj.data.RObject;
import org.eclipse.statet.rj.data.RStore;
import org.eclipse.statet.rj.data.RVector;
import org.eclipse.statet.rj.data.impl.RCharacter32Store;
import org.eclipse.statet.rj.data.impl.RDataFrame32Impl;
import org.eclipse.statet.rj.data.impl.RInteger32Store;
import org.eclipse.statet.rj.data.impl.RList32Impl;
import org.eclipse.statet.rj.data.impl.RNullImpl;
import org.eclipse.statet.rj.data.impl.RNumericB32Store;
import org.eclipse.statet.rj.data.impl.RVectorImpl;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class RUtils {

	public static RObject makeRObject(Object value, boolean unknownToNull) {
		if (value == null) {
			return RNullImpl.INSTANCE;
		} else if (value instanceof String) {
			return makeStringRVector(new String[] { (String) value });
		} else if (value instanceof Number) {
			return makeNumericRVector(new double[] { ((Number) value).doubleValue() });
		} else if (value instanceof String[]) {
			return makeStringRVector((String[]) value);
		} else if (value instanceof double[]) {
			return makeNumericRVector((double[]) value);
		} else if (value instanceof float[]) {
			float[] v = (float[]) value;
			double[] copy = new double[v.length];
			for (int i = 0; i < copy.length; i++) { copy[i] = v[i]; }
			return makeNumericRVector(copy);
		} else if (value instanceof int[]) {
			return makeIntegerRVector((int[]) value);
		} else if (value instanceof Object[]) {
			Object[] v = (Object[]) value;
			RObject[] rV = new RObject[v.length];
			for (int i = 0; i < v.length; i++) { rV[i] = makeRObject(v[i], unknownToNull); }
			String[] names = IntStream.range(1, v.length + 1).mapToObj(i -> String.valueOf(i)).toArray(i -> new String[i]);
			return new RList32Impl(rV, names);
		} else {
			if (unknownToNull) return RNullImpl.INSTANCE;
			else throw new RuntimeException("Unsupported data type: " + value.getClass().getName());
		}
	}
	
	public static int[] makeMissingIndexArray(double[] array) {
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

	public static RObject makeStringRVector(String[] array) {
		return new RVectorImpl<RCharacterStore>(new RCharacter32Store(array));
	}

	public static RObject makeNumericRVector(double[] array) {
		return new RVectorImpl<RNumericStore>(new RNumericB32Store(array));
	}

	public static RObject makeNumericRVector(double[] array, int[] missingIndexArray) {
		return new RVectorImpl<RNumericStore>(new RNumericB32Store(array, missingIndexArray));
	}
	
	public static RObject makeNumericRVector(Double[] array) {
		double[] doubleArray = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			doubleArray[i] = array[i];
		}
		return makeNumericRVector(doubleArray);
	}

	public static RObject makeIntegerRVector(Integer[] array) {
		int[] intArray = new int[array.length];
		for (int i = 0; i < array.length; i++) {
			intArray[i] = array[i];
		}
		return new RVectorImpl<RIntegerStore>(new RInteger32Store(intArray));
	}
	
	public static RObject makeIntegerRVector(int[] array) {
		return new RVectorImpl<RIntegerStore>(new RInteger32Store(array));
	}

	public static RObject makeIntegerRVector(int[] array, int[] missingIndexArray) {
		return new RVectorImpl<RIntegerStore>(new RInteger32Store(array, missingIndexArray));
	}

	public static RDataFrame makeDoubleRDataFrame(String[] columnNames, double[][] array2D) {
		int colCount = columnNames.length;
		int rowCount = array2D[0].length;

		@SuppressWarnings("rawtypes")
		RVector[] vectors = new RVector[colCount];

		for (int i=0; i<colCount; i++) {
			double[] data = new double[rowCount];
			int arrayIndex = 0;

			for (double value : array2D[i]) {
				data[arrayIndex++] = value;

				vectors[i] = new RVectorImpl<RStore<?>>(new RNumericB32Store(data));
			}
		}

		RObject[] robjArray = vectors;

		RDataFrame df = new RDataFrame32Impl(robjArray, "data.frame", columnNames, null);
		return df;
	}
	
	public static double[][] getDouble2DArrayFromRDataFrame(RDataFrame rDataFrame) {
		double[][] doubleArray2D = new double[(int) rDataFrame.getColumnCount()][(int) rDataFrame.getRowCount()];
		
		for (int i = 0; i < rDataFrame.getColumnCount(); i++) {
			RStore<?> column = rDataFrame.getColumn(i);
			for (int j = 0; j < rDataFrame.getRowCount(); j++) {
				doubleArray2D[i][j] = column.getNum(j);
			}			
		}
		
		return doubleArray2D;
	}

	public static int getIntegerFromList(RList list, String name) {
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

	public static double getDoubleFromList(RList list, String name) {
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

	public static double getDoubleFromList(RList list, String name, int decimals) {
		double result = getDoubleFromList(list, name);
		return roundUp(result,decimals);
	}

	public static Object getAsJavaObject(RObject value) {
		if (value == null || RNullImpl.INSTANCE.equals(value)) return null;
		
		Object retVal = null;
		int valueSize = (int) value.getLength();
		RStore<?> valueData = value.getData();
		
		switch (value.getRClassName()) {
		case RObject.CLASSNAME_CHARACTER:
			if (valueSize == 1) {
				retVal = valueData.getChar(0);
			} else {
				String[] jValue = new String[valueSize];
				for (int i = 0; i < jValue.length; i++) jValue[i] = valueData.getChar(i);
				retVal = jValue;
			}
			break;
		case RObject.CLASSNAME_NUMERIC:
			if (valueSize == 1) {
				retVal = valueData.getNum(0);
			} else {
				double[] jValue = new double[valueSize];
				for (int i = 0; i < jValue.length; i++) jValue[i] = valueData.getNum(i);
				retVal = jValue;
			}
			break;
		case RObject.CLASSNAME_INTEGER:
			if (valueSize == 1) {
				retVal = valueData.getInt(0);
			} else {
				int[] jValue = new int[valueSize];
				for (int i = 0; i < jValue.length; i++) jValue[i] = valueData.getInt(i);
				retVal = jValue;
			}
			break;
		default:
			retVal = value.toString();	
		}
		//TODO add more conversion types here
		return retVal;
	}
	
	public static double roundUp(double val, int decimals) {
		if (Double.isNaN(val) || Double.isInfinite(val)) return val;
		BigDecimal bd = new BigDecimal(val);
		bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}


	public static String getStringFromList(RList list, String name) {
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
