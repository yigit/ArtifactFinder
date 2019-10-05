package com.birbit.artifactfinder

import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.Locale


internal enum class Action(vararg val names:String) {
    TRAVERSE_GMAVEN("gmaven"),
    DOWNLOAD_ARTIFACTS("download")
}

internal enum class Params(vararg val names: String) {
    DB("db"),
    ACTION("action")
}

internal data class CmdlineArgs(
    val action: Action,
    val db: File
)

private fun usage(args: Array<out String>, exception: Throwable?) {
    exception?.let {
        println(it.message)
    }
    val actionKey = Params.values().flatMap { it.names.toList() }.joinToString("|")
    val dbKey = Params.DB.names.joinToString("|")
    val actionOptions = Action.values().flatMap { it.names.toList() }.joinToString("|")
    println("Usage: --$dbKey <db file path> --$actionKey $actionOptions")
    println("received: ${args.joinToString(" ")}")
    System.exit(1)
}

private fun parseArgs(args: Array<out String>) : CmdlineArgs {
    var action : Action? = null
    var db : File? = null
    repeat(args.size / 2) {
        val type = args[it * 2]
        val value = args[it * 2 + 1]
        check(type.startsWith("--") && type.length > 2) {
            "type must start with --, but $type does not"
        }
        val parsedType = type.substring(2).toLowerCase(Locale.US)
        when {
            Params.ACTION.names.contains(parsedType) -> {
                check(action == null) {
                    "action is already set to $action"
                }
                action = Action.values().firstOrNull {
                    it.names.contains(value.toLowerCase(Locale.US))
                }
                checkNotNull(action) {
                    "invalid action $action"
                }
            }
            Params.DB.names.contains(parsedType) -> {
                check(db == null) {
                    "db is already set to ${db!!.canonicalPath}"
                }
                db = File(value)
            }
            else -> throw IllegalArgumentException("invalid command $parsedType")
        }
    }
    checkNotNull(action) {
        "must provide an action"
    }
    checkNotNull(db) {
        "must provide a database path"
    }
    return CmdlineArgs(
        action = action!!,
        db = db!!
    )
}

suspend fun main(vararg args: String) {
    val cmd = try {
        parseArgs(args)
    } catch (e:Throwable) {
        usage(args, e)
        null
    } ?: return
    println(cmd)
    try {
        ensureSingleton()
        when(cmd.action) {
            Action.TRAVERSE_GMAVEN -> ArtifactFinder(cmd.db).indexGMaven()
            Action.DOWNLOAD_ARTIFACTS -> ArtifactFinder(cmd.db).fetchArtifacts()
        }
    } catch (ex : Throwable) {
        System.exit(1)
    }
    System.exit(0)
}

fun ensureSingleton() {
    val userHome = System.getProperty("user.home")
    val file = File(userHome, "artifactFinder.lock")
    try {
        val fc = FileChannel.open(
            file.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )
        val lock = fc.tryLock()
        if (lock == null) {
            println("another instance is running")
            System.exit(0)
        }
    } catch (e: IOException) {
        System.exit(0)
        throw Error(e)
    }
}
