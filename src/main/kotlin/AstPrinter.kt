class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String {
        return expr.accept(this)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): String {
        return parenthesize(
            expr.operator.lexeme,
            expr.left, expr.right
        )
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): String {
        return parenthesize("group", expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): String {
        return if (expr.value is MetaNull) "nil" else expr.value.toString()
    }

    override fun visitVariableExpr(expr: Expr.Variable): String {
        return parenthesize("get ${expr.name.lexeme}")
    }

    override fun visitUnaryExpr(expr: Expr.Unary): String {
        return parenthesize(expr.operator.lexeme, expr.right)
    }

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }

    override fun visitAssignExpr(expr: Expr.Assign): String {
        return parenthesize("assign ${expr.name.lexeme}", expr.value)
    }

    override fun visitLogicalExpr(expr: Expr.Logical): String {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call): String {
        TODO("Not yet implemented")
    }
}