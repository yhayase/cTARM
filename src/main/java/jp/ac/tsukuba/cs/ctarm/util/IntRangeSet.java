package jp.ac.tsukuba.cs.ctarm.util;

import java.util.Iterator;

import com.carrotsearch.hppc.IntLookupContainer;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.predicates.IntPredicate;
import com.carrotsearch.hppc.procedures.IntProcedure;

/**
 * Immutable implementation for IntSet that contains all integers from begin to end-1. 
 * @author Yasuhiro Hayase
 *
 */
public class IntRangeSet implements IntSet {
	static public class IntRangeSetError extends Error {
		private static final long serialVersionUID = 1125498452754630538L;

		IntRangeSetError(String s) {
			super(s);
		}
	}
	final int begin;
	final int end;

	public IntRangeSet(int begin, int end) {
		if (begin>=end) {
			throw new IntRangeSetError("end must be larger than begin.");
		}
		
		this.begin = begin;
		this.end = end;
	}

	@Override
	public boolean equals(Object o) {
		IntRangeSet rhs = (IntRangeSet) o;
		if (rhs==null) {
			return false;
		}
		if (this.begin == rhs.begin &&
				this.end == rhs.end) {
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0xf85a9d23 ^ (begin<<16) ^ end;
	}
	
	@Override
	public String toString() {
		String delim = "";
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i=begin; i<end; i++) {
			sb.append(delim);
			sb.append(i);
			delim = ", ";
		}
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public void clear() {
		throw new IntRangeSetError("IntRangeSet is immutable.");
	}

	@Override
	public void release() {
		throw new IntRangeSetError("IntRangeSet is immutable.");
	}

	@Override
	public int removeAll(int i) {
		throw new IntRangeSetError("IntRangeSet is immutable.");
	}

	@Override
	public int removeAll(IntLookupContainer c) {
		throw new IntRangeSetError("IntRangeSet is immutable.");
	}

	@Override
	public int removeAll(IntPredicate predicate) {
		throw new IntRangeSetError("IntRangeSet is immutable.");
	}

	@Override
	public int retainAll(IntLookupContainer c) {
		throw new IntRangeSetError("IntRangeSet is immutable.");
	}

	@Override
	public int retainAll(IntPredicate predicate) {
		throw new IntRangeSetError("IntRangeSet is immutable.");
	}

	@Override
	public boolean contains(int e) {
		if (begin<=e && e<end) {
			return true;
		}
		return false;
	}

	@Override
	public <T extends IntProcedure> T forEach(T procedure) {
		for (int i=begin; i<end; i++) {
			procedure.apply(i);
		}
		return procedure;
	}

	@Override
	public <T extends IntPredicate> T forEach(T procedure) {
		for (int i=begin; i<end; i++) {
			if (!procedure.apply(i)) {
				return procedure;
			}
		}
		return procedure;
	}

	@Override
	public boolean isEmpty() {
		return begin==end;
	}

	private class MyIterator implements Iterator<IntCursor> {
		int current;
		
		public MyIterator() {
			current = begin;
		}

		@Override
		public boolean hasNext() {
			return current<end;
		}

		@Override
		public IntCursor next() {
			IntCursor rval = new IntCursor();
			rval.value = current;
			rval.index = current-begin;
			
			current += 1;
			
			return rval;
		}

		@Override
		public void remove() {
			throw new IntRangeSetError("IntRangeSet is immutable.");
		}
		
	}
	@Override
	public Iterator<IntCursor> iterator() {
		return new MyIterator();
	}

	@Override
	public int size() {
		return end-begin;
	}

	@Override
	public int[] toArray() {
		final int[] rval = new int[end-begin];
		for (int i=0; i<end-begin; i++) {
			rval[i] = i+begin;
		}
		return rval;
	}

	@Override
	public boolean add(int k) {
		throw new IntRangeSetError("IntRangeSet is immutable");
	}

	@Override
	public String visualizeKeyDistribution(int i) {
		throw new IntRangeSetError("IntRangeSet does not support visualizeKeyDistribution()");
	}


}
