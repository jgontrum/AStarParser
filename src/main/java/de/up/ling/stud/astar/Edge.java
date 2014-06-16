/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.Signature;
import java.util.Arrays;

/**
 *
 * @author johannes
 */
class Edge {
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
        return end - begin;
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

    public Edge getFirstChild() {
        return childs[0];
    }

    public Edge getSecondChild() {
        return childs[1];
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + this.symbol;
        hash = 17 * hash + this.begin;
        hash = 17 * hash + this.end;
        hash = 17 * hash + Arrays.deepHashCode(this.childs);
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
        if (!Arrays.deepEquals(this.childs, other.childs)) {
            return false;
        }
        return true;
    }
    public String toString() {
        return "<" + symbol + "," + begin + "," + end + ">";
    }

    public String toStringReadable(Signature signature) {
        return "<" + signature.getSymbolForId(symbol) + "," + begin + "," + end + ">()";
    }

}
