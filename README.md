# cTARM
A targeted association rule mining tool for extremely infrequent rules.

This tool can detect association rules with a very low minimum support number threshold by separating the antecedent item set and the consequent item set.
Lower bound of the threshold is typically less than 10, however, it varies depending on the size and characteristics of the input data set.

To build the tool, JDK 11 or higher and maven build tool is required.

```
% java -classpath path/to/hppc-0.8.1.jar:path/to/externalsortinginjava-0.3.2.jar -jar cTARM-X.Y.Z.jar 1 0.1 data/small-input-example.tsv ^x
15 rules are found.
Rule: [a] => [xA, xB] w/ freq=2 conf=0.666667
Rule: [a] => [xA] w/ freq=3 conf=1.000000
Rule: [b, a] => [xA, xB] w/ freq=2 conf=1.000000
Rule: [c, b, a] => [xB, xA] w/ freq=1 conf=1.000000
Rule: [c, a] => [xA, xB] w/ freq=1 conf=1.000000
Rule: [b] => [xB, xA] w/ freq=2 conf=1.000000
Rule: [b, c] => [xB, xA] w/ freq=1 conf=1.000000
Rule: [c] => [xB, xA] w/ freq=1 conf=1.000000
Rule: [d] => [xC] w/ freq=1 conf=1.000000
56 [msec] (for 15 rules)
```
