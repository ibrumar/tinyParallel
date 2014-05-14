/**
 * Copyright (c) 2011, Jordi Cortadella
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of the <organization> nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package interp;

import parser.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Iterator;
import java.lang.StringBuilder;
import java.util.Set;
import java.io.*;
import org.antlr.runtime.Token;



/** Class that implements the interpreter of the language. */

public class Interp {

    /** Memory of the virtual machine. */
    private Stack Stack;
    
    private static int counterSpace = 0;
    
    private static ArrayList<String> listFunc = new ArrayList<String>();

    /**
     * Map between function names (keys) and ASTs (values).
     * Each entry of the map stores the root of the AST
     * correponding to the function.
     */
    private HashMap<String,AslTree> FuncName2Tree;

    /** Standard input of the interpreter (System.in). */
    private Scanner stdin;

    /**
     * Stores the line number of the current statement.
     * The line number is used to report runtime errors.
     */
    private int linenumber = -1;

    /** File to write the trace of function calls. */
    private PrintWriter trace = null;

    /** Nested levels of function calls. */
    private int function_nesting = -1;
    
    /** Says if the compilation is done in a parallel region */
    private boolean inParallelRegion = false;

    /** Says if the compilation is done in notSync region */
    private boolean inNotSyncRegion = false;
    /**
     * Constructor of the interpreter. It prepares the main
     * data structures for the execution of the main program.
     */
    public Interp(AslTree T, String tracefile) {
        assert T != null;
        MapFunctions(T);  // Creates the table to map function names into AST nodes
        PreProcessAST(T); // Some internal pre-processing ot the AST
        Stack = new Stack(); // Creates the memory of the virtual machine
        // Initializes the standard input of the program
        stdin = new Scanner (new BufferedReader(new InputStreamReader(System.in)));
        if (tracefile != null) {
            try {
                trace = new PrintWriter(new FileWriter(tracefile));
            } catch (IOException e) {
                System.err.println(e);
                System.exit(1);
            }
        }
        function_nesting = -1;
    }
    /** Runs the program by calling the main function without parameters. */
    public void Run() throws ParallelException {
        //Basic initializations
        StringBuilder genCode = new StringBuilder();
        genCode.append("#include <iostream>" + "\n");
        genCode.append("#include <vector>" + "\n");
        genCode.append("#include <omp.h>" + "\n");
        genCode.append("using namespace std;" + "\n\n");

      //  Set<String> functionNames = FuncName2Tree.keySet();
      //  Iterator<String> funcNameIter = functionNames.iterator();
        Iterator<String> funcIter = listFunc.iterator();
      //  while (funcNameIter.hasNext()) {
        while (funcIter.hasNext()) {
            counterSpace = 0;
       //     String funcNameStr = funcNameIter.next();
         String funcNameStr = funcIter.next();
            BooleanContainer hasReferenceParams = new BooleanContainer();
            hasReferenceParams.data = new Boolean(false);
            //genCode.append("The name of the function is" + funcNameStr + "\n");
            StringBuilder generatedFunctionCode = new StringBuilder();

            if (funcNameStr.equals("main")) {
                
            }
            else {
       //       genCode.append("Help" + funcNameStr + "\n");
                AslTree funcNode = FuncName2Tree.get(funcNameStr);
               
                int funcNameSize = funcNameStr.length();
                if (funcNameStr.charAt(funcNameSize - 1) == '$') {
                    System.err.println ("Generating the parallelized version of " + funcNameStr.substring(0, funcNameSize - 1));
                    inParallelRegion = true;
                    inNotSyncRegion = false;
                    try {
                        //the name already holds the '$' character
                        generateFunction(funcNameStr, hasReferenceParams, generatedFunctionCode);
                        genCode.append(generatedFunctionCode);
                    } catch (ParallelException pe) {
                        System.err.println ("Note: The parallel synced version of " + funcNameStr + " cannot be generated");
                 //       funcNameIter.remove();
                        funcIter.remove();
                        FuncName2Tree.remove(funcNameStr);
                    }
                }
                else if (funcNameStr.charAt(funcNameSize - 1) == '_')
                {
                    inParallelRegion = true;
                    inNotSyncRegion = true;
                    System.err.println ("Generating the not_synchronized parallelized version of " + funcNameStr.substring(0, funcNameSize - 1));
                    try {
                        //the name already holds the '_' character
                        generateFunction(funcNameStr, hasReferenceParams, generatedFunctionCode);
                        genCode.append(generatedFunctionCode);
                    } catch (ParallelException pe) {
                        System.err.println ("Note: The parallel NOT synced version of " + funcNameStr + " cannot be generated");
                    //    funcNameIter.remove();
                        funcIter.remove();
                        FuncName2Tree.remove(funcNameStr);
                    }
                    //System.out.println("Those are the function keys " + FuncName2Tree.keySet());

                }
                else {
                    inParallelRegion = false;
                    inNotSyncRegion = false;
                    System.err.println ("Generating the serial version of " + funcNameStr.substring(0, funcNameSize));
                    try {
                        generateFunction(funcNameStr, hasReferenceParams, generatedFunctionCode);
                        genCode.append(generatedFunctionCode);
                    } catch (ParallelException pe) {
                     //   funcNameIter.remove();
                        funcIter.remove();
                        FuncName2Tree.remove(funcNameStr);
                        System.err.println ("Note: The serial version of " + funcNameStr + " cannot be generated");
                    }
                }
                inParallelRegion = false;
            }
            System.err.print("\n");
        }

        System.err.println ("Generating the main function\n");
        inParallelRegion = false;
        inNotSyncRegion = false;
        StringBuilder generatedFunctionCode = new StringBuilder();
        BooleanContainer hasReferenceParams = new BooleanContainer();
        counterSpace = 0;
        generateFunction("main", hasReferenceParams, generatedFunctionCode);
        genCode.append(generatedFunctionCode);
        System.out.println(genCode); 
    }
    /** Returns the contents of the stack trace */
    public String getStackTrace() {
        return Stack.getStackTrace(lineNumber());
    }

    /** Returns a summarized contents of the stack trace */

    /**
     * Gathers information from the AST and creates the map from
     * function names to the corresponding AST nodes.
     * The functions list holds until the first instruction is found
     */
    private void MapFunctions(AslTree T) { // T = PROG
        //assert T != null && T.getType() == AslLexer.LIST_FUNCTIONS;
        FuncName2Tree = new HashMap<String,AslTree> ();
        int n = T.getChildCount();
        int i = 0;
        boolean foundMain = false;
        while (i < n && !(foundMain)) {
            //genCode.append("iter " + i + " with n = " + n + "\n");
            AslTree f = T.getChild(i);
            //assert f.getType() == AslLexer.FUNC; //IF f.getType != FUNC -> exception
            foundMain = (f.getType() == AslLexer.MAIN);
            //genCode.append("f's type is " + f.getType() + "\n");
            if (!foundMain) {
                String fname = f.getText(); //ID
                if (FuncName2Tree.containsKey(fname)) {
                    throw new RuntimeException("Multiple definitions of function " + fname);
                }
                else {
                    listFunc.add(fname);
                    FuncName2Tree.put(fname, f);
                    listFunc.add(fname + "$");
                    FuncName2Tree.put(fname + "$", f);//the parallel version points the same node
                    listFunc.add(fname + "_");
                    FuncName2Tree.put(fname + "_", f);//the parallel version points the same node
                }
            }
            else 
                FuncName2Tree.put("main", f);
            ++i;
        } 
    }

    /**
     * Performs some pre-processing on the AST. Basically, it
     * calculates the value of the literals and stores a simpler
     * representation. See AslTree.java for details.
     */
    private void PreProcessAST(AslTree T) {
        if (T == null) return;
        switch(T.getType()) {
            case AslLexer.INTLIT: T.setIntValue(); break;
            case AslLexer.STRING: T.setStringValue(); break;
            case AslLexer.BOOLEAN: T.setBooleanValue(); break;
            default: break;
        }
        int n = T.getChildCount();
        for (int i = 0; i < n; ++i) PreProcessAST(T.getChild(i));
    }

    /**
     * Gets the current line number. In case of a runtime error,
     * it returns the line number of the statement causing the
     * error.
     */
    public int lineNumber() { return linenumber; }

    /** Defines the current line number associated to an AST node. */
    private void setLineNumber(AslTree t) { linenumber = t.getLine();}

    /** Defines the current line number with a specific value */
    private void setLineNumber(int l) { linenumber = l;}


    private String xTimesChar(int n){
        String res = "";
        for (int i =0; i<n; i++){
            res = res + "  ";
        }
        return res;
    }

    /**
     * Executes a function.
     * @param funcname The name of the function.
     * @param args The AST node representing the list of arguments of the caller.
     * @return The data returned by the function.*/

      private Data generateFunction (String funcnameArg, BooleanContainer hasReferenceParams, StringBuilder genCode) throws ParallelException {
        // Get the AST of the function
        AslTree f = FuncName2Tree.get(funcnameArg);
        //For main we simulate just a list of normal instructions
        if (f == null) throw new RuntimeException(" function " + funcnameArg + " not declared");

        AslTree returnType = f.getChild(0);
        String funcName = f.getText();

        switch (returnType.getType()) {
            case AslLexer.INT: genCode.append("int "); break;
            case AslLexer.BOOL: genCode.append("bool "); break;
            default: throw new RuntimeException("Not a recognized return type");
        }
        
        //we use funcnameArg because in the case of the main
        //in the tree we have a MAIN imaginary token not "main"
        genCode.append(funcnameArg + " (");

        
        // List of parameters of the callee
        AslTree p = f.getChild(1);
        int nparam = p.getChildCount(); // Number of parameters

        // Create the activation record in memory
        Stack.pushActivationRecord(funcnameArg, lineNumber());

        // Track line number. Maybe the previous line should be changed with this one
        setLineNumber(f);
         
        // Copy the parameters to the current activation record
        for (int i = 0; i < nparam; ++i) {
            AslTree paramNode = p.getChild(i);
            
            String param_type = paramNode.getText();
            
            boolean paramIsVector = (paramNode.getChildCount() != 1);

            if (!paramIsVector)
                genCode.append(param_type + " ");
            else
                genCode.append("vector<" + param_type + "> ");
           
            Data passedData = new Data(paramNode.getText());
            //if the passed data is a vector it will always be by reference
            if (AslLexer.PREF == paramNode.getChild(0).getType() || paramIsVector) {
                hasReferenceParams.data = new Boolean(true);
                genCode.append("&");
                //System.err.println("PREF");
            }

            else if (inParallelRegion) {
                passedData.setShared(false);
            }
            
            String param_name = paramNode.getChild(0).getText();
            genCode.append(param_name);
            if (i < nparam-1) {
                genCode.append(", ");
            }

            Data toDefine = new Data(paramNode.getText());
            if (paramIsVector) 
                toDefine.setVector();

            Stack.defineVariable(param_name, toDefine);
        }

        genCode.append(") {" + "\n");
        counterSpace += 2;
        // Execute the instructions
        Data result = generateListInstructions(f.getChild(2), genCode);

        counterSpace -= 2;
        genCode.append("}" + "\n\n");

        // If the result is null, then the function returns void
        if (result == null) result = new Data("void");
        
        // Destroy the activation record
        Stack.popActivationRecord();

        return result;
    }
     
    /**
     * Executes a block of instructions. The block is terminated
     * as soon as an instruction returns a non-null result.
     * Non-null results are only returned by "return" statements.
     * @param t The AST of the block of instructions.
     * @return The data returned by the instructions (null if no return
     * statement has been executed).
     */
    private Data generateListInstructions (AslTree t, StringBuilder genCode) throws ParallelException {
        assert t != null;
        assert t.getType() == AslLexer.INSTR_BLOCK;
        Data result = null;
        int ninstr = t.getChildCount();
        for (int i = 0; i < ninstr; ++i) {
            String indentation = xTimesChar(counterSpace);
            if (t.getChild(i).getType() != AslLexer.NOT_SYNC){
                genCode.append(indentation);
            }
            generateInstruction (t.getChild(i), genCode);
            if (t.getChild(i).getType() == AslLexer.ASSIGN){
            	genCode.append(";" + "\n");
            }
        }
        return null;
    }
  
    private void checkParamsArgs(AslTree paramsNode, Data typeArg, int numArg) {
        //System.err.println("The parameter "+ paramsNode.getChild(numArg).getChild(0).getText() + " has " + paramsNode.getChild(numArg).getChildCount() + " childs and the argument is a vector " + typeArg.isVector());
        if (paramsNode.getChild(numArg).getChildCount() != 1 && !typeArg.isVector())
            throw new RuntimeException ("The argument " + numArg + " should be a vector");       
        if (paramsNode.getChild(numArg).getChildCount() == 1 && typeArg.isVector())
            throw new RuntimeException ("The argument " + numArg + " shouldn't be a vector");       
        if (!typeArg.getType().equals(paramsNode.getChild(numArg).getText()))
            throw new RuntimeException ("The type of the argument " + numArg + " doesn't match to its corresponding parameter");       
    }

    /*Generates the func call with all the necessary checks
      We do a special function because a function call can be done from generateInstruction
      as well as from generateExpression
    */
    private Data generateFuncall(AslTree t, StringBuilder genCode) {
        String funcName = t.getChild(0).getText();
        String errorMessage;
        if (inParallelRegion && inNotSyncRegion) {
            errorMessage = "Call to unexisting not syncronized version of function "+ funcName;
            funcName = funcName + "_";
        }
        else if (inParallelRegion) {
            errorMessage = "Call to unexisting parallel (and syncronized) version of function "+ funcName;
            funcName = funcName + "$";
        }
        else {
            errorMessage = "Call to unexisting function "+ funcName;
        }
        
        if (!FuncName2Tree.containsKey(funcName))
            throw new RuntimeException (errorMessage);
        
        AslTree args = t.getChild(1);
        int numArgs = args.getChildCount();
        
        AslTree functionTree = FuncName2Tree.get(funcName);
        AslTree paramsNode = functionTree.getChild(1);
        int numParams = paramsNode.getChildCount();
        if (numArgs != numParams)
            throw new RuntimeException ("Number of arguments doesn't match the number of parameters in"+ funcName );
        
        genCode.append(funcName + "(");
        boolean firstIter = true;
        
        for (int i = 0; i < numArgs; ++i) {
            if (firstIter) firstIter = false;
            else genCode.append(", ");
            
            AslTree a = args.getChild(i);
            AslTree p = paramsNode.getChild(i);
            if (a.getType() != AslLexer.ID && p.getChild(0).getType() == AslLexer.PREF)
                throw new RuntimeException ("Argument " + i + "must be an identifier. The corresponding parameter is reference passed");
            
            setLineNumber(a);
            Data res = generateExpression(a, genCode); //this simply looks at the id and returns the dataType
            checkParamsArgs(paramsNode, res, i);
        }
        Data value = new Data(functionTree.getChild(0).getText()); //this tells you the type returned
        genCode.append(")");
        return value;
    }


    void generateParallelZone(AslTree t, StringBuilder genCode) throws ParallelException {
        /** Says if the compilation is done in a parallel region */
        if (inParallelRegion)
            throw new ParallelException ("Opening parallel region inside another one");
            
        // in the other case, we create the zone
        inParallelRegion = true;
        String parallelZoneHeader = "#pragma omp parallel default(shared)";
        
        if (t.getChild(0).getType() == AslLexer.PRIVATE_VAR) { //there are also private variables
            
            AslTree privateVarNode = t.getChild(0);
            parallelZoneHeader += " private("; //there must be at least one private var
            boolean first = true; 
           
            for (int i = 0; i < privateVarNode.getChildCount(); ++i) {
                Data thePrivateVar = Stack.getVariable(privateVarNode.getChild(i).getText());
                thePrivateVar.setShared(false);
                if (thePrivateVar.isVector())
                    throw new RuntimeException ("The arrays in tiny-parallel language can't be privatized");
                
                if (first) first = false;
                else parallelZoneHeader += ", ";
                parallelZoneHeader += privateVarNode.getChild(i).getText();
            }
            parallelZoneHeader += ")";
        } 
        

        genCode.append(parallelZoneHeader + "\n");
        
        genCode.append(xTimesChar(counterSpace) + "{" + "\n");
        counterSpace += 2;
       
        if (t.getChild(0).getType() == AslLexer.PRIVATE_VAR)
            generateListInstructions(t.getChild(1), genCode);
        else 
            generateListInstructions(t.getChild(0), genCode);
        
        counterSpace -= 2;
        genCode.append(xTimesChar(counterSpace) +"}" + "\n");

        
        if (t.getChild(0).getType() == AslLexer.PRIVATE_VAR) { //there are also private variables
            
            AslTree privateVarNode = t.getChild(0);

            for (int i = 0; i < privateVarNode.getChildCount(); ++i) {
                Data thePrivateVar = Stack.getVariable(privateVarNode.getChild(i).getText());
                thePrivateVar.setShared(true);
            }
        }
        // you must take care because the variables declared inside the parallel zone must die
        inParallelRegion = false;
    }



    // Function to generate the header of the for and the for parallel (there is the same, that's why this funcion exists)
    private void generateHeaderFor(AslTree t, StringBuilder genCode) throws ParallelException{
        
        genCode.append("for (");
           	
        /* header - parte assignation del variant*/
		
        String varBoucle = t.getChild(0).getChild(0).getText();
		
        Data variant = Stack.getVariable(varBoucle);
        
        //case pragma omp critical - we don't want to permit to use a variable with critical in the header of the header of a parallel_for
		if (variant.isShared() && !inNotSyncRegion && inParallelRegion) 
		    throw new RuntimeException ("Variant of the header of a parallel_for must be private variable in");
		
        if (!variant.isInteger()) throw new RuntimeException ("Variant must be an integer for a boucle for"); 
		
        generateInstruction(t.getChild(0), genCode);
		
        genCode.append(" ; ");
		
		
        /*header - parte increment*/
        AslTree forCompa = t.getChild(1);
		
        if (forCompa.getType() != AslLexer.LE &&
            forCompa.getType() != AslLexer.LT &&
            forCompa.getType() != AslLexer.GE &&
            forCompa.getType() != AslLexer.GT ) throw new RuntimeException ("Must be comparation for a boucle for"); 
            
        generateExpression(forCompa, genCode);
            	
        genCode.append(" ; ");
            	  	
        AslTree forPlus = t.getChild(2);
		
        if (forPlus.getType() != AslLexer.ASSIGN)
		    throw new RuntimeException ("Must be assignation for a boucle for"); 
        
        generateInstruction(forPlus, genCode);
        
           	
        genCode.append(") { \n");
        counterSpace += 2;
    }



                    
    /**
     * Executes an instruction. 
     * Non-null results are only returned by "return" statements.
     * @param t The AST of the instruction.
     * @return The data returned by the instruction. The data will be
     * non-null only if a return statement is executed or a block
     * of instructions executing a return.
     */
    private void generateInstruction (AslTree t, StringBuilder genCode) throws ParallelException {

        assert t != null;
        
        setLineNumber(t);

        // A big switch for all type of instructions
        switch (t.getType()) {

            case AslLexer.BEGIN_PARALLEL:
            {
                generateParallelZone(t, genCode);
                return;
            }

            case AslLexer.NOT_SYNC:
            {
                if (!inParallelRegion) {
                    System.err.print ("Note: Using a not_sync outside a parallel region is useless:"+ lineNumber() + "\n");
                    generateListInstructions(t.getChild(0), genCode);
                }
                else if (inNotSyncRegion) { 
                    System.err.println ("Note: Using a not_sync inside a not_sync is useless:"+ lineNumber() + "\n");
                    generateListInstructions(t.getChild(0), genCode);
                
                }
                else {
                    inNotSyncRegion = true;
                    generateListInstructions(t.getChild(0), genCode);
                    inNotSyncRegion = false;
                }
                return;
            }
            
            // Assignment
            case AslLexer.ASSIGN:
            {
                //The following call is used only for existance check
                AslTree identNode = t.getChild(0);
                AslTree exprNode = t.getChild(1);
                Data toChange;
                
                if (identNode.getType() != AslLexer.OPENC) {
                    toChange = Stack.getVariable(identNode.getText());
                    if (toChange.isVector())
                        throw new RuntimeException ("Error: A vector can be accessed only using vector_id[element] :" + t.getLine());

                    if (toChange.isShared() && !inNotSyncRegion && inParallelRegion)
                        genCode.append("#pragma omp critical" + "\n" + xTimesChar(counterSpace));
                    genCode.append(identNode.getText() + " = ");
                }
                
                else {
                    toChange = Stack.getVariable(identNode.getChild(0).getText());
                    checkVector(toChange);
                    
                    if (toChange.isShared() && !inNotSyncRegion && inParallelRegion)
                        genCode.append("#pragma omp critical" + "\n" + xTimesChar(counterSpace));
                    
                    genCode.append(identNode.getChild(0).getText() + "[");
                    Data vectorIndex = generateExpression(identNode.getChild(1), genCode);
                    genCode.append("] = ");

                }
                Data value = generateExpression(exprNode, genCode);
                if (!value.getType().equals(toChange.getType()))
                    throw new RuntimeException ("Right hand side value doesn't have the same type of the vector");

               // genCode.append(";\n");
                return;
            }
            
            
            // Declaration statement
            case AslLexer.DECL:
            {
                AslTree identNode = t.getChild(1);
                AslTree typeNode = t.getChild(0);
                Data value = new Data(typeNode.getText());
                
                if (inParallelRegion) value.setShared(false);

                if (identNode.getType() == AslLexer.OPENC) {
                    String vectorType = "vector<" + typeNode.getText() + "> ";
                    genCode.append( vectorType + identNode.getChild(0).getText());
                    genCode.append(" = * new " + vectorType + "(");
                    Data vectorIndex = generateExpression(identNode.getChild(1), genCode);
                    genCode.append(", 0);" + "\n"); //it fills all declared vectors with zeros
                    
                    checkInteger(vectorIndex);
                    
                    value.setVector();
                    Stack.defineVariable (identNode.getChild(0).getText(), value);
                }
                else {
                    genCode.append(typeNode.getText() + " " + identNode.getText() + ";" + "\n");//this won't work if working with
                    Stack.defineVariable (t.getChild(1).getText(), value);
                }
                return;
            }
            
            
            // If - else statement
            case AslLexer.IF:
            {

                genCode.append("if (");
            	
                Data value = generateExpression(t.getChild(0), genCode);
              
                checkBoolean(value);
                
                
                genCode.append (") { \n");
                counterSpace += 2;
                
                generateListInstructions(t.getChild(1), genCode);

                counterSpace -= 2;
                genCode.append(xTimesChar(counterSpace) + "} \n");
                
                if (t.getChildCount() == 3){//test of the presence of else statement
                    
                    genCode.append(xTimesChar(counterSpace) +"else { \n");
                    counterSpace += 2;
                    
                    generateListInstructions(t.getChild(2), genCode);
                    counterSpace -= 2;
                    genCode.append( xTimesChar(counterSpace) +"} \n");
                }
               
                return;
            }


    
            case AslLexer.FOR:
            {
                //header
                generateHeaderFor(t, genCode);
            	

            	//instructions in the for
                generateListInstructions(t.getChild(3), genCode);
                
            	counterSpace -= 2;           	
                genCode.append(xTimesChar(counterSpace) +"} \n");
            	return;

            }
            
            
            // Parallel for statement 
            case AslLexer.PARALLEL_FOR:
            {
                //test error if you are not yet in a parallel zone               
                
                if(!inParallelRegion) throw new ParallelException(); 
                      
                //print del pragma
                //genCode.append("#pragma omp for\n"+ xTimesChar(counterSpace)+ "{\n");
                genCode.append("#pragma omp for\n");
                counterSpace += 2;
                
                //gestion de private/shared by instructions/expressiones
                
                /*Header del for*/
                genCode.append(xTimesChar(counterSpace));
                generateHeaderFor(t, genCode);
                /*Cuerpo del for*/
                generateListInstructions(t.getChild(3), genCode);
                counterSpace -= 2;
                genCode.append(xTimesChar(counterSpace) +"} \n");
                counterSpace -= 2;
                return;
            }
            
            // Parallel for statement 
            case AslLexer.PAR_ASSIGN:
            {
                //test if you are not yet in a parallel zone, open one !
                if(!inParallelRegion){
                 genCode.append("#pragma omp parallel default(shared)\n" + xTimesChar(counterSpace)+ "{\n");
                 counterSpace += 2;
                 genCode.append(xTimesChar(counterSpace));
                } 
                // if(!inParallelRegion) throw new ParallelException(); 
                
                //The following call is used only for existance check - verification that for the variables used in the assign are Vectors
                AslTree id0Node = t.getChild(0);
               
                AslTree id1Node = t.getChild(1);
                
                AslTree expr = t.getChild(2);
               
                Data toChange0;
                Data toChange1;
                toChange0 = Stack.getVariable(id0Node.getText());
                checkVector(toChange0);
                toChange1 = Stack.getVariable(id1Node.getText());
                checkVector(toChange1);
                
               // Check if the vectors are of the same type
                if (toChange0.getType() == (toChange1.getType()))
                    throw new RuntimeException ("Type of the both vectors mismatch");
                    
                    /* parallel assign in a not sync ?? tiene sentido ?
                    if (toChange.isShared() && !inNotSyncRegion && inParallelRegion){
                        System.out.println("#pragma omp critical");
                    }*/
                    
                    // System.out.print(id0Node.getChild(0).getText() + "[");
                   // Data vectorIndex = generateExpression(expr,genCode);
                    // System.out.print("] = ");
                
                 genCode.append("#pragma omp for\n");
                 counterSpace += 2;
                 //genCode.append(xTimesChar(counterSpace) + "int _i; \n");
                 genCode.append(xTimesChar(counterSpace) + "for (int _i = 0 ; _i < ");
                
                 generateExpression(expr,genCode);
                 
                 genCode.append(" ; _i = _i + 1) { \n");
                 counterSpace += 2;
                 genCode.append(xTimesChar(counterSpace) + id0Node.getText() + "[_i] = " + id1Node.getText() + "[_i]; \n" );
                 counterSpace -= 2;
                 genCode.append(xTimesChar(counterSpace) +"} \n");
                 counterSpace -= 2;
                 
                 // to close the parallel region opened before
                 if(!inParallelRegion){
                    counterSpace -= 2;
                    genCode.append(xTimesChar(counterSpace)+"}\n");
                 }
                 
                 return;
            }
                   /*
                    // Definition of index i of the for
                    Data index = new Data ("int");
                    String nom_var = "_i";
                    Stack.defineVariable(nom_var, index);
                   
                    Token kfor;
                    kfor.setText("FOR");
                    AslTree tfor = new AslTree(kfor);
                    
                    
                    Token kindex;
                    kindex.setText("=");
                    AslTree tindex = new AslTree(kindex);
                 // tindex.setStringValue("=");
                 // tindex.addChild(0, Stack.getVariable(nom_var));
                 // tindex.addChild(1, "0");
                 // tfor.addChild(0, tindex);
               
                    Token ki;
                    ki.setText("_i");
                    AslTree aki = new AslTree(ki);
                    tindex.addChild(aki);
                    
                    Token k0;
                    ki.setText("0");
                    AslTree ak0 = new AslTree(k0);
                    tindex.addChild(ak0);
                    tfor.addChild(tindex);
                    
                    Token kcompa;
                    kcompa.setText("<");
                    AslTree tcompa = new AslTree(kcompa);
                   // tcompa.setStringValue("<");
                   // tcompa.addChild(0, Stack.getVariable(nom_var));
                   // tcompa.addChild(1,vectorIndex);
                   // tfor.addChild(1, tindex);
                    tcompa.addChild(aki);
                    tcompa.addChild(ak0);
                    tfor.addChild(tindex);
                    
                    Token ksupp;
                    ksupp.setText("=");
                    AslTree tsupp = new AslTree(ksupp);
                 // tsupp.setStringValue("=");
                 // tindex.addChild(0, Stack.getVariable(nom_var));
                    tindex.addChild(aki);
                    Token kadd;
                    ksupp.setText("+");
                    AslTree tadd = new AslTree(kadd);
                 // tadd.setStringValue("+");
                 // tadd.addChild(0,"_i");
                 // tadd.addChild(1,"1");
                 // tindex.addChild(1, tsupp);
                 // tfor.addChild(2, tindex);
                    tadd.addChild(aki);
                    Token k1;
                    ki.setText("1");
                    AslTree ak1 = new AslTree(k1);
                    tadd.addChild(ak1);
                    tindex.addChild(tsupp);
                    tfor.addChild(tindex);
                    
                    // Header del for
                    generateHeaderFor(tfor, genCode);
                    
                    // assignation
                    System.out.print(id0Node.getText());
                    System.out.print("[_i] = ");
                    System.out.print(id0Node.getText());
                    System.out.print("[_i] ;");
                   */                    
                
            
            // Return statement
            case AslLexer.RETURN:
            {
                if (t.getChildCount() != 0) {
                    genCode.append("return ");
                    generateExpression(t.getChild(0), genCode);
                    genCode.append(";" + "\n");
                }
                return; // No expression: returns void data
            }
            
            
            // Read statement: reads a variable and raises an exception
            // in case of a format error.
            case AslLexer.READ:
                String varName = t.getChild(0).getText();
                Stack.checkVariableExists(varName);
                genCode.append("cin >> "+ varName + ";" + "\n");
                return;


            // Write statement: it can write an expression or a string.
            case AslLexer.WRITE:
                AslTree v = t.getChild(0);
                // Special case for strings
                if (v.getType() == AslLexer.STRING) {
                    genCode.append("cout << " + v.getText() + ";" + "\n");
                    return;
                }
                else {
                    // Write an expression
                    genCode.append("cout << ");
                    generateExpression(v, genCode);
                    genCode.append(";" + "\n");
                    return;
                }


            // Function call
            case AslLexer.FUNCALL:
                generateFuncall(t, genCode);
                genCode.append(";\n");
                return;

            default: assert false; // Should never happen
        }

        // All possible instructions should have been treated.
        assert false;
        return;
    }



    /**
     * Evaluates the expression represented in the AST t.
     * @param t The AST of the expression
     * @return The type of the expression and the generated code "genCode".
     */
   
    private Data generateExpression(AslTree t, StringBuilder genCode) {
        assert t != null;

        int previous_line = lineNumber();
        setLineNumber(t);
        int type = t.getType();

        Data value = null;
        // Atoms
        switch (type) {
            // A variable
            case AslLexer.ID:
                value = new Data(Stack.getVariable(t.getText()));
                //IF it hasn't returned an exception
                genCode.append(t.getText());
                break;
            // An integer literal
            case AslLexer.OPENC:
                
                genCode.append(t.getChild(0).getText() + "[");
                Data vectorIndex = generateExpression(t.getChild(1), genCode);
                genCode.append("]");
                    
                checkInteger(vectorIndex);
                    
                Data vectorData = Stack.getVariable(t.getChild(0).getText());
                checkVector(vectorData);
                value = new Data(vectorData.getType()); // the returned data has the type
                                                        // of an element of the array
                break; 
            case AslLexer.INTLIT:
                value = new Data("int");
                genCode.append(t.getText());

                
                break;
            // A Boolean literal
            case AslLexer.BOOLEAN:
                value = new Data("bool");
                genCode.append(t.getText());

                break;
            // A function call. Checks that the function returns a result.
            case AslLexer.FUNCALL:
                //value = generateFunction(t.getChild(0).getText(), t.getChild(1));
                //assert value != null;
                /*if (value.isVoid()) {
                    throw new RuntimeException ("function expected to return a value");
                }*/
                    value = generateFuncall(t, genCode);
                break;
            default: break;
        }

        // Retrieve the original line and return
        if (value != null) {
            setLineNumber(previous_line);
            return value;
        }
        
        // Unary operators
        
        if (t.getChildCount() == 1) {
            switch (type) {
                case AslLexer.PLUS:
                    genCode.append("+");
                    break;
                case AslLexer.MINUS:
                    genCode.append("-");
                    break;
                case AslLexer.NOT:
                    genCode.append("not");
                    break;
                default: assert false; // Should never happen
            }
            setLineNumber(previous_line);
            genCode.append("(");
            value = generateExpression(t.getChild(0), genCode);
            genCode.append(")");
            
            
            switch (type) {
                case AslLexer.PLUS:
                    checkInteger(value);
                    break;
                case AslLexer.MINUS:
                    checkInteger(value);
                    break;
                case AslLexer.NOT:
                    checkBoolean(value);
                    break;
                default: assert false; // Should never happen
            }
            return value;
       }
       

       //Debemos extender la implementacion para operadores relacionales
       // y aritmeticos
               
        // Two operands
        Data value2;
        switch (type) {
            // Relational operators
            case AslLexer.EQUAL:
            case AslLexer.NOT_EQUAL:
            case AslLexer.LT:
            case AslLexer.LE:
            case AslLexer.GT:
            case AslLexer.GE:
                //given that c permits boolean comparison we do too

                value = generateExpression(t.getChild(0), genCode);
                genCode.append(" " + t.getText() + " ");
                value2 = generateExpression(t.getChild(1), genCode);
                
                if (! value.getType().equals(value2.getType())) {
                    throw new RuntimeException ("Incompatible types in relational expression");

                }
                break;

            // Arithmetic operators
            case AslLexer.PLUS:
            case AslLexer.MINUS:
            case AslLexer.MUL:
            case AslLexer.DIV:
            case AslLexer.MOD:
             
                value = generateExpression(t.getChild(0), genCode);
                genCode.append(" " + t.getText() + " ");
                value2 = generateExpression(t.getChild(1), genCode);
                if (!value.getType().equals(value2.getType())) {
                    throw new RuntimeException ("Incompatible types in arithmetic expression");
                }
 
                break;

            // Boolean operators
            case AslLexer.AND:
            case AslLexer.OR:
                // The first operand is evaluated, but the second
                // is deferred (lazy, short-circuit evaluation).
                value = evaluateBoolean(t, genCode);
                break;

            default: assert false; // Should never happen
        }
        
        setLineNumber(previous_line);
        return value;
    }
    
    /**
     * Evaluation of Boolean expressions. This function implements
     * a short-circuit evaluation. The second operand is still a tree
     * and is only evaluated if the value of the expression cannot be
     * determined by the first operand.
     * @param type Type of operator (token).
     * @param v First operand.
     * @param t AST node of the second operand.
     * @return An Boolean data with the value of the expression.
     */
    private Data evaluateBoolean (AslTree t, StringBuilder genCode) {
        // Boolean evaluation with short-circuit

        Data leftOperandType = generateExpression(t.getChild(0), genCode);
        checkBoolean(leftOperandType);
        genCode.append(" " + t.getText() + " ");
        Data rightOperandType = generateExpression(t.getChild(1), genCode);
        checkBoolean(rightOperandType);
        genCode.append(";" + "\n");
        return leftOperandType; //the type is the same
    }

    /** Checks that the data is Boolean and raises an exception if it is not. */
    private void checkBoolean (Data b) {
        if (!b.isBoolean()) {
            throw new RuntimeException ("Expecting Boolean expression");
        }
    }
    
    /** Checks that the data is integer and raises an exception if it is not. */
    private void checkInteger (Data b) {
        if (!b.isInteger() || b.isVector()) {
            throw new RuntimeException ("Expecting numerical expression");
        }
    }

    /** Checks that the data is integer and raises an exception if it is not. */
    private void checkVector (Data b) {
        if (!b.isVector()) {
            throw new RuntimeException ("Expecting a name that points to a vector");
        }
    }

    /**
     * Writes trace information of a function call in the trace file.
     * The information is the name of the function, the value of the
     * parameters and the line number where the function call is produced.
     * @param f AST of the function
     * @param arg_values Values of the parameters passed to the function
     */
    private void traceFunctionCall(AslTree f, ArrayList<Data> arg_values) {
        function_nesting++;
        AslTree params = f.getChild(1);
        int nargs = params.getChildCount();
        
        for (int i=0; i < function_nesting; ++i) trace.print("|   ");

        // Print function name and parameters
        trace.print(f.getChild(0) + "(");
        for (int i = 0; i < nargs; ++i) {
            if (i > 0) trace.print(", ");
            AslTree p = params.getChild(i);
            if (p.getType() == AslLexer.PREF) trace.print("&");
            trace.print(p.getText() + "=" + arg_values.get(i));
        }
        trace.print(") ");
        
        if (function_nesting == 0) trace.println("<entry point>");
        else trace.println("<line " + lineNumber() + ">");
    }

    /**
     * Writes the trace information about the return of a function.
     * The information is the value of the returned value and of the
     * variables passed by reference. It also reports the line number
     * of the return.
     * @param f AST of the function
     * @param result The value of the result
     * @param arg_values The value of the parameters passed to the function
     */
    private void traceReturn(AslTree f, Data result, ArrayList<Data> arg_values) {
        for (int i=0; i < function_nesting; ++i) trace.print("|   ");
        function_nesting--;
        trace.print("return");
        if (!result.isVoid()) trace.print(" " + result);
        
        // Print the value of arguments passed by reference
        AslTree params = f.getChild(1);
        int nargs = params.getChildCount();
        for (int i = 0; i < nargs; ++i) {
            AslTree p = params.getChild(i);
            if (p.getType() == AslLexer.PVALUE) continue;
            trace.print(", &" + p.getText() + "=" + arg_values.get(i));
        }
        
        trace.println(" <line " + lineNumber() + ">");
        if (function_nesting < 0) trace.close();
    }
}
