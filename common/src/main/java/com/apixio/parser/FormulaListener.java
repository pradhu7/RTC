// Generated from /Users/zlyu/Documents/webster/apx-webster-service/src/main/java/Formula.g4 by ANTLR 4.8
package com.apixio.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link FormulaParser}.
 */
public interface FormulaListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link FormulaParser#startRule}.
	 * @param ctx the parse tree
	 */
	void enterStartRule(FormulaParser.StartRuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link FormulaParser#startRule}.
	 * @param ctx the parse tree
	 */
	void exitStartRule(FormulaParser.StartRuleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code negatedExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterNegatedExpr(FormulaParser.NegatedExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code negatedExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitNegatedExpr(FormulaParser.NegatedExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code infixExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterInfixExpr(FormulaParser.InfixExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code infixExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitInfixExpr(FormulaParser.InfixExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code atomExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterAtomExpr(FormulaParser.AtomExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code atomExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitAtomExpr(FormulaParser.AtomExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterParenExpr(FormulaParser.ParenExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenExpr}
	 * labeled alternative in {@link FormulaParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitParenExpr(FormulaParser.ParenExprContext ctx);
}