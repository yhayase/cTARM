package jp.ac.tsukuba.cs.ctarm.util;

import java.util.Arrays;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;


public class IntSetUtil {

	public static boolean containsAll(IntSet sup, IntSet sub) {
		if (sup.size()<sub.size()) {
			return false;
		}
		for (IntCursor ic : sub) {
			if (! sup.contains(ic.value)) {
				return false;
			}
		}
		
		return true;
	}

	public static int[] intSetToSortedArray(IntSet is) {
		final int[] rval = new int[is.size()];
		int idx=0;
		
		for (IntCursor cur : is) {
			rval[idx] = cur.value;
			idx++;  
		}
		assert idx == is.size();
		
		Arrays.sort(rval);
		
		return rval;
	}

	public static IntSet csvToIntSet(String csvInt) {
		IntSet rval = new IntOpenHashSet();
		for (String intStr : csvInt.split(",")) {
			rval.add(Integer.parseInt(intStr));
		}
		return rval;
	}

}
