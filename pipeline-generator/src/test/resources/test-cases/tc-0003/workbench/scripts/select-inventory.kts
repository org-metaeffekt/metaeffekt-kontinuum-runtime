import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/*
 * Selects the inventory of an asset from a directory of prepared inventories:
 * finds the single artifact in the parent inventory whose id matches 'artifactIdPattern'
 * and copies '<artifact id>-inventory.xlsx' from 'inventoryPath' to the output inventory file.
 *
 * Script parameters (JSON in param.script.params):
 *   artifactIdPattern  regular expression the whole artifact id must match, e.g. "a-.*"
 *   inventoryPath      absolute path of the directory containing the inventories
 */

val scriptParams: Map<String, String> = jacksonObjectMapper().readValue(params["param.script.params"] ?: "{}")
val artifactIdPattern = Regex(scriptParams.getValue("artifactIdPattern"))
val inventoryDir = File(scriptParams.getValue("inventoryPath"))

val matchingIds = loadInventory("input.inventory.file").artifacts
    .mapNotNull { it.id }
    .filter { artifactIdPattern.matches(it) }
    .distinct()

val artifactId = matchingIds.singleOrNull()
    ?: error("Expected exactly one artifact matching [$artifactIdPattern], but found: $matchingIds")

val sourceFile = File(inventoryDir, "$artifactId-inventory.xlsx")
val outputFile = File(params.getValue("output.inventory.file"))
sourceFile.copyTo(outputFile, overwrite = true)

println("Copied inventory [$sourceFile] of artifact [$artifactId] to [$outputFile].")
