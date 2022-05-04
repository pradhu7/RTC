// Generated from /Users/zlyu/Documents/webster/apx-webster-service/src/main/java/Formula.g4 by ANTLR 4.8
package com.apixio.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link FormulaParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface FormulaVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link FormulaParser#startRule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartRule(FormulaParser.StartRuleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code negatedExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNegatedExpr(FormulaParser.NegatedExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code infixExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInfixExpr(FormulaParser.InfixExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code atomExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtomExpr(FormulaParser.AtomExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenExpr(FormulaParser.ParenExprContext ctx);
}