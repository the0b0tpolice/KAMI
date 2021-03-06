package me.zeroeightsix.kami.feature.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigBranch
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigLeaf
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigNode
import me.zeroeightsix.kami.feature.module.Module
import me.zeroeightsix.kami.util.Texts
import net.minecraft.server.command.CommandSource
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Stream

/**
 * Created by 086 on 18/11/2017.
 */
object SettingsCommand : Command() {
    private val FAILED_EXCEPTION =
        DynamicCommandExceptionType(Function { o: Any ->
            LiteralText(o.toString())
        })

    override fun register(dispatcher: CommandDispatcher<CommandSource>) {
        val moduleArgumentType = ModuleArgumentType.module()
        val settingArgumentType = SettingArgumentType.setting(moduleArgumentType, "module", 1)
        dispatcher.register(
            LiteralArgumentBuilder.literal<CommandSource>("settings")
                .then(
                    LiteralArgumentBuilder.literal<CommandSource>("list").then(
                        RequiredArgumentBuilder.argument<CommandSource, Module>(
                            "module",
                            ModuleArgumentType.module()
                        )
                            .executes { context: CommandContext<CommandSource> ->
                                val source = context.source as KamiCommandSource
                                val m =
                                    context.getArgument(
                                        "module",
                                        Module::class.java
                                    ) as Module
                                source.sendFeedback(
                                    Texts.i(
                                        Texts.append(
                                            Texts.flit(
                                                Formatting.YELLOW,
                                                m.name
                                            ),
                                            Texts.flit(
                                                Formatting.GOLD,
                                                " has the following properties:"
                                            )
                                        )
                                    )
                                )
                                m.config.list().forEach {
                                    source.sendFeedback(it)
                                }
                                0
                            }
                    )
                )
                .then(
                    LiteralArgumentBuilder.literal<CommandSource>("set")
                        .then(
                            RequiredArgumentBuilder.argument<CommandSource, Module>(
                                "module",
                                moduleArgumentType
                            )
                                .then(
                                    RequiredArgumentBuilder.argument<CommandSource, ConfigLeaf<Any>>(
                                        "setting",
                                        settingArgumentType as ArgumentType<ConfigLeaf<Any>>
                                    )
                                        .then(
                                            RequiredArgumentBuilder.argument<CommandSource, String>(
                                                "value",
                                                SettingValueArgumentType.value(settingArgumentType as ArgumentType<ConfigLeaf<*>>, "setting", 1)
                                            )
                                                .executes { context: CommandContext<CommandSource> ->
                                                    val module =
                                                        context.getArgument(
                                                            "module",
                                                            Module::class.java
                                                        ) as Module
                                                    val setting =
                                                        context.getArgument(
                                                            "setting",
                                                            ConfigLeaf::class.java
                                                        ) as ConfigLeaf<Any>
                                                    val stringValue = context.getArgument(
                                                        "value",
                                                        String::class.java
                                                    ) as String
                                                    val interf = setting.getInterface()
                                                    setting.value = interf.valueFromString(stringValue)
                                                    val (_, value) = interf.displayTypeAndValue(setting)
                                                    (context.source as KamiCommandSource).sendFeedback(
                                                        Texts.f(
                                                            Formatting.GOLD,
                                                            Texts.append(
                                                                Texts.lit("Set property "),
                                                                Texts.flit(
                                                                    Formatting.YELLOW,
                                                                    setting.name
                                                                ),
                                                                Texts.lit(" of module "),
                                                                Texts.flit(
                                                                    Formatting.YELLOW,
                                                                    module.name
                                                                ),
                                                                Texts.lit(" to "),
                                                                Texts.flit(
                                                                    Formatting.LIGHT_PURPLE,
                                                                    value
                                                                ),
                                                                Texts.lit("!")
                                                            )
                                                        )
                                                    )
                                                    0
                                                }
                                        )
                                )
                        )
                )
        )
    }
}

fun ConfigNode.list() : Stream<Text> = when (this) {
    is ConfigBranch -> {
        this.items.stream().flatMap { it.list() }
    }
    is ConfigLeaf<*> -> {
        Stream.of(this.list())
    }
    else -> {
        Stream.of(Texts.lit("unknown node"))
    }
}

fun<T> ConfigLeaf<T>.list(): Text {
    val interf = this.getInterface()
    val (type, value) = interf.displayTypeAndValue(this)
    return Texts.append(
        Texts.flit(Formatting.YELLOW, this.name),
        Texts.flit(Formatting.GRAY, " ("),
        Texts.flit(Formatting.GREEN, type),
        Texts.flit(Formatting.GRAY, ") "),
        Texts.flit(Formatting.GOLD, "= "),
        Texts.flit(Formatting.LIGHT_PURPLE, value)
    )
}