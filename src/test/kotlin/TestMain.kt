import kotlin.test.Test

class TestMain {

    val battlefieldMap = BattlefieldMap
    val input = sequenceOf(0,0,1,0,2,0,3,0,4,0,0,1,1,1,2,1,3,1,4,1,0,2,1,2,2,2,3,2,4,2).iterator()
    val height = 3
    val width = 5

    @Test
    fun `Check the initialisation of the matrix` () {
        val matrix: Array<Array<Tile>> = Array(height) { y ->
            Array(width) { x ->
                val p = Position(input.next(), input.next())
                System.err.println("Initialising position: $p for coordinates: [$x,$y]")
                Tile(
                    position = p,
                    type = 1,
                )
            }
        }
        (0.until(matrix.size)).forEach { y ->
            (0.until(matrix[y].size)).forEach { x ->
                println("Matrix: coordinates[$x,$y], value ${matrix[y][x]}")
            }
        }
    }

}