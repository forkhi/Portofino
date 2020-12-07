grammar Model;

standaloneDomain: importDeclaration* domain;
domain: annotation* DOMAIN name=IDENTIFIER (';' | '{' (domain | entity | relationship)* '}')?;

standaloneEntity: importDeclaration* entity;
entity: annotation* ENTITY name=IDENTIFIER '{'
  ID '{' idProperties+=property+ '}'
  properties+=property*
  relationshipProperty*
'}';

importDeclaration: IMPORT IDENTIFIER ('.' wildcard='*')?;

property: annotation* name=IDENTIFIER (':' type)?;

relationship: annotation* name=IDENTIFIER ':' a=type '-->' b=type;

relationshipProperty: annotation* name=IDENTIFIER '-->' type multiplicity?;
multiplicity: atLeast=NUMBER ('..' (atMost=NUMBER | unbounded='*')) | any='*';

type: name=IDENTIFIER nullable='?'?;

annotation: '@' name=IDENTIFIER annotationParams?;

annotationParams: '(' (literal | (IDENTIFIER '=' literal (',' IDENTIFIER '=' literal)*)) ')';

literal: BOOLEAN | NUMBER | STRING;

BOOLEAN: 'true' | 'false';
NUMBER: ('+' | '-')?[0-9]+('.'[0-9]+);
STRING: '"' ('\\'('"'|'\\')|.)*? '"';

SCHEMA: 'schema';
IMPORT: 'import';
ID: 'id';
ENTITY: 'entity';
DOMAIN: 'domain';
DATABASE: 'database';

IDENTIFIER: IDENTIFIER_COMPONENT ('.' IDENTIFIER_COMPONENT)*;
WS: (' ' | '\t' | '\n' | '\r') -> channel(HIDDEN);

fragment IDENTIFIER_COMPONENT: [a-zA-Z_][a-zA-Z0-9_]*;
