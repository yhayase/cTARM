package jp.ac.tsukuba.cs.ctarm.engine;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import jp.ac.tsukuba.cs.ctarm.handler.FilterOutNonClosedRulesHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Filter;

import static org.junit.jupiter.api.Assertions.*;

class AssociationRuleMinerTest {
    private static class Pair<A, B> {
        public final A a;
        public final B b;

        Pair(A a, B b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object obj) {
            if (! (obj instanceof Pair))
                return false;
            Pair p = (Pair) obj;
            return a.equals(p.a) && b.equals(p.b);
        }

        @Override
        public int hashCode() {
            return a.hashCode() ^ 0xa9823751 ^ b.hashCode();
        }
    }
    Map<Pair<IntSet, IntSet>, Pair<Integer, Double>> answer = new HashMap<>();

    class Handler implements AssociationRuleMiner.Handler {
        @Override
        public void found(IntSet antecedentItemSet, IntSet consequentItemSet, IntSet originTransactions, double confidence) {
            Pair<IntSet, IntSet> keyPair = new Pair<>(antecedentItemSet, consequentItemSet);
            assertTrue(answer.containsKey(keyPair));
            Pair<Integer, Double> val = answer.get(keyPair);
            assertEquals(val.a.intValue(), originTransactions.size());
            assertEquals(val.b.doubleValue(), confidence);
            answer.remove(keyPair);
            System.out.println("found() called");
            assertFalse(answer.containsKey(keyPair));
        }

        @Override
        public void endMiningHandler() {
            assertTrue(answer.isEmpty());
        }
    }

    @Test
    void emptyTransactionTest() {
        ArrayList<Transaction> transactionList =
                new ArrayList<>();

        // keep answer empty

        AssociationRuleMiner ruleMiner = new AssociationRuleMiner(
                transactionList, 1, 1, new Handler());
    }

    @Test
    void singleRuleTest() {
        ArrayList<Transaction> transactionList =
                new ArrayList<>();
        IntSet ant = new IntHashSet();
        ant.add(1);
        IntSet cons = new IntHashSet();
        cons.add(2);
        transactionList.add(new Transaction(ant, cons));

        answer.put(new Pair<>(ant, cons), new Pair<>(1, 1.0));

        AssociationRuleMiner ruleMiner = new AssociationRuleMiner(
                transactionList, 1, 1, new Handler());
        ruleMiner.mineAssociationRule();
    }

    @Test
    void minimumFrequencyTest() {
        ArrayList<Transaction> transactionList =
                new ArrayList<>();
        IntSet ant = new IntHashSet();
        ant.add(1);
        IntSet cons = new IntHashSet();
        cons.add(2);
        transactionList.add(new Transaction(ant, cons));

        // keep answer empty

        AssociationRuleMiner ruleMiner = new AssociationRuleMiner(
                transactionList, 2, 1, new Handler());
    }

    @Test
    void minimumConfidenceTest() {
        ArrayList<Transaction> transactionList =
                new ArrayList<>();
        IntSet ant = new IntHashSet();
        ant.add(1);
        IntSet cons = new IntHashSet();
        cons.add(2);
        transactionList.add(new Transaction(ant, cons));

        // add {1}
        transactionList.add(new Transaction(ant, new IntHashSet()));

        // keep answer empty

        AssociationRuleMiner ruleMiner = new AssociationRuleMiner(
                transactionList, 1, 0.6, new Handler());
    }

    @Test
    void nonClosedRulesTest() throws IOException {
        ArrayList<Transaction> transactionList =
                new ArrayList<>();
        IntSet ant = new IntHashSet();
        ant.add(1);
        IntSet cons = new IntHashSet();
        cons.add(2);
        cons.add(3);
        transactionList.add(new Transaction(ant, cons));

        var set2 = new IntHashSet();
        set2.add(2);
        var set3 = new IntHashSet();
        set3.add(3);

        answer.put(new Pair<>(ant, cons), new Pair<>(1, 1.0));
        answer.put(new Pair<>(ant, set2), new Pair<>(1, 1.0));
        answer.put(new Pair<>(ant, set3), new Pair<>(1, 1.0));
        //answer.put(new Pair<>(ant, new IntHashSet()), new Pair<>(1, 1.0));

        AssociationRuleMiner ruleMiner = new AssociationRuleMiner(
                transactionList, 1, 1,  new Handler());
    }

    @Test
    void filterOutNonClosedRulesHandlerTest() throws IOException {
        ArrayList<Transaction> transactionList =
                new ArrayList<>();
        IntSet ant = new IntHashSet();
        ant.add(1);
        IntSet cons = new IntHashSet();
        cons.add(2);
        cons.add(3);
        transactionList.add(new Transaction(ant, cons));

        // ``1 -> 2'' and ``1 -> 3'' must not found
        transactionList.add(new Transaction(ant, new IntHashSet()));

        FilterOutNonClosedRulesHandler fHandler = new FilterOutNonClosedRulesHandler(new Handler(), null, 0);
        AssociationRuleMiner ruleMiner = new AssociationRuleMiner(
                transactionList, 1, 1,  fHandler);
    }

}