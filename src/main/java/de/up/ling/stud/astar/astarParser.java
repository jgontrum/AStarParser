/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.Rule;
import de.up.ling.stud.astar.pcfg.Signature;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public class astarParser {
    PriorityQueue<ParseItem> agenda;
    Int2ObjectMap<Set<ParseItem>> seenItemsByEndPosition;
    Set<ParseItem> workingSet;
    Signature<String> signature;
    
    private void enqueue(ParseItem item) {
        agenda.offer(item);
        workingSet.add(item);
    }
    
    private void flush() {
        for (ParseItem item : workingSet) {
            if (seenItemsByEndPosition.containsKey(item.getEnd())) {
                seenItemsByEndPosition.get(item.getEnd()).add(item);
            } else {
                Set<ParseItem> insert = new HashSet<>();
                insert.add(item);
                seenItemsByEndPosition.put(item.getEnd(), insert);
            }
        }
    }
    
    public Tree parse(List<String> words, Pcfg pcfg) {
        /*
            Set up variables
         */
        agenda = new PriorityQueue<>(100, 
                (ParseItem elem1, ParseItem elem2) -> (elem1.getWeight() == elem2.getWeight()) 
                ? 0             // elements are equal
                : (elem1.getWeight() < elem2.getWeight()
                        ? 0             // elem1 is smaller
                        : 1));
        
        seenItemsByEndPosition = new Int2ObjectOpenHashMap<>();
        seenItemsByEndPosition.defaultReturnValue(new HashSet<>());
        
        workingSet = new HashSet<>();
        
        int n = words.size();
        
        signature = pcfg.getSignature();
        
        /*
            Create startitems
        */
        
        for (int i = 0; i < n; i++) {
            int[] rhs = new int[1];
            rhs[0] = signature.getIdforSymbol(words.get(i));
            
            System.err.println(words.get(i));
            
            for (Rule r : pcfg.getRules(rhs)) {
                enqueue(new ParseItem(r.getLhs(), i, i+1, null, null));
            }
    
        }
        
        /*
            Main loop
         */
        
        while (!agenda.isEmpty()) {
            flush();
            ParseItem item = agenda.poll();
//            System.err.println("Current Item: " + item.toStringReadable(signature));
            
            
            seenItemsByEndPosition.get(item.getBegin()).stream().forEach((candidate) -> {

                int[] rhs = new int[2];
                rhs[0] = candidate.getSymbol();
                rhs[1] = item.getSymbol();
                
                pcfg.getRules(rhs).stream().forEach((r) -> {
                    System.err.println(candidate.toStringReadable(signature));
                    System.err.println(item.toStringReadable(signature) + "\n->");
                    System.err.println(new ParseItem(r.getLhs(), candidate.getBegin(), item.getEnd(), candidate, item).toStringReadable(signature) + "\n");
                    enqueue(new ParseItem(r.getLhs(), candidate.getBegin(), item.getEnd(), candidate, item));
                });
            });
            
        }
        
        for (ParseItem finalItem : seenItemsByEndPosition.get(n)) {
            System.err.println(finalItem.toStringReadable(signature));
            if (finalItem.getEnd() == n && finalItem.getBegin() == 0 && finalItem.getSymbol() == pcfg.getStartSymbol()) {
                return createParseTree(finalItem);
            }
        }
        
        return null;
    }
    
    private Tree createParseTree(ParseItem item) {
        List<Tree<String>> children = new ArrayList<>();
        
        ParseItem child1 = item.getFirstChild();
        ParseItem child2 = item.getSecondChild();
        
        if (child1 != null && child2 != null) {
            children.add(createParseTree(child1));
            children.add(createParseTree(child2));
        }
        return Tree.create(signature.getSymbolForId(item.getSymbol()), children);
    }
    
    static private class ParseItem {
        int symbol;
        int begin;
        int end;
        ParseItem[] childs;
        

        public ParseItem(int symbol, int begin, int end, ParseItem child1, ParseItem child2) {
            this.symbol = symbol;
            this.begin = begin;
            this.end = end;
            childs = new ParseItem[2];
            childs[0] = child1;
            childs[1] = child2;
        }

        public int getLength() {
            return  end - begin;
        }

        public int getSymbol() {
            return symbol;
        }

        public int getBegin() {
            return begin;
        }

        public int getEnd() {
            return end;
        }
        
        public int getWeight() {
            return getLength();
        }
        
        public ParseItem getFirstChild() { 
            return childs[0];
        }
        
        public ParseItem getSecondChild() {
            return childs[1];
        }
        
        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + this.symbol;
            hash = 29 * hash + this.begin;
            hash = 29 * hash + this.end;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ParseItem other = (ParseItem) obj;
            if (this.symbol != other.symbol) {
                return false;
            }
            if (this.begin != other.begin) {
                return false;
            }
            if (this.end != other.end) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "<" + symbol + "," + begin + "," + end + ">";
        }
        
        public String toStringReadable(Signature signature) {
            return "<" + signature.getSymbolForId(symbol) + "," + begin + "," + end + ">()";
        }
        
        
        
    }
}
