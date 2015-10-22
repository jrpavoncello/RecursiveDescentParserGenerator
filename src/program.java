/**
 * @author Josh Pavoncello
 * Project: Assignment 1, Problem #2
 * Date: 10/22/15
 */

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The class program contains the main entry point of this code. Only one command line argument 
 * is available which is the path to a file that contains an LL(1) parsable grammar along with 
 * the corresponding parse table. It will output in the running directory of the application a file 
 * "recDescent.cpp" which is C++ source code of a recursive descent parser specifically for the provided grammar.
 * 
 * @author Josh Pavoncello
 *
 */
public class program
{
	/**
	 * First argument must be path to CFG input, the rest will be ignored.
	 * 
	 * @param args
	 * @throws java.io.IOException
	 */
	public static void main(String[] args) throws java.io.IOException
    {
        java.io.FileInputStream cfgInput = null;
        
    	cfgInput = new java.io.FileInputStream(args[0]);
    	
    	String inputString = "";
    	int nextByte = 0;
    	while((nextByte = cfgInput.read()) != -1)
    	{
    		char c = (char)nextByte;
    		inputString += c;
    	}
    	
    	cfgInput.close();
    	
    	LL1ParserGenerator generator = new program().new LL1ParserGenerator();
    	generator.Generate(inputString, "recDescent.cpp");
	}
	
	/**
	 * Responsible for reading the input string, parsing the sections of the CFG and parse table, 
	 * generating the parser source code, and printing it to file provided.
	 * 
	 * This class was created only in the interest of neatly keeping multiple classes within 
	 * same source file.
	 * 
	 * @author Josh Pavoncello
	 * 
	 */
	private class LL1ParserGenerator
	{
		public void Generate(String inputString, String outputPath) 
				throws FileNotFoundException, UnsupportedEncodingException
		{
			//Split the two main sections
	    	String[] inputSections = inputString.split("%%");
	    	
	    	//Analyze the parse table first to get information such as what terminals
	    	//we are dealing with, so we can differentiate between terminal and not.
	    	String parseTableSection = inputSections[1];
	    	
	    	//Replace Windows carriage returns, then split on line feed
	    	String[] parseTableRows = parseTableSection.replaceAll("\r", "").split("\n");
	    	
	    	//Take the first row containing the set of terminals
	    	String[] terminals = parseTableRows[1].trim().split(" ");
	    	
	    	//Used for quick reference later on
	    	HashSet<String> terminalNameSet = CreateTerminalSet(terminals);
			
	    	//Parse the parse table
	    	HashMap<String, HashMap<String, Integer>> nonTerminalToSymbolToRule = CreateParseTableHashMap(parseTableRows, terminals);
	    	
	    	//This section is where the CFG is defined
	    	String cfgProductionsSection = inputSections[0];
	    	
	    	//Build a hashmap of hashmaps that, given a nonterminal and a rule number, will get the
	    	//symbols produced (terminals and non-terminals) by that rule
	    	HashMap<String, HashMap<Integer, Symbol[]>> nonTerminalsToProducedSymbols = new HashMap<String, HashMap<Integer, Symbol[]>>();
	    	
	    	//Build a hashmap of what rule number maps to what non-terminal
	    	HashMap<Integer, String> ruleNumberToNonTerminal = new HashMap<Integer, String>();
	    	
	    	//Parse the CFG into these structures
	    	PopulateDataFromCFG(cfgProductionsSection, terminalNameSet, nonTerminalsToProducedSymbols, ruleNumberToNonTerminal);
	    	
	    	//Write to the executing directory
	    	PrintWriter writer = new PrintWriter(outputPath, "UTF-8");
	    	
	    	//Use the supplied PrintWriter to write as we process all the given data
	    	GenerateAndPrintLL1ParserCode(nonTerminalToSymbolToRule,
					nonTerminalsToProducedSymbols, ruleNumberToNonTerminal,
					writer);
        	
        	writer.close();
		}

		/**
		 * Include TokenStream class with methods to provide an easy API for ourselves as we generate code. Including:
		 * Peek() to peek ahead in the token stream for the the next token without reading it
		 * Read() to get the current token and advance the pointer to the next token
		 * IsNotEndOfInput() to check if it is safe to Peek() or Read() without exceeding the bounds of the string
		 * 
		 * More boilerplate code follows after we add our generated code
		 * 
		 * @param nonTerminalToSymbolToRule
		 * @param nonTerminalsToProducedSymbols
		 * @param ruleNumberToNonTerminal
		 * @param writer
		 */
		private void GenerateAndPrintLL1ParserCode(
				HashMap<String, HashMap<String, Integer>> nonTerminalToSymbolToRule,
				HashMap<String, HashMap<Integer, Symbol[]>> nonTerminalsToProducedSymbols,
				HashMap<Integer, String> ruleNumberToNonTerminal,
				PrintWriter writer)
		{
	    	writer.write(
"#include <string>" + "\n" +
"using namespace std;" + "\n" +
"\n" +
"class TokenStream" + "\n" +
"{" + "\n" +
"public:" + "\n" +
"	TokenStream(string& input, int index) : input(input), index(index)" + "\n" +
"	{" + "\n" +
"\n" +
"	}" + "\n" +
"\n" +
"	char& Peek()" + "\n" +
"	{" + "\n" +
"		return input[index];" + "\n" +
"	}" + "\n" +
"\n" +
"	char& Read()" + "\n" +
"	{" + "\n" +
"		return input[index++];" + "\n" +
"	}" + "\n" +
"\n" +
"	bool IsNotEndOfInput()" + "\n" +
"	{" + "\n" +
"		return index < input.size();" + "\n" +
"	}" + "\n" +
"\n" +
"private:" + "\n" +
"	string& input;" + "\n" +
"	int index;" + "\n" +
"};" + "\n" +
"\n" +
"class Parser" + "\n" +
"{" + "\n" +
"public:" + "\n");

	    	//Iterate over the hash map that represents the CFG section
	        Iterator<Entry<String, HashMap<Integer, Symbol[]>>> ntToSymbolIter = nonTerminalsToProducedSymbols.entrySet().iterator();
	        while (ntToSymbolIter.hasNext())
	        {
	            Map.Entry<String, HashMap<Integer, Symbol[]>> ntToSymbolPair = (Map.Entry<String, HashMap<Integer, Symbol[]>>)ntToSymbolIter.next();
	            
	            String nonTerminalName = ntToSymbolPair.getKey();

	            //Write the methods that are called when we must recurse in order to resolve a non-terminal to a terminal.
	            //These will return a boolean indicating whether they could resolve the non-terminal or not.
		    	writer.write(
"\n" +
"	static bool " + nonTerminalName + "(TokenStream& ts)" + "\n" +
"	{" + "\n" +
"		if(ts.IsNotEndOfInput())" + "\n" +
"		{" + "\n" +
"			switch(ts.Peek())" + "\n" +
"			{" + "\n");
	            
	            HashMap<String, Integer> symbolNameToRule = nonTerminalToSymbolToRule.get(nonTerminalName);

	            //In code we compare the value returned by Peek() to the symbols produced by the 
	            //predict set in the parse table in order to determine which rule to follow next.
	            //So we have to iterate over all terminals/non-zero-rule# pairs for the given non-terminal 
	            //provided in the parse table in order to call the correct generated function
		        Iterator<Entry<String, Integer>> symbolIter = symbolNameToRule.entrySet().iterator();
		        while (symbolIter.hasNext())
		        {
		        	Map.Entry<String, Integer> symbolPair = (Map.Entry<String, Integer>)symbolIter.next();
		        	
		        	String terminalName = symbolPair.getKey();
		        	Integer ruleNumber = symbolPair.getValue();

			    	writer.write(
"			case '" + terminalName + "':" + "\n" +
"				return _Rule" + ruleNumber.toString() + "(ts);" + "\n");
		        }

		    	writer.write(
"			}" + "\n" +
"		}" + "\n" +
"\n" +
"		return false;" + "\n" +
"	}" + "\n");
	        }

	        //Now we have to implement all the rule methods, so we'll need to iterate over the hashmap 
	        //built up saying which rule# belongs to which non terminal
	        Iterator<Entry<Integer, String>> ruleIter = ruleNumberToNonTerminal.entrySet().iterator();
	        while (ruleIter.hasNext())
	        {
	        	Map.Entry<Integer, String> rulePair = (Map.Entry<Integer, String>)ruleIter.next();
	        	
	        	Integer ruleNumber = rulePair.getKey();
	        	String nonTerminalName = rulePair.getValue();
	        	
	        	//Get the symbols (terminal and non-terminal) produced by the current rule
	        	Symbol[] producedNameToSymbol = nonTerminalsToProducedSymbols.get(nonTerminalName).get(ruleNumber);
    	    	writer.write(
"\n" +
"	static bool _Rule" + ruleNumber.toString() + "(TokenStream& ts)" + "\n" +
"	{" + "\n");

    	    	//If we didn't produce lambda by this rule, generate code to validate all conditions are met.
    	    	//This will be done all in one if statement by shortcircuiting && operations, since order is guaranteed.
				if(producedNameToSymbol.length > 0)
				{
	    	    	writer.write(
"		if(");
	    	    	String expressionText = "";
		        	
		        	for(Symbol symbol : producedNameToSymbol)
		        	{
		        		//In the case of a non-terminal, we need to recurse with its corresponding function
		        		//in order to resolve it. So we do not call Read to advance the input 
		        		if(symbol.Type == SymbolType.NonTerminal)
		        		{ 
		        			expressionText += symbol.Name + "(ts) && ";
		        		}
		        		else
		        		{
		        			//In the case of a terminal, we just need to make sure, again, that it's safe to Read 
		        			//because resolving a non-terminal may have advanced us past the end of input.
		        			//Then check if the current input symbol matches what we think it should be
		        			expressionText += "ts.IsNotEndOfInput() && ts.Read() == '" + symbol.Name + "' && ";
		        		}
		        	}
	        	
		        	writer.write(expressionText.substring(0, expressionText.length() - 4) + ")" + "\n" + 
"		{" + "\n" + 
"			return true;" + "\n" + 
"		}" + "\n" + 
"\n" + 
"		return false;" + "\n");
				}
				else
				{
					//Otherwise just return true without reading any input
		        	writer.write(
					"		return true;" + "\n");
				}

	        	writer.write(
"	}" + "\n");
	        }

	        //Write more boilerplate code, specifically the Parse function required by the assignment that will
	        //begin the parsing, return true if the input parsed successfully or false if not
        	writer.write(
"\n" +
"};" + "\n" +
"\n" +
"bool Parse(string& input)" + "\n" +
"{" + "\n" +
"	TokenStream* ts = new TokenStream(input, 0);" + "\n" +
"\n" +
"	if(ts->IsNotEndOfInput())" + "\n" +
"	{" + "\n" +
"		if(Parser::" + ruleNumberToNonTerminal.get(1) + "(*ts) && !ts->IsNotEndOfInput())" + "\n" +
"		{" + "\n" +
"			return true;" + "\n" +
"		}" + "\n" +
"	}" + "\n" +
"\n" +
"	return false;" + "\n" +
"}");
		}

		/**
		 * Build a hashmap of hashmaps to represent the parse table, only filling in the entries that are non-zero
		 * 
		 * @param parseTableRows
		 * @param terminals
		 * @return
		 */
		private HashMap<String, HashMap<String, Integer>> CreateParseTableHashMap(
				String[] parseTableRows, String[] terminals)
		{
			HashMap<String, HashMap<String, Integer>> nonTerminalToSymbolToRule = new HashMap<String, HashMap<String, Integer>>();
	    	
	    	for(int rowIndex = 2; rowIndex < parseTableRows.length; rowIndex++)
	    	{
	    		HashMap<String, Integer> symbolToRule = new HashMap<String, Integer>();
	    		
	    		String parseTableRow = parseTableRows[rowIndex];
	    		
	    		//Columns are separated by spaces
	    		String[] parseTableRowEntries = parseTableRow.split(" ");
	    		
	    		//First column is the non terminal
	    		String nonTerminal = parseTableRowEntries[0];
	    		
	    		//The rest are the corresponding numbered rule we should use when we encounter the 
	    		//symbol given by the column header
	    		for(int columnIndex = 1; columnIndex < parseTableRowEntries.length; columnIndex++)
	    		{
	    			Integer ruleNumber = Integer.parseInt(parseTableRowEntries[columnIndex]);
	    			
	    			//Only add if non-zero to reduce size of maps
	    			if(ruleNumber.intValue() != 0)
	    			{
	    				symbolToRule.put(terminals[columnIndex - 1], ruleNumber);
	    			}
	    		}
	    		
	    		nonTerminalToSymbolToRule.put(nonTerminal, symbolToRule);
	    	}
	    	
			return nonTerminalToSymbolToRule;
		}

		/**
		 * Creates a HashSet given the input string array of terminals.
		 * 
		 * @param terminals
		 * @return
		 */
		private HashSet<String> CreateTerminalSet(String[] terminals)
		{
			HashSet<String> terminalNameSet = new HashSet<String>();
	    	for(String terminal : terminals)
	    	{
	    		terminalNameSet.add(terminal);
	    	}
	    	
			return terminalNameSet;
		}
		
		/**
		 * Parses and populates the CFG into the nonTerminalsToProducedSymbols and ruleNumberToNonTerminal data structures for reference later on.
		 * 
		 * @param cfgProductionsSection
		 * @param terminalNameSet
		 * @param nonTerminalsToProducedSymbols
		 * @param ruleNumberToNonTerminal
		 */
		private void PopulateDataFromCFG(String cfgProductionsSection, HashSet<String> terminalNameSet, HashMap<String, HashMap<Integer, Symbol[]>> nonTerminalsToProducedSymbols, HashMap<Integer, String> ruleNumberToNonTerminal)
		{
	    	String[] cfgProductionsParts = cfgProductionsSection.replaceAll("\r", "").split("\n");
	    	
	    	//Examine each row
	    	for(int cfgPartIndex = 0; cfgPartIndex < cfgProductionsParts.length; cfgPartIndex++)
	    	{
	    		String cfgProductionPart = cfgProductionsParts[cfgPartIndex];
	    		
	    		String cfgProductionPartTrimmed = cfgProductionPart.trim();
	    		
	    		//Separate the non-terminal LHS from RHS
	    		String[] cfgProductionParts = cfgProductionPartTrimmed.split("::=");
	    		
	    		String nonTerminal = cfgProductionParts[0].trim();
	    		
	    		//If there was a null production, let production string be empty
	    		String production = cfgProductionParts.length == 1 ? "" : cfgProductionParts[1].trim();
	    		
	    		//Each symbol on RHS will be delimited by a space
	    		String[] symbolNames = production.split(" ");

	    		Symbol[] productionSymbols = new Symbol[symbolNames.length];
	    		
	    		//Even after a split, an empty string will still produce an array with 1 element
	    		for(int i = 0; i < symbolNames.length; i++)
	    		{
	    			String symbolName = symbolNames[i];
	    			
	    			//Detect if that condition happened
	    			if(symbolName != null && !symbolName.equals(""))
	    			{
	    				//Create a symbol object that will tell us whether the symbol was a 
	    				//terminal or non-terminal. This will be important later when we are
	    				//deciding what code to generate for the parser source code.
		    			Symbol symbol = new Symbol();
		    			
		    			symbol.Name = symbolName;
		    			
		    			if(terminalNameSet.contains(symbolName))
		    			{
		    				symbol.Type = SymbolType.Terminal;
		    			}
		    			else
		    			{
		    				symbol.Type = SymbolType.NonTerminal;
		    			}
	
		    			productionSymbols[i] = symbol;
	    			}
	    			else
	    			{
	    				//If so, make sure the symbol array is empty so we know there was a
	    				//lambda production
	    				productionSymbols = new Symbol[0];
	    			}
	    		}
	    		
	    		//If any existing productions exist for this non-terminal, get the hashmap and add a record
	    		HashMap<Integer, Symbol[]> existingProductions = nonTerminalsToProducedSymbols.get(nonTerminal);
	    		
	    		if(existingProductions == null)
	    		{	
	    			//Otherwise, create a new one
	    			existingProductions = new HashMap<Integer, Symbol[]>();
		    		
		    		nonTerminalsToProducedSymbols.put(nonTerminal, existingProductions);
	    		}
	    		
	    		existingProductions.put(cfgPartIndex + 1, productionSymbols);
    			
				ruleNumberToNonTerminal.put(cfgPartIndex + 1, nonTerminal);
	    	}
		}
		
		private class Symbol
		{
			public String Name;
			public SymbolType Type;
		}
	}
	
	private enum SymbolType
	{
		NonTerminal,
		Terminal
	}
}
