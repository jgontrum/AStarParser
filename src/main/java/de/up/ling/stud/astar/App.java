package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.ParseException;
import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.PcfgParser;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws ParseException, FileNotFoundException
    {
        String grammar = "S\n"
                + "Det -> den [1]\n"
                + "N -> Baum [1]\n"
                + "NP -> Det N [0.5]\n"
                + "NP -> Hans [0.5]\n"
                + "S -> NP VP [1]\n"
                + "V -> mag [1]\n"
                + "VP -> V NP [1]";
        

        Pcfg pcfg;
//        pcfg = PcfgParser.parse(new StringReader(grammar));
        pcfg = PcfgParser.parse(new FileReader(new File("examples/grammar.txt")));
        
        String sentence;
        sentence = "Hans mag den Baum";
        sentence = "The market crumbled . @";
        
        StringTokenizer tokenizer = new StringTokenizer(sentence);
        List<String> inputWord = new ArrayList();
        IntList wordsDecoded = new IntArrayList();
        while (tokenizer.hasMoreTokens()) {
            String currentWord = tokenizer.nextToken();
            inputWord.add(currentWord);
            wordsDecoded.add(pcfg.getSignature().addSymbol(currentWord));
        }
        System.err.println(inputWord);

        astarParser parser = new astarParser();
        Tree res = parser.parse(inputWord, pcfg);
        System.err.println(res);
        if (res != null) res.draw();
    }
}
