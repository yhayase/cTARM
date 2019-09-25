package jp.ac.tsukuba.cs.ctarm.handler;

import java.util.Vector;

import jp.ac.tsukuba.cs.ctarm.engine.AssociationRuleMiner;

import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;

public class PrettyPrintHandler implements
		AssociationRuleMiner.Handler {
	private final Vector<String> itemNameVector;

	private int nFoundRules = 0;
	private final int eachN;
	private final NthRuleFoundHandler nthHandler;

	public PrettyPrintHandler(Vector<String> itemNameVector, NthRuleFoundHandler nthHandler, final int eachN) {
		this.itemNameVector = itemNameVector;

		assert eachN > 0;
		
		this.eachN = eachN;
		this.nthHandler = nthHandler;  
	}

	@Override
	synchronized public void found(IntSet antecedentItemSet, IntSet consequentItemSet,
			IntSet originTransactions, double confidence) {
		nFoundRules += 1;
		if (nFoundRules % eachN == 0 && nthHandler!=null) {
			nthHandler.nthRuleFound(nFoundRules);
		}

		//System.out.printf("Rule: %s\t%s\t%d\t%f\n", antecedentItemSet, consequentItemSet, originTransactions.size(), confidence);
		System.out.print("Rule: [");
		{
			String splitter = "";
			for (IntCursor cur: antecedentItemSet) {
				System.out.print(splitter);
				System.out.print(itemNameVector.get(cur.value));
				splitter = ", ";
			}
		}
		System.out.print("] => [");
		{
			String splitter = "";
			for (IntCursor cur: consequentItemSet) {
				System.out.print(splitter);
				System.out.print(itemNameVector.get(cur.value));
				splitter = ", ";
			}
		}
		//"%s\t%s\t%d\t%f\n", antecedentItemSet, consequentItemSet, originTransactions.size(), confidence);
		System.out.printf("] w/ freq=%d conf=%f\n", originTransactions.size(), confidence);

		return;
	}

	@Override
	public void endMiningHandler() {
		if (nthHandler!=null) {
			nthHandler.nthRuleFound(nFoundRules);
		}
	}
}