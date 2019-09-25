package jp.ac.tsukuba.cs.ctarm;

import jp.ac.tsukuba.cs.ctarm.engine.Transaction;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransactionTest {

	@Test
	public void testTransaction() {
		IntSet antecedentCandidateItems = new IntOpenHashSet();
		antecedentCandidateItems.add(123);
		
		IntSet consequentCandidateItems = new IntOpenHashSet();
		antecedentCandidateItems.add(456);
		
		Transaction t = 
				new Transaction(antecedentCandidateItems, consequentCandidateItems);
		
		assertEquals(antecedentCandidateItems, t.getAntecedentCandidateItems());
		assertEquals(consequentCandidateItems, t.getConsequentCandidateItems());
	}

}
