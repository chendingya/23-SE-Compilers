grammar IfStat;

@header {
package ifstat;
}

prog : stat EOF ;

stat : 'if' expr 'then' stat
     | 'if' expr 'then' stat 'else' stat
     | expr
     ;

// The following also works as expected.
//stat : 'if' expr 'then' stat ('else' stat)?
//     | expr
//     ;

expr : ID ;

ID : [a-z] ;
WS : [ \t\n\r]+ -> skip ;