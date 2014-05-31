/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.stud.astar.pcfg;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 *
 * @author koller, modified by johannes
 */
public class Pcfg {
    private boolean initialized;
    private final List<Rule> ruleList;
    private final Trie<Rule> ruleTrie;
    private final Signature<String> signature;
    private final IntSet nonTerminals;
    private final Int2ObjectMap<Set<Rule>> lhsToRule;
    private final Int2ObjectMap<Set<Rule>> hashedRhs;
    private final Int2ObjectMap<Set<Rule>> firstRhsSymbolToRules;
    private final Int2ObjectMap<Set<Rule>> secondRhsSymbolToRules;
    private int numberOfRulesInTrie;
    private boolean cnf;
    private int start;


    public Pcfg() {
        ruleTrie = new Trie<Rule>();
        ruleList = new ArrayList<Rule>();
        signature = new Signature<String>();
        nonTerminals = new IntOpenHashSet();
        lhsToRule = new Int2ObjectOpenHashMap<Set<Rule>>();
        hashedRhs = new Int2ObjectOpenHashMap<Set<Rule>>();
        firstRhsSymbolToRules = new Int2ObjectOpenHashMap<>();
        secondRhsSymbolToRules = new Int2ObjectOpenHashMap<>();
        numberOfRulesInTrie = 0;
        cnf = true;
        initialized = false;
    }
    
    /**
     * Adds a new rule to the PCFG.
     * 
     * @param lhs
     * @param rhs
     * @param prob 
     */
    public void addStringRuleToSignature(String lhs, List<String> rhs, double prob) {
        int lhsId = signature.addSymbol(lhs);
        int[] rhsId = new int [rhs.size()];
        for (int i = 0; i < rhs.size(); i++) {
            if (rhs.get(i).length() > 2 &&  rhs.get(i).startsWith("+") && rhs.get(i).endsWith("+")) {
                rhsId[i] = signature.addSymbol(rhs.get(i).substring(1, rhs.get(i).length()-1));
            } else {
                rhsId[i] = signature.addSymbol(rhs.get(i));
            }
        }
        Rule intRule = new Rule(lhsId, rhsId, prob);
        addRule(intRule);
    }
    
    private void addRule(Rule rule) {
        // Map the rule to its LHS symbol
        if (!lhsToRule.containsKey(rule.getLhs())) {
            Set<Rule> insert = new HashSet<Rule>();
            insert.add(rule);
            lhsToRule.put(rule.getLhs(), insert);
        } else {
            lhsToRule.get(rule.getLhs()).add(rule);
        }
        nonTerminals.add(rule.getLhs());
        ruleList.add(rule);        
    }
    
    /**
     * Declares the given string as the startsymbol.
     *
     * @param sym
     */
    public final void setStringStartSymbol(String sym) {
        setStartSymbol(signature.addSymbol(sym));
    }
    
    private void setStartSymbol(int start) {
        this.start = start;
    }
   
    public void initialize() {
        initialized = true;
        ruleList.stream().forEach((r) -> {
            ruleTrie.put(r.getRhs(), r);
            if (r.getRhs().length == 2){
                int first = r.getRhs()[0];
                int second = r.getRhs()[1];
                
                Set<Rule> firstRules = firstRhsSymbolToRules.get(first);
                Set<Rule> secondRules = secondRhsSymbolToRules.get(second);                
                
                if (firstRules == null) {
                    firstRules = new HashSet<Rule>();
                    firstRules.add(r);
                    firstRhsSymbolToRules.put(first, firstRules);
                } else {
                    firstRules.add(r);
                }
                
                if (secondRules == null) {
                    secondRules = new HashSet<Rule>();
                    secondRules.add(r);
                    secondRhsSymbolToRules.put(second, secondRules);
                } else {
                    secondRules.add(r);
                }

            }
        });
    }
    
    /**
     * Make a few post processing steps like figuring out, if the grammar is in
     * Chomsky Normal Form and handle unary rules.
     */
    private void postProcessing() {
        if (!initialized) {
            initialized = true;
            // The set 'terminals' still contains all terminal and nonterminal symbols.
            // So we have to intersect it with the 'nonterminal'-set.
            for (Rule r : ruleList) {
                if (r.getRhs().length == 1 && !r.isPOS() && nonTerminals.contains(r.getRhs()[0])) {
                    cnf = false;
                    for (Rule nextRule : getRulesForLhs(r.getRhs()[0])) {
                        List<Rule> chain = new ArrayList<Rule>();
                        IntSet visited = new IntOpenHashSet();
                        chain.add(r);
                        visited.add(r.getLhs());
                        processUnaryRule(nextRule, chain, r.getProb(), visited);
                    }
                } else {
                    ruleTrie.put(r.getRhs(), r);
                    int hash = intHash(r.getRhs());
                    if (hashedRhs.containsKey(hash)) {
                        hashedRhs.get(hash).add(r);
                    } else {
                        Set<Rule> insert = new HashSet<Rule>();
                        insert.add(r);
                        hashedRhs.put(hash, insert);
                    }
                    ++numberOfRulesInTrie;
                }
            }
        }
    }
    
    
    /**
     * Goes recursively through the chain of unary rules until we find a binary one.
     * @param rule
     * @param chain
     * @param prob
     * @param visited 
     */
    private void processUnaryRule(Rule rule, List<Rule> chain, double prob, IntSet visited) {
        if (!visited.contains(rule.getLhs())) {
            visited.add(rule.getLhs());
            // Recursive case: This rule is another unary rule.
            if (rule.getRhs().length == 1  && ! rule.isPOS() && nonTerminals.contains(rule.getRhs()[0])) {
                for (Rule nextRule : getRulesForLhs(rule.getRhs()[0])) {
                    chain.add(rule);
                    processUnaryRule(nextRule, chain, prob * rule.getProb(), visited);
                }
            } else { //Base case: Finaly a binary rule! Add the chain to the map
                List<Rule> endedChain = new ArrayList<Rule>(chain);
                Rule bridge = new Rule(chain.get(0).getLhs(), rule.getRhs(), rule.getProb() * prob, endedChain,rule.isPOS());
                // Add the modified unary rule to the trie
                ruleTrie.put(rule.getRhs(), bridge);
                int hash = intHash(bridge.getRhs());
                if (hashedRhs.containsKey(hash)) {
                    hashedRhs.get(hash).add(bridge);
                } else {
                    Set<Rule> insert = new HashSet<Rule>();
                    insert.add(bridge);
                    hashedRhs.put(hash, insert);
                }
//                System.err.println("Unary rule set for: " + chain.get(0).toString(signature));
                ++numberOfRulesInTrie;
            }
        }
    }
    
    /**
     * Creates a new PCFG in which the terminal symbols are replaced by their POS-tags.
     * @return POS-tagged PCFG
     */
    public Pcfg addDummyPosRules() {
        Pcfg ret = new Pcfg();
        Signature newSig = ret.getSignature();
        IntSet preTerminals = new IntArraySet();
        IntSet chainSymbols = new IntArraySet();

        // Generate a list of all terminal symbols. We need them to create unary rules for them!
        for (Rule rule : ruleList) {
            // A terminal symbol only appears in an unary rule. Check for that first.
            if (rule.getRhs().length == 1) {
                // Make sure that the symbol on the right side is not a nonterminal.
                if (!nonTerminals.contains(rule.getRhs()[0])) {
                    preTerminals.add(rule.getLhs()); // Save the LHS, we create a rule for it later
                } else {
                    if (rule.getLhs() == rule.getRhs()[0]) {
                        chainSymbols.add(rule.getLhs());

                    }
                }
            }
        }

        // Keep only the chainSymbols in the nonterminal set, that are no preterminals (to avoid duplicated rules)
        for (int symbol : chainSymbols) {
            if (preTerminals.contains(symbol)) {
                nonTerminals.remove(symbol);
            }
        }

        // Set the starting symbol.
        ret.setStartSymbol(newSig.addSymbol(signature.getSymbolForId(getStartSymbol())));
        
        // Create rules for all old rules, that do not expand into terminals
        for (Rule rule : ruleList) {
            // Conditions: Binary rule or unary rule that expands to a nonterminal
            if (rule.getRhs().length == 2 || (rule.getRhs().length == 1 && nonTerminals.contains(rule.getRhs()[0]))) {
                int newLhs = newSig.addSymbol(signature.getSymbolForId(rule.getLhs()));
                int[] newRhs = new int[rule.getRhs().length];
                for (int i = 0; i < newRhs.length; i++) {
                    newRhs[i] = newSig.addSymbol(signature.getSymbolForId(rule.getRhs()[i]));
                }
                ret.addRule(new Rule(newLhs, newRhs, rule.getProb()));
            }
        }
        
        // Now create the new POS-rules
        for (int symbol : preTerminals) {
            int newLhs = newSig.addSymbol(signature.getSymbolForId(symbol));
            int[] newRhs = new int[1];
            newRhs[0] = newLhs;
            double prob = 1;

            for (Rule r : ret.getRulesForLhs(newLhs)) {
                if (r.getRhs().length == 1 && r.getRhs()[0] == newLhs) {
                    ret.ruleList.remove(r);
                } else {
                    prob = prob - r.getProb();
                }
            }
            ret.addRule(new Rule(newLhs, newRhs, prob, true));
        }
        ret.initialize();
        return ret;
    }
    
    
    // Accessor functions:
    
    public int getStartSymbol() {
        return start;
    }
    
    public Signature<String> getSignature() {
        return this.signature;
    }
    
    /**
     * Returns a collection of Rules for a given sequence of symbols,
     * representing the rhs of a rule.
     * @param rhs
     * @return 
     */
    public Collection<Rule> getRules(int[] rhs) {
        if (!initialized) {
            initialize();
        }
        return ruleTrie.get(rhs);
    }
    
    public Collection<Rule> getRulesFromHash(int rhs1, int rhs2) {
        if (!initialized) {
            initialize();
        }
        int hash = intHash(rhs1, rhs2);
        if (hashedRhs.containsKey(hash)) {
            return hashedRhs.get(hash);
        } else return null;
    }
    
    /**
     * Like getRules(int[] rhs), but takes a List of boxed Integers as argument.
     * Only for compatibility reasons! Use the other function instead!
     *
     * @param rhs
     * @return
     */
    public Collection<Rule> getRules(List<Integer> rhs) {
        if (!initialized) {
            initialize();
        }
        int[] ret = new int[rhs.size()];
        for (int i = 0; i < rhs.size(); ++i) {
            ret[i] = rhs.get(i);
        }
        return getRules(ret);
    }
    
    public Collection<Rule> getAllRules() {
        if (!initialized) {
            initialize();
        }
        return ruleList;
    }
    
    /**
     * Returns a collection of rules that have a given symbol on the lhs.
     * @param lhs
     * @return 
     */
    public Collection<Rule> getRulesForLhs(int lhs) {
        if (lhsToRule.containsKey(lhs)) {
            return lhsToRule.get(lhs);
        } else {
            return new HashSet<Rule>();
        }
    }
    
    public Collection<Rule> getRulesForFirstRhsSymbol(int rhs1) {
        if (!initialized) {
            initialize();
        }
        Collection<Rule> ret = firstRhsSymbolToRules.get(rhs1);
        if (ret == null) return new HashSet<>();
        else return ret;
    }
    
    public Collection<Rule> getRulesForSecondRhsSymbol(int rhs2) {
        if (!initialized) {
            initialize();
        }
        Collection<Rule> ret = secondRhsSymbolToRules.get(rhs2);
        if (ret == null) {
            return new HashSet<>();
        } else {
            return ret;
        }
    }

    
    /**
     * Returns a Trie, that stores the information for all symbols 
     * right of the given symbol.
     * @param id
     * @return 
     */
    public Trie<Rule> getTrieForId(int id) {
        if (!initialized) {
            initialize();
        }
        return ruleTrie.getSubtrie(id);
    }
        
    public boolean isCNF() {
        return cnf;
    }
    
    public int numNT() {
        return nonTerminals.size();
    }
    
    public boolean isValidPCFG() {
        for (int lhs : nonTerminals) {
            double prob = 0;
            for (Rule r : getRulesForLhs(lhs)) {
                prob += r.getProb();
            }
            // Round at the 10th point
            if (Math.round(prob * Math.pow(10, 10)) / Math.pow(10, 10) != 1) {
                System.err.println("Failing at: " + signature.getSymbolForId(lhs) + " with a probability of: " + prob + "\nThese are the rules: ");
                for (Rule r : getRulesForLhs(lhs)) {
                    System.err.println("- " + r.toString(signature));
                }
                return false;
            }
        }
        return true;
    }
    
    public int getNumberOfRulesInTrie() {
        return numberOfRulesInTrie;
    }
    
    @Override
    public String toString() {
        if (initialized) {
            StringBuilder buf = new StringBuilder(signature.getSymbolForId(getStartSymbol()) + "\n");

            for( Rule rule : ruleList ) {
                buf.append(rule.toString(signature)).append("\n");
            }

            return buf.toString();
        } else {
            return "PCFG must be initialized before it can be used.";
        }
    }
    
    public String toStringRaw() {
        if (initialized) {
            StringBuilder buf = new StringBuilder(signature.getSymbolForId(getStartSymbol()) + "\n");

            for (Rule rule : ruleList) {
                buf.append(rule.toString()).append("\n");
            }

            return buf.toString();
        } else {
            return "PCFG must be initialized before it can be used.";
        }
    }
    
    // -> http://de.wikipedia.org/wiki/Cantorsche_Paarungsfunktion#Implementierung_der_Berechnungen_in_Java
    public int intHash(int x, int y) {
        return (x + y) * (x + y + 1) / 2 + y;
    }
    
    public int intHash(int [] ints) {
        if (ints.length == 2) {
            return intHash(ints[0], ints[1]);
        } else return -1;
    }
    
    public double getDensenessOfTrie()
    {
        double denseness = 0;
        double numOfUsedNTs = 0;
        int numAllNTs = nonTerminals.size();
        
        // count all left nonterminals of the rhs in the trie
        for (int nt : nonTerminals)
        {
            Trie<Rule> subTrie = getTrieForId(nt);
            // if there is a subtrie (which means that there are two edges), 
            // increment the counter of used nonterminals
            if (subTrie != null)
            {
                ++numOfUsedNTs;
            }
        }

        for (int nt : nonTerminals)
        {
            double newPathProb = 0;
            // calculate the probablity of the first half path
            double firstHalfPath = numOfUsedNTs / numAllNTs / numOfUsedNTs;

            Trie<Rule> subTrie = getTrieForId(nt);
            if (subTrie != null)
            {
                // get the set of outgoing transitions of this nt
                IntSet branchSet = subTrie.getBranches();
                
                // count branches of nonterminal
                if (!branchSet.isEmpty())
                {
                    double branchSetSize = branchSet.size();
                    // for each branch calculate the probability
                    for (int branch : branchSet)
                    {
                        double secondHalfPath = branchSetSize / numAllNTs / branchSetSize;
                        
                        // multiply the first and second pathpart
                        newPathProb = firstHalfPath * secondHalfPath;

                        // add the new probability to the whole result
                        denseness = denseness + newPathProb;
                    }
                }
            } 
            
        }
        
        return denseness;
    }
   
   
}
