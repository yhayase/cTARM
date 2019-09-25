package jp.ac.tsukuba.cs.ctarm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ac.tsukuba.cs.ctarm.engine.AssociationRuleMiner;
import jp.ac.tsukuba.cs.ctarm.handler.FilterOutNonClosedRulesHandler;
import jp.ac.tsukuba.cs.ctarm.handler.NthRuleFoundHandler;
import jp.ac.tsukuba.cs.ctarm.handler.PrettyPrintHandler;
import jp.ac.tsukuba.cs.ctarm.engine.Transaction;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;

public class Main implements NthRuleFoundHandler {
	int nFoundRules = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		//(new BufferedReader(new InputStreamReader(System.in))).readLine();

		final int thFreq = Integer.parseInt(args[0]);
		final double thConf = Double.parseDouble(args[1]);
		final File inputTSV = new File(args[2]);
		final Pattern consequentItemNamePattern = Pattern.compile(args[3]);
		
		Main m = new Main();
		m.run(thFreq, thConf, inputTSV, consequentItemNamePattern);
	}
	
	@Override
	public void nthRuleFound(int nth) {
		nFoundRules = nth;
		System.err.println(nth + " rules are found.");
	}
	
	void run(final int thFreq, final double thConf,	final File inputTSV,
			final Pattern consequentItemNamePattern) throws IOException
	{	
		ArrayList<Transaction> transactionList = 
				new ArrayList<Transaction>();

		final Vector<String> itemNameVector = new Vector<>();
		final Map<String, Integer> itemNameToIdMap = new HashMap<>(); 
		
		BufferedReader br = null;
		try {
			try {
				br = new BufferedReader(new FileReader(inputTSV));
				String line;
	
				
				while ((line = br.readLine()) != null) {
					IntSet ant = new IntOpenHashSet();
					IntSet cons = new IntOpenHashSet();

					String[] items = line.split("\t");
					for (String item : items) {
						Integer itemId = itemNameToIdMap.get(item);
						if (itemId == null) {
							itemNameVector.add(item);
							itemId = itemNameVector.size()-1;
							itemNameToIdMap.put(item, itemId);
						}
						int itemIdInt = itemId;
						Matcher m = consequentItemNamePattern.matcher(item);

						// TODO Why matches() doesn't works?
						//if (m.matches()) {
						if (m.find()) {
							cons.add(itemIdInt);
						} else {
							ant.add(itemIdInt);
						}
					}
					transactionList.add(new Transaction(ant, cons));
				}
			} finally {
				if (br!=null) {
					br.close();
				}
			}
		} catch (Exception e) {
			System.err.println(e);
			return;
		}

		final AssociationRuleMiner.Handler prettyPrintHandler = new PrettyPrintHandler(itemNameVector, null, 1000);
		final AssociationRuleMiner.Handler filterOutHandler = new FilterOutNonClosedRulesHandler(prettyPrintHandler, this, 1000);

		AssociationRuleMiner.Handler handler = filterOutHandler;
		
		AssociationRuleMiner ruleMiner = new AssociationRuleMiner(
				transactionList, thFreq, thConf, handler);

		final long startTime = System.currentTimeMillis();
		ruleMiner.mineAssociationRule();
		final long endTime = System.currentTimeMillis();
		System.err.println((endTime-startTime) + " [msec] (for " + nFoundRules + " rules)");

	}
}
