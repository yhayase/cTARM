package jp.ac.tsukuba.cs.ctarm.engine;

import com.carrotsearch.hppc.IntSet;

public class Transaction {
	private final IntSet antecedentCandidateItems;
	private final IntSet consequentCandidateItems;
	
	public Transaction(IntSet antecedentCandidateItems, IntSet consequentCandidateItems) {
		assert antecedentCandidateItems!=null;
		assert consequentCandidateItems!=null;
		
		this.antecedentCandidateItems = antecedentCandidateItems;
		this.consequentCandidateItems = consequentCandidateItems;
	}
	
	public IntSet getAntecedentCandidateItems() {
		return antecedentCandidateItems;
	}
	
	public IntSet getConsequentCandidateItems() {
		return consequentCandidateItems;
	}
}
