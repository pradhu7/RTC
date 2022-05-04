package com.apixio.parser;

import java.util.Map;

public class TransformFormulaVisitor extends FormulaBaseVisitor<String> {
    private Map<String, String> transformationMap;
    private String conceptPrefix;
    public TransformFormulaVisitor(Map<String, String> conceptNameToIdMap, String conceptPrefix) {
        this.transformationMap = conceptNameToIdMap;
        this.conceptPrefix = conceptPrefix;
    }

    @Override
    public String visitStartRule(FormulaParser.StartRuleContext ctx) {
        return this.visit(ctx.expr());
    }

    @Override
    public String visitInfixExpr(FormulaParser.InfixExprContext ctx) {
        String left = visit(ctx.left);
        String right = visit(ctx.right);
        String op = ctx.op.getText();
        String leftExpr;
        String rightExpr;
        if (ctx.left instanceof FormulaParser.AtomExprContext || ctx.left instanceof FormulaParser.ParenExprContext)
            leftExpr = left;
          else
            leftExpr =  "(" + left  + ")";

        if (ctx.right instanceof FormulaParser.AtomExprContext || ctx.right instanceof FormulaParser.ParenExprContext)
            rightExpr = right;
        else
            rightExpr =  "(" + right  + ")";

        return leftExpr + " " + op + " " + rightExpr;
    }

    @Override
    public String visitAtomExpr(FormulaParser.AtomExprContext ctx) {
        return conceptPrefix + transformationMap.get(ctx.getText());
    }

    @Override
    public String visitParenExpr(FormulaParser.ParenExprContext ctx) {
        String middle = this.visit(ctx.expr());
        if (ctx.expr() instanceof FormulaParser.ParenExprContext)
            return middle;
        else
            return "(" + this.visit(ctx.expr()) + ")";
    }

    @Override
    public String visitNegatedExpr(FormulaParser.NegatedExprContext ctx) {
        String conceptStr = this.visit(ctx.expr());
        return "!" + conceptStr;
    }
}