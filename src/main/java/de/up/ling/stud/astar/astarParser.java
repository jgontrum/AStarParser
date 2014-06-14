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
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import edu.stanford.nlp.util.BinaryHeapPriorityQueue;

/**
 *
 * @author gontrum
 */
public class astarParser {
    BinaryHeapPriorityQueue<Edge> agenda;
    Signature<String> signature;
    
    Int2ObjectMap<Set<Edge>> seenItemsByStartPosition;
    Int2ObjectMap<Set<Edge>> seenItemsByEndPosition;
    
    Set<Edge> workingSet;
    Object2DoubleMap<Edge> insideMap; //< Log inside score for spans
    
    private void enqueue(Edge item) {
        agenda.add(item, item.getWeight());
        workingSet.add(item);
    }
    
    private Edge poll() {
        return agenda.removeFirst();
    }
    
    // Save items from a temporary set to the maps
    private void flush() { 
        for (Edge item : workingSet) {
            // Save by end
            if (seenItemsByEndPosition.containsKey(item.getEnd())) {
                seenItemsByEndPosition.get(item.getEnd()).add(item);
            } else {
                Set<Edge> insert = new HashSet<>();
                insert.add(item);
                seenItemsByEndPosition.put(item.getEnd(), insert);
            }
            // Save by start
            if (seenItemsByStartPosition.containsKey(item.getBegin())) {
                seenItemsByStartPosition.get(item.getBegin()).add(item);
            } else {
                Set<Edge> insert = new HashSet<>();
                insert.add(item);
                seenItemsByStartPosition.put(item.getBegin(), insert);
            }
        }
    }
    
    private void updateInside(Edge span, double value) {
        if (insideMap.containsKey(span)) {
            if (value > insideMap.get(span)) {
                insideMap.put(span, value);
            }
        } else {
            insideMap.put(span, value);
        }

    }
    
    private double getInside(Edge span) {
        return insideMap.containsKey(span)? insideMap.get(span) : Double.NEGATIVE_INFINITY;
    }
    
    public Tree parse(List<String> words, Pcfg pcfg) {
        /*
            Set up variables
         */
        agenda = new BinaryHeapPriorityQueue<>();
        
        seenItemsByEndPosition = new Int2ObjectOpenHashMap<>();
        seenItemsByEndPosition.defaultReturnValue(new HashSet<>());
        seenItemsByStartPosition = new Int2ObjectOpenHashMap<>();
        seenItemsByStartPosition.defaultReturnValue(new HashSet<>());
        
        workingSet = new HashSet<>();
        
        insideMap = new Object2DoubleOpenHashMap<>();
        insideMap.defaultReturnValue(Double.NEGATIVE_INFINITY);
        
        int n = words.size();
        
        signature = pcfg.getSignature();
        
        /*
            Create startitems
        */
        
        for (int i = 0; i < n; i++) {
            int[] rhs = new int[1];
            rhs[0] = signature.getIdforSymbol(words.get(i));
            
//            System.err.println(words.get(i));
            
            for (Rule r : pcfg.getRules(rhs)) {
//                System.err.println("-> " +r.getLhs());
                Edge edge = new Edge(r.getLhs(), i, i+1, null, null);
                
                updateInside(edge, Math.log(r.getProb()));
                enqueue(edge);
            }
    
        }
        
        /*
            Main loop
         */
        int edgeCounter = 0;
        while (!agenda.isEmpty()) { 
            ++edgeCounter;
            flush();
            Edge item = poll();
//            System.err.println("Current Item: " + item.toStringReadable(signature));
            
            seenItemsByEndPosition.get(item.getBegin()).stream().forEach((candidate) -> {
                int[] rhs = new int[2];
                rhs[0] = candidate.getSymbol();
                rhs[1] = item.getSymbol();
                
                pcfg.getRules(rhs).stream().forEach((r) -> {
                    Edge edge = new Edge(r.getLhs(), candidate.getBegin(), item.getEnd(), candidate, item);
                    
                    double insideLeft = getInside(candidate);
                    double insideRight = getInside(item);
                    double newInside = insideLeft + insideRight + Math.log(r.getProb());
                    updateInside(edge, newInside);
                    
                    enqueue(edge);
                });
            });
            
            seenItemsByStartPosition.get(item.getEnd()).stream().forEach((candidate) -> {
                int[] rhs = new int[2];
                rhs[0] = item.getSymbol();
                rhs[1] = candidate.getSymbol();

                pcfg.getRules(rhs).stream().forEach((r) -> {
                    Edge edge = new Edge(r.getLhs(), item.getBegin(), candidate.getEnd(), item, candidate);
                    
                    double insideLeft = getInside(item);
                    double insideRight = getInside(candidate);
                    double newInside = insideLeft + insideRight + Math.log(r.getProb());
                    updateInside(edge, newInside);
                    
                    enqueue(edge);
                });
            });
            
        }
        
        System.err.println("Edges: " + edgeCounter);
        
        
        // Find final item and create a parse tree
        for (Edge finalItem : seenItemsByEndPosition.get(n)) {
            if (finalItem.getEnd() == n && finalItem.getBegin() == 0 && finalItem.getSymbol() == pcfg.getStartSymbol()) {
                return createParseTree(finalItem);
            }
        }
        
        return null;
    }

    
    private Tree createParseTree(Edge item) {
        List<Tree<String>> children = new ArrayList<>();
        
        Edge child1 = item.getFirstChild();
        Edge child2 = item.getSecondChild();
        
        if (child1 != null && child2 != null) {
            children.add(createParseTree(child1));
            children.add(createParseTree(child2));
        }
        return Tree.create(signature.getSymbolForId(item.getSymbol()), children);
    }
    
    static private class Edge {
        int symbol;
        int begin;
        int end;
        Edge[] childs;
        

        public Edge(int symbol, int begin, int end, Edge child1, Edge child2) {
            this.symbol = symbol;
            this.begin = begin;
            this.end = end;
            childs = new Edge[2];
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
        
        public double getWeight() {
            return getLength();
        }
        
        public Edge getFirstChild() { 
            return childs[0];
        }
        
        public Edge getSecondChild() {
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
            final Edge other = (Edge) obj;
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
