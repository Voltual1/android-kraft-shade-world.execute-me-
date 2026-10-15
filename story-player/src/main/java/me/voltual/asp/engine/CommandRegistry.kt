package me.voltual.asp.engine

typealias CommandExecutor<TResult> = suspend (command: ParsedLine.Command) -> TResult

class CommandRegistry<TResult> {
    private val executors = mutableMapOf<String, MutableList<CommandExecutor<TResult>>>()

    fun register(command: String, executor: CommandExecutor<TResult>): () -> Unit {
        val key = command.lowercase()
        val list = executors.getOrPut(key) { mutableListOf() }
        list.add(executor)
        return {
            executors[key]?.let { current ->
                current.remove(executor)
                if (current.isEmpty()) executors.remove(key)
            }
        }
    }

    fun has(command: String): Boolean = executors.containsKey(command.lowercase())

    fun get(command: String): List<CommandExecutor<TResult>>? = executors[command.lowercase()]
}