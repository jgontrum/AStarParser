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
import edu.stanford.nlp.util.BinaryHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public class astarParser   {
    private BinaryHeapPriorityQueue<Edge> agenda;       // The driving agenda as a Stanford priority queue
    private Signature<String> signature;                // Signature from the grammar to translate symbol IDs to strings
    
    private Int2ObjectMap<List<Edge>> seenItemsByStartPosition;  // This represents our chart for the CKY parser
    private Int2ObjectMap<List<Edge>> seenItemsByEndPosition;    // Dito.
    
    private LongSet completedEdges; // Stores all edges, that have been taken from the agenda, to prevent putting them back on it again. 
                                    // The edges are strored as along value, containing their symbol, begin and end.
    
    private List<Edge> edgeStorage;      // Save all newly created edges here so we do not change the chart while iterating over it.
    private Long2DoubleMap insideMap;   // Log inside score for edges. Using the long encoding of the edges.
    
    private int n;                      // The length of the current sentence
    private Pcfg pcfg;                  // The grammar to parse the current sentence with
    
    private Summarizer summarizer;      // An object of a summarizer, e.g. SXSummary
    
    // Put an edge on the agenda if needed
    private void enqueue(Edge item, double weight) {
        if (agenda.contains(item)) {
            agenda.decreasePriority(item, weight);
        } else {
            agenda.add(item, weight); 
            edgeStorage.add(item);
        }
    }
    
    // Take an edge from the agenda and mark it as completed
    private Edge dequeue() {
        Edge ret = agenda.removeFirst();
        completedEdges.add(ret.asLongEncoding());
        return ret;
    }
    
    // Save items from a temporary set to the maps
    private boolean flush() { 
        for (Edge item : edgeStorage) {
            // Save by end
            if (seenItemsByEndPosition.containsKey(item.getEnd())) {
                seenItemsByEndPosition.get(item.getEnd()).add(item);
            } else {
                List<Edge> insert = new ArrayList<>();
                insert.add(item);
                seenItemsByEndPosition.put(item.getEnd(), insert);
            }
            // Save by start
            if (seenItemsByStartPosition.containsKey(item.getBegin())) {
                seenItemsByStartPosition.get(item.getBegin()).add(item);
            } else {
                List<Edge> insert = new ArrayList<>();
                insert.add(item);
                seenItemsByStartPosition.put(item.getBegin(), insert);
            }
            
            if (isStartItem(item)) return true;
        }
        return false;
    }
    
    // Update the inside value of an edge
    private void updateInside(Edge span, double value) {
        long spanAsLong = span.asLongEncoding();
        if (insideMap.containsKey(spanAsLong)) {
            if (value > insideMap.get(spanAsLong)) {
                insideMap.put(spanAsLong, value);
            }
        } else {
            insideMap.put(spanAsLong, value);
        }
    }
    
    // Returns the inside value of an edge
    private double getInside(Edge span) {
        long spanAsLong = span.asLongEncoding();
        return insideMap.containsKey(spanAsLong)? insideMap.get(spanAsLong) : Double.NEGATIVE_INFINITY;
    }
    
    public Tree<String> parse(List<String> words, Pcfg grammar) {
        /*
            Set up variables
         */
        agenda = new BinaryHeapPriorityQueue<>();
        
        seenItemsByEndPosition = new Int2ObjectOpenHashMap<>();
        seenItemsByEndPosition.defaultReturnValue(new ArrayList<>());
        seenItemsByStartPosition = new Int2ObjectOpenHashMap<>();
        seenItemsByStartPosition.defaultReturnValue(new ArrayList<>());
        
        completedEdges = new LongOpenHashSet();
        edgeStorage = new ArrayList<>();
        
        insideMap = new Long2DoubleOpenHashMap();
        insideMap.defaultReturnValue(Double.NEGATIVE_INFINITY);
                
        pcfg = grammar;
        signature = pcfg.getSignature();
        
        n = words.size();
        summarizer = new SXEstimate(pcfg);
        
        /*
            Create startitems
        */
        for (int i = 0; i < n; i++) {
            int[] rhs = new int[1];
            rhs[0] = signature.getIdforSymbol(words.get(i));
            
//            System.err.println("Current word: " + words.get(i) +  "\twith ID: "+ rhs[0]);
            boolean found = false;
            for (Rule r : pcfg.getRules(rhs)) {
                found = true;
                Edge edge = new Edge(r.getLhs(), i, i+1, null, null);
                
                double inside = Math.log(r.getProb());
                double outsideEstimate = summarizer.evaluate(edge, words);
                
                updateInside(edge, inside);
                
                enqueue(edge, inside + outsideEstimate);
            }
            System.err.println(found? "Found! " : "Not found...");
    
        }
        
        /*
            Main loop
         */
        int edgeCounter = 0;
        while (!agenda.isEmpty()) { 
            ++edgeCounter;
            if (flush()) break;
            Edge item = dequeue();
//            System.err.println("Current Item: " + item.toStringReadable(signature));
            
            seenItemsByEndPosition.get(item.getBegin()).stream().forEach((candidate) -> {
                int[] rhs = new int[2];
                rhs[0] = candidate.getSymbol();
                rhs[1] = item.getSymbol();
                
                pcfg.getRules(rhs).stream().forEach((r) -> {
                    Edge edge = new Edge(r.getLhs(), candidate.getBegin(), item.getEnd(), candidate, item);

                    if (!completedEdges.contains(edge.asLongEncoding())) {
                        double insideLeft = getInside(candidate);
                        double insideRight = getInside(item);
                        double newInside = insideLeft + insideRight + Math.log(r.getProb());

                        double outsideEstimate = summarizer.evaluate(edge, words); 
                        
                        updateInside(edge, newInside);
                        enqueue(edge, newInside + outsideEstimate);
                    }

                });
            });
            
            seenItemsByStartPosition.get(item.getEnd()).stream().forEach((candidate) -> {
                int[] rhs = new int[2];
                rhs[0] = item.getSymbol();
                rhs[1] = candidate.getSymbol();

                pcfg.getRules(rhs).stream().forEach((r) -> {
                    Edge edge = new Edge(r.getLhs(), item.getBegin(), candidate.getEnd(), item, candidate);
                    
                    if (!completedEdges.contains(edge.asLongEncoding())) {
                        double insideLeft = getInside(item);
                        double insideRight = getInside(candidate);
                        double newInside = insideLeft + insideRight + Math.log(r.getProb());
                        
                        double outsideEstimate = summarizer.evaluate(edge, words);

                        updateInside(edge, newInside);
                        enqueue(edge, newInside + outsideEstimate);                        
                    }

                });
            });
            
        }
        
//        System.err.println("Edges: " + edgeCounter);
        
        
        // Find final item and create a parse tree
        for (Edge finalItem : seenItemsByEndPosition.get(n)) {
            if (isStartItem(finalItem)) {
                return createParseTree(finalItem);
            }
        }
        
        return null;
    }
    
    private boolean isStartItem(Edge item) {
        return item.getEnd() == n && item.getBegin() == 0 && item.getSymbol() == pcfg.getStartSymbol();
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
    
}
