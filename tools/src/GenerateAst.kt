import java.io.PrintWriter
import java.util.*
import kotlin.system.exitProcess

val expressions = arrayOf(
    "Assign   : Token name, Expr value",
    "Binary   : Expr left, Token operator, Expr right",
    "Call     : Expr callee, Token paren, MutableList<Expr> arguments",
    "Grouping : Expr expression",
    "Literal  : Any value",
    "Logical  : Expr left, Token operator, Expr right",
    "Unary    : Token operator, Expr right",
    "Variable : Token name"
)

val statements = arrayOf(
    "Block      : MutableList<Stmt> statements",
    "Expression : Expr expression",
    "If         : Expr condition, Stmt thenBranch, Stmt? elseBranch",
    "Function   : Token name, MutableList<Token> params, MutableList<Stmt> body",
    "Print      : Expr expression",
    "Return     : Token keyword, Expr value",
    "Var        : Token name, Expr initializer",
    "While      : Expr condition, Stmt body"
)

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Usage: generate_ast <output directory>")
        exitProcess(64)
    }
    val outputDir = args[0]
    defineAst(outputDir, "Expr", expressions)
    defineAst(outputDir, "Stmt", statements)
}

private fun defineAst(
    outputDir: String, baseName: String, types: Array<String>
) {
    val path = "$outputDir/$baseName.kt"
    val writer = PrintWriter(path, "UTF-8")
    writer.println("// THIS FILE IS AUTOGENERATED DON'T MODIFY MANUALLY!")
    writer.println("abstract class $baseName {")
    defineVisitor(writer, baseName, types)
    types.forEach {
        val className = it.split(":")[0].trim()
        val fields = it.split(":")[1].trim()
        defineType(writer, baseName, className, fields)
    }
    writer.println()
    writer.println("    abstract fun <R> accept(visitor: Visitor<R>): R")
    writer.println("}")
    writer.close()
}

private fun defineType(
    writer: PrintWriter, baseName: String,
    className: String, fieldList: String
) {


    // Store parameters in fields.
    val fields = fieldList.split(", ").toTypedArray()

    val constructer_parameters = fields.map {
        val (type, name) = it.split(" ")
        "val $name: $type"
    }

    writer.println(
        """    data class $className(${constructer_parameters.joinToString(", ")}) : $baseName() {"""
    )

    writer.println("        override fun <R> accept(visitor: Visitor<R>): R {")
    writer.println("            return visitor.visit$className$baseName(this)")
    writer.println("        }")
    writer.println("    }")
}

private fun defineVisitor(
    writer: PrintWriter, baseName: String, types: Array<String>
) {
    writer.println("    interface Visitor<R> {")
    for (type: String in types) {
        val typeName = type.split(":")[0].trim { it <= ' ' }
        writer.println(
            """       fun visit$typeName$baseName(${baseName.lowercase(Locale.getDefault())}: $typeName): R"""
        )
    }
    writer.println("    }")
}


