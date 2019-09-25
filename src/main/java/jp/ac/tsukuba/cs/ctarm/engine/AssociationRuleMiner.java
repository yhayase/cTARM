package jp.ac.tsukuba.cs.ctarm.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.carrotsearch.hppc.cursors.IntCursor;

public class AssociationRuleMiner {
	/**
	 * @author hayase
	 * Handler for found rules.
	 */
	public interface Handler {
		/**
		 * Callback method invoked when a rule is found.
		 * @param antecedentItemSet antecedent part of the rule.
		 * @param consequentItemSet consequent part of the rule.
		 * @param originTransactions a transaction set which supports the rule, i.e. contains all items in the rule.
		 * @param confidence confidence value of the rule.
		 */
		void found(IntSet antecedentItemSet, IntSet consequentItemSet, 
				IntSet originTransactions, double confidence);
		
		void endMiningHandler();
	}

	final List<Transaction> transactionList; 
	final int MINIMUM_FREQUENCY;
	final double MINIMUM_CONFIDENCE;
	final Handler ruleFoundHandler;
	
	final IntObjectMap<IntSet> antecedentItemIdToTransactionIdMap = new IntObjectOpenHashMap<>();
	final Map<IntSet, IntSet> antecedentFrequentItemSetToTransactionsMap = new HashMap<>();
	
	public AssociationRuleMiner(List<Transaction> transactionList,
			int minimumFrequency, double minimumConfidence, 
			Handler handler) {
		this.transactionList = transactionList;
		MINIMUM_FREQUENCY = minimumFrequency;
		MINIMUM_CONFIDENCE = minimumConfidence;
		ruleFoundHandler = handler;
		
		for (int transactionId=0; transactionId<transactionList.size(); transactionId++) {
			Transaction transaction = transactionList.get(transactionId);

			IntSet antecedentItemSet = transaction.getAntecedentCandidateItems(); 
			for (IntCursor itemIdCursor : antecedentItemSet) {
				int itemId = itemIdCursor.value;
				
				IntSet transactionSet = antecedentItemIdToTransactionIdMap.get(itemId);
				if (transactionSet==null) {
					transactionSet = new IntOpenHashSet();
					antecedentItemIdToTransactionIdMap.put(itemId, transactionSet);
				}
				transactionSet.add(transactionId);
			}
		}
		//System.out.println(antecedentItemIdToTransactionIdMap);
	}
	
	private class ConsequentItemSetHandler implements FrequentItemSetMiner.Handler, Runnable {
		final BlockingQueue<Object[]> queue;
		Set<Thread> threadSet = new HashSet<>(); 

		public ConsequentItemSetHandler() {
			int nProcessors = Runtime.getRuntime().availableProcessors();
			queue = new ArrayBlockingQueue<>(nProcessors*10);
			
			for(int i=0; i<Math.max(1, nProcessors); i++) {
				Thread t = new Thread(this);
				threadSet.add(t);
				t.setPriority(Thread.MIN_PRIORITY); // to avoid blocking the main thread.
				t.start();
			}
		}
		
		@Override
		public void endMiningHandler() {
			try {
				queue.put(new Object[0]);

				for (Thread t : threadSet) {
					t.join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			ruleFoundHandler.endMiningHandler();
		}

		@Override
		public void run() {
			try {
				for (;;) {
					Object[] oa = queue.take();
					if (oa.length == 0) {
						queue.put(oa);
						return;
					}
					assert oa.length == 2;
					
					final IntSet consequentItemSet = (IntSet)oa[0];
					final IntSet consequentOriginTransactions = (IntSet)oa[1];
					
					closedItemSetHandlerInternal(consequentItemSet, consequentOriginTransactions);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void allItemSetHandler(IntSet itemSet, IntSet originTransactions) {
			// nop
			return;
		}

		@Override
		public void closedItemSetHandler(final IntSet consequentItemSet,
				final IntSet consequentOriginTransactions) {
			try {
				queue.put(new Object[]{consequentItemSet, consequentOriginTransactions});
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		private void closedItemSetHandlerInternal(final IntSet consequentItemSet,
				final IntSet consequentOriginTransactions) {
			if (consequentItemSet.size()==0) {
				return;
			}
			
			Map<IntSet, IntSet> initialResult =	new HashMap<>();
			
			FrequentItemSetMiner.Handler antecedentItemSetHandler = new FrequentItemSetMiner.Handler() {
				// calculate confidence of a found association.
				@Override
				public void allItemSetHandler(IntSet antecedentItemSet, IntSet antecedentOriginTransactions) {
					// Rule "antecedentItemSet => consequentItemSet"
					// w/ freq=antecedentOriginTransactions.size() is found.
					
					if (antecedentItemSet.size()==0) {
						return;
					}

					int containingTransactions = 0;
					containingTransactions = countTransactionsThatContainsAntecedentItems(antecedentItemSet, antecedentOriginTransactions);
					//containingTransactions = countTransactionsThatContainsAntecedentItemsWithOnDemandCaching(antecedentItemSet, antecedentOriginTransactions);
					//containingTransactions = countTransactionsThatContainsAntecedentItemsQuickly(antecedentItemSet);

					final double confidence = antecedentOriginTransactions.size() / (double) containingTransactions;
					if (confidence >= MINIMUM_CONFIDENCE) {
						// Rule "antecedentItemSet => consequentItemSet" passes the threshold check.
						ruleFoundHandler.found(antecedentItemSet, consequentItemSet, antecedentOriginTransactions, confidence);
					}
				}

				// 
				@SuppressWarnings("unused")
				private int countTransactionsThatContainsAntecedentItemsPrecomputed(IntSet itemSet) {
					assert antecedentFrequentItemSetToTransactionsMap.containsKey(itemSet);

					return antecedentFrequentItemSetToTransactionsMap.get(itemSet).size();
				}

				private ObjectIntMap<IntSet> countingCache = new ObjectIntOpenHashMap<>();
				@SuppressWarnings("unused")
				private int countTransactionsThatContainsAntecedentItemsWithOnDemandCaching(IntSet itemSet,
						IntSet originTransactions) {
					synchronized (countingCache) 
					{
						if (countingCache.containsKey(itemSet)) {
							return countingCache.get(itemSet);
						}
					}
					
					int count = countTransactionsThatContainsAntecedentItems(itemSet, originTransactions);
					
					synchronized (countingCache)
					{
						countingCache.put(itemSet, count);
					}
					
					return count;
				}

				// count itemset appearance based on minimum-frequent item.
				private int countTransactionsThatContainsAntecedentItems(IntSet itemSet,
						IntSet originTransactions) {
					
					int containingTransactions = originTransactions.size();

					IntSet minimumTransactionSet = null;
					for (IntCursor intCursor : itemSet) {
						IntSet transactionSet = antecedentItemIdToTransactionIdMap.get(intCursor.value);
						assert transactionSet != null;
						
						if (minimumTransactionSet == null || 
								transactionSet.size() < minimumTransactionSet.size()) {
							minimumTransactionSet = transactionSet;
						}
					}

					forTransactions:
					for (IntCursor intCursor : minimumTransactionSet) {
						int transactionId = intCursor.value;

						if (originTransactions.contains(transactionId)) {
							continue;
						}

						Transaction t = transactionList.get(transactionId);
						IntSet a = t.getAntecedentCandidateItems();
						for (IntCursor c : itemSet) {
							if (! a.contains(c.value)) {
								continue forTransactions;
							}
						}
						containingTransactions += 1;

						final double currentConfidence = originTransactions.size() / (double) containingTransactions + consequentOriginTransactions.size();
						if (currentConfidence < MINIMUM_CONFIDENCE) {
							break forTransactions;
						}
					}
					
					return containingTransactions;
				}

				// This is a straightforward implementation
				@SuppressWarnings("unused")
				private int countTransactionsThatContainsAntecedentItemsNaively(IntSet itemSet) {
					int containingTransactions = 0;

					forAllTransactions: for (Transaction t : transactionList) {
						IntSet a = t.getAntecedentCandidateItems();
						for (IntCursor c : itemSet) {
							if (! a.contains(c.value)) {
								continue forAllTransactions;
							}
						}
						containingTransactions += 1;
					}
					
					return containingTransactions;
				}
				
				@Override
				public void closedItemSetHandler(IntSet itemSet,
						IntSet originTransactions) {
					// nop
					return;
				}

				@Override
				public void endMiningHandler() {
					// nop
					return;
				}
			};
			
			initialResult.put(new IntOpenHashSet(), consequentOriginTransactions);

			//System.out.println("==== antecedent items for " + consequentItemSet + " ====");
			FrequentItemSetMiner.mineNextFrequentItemSet(
					transactionList,
					ItemSetSelector.antecedentCandidateSelector,
					MINIMUM_FREQUENCY,
					5,
					antecedentItemSetHandler, initialResult);
			//System.out.println("==== end ====");
		}
	}

	public void mineAssociationRule() {
		/*
		final long startTime = System.currentTimeMillis();
		FrequentItemSetMiner.mineFrequentItemSet(transactionList, 
				ItemSetSelector.antecedentCandidateSelector,MINIMUM_FREQUENCY,
				new AntecedentItemSetHandler());

		final long intermediateTime = System.currentTimeMillis();
		*/
		FrequentItemSetMiner.mineFrequentItemSet(
				transactionList,
				ItemSetSelector.consequentCandidateSelector,
				MINIMUM_FREQUENCY,
				100,
				new ConsequentItemSetHandler());
		//final long endTime = System.currentTimeMillis();
		//System.out.printf("%d [msec] for caching, %d [msec] for rule mining\n", (intermediateTime-startTime), (endTime-intermediateTime));
	}
}
