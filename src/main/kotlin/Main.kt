import java.util.*
import java.io.*
import kotlin.math.*

/**
 * Win the water fight by controlling the most territory, or out-soak your opponent!
 **/

object Commander {
    // MOVE x y | SHOOT id | THROW x y | HUNKER_DOWN | MESSAGE text
    fun commandMove(p: Position) = "MOVE ${p.x} ${p.y}"

    fun commandShoot(agentId: Int) = "SHOOT $agentId"

    fun commandThrow(p: Position) = "THROW ${p.x} ${p.y}"

    fun commandHunkerDown() = "HUNKER_DOWN"

    fun commandThreaten(threat: String) = "MESSAGE $threat"

    fun commandAgent(agentId: Int, vararg commands: String): String = "$agentId;${commands.joinToString(";") { it }}"
}

object DistanceCalculator {
    fun manhatanDistance(p1: Position, p2: Position): Int {
        return abs(p1.x - p2.x) + abs(p1.y - p2.y)
    }
}

data class Position (
    val x: Int,
    val y: Int,
)

data class Agent (
    val agentId: Int,
    val playerId: Int,
    var shootCooldown: Int,
    val optimalRange: Int,
    val soakingPower: Int,
    var splashBombs: Int,
    var wetness: Int = 0,
    var position: Position? = null,
    var aliveInRound: Int = 0
) {
    fun isAlive(currentRound: Int) = currentRound == aliveInRound
}

data class Tile (
    val position: Position,
    val type: Int,
    var occupiedByAgent: Int? = null,
)

class BattlefieldMap (
    val width: Int,
    val height: Int,
    val input: Scanner,
) {
    val matrix: Array<Array<Tile>>

    init {
        matrix = Array(height) {
            Array(width) {
                Tile(
                    position = Position(input.nextInt(), input.nextInt()),
                    type = input.nextInt(),
                )
            }
        }
    }

    companion object {
        fun fromInput(input: Scanner): BattlefieldMap =
            BattlefieldMap(
                width = input.nextInt(),
                height = input.nextInt(),
                input = input,
            )
    }
}

fun getAgents(input: Scanner): Map<Int, Agent> {
    val agentDataCount = input.nextInt() // Total number of agents in the game

    return 0.until(agentDataCount)
        .map {
            Agent(
                agentId = input.nextInt(),
                playerId = input.nextInt(),
                shootCooldown = input.nextInt(),
                optimalRange = input.nextInt(),
                soakingPower = input.nextInt(),
                splashBombs = input.nextInt(),
            )
        }
        .associateBy { it.agentId }
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val myId = input.nextInt() // Your player id (0 or 1)
    val allAgents = getAgents(input)
    val myAgents = allAgents.filter { it.value.playerId == myId }
    val theirAgents = allAgents.filter { it.value.playerId != myId }

    val battlefieldMap = BattlefieldMap.fromInput(input)
    var roundNumber = 0

    // game loop
    while (true) {
        val agentCount = input.nextInt() // Total number of agents still in the game
        roundNumber += 1

        // Game information update
        for (i in 0 until agentCount) {
            val agentId = input.nextInt()
            with (allAgents[agentId]!!) {
                position = Position(input.nextInt(), input.nextInt())
                shootCooldown = input.nextInt() // Number of turns before this agent can shoot
                splashBombs = input.nextInt()
                wetness = input.nextInt() // Damage (0-100) this agent has taken
                aliveInRound = roundNumber
            }
        }

        val myAgentCount = input.nextInt() // Number of alive agents controlled by you (not used val myAgentCount)
        System.err.println("My agent Count = $myAgentCount == ${myAgents.entries.count { it.value.isAlive(roundNumber) }}")

        // Game objectives:
        val shootTargetAgent: Agent = theirAgents.values.filter { it.isAlive(roundNumber) }.maxBy { it.wetness }
        System.err.println("How wet is target? wetness=${shootTargetAgent.wetness}")
        val positionTarget: Position = shootTargetAgent.position!!

        // Game action loop
        myAgents
            .values
            .filter { it.wetness < 100 }
            .forEach { agent ->
                // Write an action using println()
                // To debug: System.err.println("Debug messages...");
                // One line per agent: <agentId>;<action1;action2;...> actions are "MOVE x y | SHOOT id | THROW x y | HUNKER_DOWN | MESSAGE text"

                val commands: MutableList<String> = mutableListOf<String>()
                val distanceFromTarget = DistanceCalculator.manhatanDistance(agent.position!!, shootTargetAgent.position!!)

                if (distanceFromTarget > agent.optimalRange) {
                    commands.add(Commander.commandMove(positionTarget))
                }
                commands.add(Commander.commandShoot(shootTargetAgent.agentId))

                println(Commander.commandAgent(agent.agentId, *commands.toTypedArray()))
            }
    }
}