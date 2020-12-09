
public abstract class BinaryOperator implements Expression {
    protected Expression left;
    protected Expression right;
    protected String symbol;

    /**
     * @param symbol    The symbol to print for formatting (can also be used by sub classes for calculations
     * @param left      The left child of the expresion
     * @param right     The right child of the expresion
     */
    public BinaryOperator(String symbol, Expression left, Expression right) {
        this.symbol = symbol;
        this.left = left;
        this.right = right;
    }

    /**
     * @param left  Value of evaluated left node
     * @param right Value of evaluated right node
     * @return      Sublcass defined evaluation given two input values
     */
    abstract double evaluate(double left, double right);

    /**
     * @param x the given value of x to propegate down to variable leaf nodes
     * @return  The value of the abstract evaluate method, solved for the inputs of left and right
     */
    @Override
    public double evaluate(double x) {
        return this.evaluate(right.evaluate(x), left.evaluate(x));
    }

    /**
     * @param stringBuilder the StringBuilder to use for building the String representation
     * @param indentLevel   the indentation level (number of tabs from the left margin) at which to start
     */
    @Override
    public void convertToString(StringBuilder stringBuilder, int indentLevel) {
        Expression.indent(stringBuilder, indentLevel);
        stringBuilder.append(this.symbol + "\n");
        left.convertToString(stringBuilder, indentLevel+1);
        right.convertToString(stringBuilder, indentLevel+1);
    }

}
