import java.util.*
import kotlin.math.*

/**
 * Win the water fight by controlling the most territory, or out-soak your opponent!
 **/

object Commander {
    // MOVE x y | SHOOT id | THROW x y | HUNKER_DOWN | MESSAGE text
    // One line per agent: <agentId>;<action1;action2;...> actions are "MOVE x y | SHOOT id | THROW x y | HUNKER_DOWN | MESSAGE text"

    fun commandMove(p: Position) = "MOVE ${p.x} ${p.y}"

    fun commandShoot(agentId: Int) = "SHOOT $agentId"

    fun commandThrow(p: Position) = "THROW ${p.x} ${p.y}"

    fun commandHunkerDown() = "HUNKER_DOWN"

    fun commandThreaten(threat: String) = "MESSAGE $threat"

    fun commandAgent(agentId: Int, vararg commands: String): String = "$agentId;${commands.joinToString(";") { it }}"
}

object DistanceCalculator {
    // Manhatan Distance
    fun distanceBetween(p1: Position, p2: Position): Int {
        return abs(p1.x - p2.x) + abs(p1.y - p2.y)
    }

    fun distanceBetweenAgents(a1: Agent, a2: Agent): Int {
        return DistanceCalculator.distanceBetween(a1.position!!, a2.position!!)
    }
}

object TargetFinder {
    fun closestTarget(agent: Agent, enemies: Collection<Agent>): Agent {
        return enemies
            .filter { it.isAlive() }
            .sortedWith(
                compareBy
                { DistanceCalculator.distanceBetweenAgents(agent, it) },
            ).first()
    }

    fun wettestTarget(agent: Agent, enemies: Collection<Agent>): Agent {
        return enemies
            .filter { it.isAlive() }
            .sortedWith(
                compareBy
                { it.wetness },
            ).first()
    }

    fun closestUncoveredTarget(agent: Agent, enemies: Collection<Agent>, battlefieldMap: BattlefieldMap, ): Agent {
        return enemies
            .filter { it.isAlive() }
            .sortedWith(
                compareBy(
                    { DistanceCalculator.distanceBetweenAgents(agent, it) },
                    { battlefieldMap.findCoversForAgent(it, 1).count() },
                )
            ).also { System.err.println("Agents sorted: ${it.map { it.position }}") }
            .first()
    }

    fun Agent.targetCover(target: Agent, targetAvailableCovers: List<CoverForAgent>): Tile {
        val myPosition = this.position
        val targetPosition = target.position

        val effectiveCovers = this.effectiveCovers(targetAvailableCovers, target)
        // cover value, get max
        // return value

        // then later compare to see which target has lower cover value
    }

    fun Agent.effectiveCovers(covers: List<CoverForAgent>, target: Agent): List<CoverForAgent> {
        val normalisedDifference = this.position!!
            .subtract(target.position!!)
            .normalise()

        val effectiveCovers = covers.filter {
            it.tile.position == target.position!!.subtractX(normalisedDifference)
                    || it.tile.position == target.position!!.subtractY(normalisedDifference)
        }

        effectiveCovers.filter {
            it.tile.position.distanceFrom(this.position!!) > sqrt(2.0)
        }
        return effectiveCovers
    }
}

data class Position(
    val x: Int,
    val y: Int,
) {
    fun subtractX(other: Position): Position = Position(this.x - other.x, this.y)

    fun subtractY(other: Position): Position = Position(this.x, this.y - other.y)

    fun subtract(other: Position): Position = Position(this.x - other.x, this.y - other.y)

    fun distanceFrom(other: Position): Double = sqrt(
        (this.x - other.x).toDouble().pow(2)  + (this.y - other.y).toDouble().pow(2)
    )

    fun normalise(): Position = Position(
        normaliseCoordinate(this.x),
        normaliseCoordinate(this.y),
    )

    companion object {
        fun normaliseCoordinate(coordinate: Int) =
            when {
                coordinate > 1 -> 1
                coordinate < -1 -> -1
                else -> 0
            }
    }
}

enum class AgentStatus {
    ALIVE,
    DEAD,
}

data class Agent(
    val agentId: Int,
    val playerId: Int,
    var shootCooldown: Int,
    val optimalRange: Int,
    val soakingPower: Int,
    var splashBombs: Int,
    var wetness: Int = 0,
    var position: Position? = null,
    var status: AgentStatus = AgentStatus.DEAD
) {
    fun isAlive() = status == AgentStatus.ALIVE
    fun isDead() = status == AgentStatus.DEAD
}

data class Tile(
    val position: Position,
    val type: Int,
)

data class CoverForAgent(
    val agentId: Int,
    val tile: Tile,
    val distance: Int,
)

class BattlefieldMap(
    val width: Int,
    val height: Int,
    val input: Scanner,
) {
    val matrix: Array<Array<Tile>> = Array(height) {
        Array(width) {
            Tile(
                position = Position(input.nextInt(), input.nextInt()),
                type = input.nextInt(),
            )
        }
    }

    private fun Position.getTile(): Tile = matrix[y][x]

    private fun Tile.getAdjacentTiles(): List<Tile> = position.getAdjacentTiles()

    private fun Position.getAdjacentTiles(): List<Tile> =
        buildList<Tile>() {
            if (x - 1 > 0) add(Position(x - 1, y).getTile())
            if (x + 1 < width) add(Position(x + 1, y).getTile())
            if (y - 1 > 0) add(Position(x, y - 1).getTile())
            if (y + 1 < height) add(Position(x, y + 1).getTile())
        }

    fun findCoversForAgent(agent: Agent, maxDistance: Int = 3): List<CoverForAgent> {
//        System.err.println("Call findCoverForAgent, Agent Position -> ${agent.position}")
        fun isValidAdjacentTile(tile: Tile, seenTiles: Set<Tile>): Boolean {
            val result = when {
                tile in seenTiles -> false
                DistanceCalculator.distanceBetween(agent.position!!, tile.position) > maxDistance -> false
                else -> true
            }
//            System.err.println("Checking if tile is valid, tile: $tile, result=${result}")
            return result
        }

        val covers: MutableList<CoverForAgent> = mutableListOf()
        val seenTiles: MutableSet<Tile> = mutableSetOf()
        val tilesToExplore: MutableList<Tile> = mutableListOf(
            *agent.position!!
                .getAdjacentTiles()
                .filter { isValidAdjacentTile(it, seenTiles) }.toTypedArray()
        )

        while (tilesToExplore.isNotEmpty()) {
            val tile = tilesToExplore.removeFirst()
            if (tile.type != 0) {
                covers.add(
                    CoverForAgent(
                        agentId = agent.agentId,
                        tile = tile,
                        distance = DistanceCalculator.distanceBetween(agent.position!!, tile.position),
                    )
                )
            }
            seenTiles.add(tile)
            tilesToExplore.addAll(tile.getAdjacentTiles().filter { isValidAdjacentTile(it, seenTiles) })

        }
        covers.sortWith(compareBy({ it.distance }, { -it.tile.type }))
        return covers
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

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val myId = input.nextInt() // Your player id (0 or 1)
    val allAgents = getAgents(input)
    val myAgents = allAgents.filter { it.value.playerId == myId }
    val theirAgents = allAgents.filter { it.value.playerId != myId }
    val battlefieldMap = BattlefieldMap.fromInput(input)

    // game loop
    while (true) {
        val agentCount = input.nextInt() // Total number of agents still in the game

        // Game information update
        allAgents.values.forEach { it.status = AgentStatus.DEAD }  // Assume all agents are dead until proven contrary

        for (i in 0 until agentCount) {
            val agentId = input.nextInt()
            with(allAgents[agentId]!!) {
                position = Position(input.nextInt(), input.nextInt())
                shootCooldown = input.nextInt() // Number of turns before this agent can shoot
                splashBombs = input.nextInt()
                wetness = input.nextInt() // Damage (0-100) this agent has taken
                status = AgentStatus.ALIVE // Agent proved it is alive
            }
        }

        val myAgentCount = input.nextInt() // Number of alive agents controlled by you (not used val myAgentCount)
        System.err.println("My agent Count = $myAgentCount == ${myAgents.entries.count { it.value.isAlive() }}")

        // Game objectives:

        // calculate ideal target per agent
        // calculate ideal cover
        // resolve ideal cover collisions

        // Game action loop
        myAgents
            .values
            .filter { it.isAlive() }
            .forEach { agent ->
                // Write an action using println()
                // To debug: System.err.println("Debug messages...");
                val commands: MutableList<String> = mutableListOf<String>()

                val bestCover = battlefieldMap.findCoversForAgent(agent).first()
                val bestTarget = TargetFinder.closestUncoveredTarget(
                    agent = agent,
                    enemies = theirAgents.values,
                    battlefieldMap = battlefieldMap,
                )
                commands.add(Commander.commandMove(bestCover.tile.position))
                commands.add(Commander.commandShoot(bestTarget.agentId))

                println(Commander.commandAgent(agent.agentId, *commands.toTypedArray()))
            }
    }
}