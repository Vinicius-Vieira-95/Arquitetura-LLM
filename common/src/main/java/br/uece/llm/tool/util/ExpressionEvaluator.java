package br.uece.llm.tool.util;

/**
 * Avaliador de expressoes aritmeticas por descida recursiva.
 * Suporta + - * / , parenteses, numeros decimais e negacao unaria.
 *
 * Existe para que a CalculatorTool tenha uma execucao real e deterministica,
 * sem depender de engine de script externa (removida do JDK moderno).
 *
 * Gramatica:
 *   expr   := term   (('+' | '-') term)*
 *   term   := factor (('*' | '/') factor)*
 *   factor := number | '(' expr ')' | ('+' | '-') factor
 */
public final class ExpressionEvaluator {

    private final String src;
    private int pos;

    private ExpressionEvaluator(String src) {
        this.src = src;
    }

    /** Avalia a expressao e devolve o resultado. Lanca IllegalArgumentException se invalida. */
    public static double evaluate(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("Expressao vazia.");
        }
        ExpressionEvaluator ev = new ExpressionEvaluator(expression);
        double result = ev.parseExpr();
        ev.skipSpaces();
        if (ev.pos < ev.src.length()) {
            throw new IllegalArgumentException(
                    "Caractere inesperado em: '" + ev.src.substring(ev.pos) + "'");
        }
        return result;
    }

    private double parseExpr() {
        double value = parseTerm();
        while (true) {
            skipSpaces();
            char c = peek();
            if (c == '+') { pos++; value += parseTerm(); }
            else if (c == '-') { pos++; value -= parseTerm(); }
            else return value;
        }
    }

    private double parseTerm() {
        double value = parseFactor();
        while (true) {
            skipSpaces();
            char c = peek();
            if (c == '*') { pos++; value *= parseFactor(); }
            else if (c == '/') {
                pos++;
                double divisor = parseFactor();
                if (divisor == 0.0) {
                    throw new IllegalArgumentException("Divisao por zero.");
                }
                value /= divisor;
            } else {
                return value;
            }
        }
    }

    private double parseFactor() {
        skipSpaces();
        char c = peek();
        if (c == '+') { pos++; return parseFactor(); }
        if (c == '-') { pos++; return -parseFactor(); }
        if (c == '(') {
            pos++;
            double value = parseExpr();
            skipSpaces();
            if (peek() != ')') {
                throw new IllegalArgumentException("Parentese ')' esperado.");
            }
            pos++;
            return value;
        }
        return parseNumber();
    }

    private double parseNumber() {
        skipSpaces();
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        if (start == pos) {
            throw new IllegalArgumentException("Numero esperado na posicao " + pos + ".");
        }
        try {
            return Double.parseDouble(src.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Numero invalido: '" + src.substring(start, pos) + "'");
        }
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private void skipSpaces() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
