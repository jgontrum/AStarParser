package de.up.ling.stud.astar;

import de.up.ling.stud.astar.pcfg.ParseException;
import de.up.ling.stud.astar.pcfg.Pcfg;
import de.up.ling.stud.astar.pcfg.PcfgParser;
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
    public static void main( String[] args ) throws ParseException
    {
        String grammar = "S\n"
                + "Det -> den [1]\n"
                + "N -> Baum [1]\n"
                + "NP -> Det N [0.5]\n"
                + "NP -> Hans [0.5]\n"
                + "S -> NP VP [1]\n"
                + "V -> mag [1]\n"
                + "VP -> V NP [1]";

        Pcfg pcfg = PcfgParser.parse(new StringReader(grammar));

        String sentence = "Hans mag den Baum";

        StringTokenizer tokenizer = new StringTokenizer(sentence);
        List<String> inputWord = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            inputWord.add(tokenizer.nextToken());
        }
        System.err.println(inputWord);

        astarParser parser = new astarParser();    
        System.err.println(parser.parse(inputWord, pcfg));
    }
}
