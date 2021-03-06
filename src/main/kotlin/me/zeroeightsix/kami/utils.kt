package me.zeroeightsix.kami

import com.mojang.blaze3d.platform.GlStateManager
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigBranch
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigLeaf
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigNode
import me.zeroeightsix.kami.mixin.extend.applyCameraTransformations
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.util.stream.Stream

val mc: MinecraftClient = MinecraftClient.getInstance()

fun <T, A> T.map(mapper: (T) -> A) = mapper(this)
fun <T> Boolean.to(ifTrue: T, ifFalse: T) = if (this) ifTrue else ifFalse
fun <T> Boolean.then(block: () -> T): T? {
    if (this) return block()
    return null
}

fun <T> Boolean.then(ifTrue: () -> T, ifFalse: () -> T) = if (this) ifTrue() else ifFalse()
fun Boolean.notThen(block: () -> Unit) = (!this).then(block)
fun Boolean.conditionalWrap(before: () -> Unit, during: () -> Unit, after: () -> Unit) {
    if (this) before()
    during()
    if (this) after()
}

fun ByteArray.backToString(): String {
    var str = ""
    for (c in this) {
        if (c == 0.toByte()) break
        str += c.toChar()
    }
    return str
}

fun <T> Iterator<T>.forEachRemainingIndexed(startAt: Int = 0, action: (Int, T) -> Unit) {
    var index = startAt
    while (hasNext()) {
        action(index, next())
        index++
    }
}

/**
 * Returns a resettable, infinitely cycling iterator over this iterable.
 * Please note this will throw an exception if the underlying collection is empty.
 */
fun <T> Iterable<T>.cyclingIterator(): CyclingIterator<T> = CyclingIterator(this)

class CyclingIterator<T>(private val iterable: Iterable<T>) : Iterator<T> {
    var internalIterator = iterable.iterator()

    override fun hasNext(): Boolean = true

    override fun next(): T {
        if (!internalIterator.hasNext()) reset()
        return internalIterator.next()
    }

    fun reset() {
        internalIterator = iterable.iterator()
    }
}

fun ConfigNode.flattenedStream(): Stream<ConfigLeaf<*>> {
    return when (this) {
        is ConfigBranch -> {
            items.stream().flatMap { it.flattenedStream() }
        }
        is ConfigLeaf<*> -> {
            Stream.of(this)
        }
        else -> Stream.empty()
    }
}

inline fun matrix(block: () -> Unit) {
    GlStateManager.pushMatrix()
    block()
    GlStateManager.popMatrix()
}

operator fun Vec3d.times(factor: Double): Vec3d = multiply(factor)

inline fun unreachable(): Nothing = TODO()

fun setNoBobbingCamera() {
    // Set up the camera
    mc.gameRenderer.applyCameraTransformations(mc.tickDelta)
    // If view bobbing was enabled, the model view matrix is now twisted and turned, so we reset it
    GlStateManager.matrixMode(GL11.GL_MODELVIEW)
    GlStateManager.loadIdentity()
    // Finish up camera rotations, now without view bobbing
    GlStateManager.rotatef(mc.gameRenderer.camera.pitch, 1.0f, 0.0f, 0.0f)
    GlStateManager.rotatef(mc.gameRenderer.camera.yaw + 180.0f, 0.0f, 1.0f, 0.0f)
}
