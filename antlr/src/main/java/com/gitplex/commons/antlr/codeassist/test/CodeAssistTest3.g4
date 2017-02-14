grammar CodeAssistTest3;

query: criteria+ EOF;

criteria: ('title'|'author') ':' value;

value: QuotedValue|NQuotedValue;

QuotedValue: '"' (ESCAPE|~["\\])+? '"';
NQuotedValue: [a-zA-Z1-9_]+;

fragment
ESCAPE: '\\'["\\];

WS: [ \t\r\n]+ -> skip;