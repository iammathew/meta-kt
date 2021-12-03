import java.io.File
import kotlin.system.exitProcess


var hadError = false
var hadRuntimeError = false
val interpreter = Interpreter {
    runtimeError(it)
}

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: meta [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}

fun runPrompt() {
    while (true) {
        print("> ")
        val line = readLine() ?: break
        run(line)
        hadError = false
    }
}

fun runFile(path: String) {
    val src = File(path).readText()
    run(src)
    if (hadError) exitProcess(65)
    if (hadRuntimeError) System.exit(70)
}

fun run(source: String) {
    val scanner = Scanner(source)
    val tokens: MutableList<Token> = scanner.scanTokens()
    val parser = Parser(tokens) { token, message -> error(token, message) }
    val statements = parser.parse()
    interpreter.interpret(statements)

    // Stop if there was a syntax error.
    if (hadError) return
}



fun error(line: Int, message: String) {
    report(line, "", message)
}

fun error(token: Token, message: String?) {
    if (token.type === TokenType.EOF) {
        report(token.line, " at end", message!!)
    } else {
        report(token.line, " at '" + token.lexeme + "'", message!!)
    }
}

fun report(line: Int, where: String, message: String) {
    System.err.println("[line $line] Error$where: $message")
    hadError = true
}

fun runtimeError(error: RuntimeError) {
    System.err.println(
        "${error.message}[line ${error.token.line}]"
    )
    hadRuntimeError = true
}




