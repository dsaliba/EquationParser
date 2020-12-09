

import org.omg.CORBA.BAD_PARAM;

import java.util.function.*;


public class SimpleExpressionParser implements ExpressionParser {


	/*
	 * Grammar:
	 * S -> A | P
	 * A -> A+M | A-M | M
	 * M -> M*E | M/E | E
	 * E -> P^E | P
	 * P -> (S) | L | V
	 * L -> <float>
	 * V -> x
	 */
	public Expression parse (String str) throws ExpressionParseException {
		str = str.replaceAll(" ", "");
		Expression expression = parseStartExpression(str);
		if (expression == null) {
			throw new ExpressionParseException("Cannot parse expression: " + str);
		}

		return expression;
	}
	
	protected Expression parseStartExpression (String str) {
		Expression expression = null;
		 expression = parseAdditiveExpression(str);
		 if (expression == null) {
		 	expression = parseParentheticalExpression(str);
		 }
		return expression;
	}

	/**
	 * @param str	Input string to be parsed
	 * @return		A grammar compliant instance of an Additive expresion or null if none can be created
	 */
	protected Expression parseAdditiveExpression (String str) {
		if (str.length()==0) return null;
		int i = str.indexOf('+');	//This while loop syntax can be found in a few methods here, its purpose to evaluate code for all instances of a character in a string
		while (i >= 0) {
			Expression left = parseAdditiveExpression(str.substring(0, i));
			Expression right = parseMultiplicativeExpression(str.substring(i+1));
			if (left!=null && right!=null) {
				return new AdditiveOperation("+", left, right);
			}
			i = str.indexOf("+", i + 1);
		}

		i = str.indexOf('-');
		while (i >= 0) {
			Expression left = parseAdditiveExpression(str.substring(0, i));
			Expression right = parseMultiplicativeExpression(str.substring(i+1));
			if (left!=null && right!=null) {
				return new AdditiveOperation("-", left, right);
			}
			i = str.indexOf("-", i + 1);
		}

		return parseMultiplicativeExpression(str);	//If no binary addition or subtraction node can be created attempt with boosted precedence
	}

	/**
	 * @param str	Input string to be parsed
	 * @return		A grammar compliant instance of an Multiplicative expresion or null if none can be created
	 */
	protected Expression parseMultiplicativeExpression (String str) {
		if (str.length()==0) return null;
		int i = str.indexOf("*");
		while (i >= 0) {
			Expression left = parseMultiplicativeExpression(str.substring(0, i));
			Expression right = parseExponativeExpression(str.substring(i+1));
			if (left!=null && right!=null) {
				return new MultiplicativeOperation("*", left, right);
			}
			i = str.indexOf("*", i + 1);
		}

		i = str.indexOf('/');
		while (i >= 0) {
			Expression left = parseMultiplicativeExpression(str.substring(0, i));
			Expression right = parseExponativeExpression(str.substring(i+1));
			if (left!=null && right!=null) {
				return new MultiplicativeOperation("/", left, right);
			}
			i = str.indexOf("/", i + 1);
		}

		return parseExponativeExpression(str);	//If no binary multiplication or division node can be created attempt with boosted precedence
	}

	/**
	 * @param str	Input string to be parsed
	 * @return		A grammar compliant instance of an Exponative expresion or null if none can be created
	 */
	protected Expression parseExponativeExpression (String str) {
		if (str.length()==0) return null;
		int i = str.indexOf('^');
		while (i >= 0) {
			Expression left = parseParentheticalExpression(str.substring(0, i));
			Expression right = parseExponativeExpression(str.substring(i+1, str.length()));
			if (left!=null && right!=null) {
				return new ExponativeOperation(left, right);
			}
			i = str.indexOf("^", i + 1);
		}


		return parseParentheticalExpression(str);	//If no binary exponative node can be created attempt with boosted precedence
	}

	/**
	 * @param str	Input string to be parsed
	 * @return		A grammar compliant instance of an Parenthetical expresion or null if none can be created
	 */
	protected Expression parseParentheticalExpression (String str) {
		if (str.length()==0) return null;	//These null checks avoid instances such as () causing problems
		Expression expression = null;
		if (str.charAt(0)=='(' && str.charAt(str.length()-1)==')') {				//If there are parenthesis encapsulating the string form a parenthetical node
			expression = parseStartExpression(str.substring(1, str.length()-1));
		}
		if (expression != null) {
			return new ParentheticalOperation(expression);
		}
		expression = parseLiteralExpression(str);
		if (expression == null) {
			expression = parseVariableExpression(str);
		}
		return expression;
	}

	/**
	 * @param str Input string to be parsed
	 * @return a Node that will be evalutated as whatever variable value is passed or null if none can be created
	 */
	protected VariableExpression parseVariableExpression (String str) {
		if (str.equals("x")) {
			return new VariableExpression();
		}
		return null;
	}

	/**
	 * @param str 	Input string to be parsed
	 * @return		A node evaluating to a literal constant floating point number or null if none can be created
	 */
	private LiteralExpression parseLiteralExpression (String str) {
		// From https://stackoverflow.com/questions/3543729/how-to-check-that-a-string-is-parseable-to-a-double/22936891:
		final String Digits     = "(\\p{Digit}+)";
		final String HexDigits  = "(\\p{XDigit}+)";
		// an exponent is 'e' or 'E' followed by an optionally 
		// signed decimal integer.
		final String Exp        = "[eE][+-]?"+Digits;
		final String fpRegex    =
		    ("[\\x00-\\x20]*"+ // Optional leading "whitespace"
		    "[+-]?(" +         // Optional sign character
		    "NaN|" +           // "NaN" string
		    "Infinity|" +      // "Infinity" string

		    // A decimal floating-point string representing a finite positive
		    // number without a leading sign has at most five basic pieces:
		    // Digits . Digits ExponentPart FloatTypeSuffix
		    // 
		    // Since this method allows integer-only strings as input
		    // in addition to strings of floating-point literals, the
		    // two sub-patterns below are simplifications of the grammar
		    // productions from the Java Language Specification, 2nd 
		    // edition, section 3.10.2.

		    // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
		    "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

		    // . Digits ExponentPart_opt FloatTypeSuffix_opt
		    "(\\.("+Digits+")("+Exp+")?)|"+

		    // Hexadecimal strings
		    "((" +
		    // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
		    "(0[xX]" + HexDigits + "(\\.)?)|" +

		    // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
		    "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

		    ")[pP][+-]?" + Digits + "))" +
		    "[fFdD]?))" +
		    "[\\x00-\\x20]*");// Optional trailing "whitespace"

		if (str.matches(fpRegex)) {
			// TODO implement the LiteralExpression class and uncomment line below
			return new LiteralExpression(str);
		}
		return null;
	}


	//For minimum code repetition Nodes represent operations rather than grammar specific expresions, so while M can be of type A
	//that is resolved during parsing rather than in the architecture of the tree (this also apeared more consistant with the example printed output

	private class VariableExpression implements Expression {
		@Override
		public double evaluate(double x) {
			return x;
		}

		@Override
		public void convertToString(StringBuilder stringBuilder, int indentLevel) {
			Expression.indent(stringBuilder, indentLevel);
			stringBuilder.append("x" + "\n");
		}
	}

	public class LiteralExpression implements Expression{
		private float value;

		public LiteralExpression(String s) {
			this.value = Float.parseFloat(s);
		}

		@Override
		public double evaluate(double x) {
			return value;
		}

		@Override
		public void convertToString(StringBuilder stringBuilder, int indentLevel) {
			Expression.indent(stringBuilder, indentLevel);
			stringBuilder.append(this.value + "\n");
		}
	}

	private class MultiplicativeOperation extends BinaryOperator {

		public MultiplicativeOperation(String symbol, Expression left, Expression right) {
			super(symbol, left, right);
		}

		@Override
		double evaluate(double left, double right) {
			if (super.symbol.equals("*")) {
				return left*right;
			} else {	//Due to the nature of parsing we can infer that symbol must equal "/" in this case
				return left/right;
			}
		}
	}

	private class AdditiveOperation extends BinaryOperator {

		public AdditiveOperation(String symbol, Expression left, Expression right) {
			super(symbol, left, right);
		}

		@Override
		double evaluate(double left, double right) {
			if (super.symbol.equals("+")) {
				return left+right;
			} else {	//Due to the nature of parsing we can infer that symbol must equal "-" in this case
				return left-right;
			}
		}
	}

	private class ExponativeOperation extends BinaryOperator {

		public ExponativeOperation(Expression left, Expression right) {
			super("^", left, right);
		}

		@Override
		double evaluate(double left, double right) {
			return Math.pow(left, right);
		}
	}

	//This could be treated like a unary operator and grouped in with other operators such as ++ and -- but since it is the only
	//unary operator here I dont bother creating an abstract class for it
	private class ParentheticalOperation implements Expression {
		Expression child;

		public ParentheticalOperation (Expression child) {
			this.child = child;
		}

		@Override
		public double evaluate(double x) {
			return child.evaluate(x);
		}

		@Override
		public void convertToString(StringBuilder stringBuilder, int indentLevel) {
			Expression.indent(stringBuilder, indentLevel);
			stringBuilder.append("()" + "\n");
			child.convertToString(stringBuilder, indentLevel+1);
		}
	}

}
