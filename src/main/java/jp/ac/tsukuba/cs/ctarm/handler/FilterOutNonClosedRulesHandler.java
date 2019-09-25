package jp.ac.tsukuba.cs.ctarm.handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import jp.ac.tsukuba.cs.ctarm.engine.AssociationRuleMiner;
import jp.ac.tsukuba.cs.ctarm.engine.AssociationRuleMiner.Handler;
import jp.ac.tsukuba.cs.ctarm.util.IntSetUtil;

import com.carrotsearch.hppc.IntSet;
import com.google.code.externalsorting.ExternalSort;

public class FilterOutNonClosedRulesHandler implements
		AssociationRuleMiner.Handler {
	private final AssociationRuleMiner.Handler nextHandler;

	private final File tmpFile = File.createTempFile("rules", ".tmp");
	private final PrintStream tmpOut = new PrintStream(tmpFile);
	{
		tmpFile.deleteOnExit();
	}

	private int nFoundRules = 0;
	private final int eachN;
	private final NthRuleFoundHandler nthHandler;
	
	public FilterOutNonClosedRulesHandler(Handler nextHandler, NthRuleFoundHandler nthHandler, final int eachN) throws IOException {
		assert nextHandler != null;
		assert eachN > 0;
		
		this.nextHandler = nextHandler;
		
		this.eachN = eachN;
		this.nthHandler = nthHandler;  
	}

	static String joinArray(int[] ia, String splitter) {
		StringBuilder sb = new StringBuilder();
		String s = "";
		for (int i : ia) {
			sb.append(s);
			sb.append(i);
			s = splitter;
		}

		return sb.toString();
	}

	@Override
	synchronized public void found(IntSet antecedentItemSet, IntSet consequentItemSet,
			IntSet originTransactions, double confidence) {
		nFoundRules += 1;
		if (nFoundRules % eachN == 0 && nthHandler!=null) {
			nthHandler.nthRuleFound(nFoundRules);
		}

		tmpOut.print(joinArray(IntSetUtil.intSetToSortedArray(antecedentItemSet), ","));
		tmpOut.print("\t");
		tmpOut.print(joinArray(IntSetUtil.intSetToSortedArray(originTransactions), ","));
		tmpOut.print("\t");
		tmpOut.printf("%a", confidence);
		tmpOut.print("\t");
		tmpOut.print(joinArray(IntSetUtil.intSetToSortedArray(consequentItemSet), ","));
		tmpOut.print("\n");
	}

	@Override
	public void endMiningHandler() {
		if (nthHandler!=null) {
			nthHandler.nthRuleFound(nFoundRules);
		}
		
		File sortedTmpFile;
		try {
			sortedTmpFile = File.createTempFile("srules", ".tmp");
		} catch (IOException e) {
			return;
		}
		
		try {
			tmpOut.close();

			sortedTmpFile.deleteOnExit();
			ExternalSort.sort(tmpFile, sortedTmpFile);
			
			BufferedReader reader = new BufferedReader(new FileReader(sortedTmpFile));

			String line;

			String[] prefixOfPreviousGroup = new String[3];
			Vector<String> consequentPartListOfPreviousGroup = new Vector<>();
			while ((line = reader.readLine()) != null) {
				final String[] fields = line.split("\t");
				
				boolean equalToPreviousGroup = true;
				for (int i=0; i<3; i++) {
					if (prefixOfPreviousGroup[i] == null || !fields[i].equals(prefixOfPreviousGroup[i])) {
						equalToPreviousGroup = false;
						break;
					}
				}
				if (!equalToPreviousGroup) {
					if (prefixOfPreviousGroup[0]!=null) {
						processClosedRules(prefixOfPreviousGroup, consequentPartListOfPreviousGroup);
					}

					// reset previous group
					for (int i=0; i<3; i++) {
						prefixOfPreviousGroup[i] = fields[i];
					}
					consequentPartListOfPreviousGroup.clear();
				}
				
				consequentPartListOfPreviousGroup.add(fields[3]);
			}
			if (prefixOfPreviousGroup[0]!=null) {
				processClosedRules(prefixOfPreviousGroup, consequentPartListOfPreviousGroup);
			}

			reader.close();

		} catch(IOException e) {
			System.err.println(e);
		} finally {
			tmpFile.delete();
			sortedTmpFile.delete();
		}
	}

	private void processClosedRules(String[] prefix,
			List<String> consequentPartStrList) {
		HashSet<IntSet> consequentPartSet = new HashSet<>();

		for (String consequentPartStr : consequentPartStrList) {
			IntSet consequentPart = IntSetUtil.csvToIntSet(consequentPartStr);
			
			boolean needToAdd = true;
			for (Iterator<IntSet> i = consequentPartSet.iterator(); i.hasNext();) {
				IntSet existingConsequentPart = i.next();

				if (IntSetUtil.containsAll(existingConsequentPart, consequentPart)) {
					needToAdd = false;
					break;
				}
				
				if (IntSetUtil.containsAll(consequentPart, existingConsequentPart)) {
					i.remove();
				}
			}
			if (needToAdd) {
				consequentPartSet.add(consequentPart);
			}
		}
		
		// call next handler
		IntSet antecedentItemSet = IntSetUtil.csvToIntSet(prefix[0]);
		IntSet originTransactions= IntSetUtil.csvToIntSet(prefix[1]);
		double confidence = Double.parseDouble(prefix[2]); 
		for (IntSet consequentPart : consequentPartSet) {
			nextHandler.found(antecedentItemSet, consequentPart, originTransactions, confidence);
		}
	}
}