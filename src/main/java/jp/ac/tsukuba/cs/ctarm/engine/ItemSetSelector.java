package jp.ac.tsukuba.cs.ctarm.engine;

import com.carrotsearch.hppc.IntSet;

public interface ItemSetSelector {
	IntSet select(Transaction t);
	
	public static class AntecedentCandidateSelector implements ItemSetSelector {
		@Override
		public IntSet select(Transaction t) {
			return t.getAntecedentCandidateItems();
		}
	}

	public static class ConsequentCandidateSelector implements ItemSetSelector {
		@Override
		public IntSet select(Transaction t) {
			return t.getConsequentCandidateItems();
		}
	}
	
	public static final ItemSetSelector antecedentCandidateSelector = new AntecedentCandidateSelector();
	public static final ItemSetSelector consequentCandidateSelector = new ConsequentCandidateSelector();
}
