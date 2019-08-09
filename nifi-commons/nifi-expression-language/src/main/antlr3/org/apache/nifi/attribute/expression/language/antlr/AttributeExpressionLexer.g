/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
lexer grammar AttributeExpressionLexer;

@header {
	package org.apache.nifi.attribute.expression.language.antlr;
	import org.apache.nifi.attribute.expression.language.exception.AttributeExpressionLanguageParsingException;
}

@rulecatch {
  catch(final Exception e) {
    throw new AttributeExpressionLanguageParsingException(e);
  }
}

@members {
  public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
    final StringBuilder sb = new StringBuilder();
    if ( e.token == null ) {
    	sb.append("Unrecognized token ");
    } else {
    	sb.append("Unexpected token '").append(e.token.getText()).append("' ");
    }
    sb.append("at line ").append(e.line);
    if ( e.approximateLineInfo ) {
    	sb.append(" (approximately)");
    }
    sb.append(", column ").append(e.charPositionInLine);
    sb.append(". Query: ").append(e.input.toString());

    throw new AttributeExpressionLanguageParsingException(sb.toString());
  }

  public void recover(RecognitionException e) {
  	final StringBuilder sb = new StringBuilder();
    if ( e.token == null ) {
    	sb.append("Unrecognized token ");
    } else {
    	sb.append("Unexpected token '").append(e.token.getText()).append("' ");
    }
    sb.append("at line ").append(e.line);
    if ( e.approximateLineInfo ) {
    	sb.append(" (approximately)");
    }
    sb.append(", column ").append(e.charPositionInLine);
    sb.append(". Query: ").append(e.input.toString());

    throw new AttributeExpressionLanguageParsingException(sb.toString());
  }
}


// PUNCTUATION & SPECIAL CHARACTERS
WHITESPACE : (' '|'\t'|'\n'|'\r')+ { $channel = HIDDEN; };
COMMENT : '#' ( ~('\n') )* '\n' { $channel = HIDDEN; };

DOLLAR : '$';
LPAREN	: '(';
RPAREN	: ')';
LBRACE  : '{';
RBRACE  : '}';
COLON	: ':';
COMMA	: ',';
DOT		: '.';
SEMICOLON : ';';
WHOLE_NUMBER : OP? ('0'..'9')+;

DECIMAL :    OP? ('0'..'9')+ '.' ('0'..'9')* EXP?
           | OP? '.' ('0'..'9')+ EXP?
           | OP? ('0'..'9')+ EXP;

fragment OP: ('+'|'-');
fragment EXP : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

TRUE	: 'true';
FALSE	: 'false';

//
// FUNCTION NAMES
//

// ATTRIBUTE KEY SELECTION FUNCTIONS
ANY_ATTRIBUTE : 'anyAttribute';
ANY_MATCHING_ATTRIBUTE : 'anyMatchingAttribute';
ALL_ATTRIBUTES : 'allAttributes';
ALL_MATCHING_ATTRIBUTES : 'allMatchingAttributes';
ANY_DELINEATED_VALUE : 'anyDelineatedValue';
ALL_DELINEATED_VALUES : 'allDelineatedValues';

// NO-SUBJECT FUNCTIONS
NEXT_INT	: 'nextInt';
IP	: 'ip';
UUID : 'UUID';
HOSTNAME : 'hostname';	// requires boolean arg: prefer FQDN
NOW	: 'now';


// 0 arg functions
TO_UPPER : 'toUpper';
TO_LOWER : 'toLower';
TO_STRING : 'toString';
LENGTH : 'length';
TRIM	: 'trim';
IS_NULL	: 'isNull';
IS_EMPTY : 'isEmpty';
NOT_NULL : 'notNull';
TO_NUMBER : 'toNumber';
TO_DECIMAL : 'toDecimal';
URL_ENCODE : 'urlEncode';
URL_DECODE : 'urlDecode';
NOT : 'not';
COUNT : 'count';
RANDOM : 'random';
ESCAPE_JSON : 'escapeJson';
ESCAPE_XML : 'escapeXml';
ESCAPE_CSV : 'escapeCsv';
ESCAPE_HTML3 : 'escapeHtml3';
ESCAPE_HTML4 : 'escapeHtml4';
UNESCAPE_JSON : 'unescapeJson';
UNESCAPE_XML : 'unescapeXml';
UNESCAPE_CSV : 'unescapeCsv';
UNESCAPE_HTML3 : 'unescapeHtml3';
UNESCAPE_HTML4 : 'unescapeHtml4';
BASE64_ENCODE : 'base64Encode';
BASE64_DECODE : 'base64Decode';
GET_STATE_VALUE: 'getStateValue';
TO_BIGDECIMAL : 'toBigDecimal'; //Add BigDecimal 
TO_PERCENT : 'toPercent';
BIGDECIMAL_ABS : 'abs';
BIGDECIMAL_NEGATE : 'negate';
BIGDECIMAL_FLOATVALUE : 'floatValue';
BIGDECIMAL_INTVALUE : 'intValue';
BIGDECIMAL_LONGVALUE : 'longValue';
BIGDECIMAL_SHORTVALUE : 'shortValue';

// 1 arg functions
SUBSTRING_AFTER	: 'substringAfter';
SUBSTRING_BEFORE : 'substringBefore';
SUBSTRING_AFTER_LAST : 'substringAfterLast';
SUBSTRING_BEFORE_LAST : 'substringBeforeLast';
STARTS_WITH : 'startsWith';
ENDS_WITH : 'endsWith';
CONTAINS : 'contains';
PREPEND	: 'prepend';
APPEND	: 'append';
INDEX_OF : 'indexOf';
LAST_INDEX_OF : 'lastIndexOf';
REPLACE_NULL : 'replaceNull';
REPLACE_EMPTY : 'replaceEmpty';
FIND	: 'find';	// regex
MATCHES : 'matches';	// regex
EQUALS	: 'equals';
EQUALS_IGNORE_CASE : 'equalsIgnoreCase';
GREATER_THAN	: 'gt';
LESS_THAN		: 'lt';
GREATER_THAN_OR_EQUAL	: 'ge';
LESS_THAN_OR_EQUAL		: 'le';
FORMAT			: 'format'; // takes string date format; uses SimpleDateFormat
TO_DATE			: 'toDate'; // takes string date format; converts the subject to a Long based on the date format
ADD_YEARS : 'addYears';
ADD_MONTHS : 'addMonths';
ADD_WEEKS : 'addWeeks';
ADD_DAYS : 'addDays';
ADD_HOURS : 'addHours';
ADD_MINS : 'addMinutes';
ADD_SECS : 'addSeconds';
GET_YEAR : 'getYear';
GET_MONTH : 'getMonth';
GET_DAY_OF_YEAR : 'getDayOfYear';
GET_DAY_OF_MONTH : 'getDayOfMonth';
GET_DAY_OF_WEEK : 'getDayOfWeek';
GET_DAY_OF_WEEK_IN_MONTH: 'getDayOfWeekInMonth';
GET_WEEK_OF_MONTH : 'getWeekOfMonth';
GET_WEEK_OF_YEAR : 'getWeekOfYear';
GET_HOUR : 'getHour';
GET_HOUR_OF_DAY : 'getHourOfDay';
GET_MINUTE : 'getMinute';
GET_SECOND : 'getSecond';
GET_MILLISECOND : 'getMilliSecond';
GET_DATE_FOR_WEEK : 'getDateForWeek';
GET_QUARTER : 'getQuarter'; 
AFTER : 'after';
BEFORE : 'before';
IS_WEEKEND : 'isWeekend';
MOD : 'mod';
PLUS : 'plus';
MINUS : 'minus';
MULTIPLY : 'multiply';
// DIVIDE : 'divide';
MATH : 'math';
FROM_RADIX : 'fromRadix';
TO_RADIX : 'toRadix';
OR : 'or';
AND : 'and';
JOIN : 'join';
TO_LITERAL : 'literal';
JSON_PATH : 'jsonPath';
BIGDECIMAL_COMPARETO : 'compareTo';
BIGDECIMAL_MIN : 'min';
BIGDECIMAL_MAX : 'max';
BIGDECIMAL_MOVELEFT : 'moveLeft';
BIGDECIMAL_MOVERIGHT : 'moveRight';
BIGDECIMAL_POW : 'pow';
BIGDECIMAL_POWOFTEN : 'powOfTen';


// 2 arg functions
SUBSTRING	: 'substring';
REPLACE	: 'replace';
REPLACE_FIRST	: 'replaceFirst';
REPLACE_ALL : 'replaceAll';
IF_ELSE : 'ifElse';

// 1-3 arg functions
BIGDECIMAL_SETSCALE : 'setScale';
DIVIDE : 'divide';

// 4 arg functions
GET_DELIMITED_FIELD	: 'getDelimitedField';

// unlimited arg functions
IN : 'in';

// STRINGS
STRING_LITERAL
@init{StringBuilder lBuf = new StringBuilder();}
	:
		(
			'"'
				(
					escaped=ESC {lBuf.append(getText());} |
				  	normal = ~( '"' | '\\' | '\n' | '\r' | '\t' ) { lBuf.appendCodePoint(normal);}
				)*
			'"'
		)
		{
			setText(lBuf.toString());
		}
		|
		(
			'\''
				(
					escaped=ESC {lBuf.append(getText());} |
				  	normal = ~( '\'' | '\\' | '\n' | '\r' | '\t' ) { lBuf.appendCodePoint(normal);}
				)*
			'\''
		)
		{
			setText(lBuf.toString());
		}
		;


fragment
ESC
	:	'\\'
		(
				'"'		{ setText("\""); }
			|	'\''	{ setText("\'"); }
			|	'r'		{ setText("\r"); }
			|	'n'		{ setText("\n"); }
			|	't'		{ setText("\t"); }
			|	'\\'	{ setText("\\\\"); }
			|	nextChar = ~('"' | '\'' | 'r' | 'n' | 't' | '\\')
				{
					StringBuilder lBuf = new StringBuilder(); lBuf.append("\\\\").appendCodePoint(nextChar); setText(lBuf.toString());
				}
		)
	;

ATTRIBUTE_NAME : (
				  ~('$' | '{' | '}' | '(' | ')' | '[' | ']' | ',' | ':' | ';' | '/' | '*' | '\'' | ' ' | '\t' | '\r' | '\n' | '0'..'9')
				  ~('$' | '{' | '}' | '(' | ')' | '[' | ']' | ',' | ':' | ';' | '/' | '*' | '\'' | ' ' | '\t' | '\r' | '\n')*
				 );