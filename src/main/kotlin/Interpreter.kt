class Interpreter(private val errorCallback: (error: RuntimeError) -> Any) : Expr.Visitor<Any>, Stmt.Visitor<Unit> {
    val globals = Environment()
    private var environment = globals

    init {
        globals.define("clock", object : MetaCallable {
            override fun arity(): Int {
                return 0
            }

            override fun call(
                interpreter: Interpreter,
                arguments: List<Any>
            ): Any {
                return System.currentTimeMillis().toDouble() / 1000.0
            }

            override fun toString(): String {
                return "<native fn>"
            }
        })
    }

    fun interpret(statements: List<Stmt>) {
        try {
            statements.forEach {
                execute(it)
            }
        } catch (error: RuntimeError) {
            errorCallback(error)
        }
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expression)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        val value: Any = evaluate(stmt.initializer)

        environment.define(stmt.name.lexeme, value)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = MetaFunction(stmt)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any {
        return expr.value
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any {
        return environment.get(expr.name)
    }

    override fun visitBinaryExpr(expr: Expr.Binary): Any {
        val left = evaluate(expr.left)
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.BANG_EQUAL -> !isEqual(left, right)
            TokenType.EQUAL_EQUAL -> isEqual(left, right)
            TokenType.GREATER -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double > right as Double
            }
            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double >= right as Double
            }
            TokenType.LESS -> {
                checkNumberOperands(expr.operator, left, right)
                return (left as Double) < (right as Double)
            }
            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double <= right as Double
            }
            TokenType.MINUS -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double - right as Double
            }
            TokenType.SLASH -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double / right as Double
            }
            TokenType.STAR -> {
                checkNumberOperands(expr.operator, left, right)
                return left as Double * right as Double
            }
            TokenType.PLUS -> {
                if (left is String && right is String) return left + right
                if (left is Double && right is Double) return left + right
                throw RuntimeError(
                    expr.operator,
                    "Operands must be two numbers or two strings."
                )
            }
            else -> {
                throw RuntimeError(
                    expr.operator,
                    "Unknown operator for binary expression!"
                )
            }
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any {
        return evaluate(expr.expression)
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                return -(right as Double)
            }
            TokenType.BANG -> !isTruthy(right)
            else -> {}
        }
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any {
        val value = evaluate(expr.value)
        environment.assign(expr.name, value)
        return value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any {
        val left = evaluate(expr.left)

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left
        } else if (expr.operator.type == TokenType.AND) {
            if (!isTruthy(left)) return left
        }

        return evaluate(expr.right)
    }

    override fun visitCallExpr(expr: Expr.Call): Any {
        val callee = evaluate(expr.callee)
        val args = expr.arguments.map { it -> evaluate(it) }
        if(callee !is MetaCallable) {
            throw RuntimeError(expr.paren, "Can only call function and classes.")
        }
        if (args.size != callee.arity()) {
            throw RuntimeError(
                expr.paren, """Expected ${callee.arity()} arguments but got ${args.size}."""
            )
        }
        return callee.call(this, args)
    }

    fun evaluate(expr: Expr): Any {
        return expr.accept(this)
    }

    fun execute(stmt: Stmt) {
        stmt.accept(this)
    }

    fun executeBlock(
        statements: List<Stmt>,
        environment: Environment
    ) {
        val previous = this.environment
        try {
            this.environment = environment
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    private fun isTruthy(obj: Any): Boolean {
        if (obj is MetaNull) return false
        return if (obj is Boolean) obj else true
    }

    private fun isEqual(a: Any, b: Any): Boolean {
        if (a is MetaNull && b is MetaNull) return true
        return if (a is MetaNull) false else a == b
    }

    private fun checkNumberOperand(operator: Token, operand: Any) {
        if (operand is Double) return
        throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(
        operator: Token,
        left: Any, right: Any
    ) {
        if (left is Double && right is Double) return
        throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun stringify(obj: Any): String {
        if (obj is MetaNull) return "nil"
        if (obj is Double) {
            var text = obj.toString()
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length - 2)
            }
            return text
        }
        return obj.toString()
    }
}

class RuntimeError(val token: Token, message: String?) : RuntimeException(message)
