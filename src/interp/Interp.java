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
import java.util.Set;
import java.io.*;

/** Class that implements the interpreter of the language. */

public class Interp {

    /** Memory of the virtual machine. */
    private Stack Stack;

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
    public void Run() {
        //Basic initializations
        System.out.println("#include <iostream>");
        System.out.println("#include <vector>");
        System.out.println("using namespace std;");

        Set<String> functionNames = FuncName2Tree.keySet();
        Iterator<String> funcNameIter = functionNames.iterator();
        while (funcNameIter.hasNext()) {
            String funcNameStr = funcNameIter.next();
            //System.out.println("The name of the function is" + funcNameStr);
            if (funcNameStr.equals("main")) generateFunction("main");
            else {
       //         System.out.println("Help" + funcNameStr);
                AslTree funcNode = FuncName2Tree.get(funcNameStr);
                generateFunction(funcNameStr);
            }
        }
        
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
            //System.out.println("iter " + i + " with n = " + n);
            AslTree f = T.getChild(i);
            //assert f.getType() == AslLexer.FUNC; //IF f.getType != FUNC -> exception
            foundMain = (f.getType() == AslLexer.MAIN);
            //System.out.println("f's type is " + f.getType());
            if (!foundMain) {
                String fname = f.getText(); //ID
                if (FuncName2Tree.containsKey(fname)) {
                    throw new RuntimeException("Multiple definitions of function " + fname);
                }
                else {
                    FuncName2Tree.put(fname, f);
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


    
    /**
     * Executes a function.
     * @param funcname The name of the function.
     * @param args The AST node representing the list of arguments of the caller.
     * @return The data returned by the function.*/
      private Data generateFunction (String funcnameArg) {
        // Get the AST of the function
        AslTree f = FuncName2Tree.get(funcnameArg);
        //For main we simulate just a list of normal instructions
        if (f == null) throw new RuntimeException(" function " + funcnameArg + " not declared");

        AslTree returnType = f.getChild(0);
        String funcName = f.getText();

        switch (returnType.getType()) {
            case AslLexer.INT: System.out.print("int "); break;
            case AslLexer.BOOL: System.out.print("bool "); break;
            default: throw new RuntimeException("Not a recognized return type");
        }
        
        //we use funcnameArg because in the case of the main
        //in the tree we have a MAIN imaginary token not "main"
        System.out.print(funcnameArg + " (");

        
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
            System.out.print(param_type + " ");
            
            if (AslLexer.PREF == paramNode.getChild(0).getType()) {
                System.out.print("&");
            }
            
            String param_name = paramNode.getChild(0).getText();
            System.out.print(param_name);
            if (i < nparam-1) {
                System.out.print(", ");
            }
            Stack.defineVariable(param_name, new Data(paramNode.getText()));
        }

        System.out.println (") {");
        // Execute the instructions
        Data result = generateListInstructions (f.getChild(2));
        System.out.println ("}");

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
    private Data generateListInstructions (AslTree t) {
        assert t != null;
        assert t.getType() == AslLexer.INSTR_BLOCK;
        Data result = null;
        int ninstr = t.getChildCount();
        for (int i = 0; i < ninstr; ++i) {
            generateInstruction (t.getChild(i));
        }
        return null;
    }
  
    private void checkParamsArgs(AslTree paramsNode, Data typeArg, int numArg) {
        //System.out.println("type arg is " + typeArg.getType() + "and type param is "+ paramsNode.getChild(numArg).getText());
        if (!typeArg.getType().equals(paramsNode.getChild(numArg).getText()))
            throw new RuntimeException ("The type of the argument " + numArg + " doesn't match to its corresponding parameter");
            
    }

    /*Generates the func call with all the necessary checks
      We do a special function because a function call can be done from generateInstruction
      as well as from generateExpression
    */
    private Data generateFuncall(AslTree t) {
        String funcName = t.getChild(0).getText();
        if (!FuncName2Tree.containsKey(funcName))
            throw new RuntimeException ("Call to unexisting function "+ funcName );
        
        AslTree args = t.getChild(1);
        int numArgs = args.getChildCount();
        
        AslTree functionTree = FuncName2Tree.get(funcName);
        AslTree paramsNode = functionTree.getChild(1);
        int numParams = paramsNode.getChildCount();
        if (numArgs != numParams)
            throw new RuntimeException ("Number of arguments doesn't match the number of parameters in"+ funcName );
        
        System.out.print(funcName + "(");
        boolean firstIter = true;
        
        for (int i = 0; i < numArgs; ++i) {
            if (firstIter) firstIter = false;
            else System.out.print(", ");
            
            AslTree a = args.getChild(i);
            AslTree p = paramsNode.getChild(i);
            if (a.getType() != AslLexer.ID && p.getChild(0).getType() == AslLexer.PREF)
                throw new RuntimeException ("Argument " + i + "must be an identifier. The corresponding parameter is reference passed");
            
            setLineNumber(a);
            Data res = generateExpression(a);
            checkParamsArgs(paramsNode, res, i);
        }
        Data value = new Data(functionTree.getChild(0).getText()); //this tells you the type returned
        System.out.println(");");
        return value;
    }



    /**
     * Executes an instruction. 
     * Non-null results are only returned by "return" statements.
     * @param t The AST of the instruction.
     * @return The data returned by the instruction. The data will be
     * non-null only if a return statement is executed or a block
     * of instructions executing a return.
     */
    private void generateInstruction (AslTree t) {
        assert t != null;
        
        setLineNumber(t);

        // A big switch for all type of instructions
        switch (t.getType()) {

            // Assignment
            case AslLexer.ASSIGN:
            {
                //The following call is used only for existance check
                AslTree identNode = t.getChild(0);
                AslTree exprNode = t.getChild(1);
                Data toChange;
                if (identNode.getType() != AslLexer.OPENC) {
                    toChange = Stack.getVariable(identNode.getText());
                    System.out.print(identNode.getText() + " = ");
                }
                else {
                    toChange = Stack.getVariable(identNode.getChild(0).getText());
                    checkVector(toChange);
                    
                    System.out.print(identNode.getChild(0).getText() + "[");
                    Data vectorIndex = generateExpression(identNode.getChild(1));
                    System.out.print("] = ");

                }
                Data value = generateExpression(exprNode);
                if (!value.getType().equals(toChange.getType()))
                    throw new RuntimeException ("Right hand side value doesn't have the same type of the vector");

                System.out.print(";\n");
                return;

            }
            // If-then-else
            case AslLexer.DECL:
            {
                AslTree identNode = t.getChild(1);
                AslTree typeNode = t.getChild(0);
                Data value = new Data(typeNode.getText());

                if (identNode.getType() == AslLexer.OPENC) {
                    String vectorType = "vector<" + typeNode.getText() + "> ";
                    System.out.print( vectorType + identNode.getChild(0).getText());
                    System.out.print(" = * new " + vectorType + "(");
                    Data vectorIndex = generateExpression(identNode.getChild(1));
                    System.out.println(", 0);"); //it fills all declared vectors with zeros
                    
                    checkInteger(vectorIndex);
                    
                    value.setVector();
                    Stack.defineVariable (identNode.getChild(0).getText(), value);
                }
                else {
                    System.out.println(typeNode.getText() + " " + identNode.getText() + ";");//this won't work if working with
                    Stack.defineVariable (t.getChild(1).getText(), value);
                }
                return;
            }
            case AslLexer.IF:
            {
                Data value = generateExpression(t.getChild(0));
                checkBoolean(value);
                
                generateListInstructions(t.getChild(1));
                // Is there else statement ?
                generateListInstructions(t.getChild(2));
                return;
            }
            // While
           /* case AslLexer.WHILE:
                while (true) {
                    value = generateExpression(t.getChild(0));
                    checkBoolean(value);
                    if (!value.getBooleanValue()) return null;
                    Data r = generateListInstructions(t.getChild(1));
                    if (r != null) return r;
                }*/

            // Return
            case AslLexer.RETURN:
                if (t.getChildCount() != 0) {
                    System.out.print("return ");
                    generateExpression(t.getChild(0));
                    System.out.println(";");
                }
                return; // No expression: returns void data

            // Read statement: reads a variable and raises an exception
            // in case of a format error.
            case AslLexer.READ:
                String varName = t.getChild(0).getText();
                Stack.checkVariableExists(varName);
                System.out.println("cin >> "+ varName + ";");
                return;

            // Write statement: it can write an expression or a string.
            case AslLexer.WRITE:
                AslTree v = t.getChild(0);
                // Special case for strings
                if (v.getType() == AslLexer.STRING) {
                    System.out.println("cout << " + v.getText() + ";");
                    return;
                }
                else {
                    // Write an expression
                    System.out.print("cout << ");
                    generateExpression(v);
                    System.out.println(";");
                    return;
                }

            // Function call
            case AslLexer.FUNCALL:
                generateFuncall(t);
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
     * @return The value of the expression.
     */
   
    private Data generateExpression(AslTree t) {
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
                System.out.print(t.getText());
                break;
            // An integer literal
            case AslLexer.OPENC:
                
                System.out.print(t.getChild(0).getText() + "[");
                Data vectorIndex = generateExpression(t.getChild(1));
                System.out.print("]");
                    
                checkInteger(vectorIndex);
                    
                Data vectorData = Stack.getVariable(t.getChild(0).getText());
                checkVector(vectorData);
                value = new Data(vectorData.getType()); // the returned data has the type
                                                        // of an element of the array
                break; 
            case AslLexer.INTLIT:
                value = new Data("int");
                System.out.print(t.getText());
                
                break;
            // A Boolean literal
            case AslLexer.BOOLEAN:
                value = new Data("bool");
                System.out.print(t.getText());

                break;
            // A function call. Checks that the function returns a result.
            case AslLexer.FUNCALL:
                //value = generateFunction(t.getChild(0).getText(), t.getChild(1));
                //assert value != null;
                /*if (value.isVoid()) {
                    throw new RuntimeException ("function expected to return a value");
                }*/
                    value = generateFuncall(t);
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
                    System.out.print("+");
                    break;
                case AslLexer.MINUS:
                    System.out.print("-");
                    break;
                case AslLexer.NOT:
                    System.out.print("not");
                    break;
                default: assert false; // Should never happen
            }
            setLineNumber(previous_line);
            System.out.print("(");
            value = generateExpression(t.getChild(0));
            System.out.print(")");
            
            
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
                value = generateExpression(t.getChild(0));
                System.out.print(" " + t.getText() + " ");
                value2 = generateExpression(t.getChild(1));
                if (value.getType() != value2.getType()) {
                  throw new RuntimeException ("Incompatible types in relational expression");
                }
                break;

            // Arithmetic operators
            case AslLexer.PLUS:
            case AslLexer.MINUS:
            case AslLexer.MUL:
            case AslLexer.DIV:
            case AslLexer.MOD:
             
                value = generateExpression(t.getChild(0));
                System.out.print(" " + t.getText() + " ");
                value2 = generateExpression(t.getChild(1));
                if (!value.getType().equals(value2.getType())) {
                    throw new RuntimeException ("Incompatible types in arithmetic expression");
                }
 
                break;

            // Boolean operators
            case AslLexer.AND:
            case AslLexer.OR:
                // The first operand is evaluated, but the second
                // is deferred (lazy, short-circuit evaluation).
                value = evaluateBoolean(t);
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
    private Data evaluateBoolean (AslTree t) {
        // Boolean evaluation with short-circuit

        Data leftOperandType = generateExpression(t.getChild(0));
        checkBoolean(leftOperandType);
        System.out.print(" " + t.getText() + " ");
        Data rightOperandType = generateExpression(t.getChild(1));
        checkBoolean(rightOperandType);
        System.out.println(";");
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
