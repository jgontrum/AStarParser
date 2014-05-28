/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.Rule;
import de.up.ling.stud.astar.pcfg.Signature;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
    Signature signature;
    
    private void enqueue(ParseItem item) {
        agenda.offer(item);
        workingSet.add(item);
    }
    
    private void flush() {
        workingSet.forEach((item) -> { seenItemsByEndPosition.get(item.getEnd()).add(item); });
    }
    
    public boolean parse(List<String> words, Pcfg pcfg) {
        /*
            Set up variables
         */
        agenda = new PriorityQueue<>(100, 
                (ParseItem elem1, ParseItem elem2) -> (elem1.getLength() == elem2.getLength()) 
                ? 0             // elements are equal
                : (elem1.getLength() < elem2.getLength()
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
            rhs[0] = signature.addSymbol(words.get(i));
            
            for (Rule r : pcfg.getRules(rhs)) {
                enqueue(new ParseItem(r.getLhs(), i, i+1));
            }
    
        }
        
        /*
            Main loop
         */
        
        while (!agenda.isEmpty()) {
            flush();
            ParseItem item = agenda.poll();
            System.err.println("Current Item: " + item.toStringReadable(signature));
            
            
            seenItemsByEndPosition.get(item.getBegin()).stream().forEach((candidate) -> {
                int[] rhs = new int[2];
                rhs[0] = candidate.getSymbol();
                rhs[1] = item.getSymbol();
                
                pcfg.getRules(rhs).stream().forEach((r) -> {
                    System.err.println("New Item: " + new ParseItem(r.getLhs(), candidate.getBegin(), item.getEnd()).toStringReadable(signature));
                    enqueue(new ParseItem(r.getLhs(), candidate.getBegin(), item.getEnd()));
                });
            });
            
        }
        
        
        
        return seenItemsByEndPosition.get(n).contains(new ParseItem(pcfg.getStartSymbol(),0,n));
    }
    
    static private class ParseItem {
        int symbol;
        int begin;
        int end;

        public ParseItem(int symbol, int begin, int end) {
            this.symbol = symbol;
            this.begin = begin;
            this.end = end;
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
            return "<" + signature.getSymbolForId(symbol) + "," + begin + "," + end + ">";
        }
        
        
        
    }
}
