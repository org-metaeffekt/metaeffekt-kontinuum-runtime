/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

val inventory = loadInventory("input.inventory.file")
val scriptParams: Map<String, String> = jacksonObjectMapper().readValue(params["param.script.params"] ?: "{}")
val (hardware, everythingElse) = inventory.artifacts.partition { predicates.isHardwareOfType(it) || it[Artifact.Attribute.COMPONENT] == "Base Board 330E" }

inventory.artifacts = everythingElse.map { transformations.stripHashes(it) }
    .map { transformations.deriveArchitectureFromId(it) }
    .map { transformations.stripArchitecture(it) }
    .map { transformations.deriveComponentName(it) }
    .map { transformations.deriveVersionFromIdsUUID4Part(it) }
    .map { transformations.stripMatchingVersion(it) }
    .map { transformations.normalizeVersion(it) }
    .map { transformations.normalizeArchitecture(it) }
    .map { transformations.replaceApos(it) }
    .filter { predicates.removeVolumes(it) }
    .filter { !it["Version"].isNullOrBlank() }
    .filter { !it["Version"].equals("None") }
    .removeMsiWithMatchingVersion().map { it.apply { it.id = it.id.reduceBlanks() } }
    .mergeDuplicatesBy({ it.id }, { it.checksum }, { it.version }, { it.groupId })

val grouped = inventory.artifacts.groupBy {
    when {
        it[KEY_TYPE]?.contains("driver") == true -> "driver"
        it["Windows Source"]?.equals("Win32_InstalledStoreProgram") == true || it[KEY_TYPE]?.contains("""operating system|bios""".toRegex()) == true -> "os"
        it["Component"].contains("""Visual C\+\+|[Ww]indows|mstsc|SnippingTool""".toRegex()) -> "os"
        else -> "application"
    }
}

val type = scriptParams.getValue("part")
writeInventory(Inventory().apply { artifacts = grouped[type] }, "output.inventory.file", true)

fun Transformations.deriveComponentName(artifact: Artifact): Artifact {
    if (artifact.component.isNullOrBlank()) {
        artifact.component = artifact.id
    }
    return artifact
}

fun Transformations.normalizeVersion(artifact: Artifact): Artifact {
    artifact["Version"] = artifact["Version"]?.trimStart('V')
    return artifact
}

fun Transformations.deriveVersionFromIdsUUID4Part(artifact: Artifact): Artifact {
    val uuid4 = """([a-f0-9]{8}-?[a-f0-9]{4}-?4[a-f0-9]{3}-?[89ab][a-f0-9]{3}-?[a-f0-9]{12}\Z)""".toRegex()
    return artifact.apply {
        uuid4.find(this.id)?.groupValues?.let {
            if (it.size == 2) {
                this.set("Version", it[1])
            }
        }
    }
}

// Tempoary fix; replaces apostrophes that cause an issue during report generation
fun Transformations.replaceApos(artifact: Artifact): Artifact {
    artifact["Component"] = artifact["Component"]?.replace("’", "");
    artifact["Id"] = artifact["Id"]?.replace("’", "");

    artifact["Component"] = artifact["Component"]?.replace("}#", "_");
    artifact["Id"] = artifact["Id"]?.replace("’}#", "_");

    return artifact
}
