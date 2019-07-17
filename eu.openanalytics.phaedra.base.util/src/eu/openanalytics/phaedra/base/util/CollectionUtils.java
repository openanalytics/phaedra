package eu.openanalytics.phaedra.base.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CollectionUtils {

	/**
	 * Returns the first element of a List, or null if there is no first element.
	 *
	 * @param list The List whose first element you want.
	 * @return The first element, or null if the List is empty or null.
	 */
	public static <E> E getFirstElement(List<E> list) {
		if (list == null || list.isEmpty()) return null;
		return list.get(0);
	}

	/**
	 * Return the most recently added element. Null if there is none.
	 *
	 * @param list The List whose most recent element you want.
	 * @return The most recently added element, or null if there is none.
	 */
	public static <E> E getLastElement(List<E> list) {
		if (list == null || list.isEmpty()) return null;
		return list.get(list.size()-1);
	}

	/**
	 * Look for the first element in the Iterable that matches the Predicate.
	 * Returns null if no element was found.
	 *
	 * @param iterable The Iterable to check.
	 * @param predicate The Predicate to check against.
	 * @return The first match, or null if no match exists.
	 */
	public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate) {
		for (T element: iterable) {
			if (predicate.test(element)) return element;
		}
		return null;
	}

	public static <T> List<T> findAll(List<T> list, Predicate<? super T> predicate) {
		// Note: list could be an IndirectList which doesn't support streaming.
		List<T> results = new ArrayList<T>();
		for (T element: list) {
			if (predicate.test(element)) results.add(element);
		}
		return results;
	}

	public static <F, T> List<T> transform(F[] array, Function<? super F, ? extends T> function) {
		return Arrays.stream(array).map(function).collect(Collectors.toList());
	}

	public static <F, T> List<T> transform(List<F> list, Function<? super F, ? extends T> function) {
		return list.stream().map(function).collect(Collectors.toList());
	}

	public static <F> String[] transformToStringArray(List<F> list, Function<? super F, String> function) {
		return list.stream().map(function).toArray(String[]::new);
	}

	/**
	 * Add an element to a List, but only if that element isn't there yet.
	 *
	 * @param list The List to add the element to.
	 * @param element The element to add.
	 */
	public static <E> void addUnique(List<E> list, E element) {
		if (!list.contains(element)) list.add(element);
	}

	public static <E> void addUnique(List<E> list, List<E> addList) {
		for (E element : addList){
			addUnique(list, element);
		}
	}

	@SuppressWarnings("unchecked")
	public static <E> void addUnique(List<E> list, E... elements) {
		for (E element : elements){
			addUnique(list, element);
		}
	}

	/**
	 * Ensures that the collection with unique elements contains/not contains the given element.
	 * If required, the element is added or removed.
	 * 
	 * @param c the collection
	 * @param element the element
	 * @param contains if the collection should contain or not contain the element
	 */
	public static <E> void setContains(Collection<E> c, E element, boolean contains) {
		if (contains) {
			if (!c.contains(c)) c.add(element);
		}
		else {
			c.remove(element);
		}
	}
	
	/**
	 * Returns if the collections contains any of the given elements.
	 * 
	 * @param c the collection
	 * @param e1 element to check
	 * @param e2 element to check
	 * @return <code>true</code> if the collections contains at least one of the elements
	 */
	public static <E> boolean containsAny(Collection<E> c, E e1, E e2) {
		return (c.contains(e1) || c.contains(e2));
	}
	
	/**
	 * Find the index of an item in an array.
	 *
	 * @param array The array of items.
	 * @param item The item to find.
	 * @return The index of the item, or -1 if it isn't in the array.
	 */
	public static int find(Object[] array, Object item) {
		for (int i=0; i<array.length; i++) {
			Object o = array[i];
			if (item.equals(o)) return i;
		}
		return -1;
	}

	/**
	 * Find the index of an item in an array.
	 *
	 * @param array The array of items.
	 * @param item The item to find.
	 * @return The index of the item, or -1 if it isn't in the array.
	 */
	public static int find(int[] array, int item) {
		for (int i=0; i<array.length; i++) {
			int o = array[i];
			if (item == o) return i;
		}
		return -1;
	}

	/**
	 * Test whether an item is contained at least once in an array.
	 *
	 * @param array The array of items.
	 * @param item The item to find.
	 * @return True if the item is in the array, false otherwise.
	 */
	public static boolean contains(Object[] array, Object item) {
		return find(array, item) >= 0;
	}

	/**
	 * Test whether an item is contained at least once in an array.
	 *
	 * @param array The array of items.
	 * @param item The item to find.
	 * @return True if the item is in the array, false otherwise.
	 */
	public static boolean contains(int[] array, int item) {
		return find(array, item) >= 0;
	}

	/**
	 * Convert a List of Doubles into an array of doubles.
	 *
	 * @param list The List of Doubles.
	 * @return An array of doubles.
	 */
	public static double[] toArray(List<Double> list) {
		double[] array = new double[list.size()];
		for (int i=0; i<array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	public static int[] toIntArray(List<Integer> list) {
		int[] array = new int[list.size()];
		for (int i=0; i<array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	public static double[] toDoubles(float[] floats) {
		if (floats == null) return null;
		double[] array = new double[floats.length];
		for (int i=0; i<array.length; i++) {
			array[i] = floats[i];
		}
		return array;
	}

	public static float[] toFloats(double[] doubles) {
		if (doubles == null) return null;
		float[] array = new float[doubles.length];
		for (int i=0; i<array.length; i++) {
			array[i] = (float)doubles[i];
		}
		return array;
	}

	public static <E> String toString(Collection<E> list, String separator) {
		StringBuilder builder = new StringBuilder();
		list.stream().forEach(item -> {
			if (builder.length() != 0) builder.append(separator);
			builder.append(item.toString());
		});
		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	public static <E> E[] listToArray(List<E> list) {
		if (list.isEmpty()) return null;
		Class<E> clazz = (Class<E>)list.get(0).getClass();
		E[] array = (E[]) Array.newInstance(clazz, list.size());
		for (int i=0; i<list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	/**
	 * Create an array of integers, starting with value startValue at index 0,
	 * incrementing up to startValue + size at index size - 1.
	 *
	 * @param size The size of the array to create.
	 * @param startValue The first value, at index 0.
	 * @return An array filled with incrementing integers, e.g. {0, 1, 2, ...}
	 */
	public static int[] fillIncrementingArray(int size, int startValue) {
		int[] array = new int[size];
		int value = startValue;
		for (int i=0; i<size; i++) {
			array[i] = value;
			value++;
		}
		return array;
	}

	/**
	 * Create an array of integers, starting with value startValue at index 0,
	 * incrementing up to startValue + size at index size - 1.
	 *
	 * @param size The size of the array to create.
	 * @param startValue The first value, at index 0.
	 * @return An array filled with incrementing integers, e.g. {0, 1, 2, ...}
	 */
	public static Integer[] fillIncrementingObjectArray(int size, int startValue) {
		Integer[] array = new Integer[size];
		int value = startValue;
		for (int i=0; i<size; i++) {
			array[i] = value;
			value++;
		}
		return array;
	}

	public static void shuffleArray(Object[] array) {
		int index;
		Object temp;
		Random rnd = new Random();
		for (int i = array.length - 1; i > 0; i--) {
			index = rnd.nextInt(i + 1);
			temp = array[index];
			array[index] = array[i];
			array[i] = temp;
		}
	}

	public static int getSize(Object array) {
		if (array instanceof float[]) return ((float[])array).length;
		else if (array instanceof double[]) return ((double[])array).length;
		else if (array instanceof int[]) return ((int[])array).length;
		else if (array instanceof short[]) return ((short[])array).length;
		else if (array instanceof long[]) return ((long[])array).length;
		else if (array instanceof String[]) return ((String[])array).length;
		else if (array instanceof char[]) return ((char[])array).length;
		else if (array instanceof byte[]) return ((byte[])array).length;
		else if (array instanceof Object[]) return ((Object[])array).length;
		else return 0;
	}

	public static Object removeElements(Object array, int[] itemsToRemove) {
		if (itemsToRemove == null || itemsToRemove.length == 0) return array;

		Arrays.sort(itemsToRemove);
		int inputIndex = 0;
		int outputIndex = 0;

		if (array instanceof float[]) {
			float[] input = (float[]) array;
			float[] result = new float[input.length - itemsToRemove.length];
			while (inputIndex < input.length) {
				if (contains(itemsToRemove, inputIndex)) inputIndex++;
				else result[outputIndex++] = input[inputIndex++];
			}
			return result;
		} else if (array instanceof double[]) {
			double[] input = (double[]) array;
			double[] result = new double[input.length - itemsToRemove.length];
			while (inputIndex < input.length) {
				if (contains(itemsToRemove, inputIndex)) inputIndex++;
				else result[outputIndex++] = input[inputIndex++];
			}
			return result;
		} else if (array instanceof int[]) {
			int[] input = (int[]) array;
			int[] result = new int[input.length - itemsToRemove.length];
			while (inputIndex < input.length) {
				if (contains(itemsToRemove, inputIndex)) inputIndex++;
				else result[outputIndex++] = input[inputIndex++];
			}
			return result;
		} else if (array instanceof long[]) {
			long[] input = (long[]) array;
			long[] result = new long[input.length - itemsToRemove.length];
			while (inputIndex < input.length) {
				if (contains(itemsToRemove, inputIndex)) inputIndex++;
				else result[outputIndex++] = input[inputIndex++];
			}
			return result;
		} else if (array instanceof String[]) {
			String[] input = (String[]) array;
			String[] result = new String[input.length - itemsToRemove.length];
			while (inputIndex < input.length) {
				if (contains(itemsToRemove, inputIndex)) inputIndex++;
				else result[outputIndex++] = input[inputIndex++];
			}
			return result;
		}
		throw new IllegalArgumentException("Unsupported array type: " + array.getClass());
	}

	/**
	 * Compare 2 lists to see if they contain the same objects regardless of order.
	 *
	 * If both lists are null, it counts as an equal.
	 *
	 * @param list1 List 1
	 * @param list2 List 2
	 * @return True if both lists contain the same objects.
	 */
	public static <T> boolean equalsIgnoreOrder(List<T> list1, List<T> list2) {
		// Same reference, same list.
		if (list1 == list2) return true;
		// One of them is null, not equal unless both null.
		if (list1 == null) return list2 == null;
		if (list2 == null) return list1 == null;

		// Different size, not equal.
		if (list1.size() != list2.size()) return false;

		// Put them in a Set and compare.
		Set<T> temp1 = new HashSet<>(list1);
		Set<T> temp2 = new HashSet<>(list2);
		return temp1.equals(temp2);
	}

	public static double[][] copyOf(double[][] array) {
		double[][] newArray = new double[array.length][];
		for (int i = 0; i< array.length; i++) {
			newArray[i] = Arrays.copyOf(array[i], array[i].length);
		}
		return newArray;
	}

	public static double[] merge(List<double[]> arrays) {
		int len = 0;
		for (double[] v: arrays) len += v.length;
		double[] output = new double[len];
		int offset = 0;
		for (double[] v: arrays) {
			System.arraycopy(v, 0, output, offset, v.length);
			offset += v.length;
		}
		return output;
	}
	
	public static int length(Object array) {
		if (array == null || array.getClass().getComponentType() == null) return 0;
		else if (array instanceof byte[]) return ((byte[]) array).length;
		else if (array instanceof char[]) return ((char[]) array).length;
		else if (array instanceof short[]) return ((short[]) array).length;
		else if (array instanceof int[]) return ((int[]) array).length;
		else if (array instanceof long[]) return ((long[]) array).length;
		else if (array instanceof float[]) return ((float[]) array).length;
		else if (array instanceof double[]) return ((double[]) array).length;
		else if (array instanceof Object[]) return ((Object[]) array).length;
		else return 0;
	}
}
