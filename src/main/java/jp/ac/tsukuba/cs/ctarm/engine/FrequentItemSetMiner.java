package jp.ac.tsukuba.cs.ctarm.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntIntHashMap;
import jp.ac.tsukuba.cs.ctarm.util.IntRangeSet;

import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.cursors.IntCursor;

public class FrequentItemSetMiner {
	public interface Handler {
		void allItemSetHandler(IntSet itemSet, IntSet originTransactions);
		void closedItemSetHandler(IntSet itemSet, IntSet originTransactions);
		void endMiningHandler();
	}

	
	static public void mineFrequentItemSet(
			final List<Transaction> transactionList,
			final ItemSetSelector selector,
			final int minimumFrequency,
			final int nRecurse,
			final Handler handler) {

		IntIntHashMap itemCount = new IntIntHashMap();
		for (Transaction t : transactionList) {
			IntSet itemSet = selector.select(t);
			for (IntCursor intCursor : itemSet) {
				int item = intCursor.value;
				itemCount.putOrAdd(item, 1, 1);
			}
		}

		ArrayList<IntSet> itemSetList = new ArrayList<>();
		for (Transaction t : transactionList) {
			IntSet itemSet = selector.select(t);
			
			IntHashSet newItemSet = new IntHashSet();
			for (IntCursor intCursor : itemSet) {
				int item = intCursor.value;
				if (itemCount.get(item)>=minimumFrequency) {
					newItemSet.add(item);
				}
			}
			itemSetList.add(newItemSet);
		}
		
		
		Map<IntSet, IntSet> emptyToAllMap = 
				new HashMap<>();
		IntSet allTransactionIdSet = new IntRangeSet(0, transactionList.size());

		emptyToAllMap.put(new IntHashSet(), allTransactionIdSet);
		
		mineNextFrequentItemSet2(itemSetList, minimumFrequency,
				nRecurse, handler, emptyToAllMap);
	}
	
	private static void mineNextFrequentItemSet2(
			final List<IntSet> itemSetList,
			final int minimumFrequency,
			final int nRecurse,
			final Handler handler,
			final Map<IntSet, IntSet> previousLevelResult) {
		if (nRecurse <= 0) {
			handler.endMiningHandler();
			return;
		}
		
		//System.out.println(previousLevelResult);
		
		Map<IntSet, IntSet> currentLevelResult = 
				new HashMap<>();
		for (final Map.Entry<IntSet, IntSet> e : previousLevelResult.entrySet()) {
			final IntSet previousItemSet = e.getKey();
			final IntSet previousOriginTransactions = e.getValue();
			if (previousOriginTransactions.size() < minimumFrequency) {
				continue;
			}

			if (handler != null) {
				handler.allItemSetHandler(previousItemSet, previousOriginTransactions);
			}

			Map<IntSet, IntSet> nextResultForPreviousItemSet = new HashMap<>();
			for (final IntCursor transactionIdCursor : previousOriginTransactions) {
				final int transactionId = transactionIdCursor.value;
				
				final IntSet itemSet = itemSetList.get(transactionId);
				
				for (IntCursor itemIdCursor : itemSet) {
					final int itemId = itemIdCursor.value;
					
					if (previousItemSet.contains(itemId)) {
						continue;
					}

					IntSet subItemSet = new IntHashSet(previousItemSet);
					subItemSet.add(itemId);
					
					final IntSet originTransactions = nextResultForPreviousItemSet.get(subItemSet);
					if (originTransactions!=null) {
						originTransactions.add(transactionId);
					} else { 
						final IntSet originTransactions2 = new IntHashSet();
						originTransactions2.add(transactionId);
						nextResultForPreviousItemSet.put(subItemSet, originTransactions2);
					}
				}
			}

			// add all entries of nextResultForPreviousItemSet to currentLevelResult
			boolean previousItemSetWasClosed = true;
			for(Map.Entry<IntSet, IntSet> r : nextResultForPreviousItemSet.entrySet()) {
				final IntSet itemSet = r.getKey();
				final IntSet transactionSet = r.getValue();
				
				final IntSet resultTransactions = currentLevelResult.get(itemSet);
				if (resultTransactions!=null) {
					for (IntCursor intCursor : transactionSet) {
						resultTransactions.add(intCursor.value);
					}
				} else { 
					currentLevelResult.put(itemSet, transactionSet);
				}

				// check whether all transactions are included in a next level transaction
				if (transactionSet.size() == previousOriginTransactions.size()) {
					previousItemSetWasClosed = false;
				}
			}
			
			if (previousItemSetWasClosed) {
				handler.closedItemSetHandler(previousItemSet, previousOriginTransactions);
			}
			
		}
		
		previousLevelResult.clear();
		if (currentLevelResult.size()==0) {
			handler.endMiningHandler();
			return;
		}
		mineNextFrequentItemSet2(itemSetList, minimumFrequency,
				nRecurse-1, handler, currentLevelResult);
	}
	
	static void mineNextFrequentItemSet(
			final List<Transaction> transactionList,
			final ItemSetSelector selector,
			final int minimumFrequency,
			final int nRecurse,
			final Handler handler,
			final Map<IntSet, IntSet> previousLevelResult) {		

		final IntSet relatedTransactionIds = new IntHashSet();
		for (final Map.Entry<IntSet, IntSet> e : previousLevelResult.entrySet()) {
			// final IntSet previousItemSet = e.getKey();
			final IntSet previousOriginTransactions = e.getValue();
			for (IntCursor intCursor : previousOriginTransactions) {
				relatedTransactionIds.add(intCursor.value);
			}
		}		
		
		IntIntHashMap itemCount = new IntIntHashMap();

		for (IntCursor transactionsCursor : relatedTransactionIds) {
			final int transactionId = transactionsCursor.value;
			final Transaction t = transactionList.get(transactionId);
			IntSet itemSet = selector.select(t);

			for (IntCursor itemSetCursor : itemSet) {
				int item = itemSetCursor.value;
				itemCount.putOrAdd(item, 1, 1);
			}
		}

		ArrayList<IntSet> itemSetList = new ArrayList<>(transactionList.size());
		for (int i=0; i<transactionList.size(); i++) {
			itemSetList.add(null);
		}
		for (IntCursor transactionsCursor : relatedTransactionIds) {
			final int transactionId = transactionsCursor.value;
			final Transaction t = transactionList.get(transactionId);
			IntSet itemSet = selector.select(t);
			
			IntHashSet newItemSet = new IntHashSet();
			for (IntCursor intCursor : itemSet) {
				int item = intCursor.value;
				if (itemCount.get(item)>=minimumFrequency) {
					newItemSet.add(item);
				}
			}
			itemSetList.set(transactionId, newItemSet);
		}
		
		mineNextFrequentItemSet2(itemSetList, minimumFrequency,
				nRecurse, handler, previousLevelResult);
	}	
}
