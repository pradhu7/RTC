grammar Formula;

startRule : expr ;
expr
   :   '(' expr ')'                              # parenExpr
   |   left=expr op=(OP_AND|OP_OR) right=expr    # infixExpr
   |   value=CONCEPT                             # atomExpr
   |   '!' expr                                  # negatedExpr
   ;
OP_AND: '&' ;
OP_OR: '|' ;
CONCEPT: (ALPHABET | NUMBER | SYMBOL)+ ;
fragment ALPHABET  : [a-zA-Z]+ ;
fragment NUMBER : [0-9]+ ;
fragment SYMBOL : [-,'/:%_\][]+ ;
WS  :   (' ' | '\t' | '\r' '\n') -> channel(HIDDEN) ;