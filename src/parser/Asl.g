grammar Asl;

options {
    output = AST;
    ASTLabelType = AslTree;
}




/* ASL IMPORTED TOKENS!!!!*/

tokens {
    PROG;
    MAIN;
    INSTR_BLOCK;
    ASSIGN;     // Assignment instruction
    PAR_ASSIGN;     // Assignment instruction
    PARAMS;     // List of parameters in the declaration of a function
    FUNCALL;    // Function call
    ARGLIST;    // List of arguments passed in a function call
    LIST_INSTR; // Block of instructions
    BOOLEAN;    // Boolean atom (for Boolean constants "true" or "false")
    PVALUE;     // Parameter by value in the list of parameters
    PREF;       // Parameter by reference in the list of parameters
    DECL;
}

@header {
package parser;
import interp.AslTree;
}

@lexer::header {
package parser;
}

instruction
			:	assign ';'!          // Assignment
			|	ite_stmt        // if-then-else
			|	funcall ';'!        // Call to a procedure (no result produced)
			|	return_stmt ';'!    // Return statement
			|	read ';'!          // Read a variable
			| 	write ';'!          // Write a string or an expression
			|	meufor
			|	parallel_instruction
			|	decl ';'!
			;

/*LANGUAGE SPECIFIC TOKENS*/


prog	:	func* main EOF -> ^(PROG func* main)
		;

main	:	instruction* -> ^(MAIN INT PARAMS ^(INSTR_BLOCK instruction*))
		;

func	:	type ID^ params block_instructions
		;

type	:	INT|BOOL
		;

params	: '(' paramlist? ')' -> ^(PARAMS paramlist?)
        ;

// Parameters are separated by commas
paramlist: param (','! param)*
        ;//preguntar si se debe implementar paso por valor o por referencia
 
param	:	type '&' id=ID -> ^(type PREF[$id,$id.text])
		|	type id=ID (OPENC CLOSEC)? -> ^(type PVALUE[$id,$id.text] OPENC?)
		;

block_instructions	: '{' instruction* '}' -> ^(INSTR_BLOCK instruction*)
		;

//parallel_instruction_block : BEGIN_PARALLEL '{'! parallel_bloc_header block_instructions'}'!  END_PARALLEL


parallel_instruction	:	BEGIN_PARALLEL^  parallel_bloc_header_first parallel_bloc_header block_instructions END_PARALLEL! |
						    NOT_SYNC^ block_instructions	|
							PARALLEL_FOR for_header reduction_clause? block_instructions 
						    -> ^(PARALLEL_FOR for_header  block_instructions reduction_clause?) |
							ID eq=PAR_EQUAL ID '$' expr '$;'-> ^(PAR_ASSIGN[$eq,":="] ID ID expr) |
							BARRIER;

reduction_clause		:	REDUCTION^ '('! (PLUS|MINUS|AND|MUL) ':'! ID ')';

parallel_bloc_header_first : (FIRST_PRIVATE_VAR^ ':'! ID (','! ID)* ';'!)? ;

//variables compartidas por defecto
parallel_bloc_header	:	(PRIVATE_VAR^ ':'! ID (','! ID)* ';'!)? ;


for_header: '('! assign  ';'! expr ';'! assign ')'!;

// Assignment
assign	:	ident eq=EQUAL expr -> ^(ASSIGN[$eq,"="] ident expr)
        ; //bien
        

decl		:	type ident -> ^(DECL type ident) ;

// if-then-else (else is optional)
ite_stmt	:	IF^ '('! expr ')'!  block_instructions  (ELSE! block_instructions)?
            ; //bloque instrucciones debe ser { instrucciones* }

// while statement
meufor	:	FOR^ for_header block_instructions 
            ;

// Return statement with an expression
return_stmt	:	RETURN^ expr?
        ;

// Read a variable
read	:	READ^ ID
        ;

// Write an expression or a string
write	:   WRITE^ (expr | STRING )
        ;

// Grammar for expressions with boolean, relational and aritmetic operators
expr    :   boolterm (OR^ boolterm)*
        ;

boolterm:   boolfact (AND^ boolfact)*
        ;

boolfact:   num_expr ((EQUALEQUAL^ | NOT_EQUAL^ | LT^ | LE^ | GT^ | GE^) num_expr)?
        ;

num_expr:   term ( (PLUS^ | MINUS^) term)*
        ;

term    :   factor ( (MUL^ | DIV^ | MOD^) factor)*
        ;

factor  :   (NOT^ | PLUS^ | MINUS^)? atom
        ;

// Atom of the expressions (variables, integer and boolean literals).
// An atom can also be a function call or another expression
// in parenthesis
atom    :  ident 
        |   INTLIT
        |   (b=TRUE | b=FALSE)  -> ^(BOOLEAN[$b,$b.text])
        |   funcall
        |   '('! expr ')'!
        ;

ident   :  ID (OPENC^ expr CLOSEC!)?
		  ;

// A function call has a lits of arguments in parenthesis (possibly empty)
funcall :   ID '(' expr_list? ')' -> ^(FUNCALL ID ^(ARGLIST expr_list?))
        ;

// A list of expressions separated by commas
expr_list:  expr (','! expr)*
        ;


// END ASL IMPORTED TOKENS


//BASIC TOKENS

//TYPE	:	INT                                |BOOL;
INT	:	'int';
BOOL	:	'bool';
OPENC	:	'[' ;
CLOSEC	:	']' ;
EQUAL	:	 '=' ;
PAR_EQUAL	:	':=';
EQUALEQUAL	: '==' ;
NOT_EQUAL: 	'!=' ;
LT	 	: '<' ;
LE	:	 '<=';
GT	:	 '>';
GE	    : '>=';
PLUS	: '+' ;
MINUS	: '-' ;
MUL	    : '*';
DIV	    : '/';
MOD	    : '%' ;
NOT	    : 'not';
AND	    : 'and' ;
OR	    : 'or' ;	
IF  	: 'if' ;
ELSE	: 'else' ;
BARRIER : 'barrier';
PARALLEL_FOR	:	'parallel_for';
PRIVATE_VAR		:	'private_var';
FIRST_PRIVATE_VAR		:	'first_private_var';
BEGIN_PARALLEL	:	'begin_parallel';
END_PARALLEL	:	'end_parallel';
NOT_SYNC			:	'not_sync';
REDUCTION		:	'reduction';
FOR	:	'for';
FUNC	: 'func' ;
RETURN	: 'return' ;
READ	: 'read' ;
WRITE	: 'write' ;
TRUE    : 'true' ;
FALSE   : 'false';
ID  	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')* ;
INTLIT	:	'0'..'9'+ ;

// C-style comments
COMMENT	: '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    	| '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    	;

// Strings (in quotes) with escape sequences        
STRING  :  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
        ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    ;

// White spaces
WS  	: ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    	;


