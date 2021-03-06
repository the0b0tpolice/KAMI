package me.zeroeightsix.kami.gui.windows.modules

import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.*
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.collapsingHeader
import imgui.ImGui.currentWindow
import imgui.ImGui.isItemClicked
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.text
import imgui.ImGui.treeNodeBehaviorIsOpen
import imgui.ImGui.treeNodeEx
import imgui.api.demoDebugInformations
import imgui.dsl.dragDropSource
import imgui.dsl.dragDropTarget
import imgui.dsl.menuItem
import imgui.dsl.popupContextItem
import imgui.dsl.window
import imgui.internal.ItemStatusFlag
import imgui.internal.or
import io.github.fablabsmc.fablabs.api.fiber.v1.FiberId
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigLeaf
import me.zeroeightsix.kami.feature.FeatureManager
import me.zeroeightsix.kami.feature.command.getInterface
import me.zeroeightsix.kami.feature.module.Module
import me.zeroeightsix.kami.flattenedStream
import me.zeroeightsix.kami.gui.View.modulesOpen
import me.zeroeightsix.kami.gui.windows.Settings
import me.zeroeightsix.kami.gui.windows.modules.Payloads.KAMI_MODULE_PAYLOAD
import me.zeroeightsix.kami.setting.visibilityType

object Modules {

    internal var windows = getDefaultWindows()
    private val newWindows = mutableSetOf<ModuleWindow>()
    private val baseFlags = TreeNodeFlag.SpanFullWidth or TreeNodeFlag.OpenOnDoubleClick or TreeNodeFlag.NoTreePushOnOpen

    /**
     * Returns if this module has detached
     */
    fun collapsibleModule(
        module: Module,
        source: ModuleWindow,
        sourceGroup: String
    ): ModuleWindow? {
        val nodeFlags = if (!module.enabled) baseFlags else (baseFlags or TreeNodeFlag.Selected)
        val label = "${module.name}-node"
        var moduleWindow: ModuleWindow? = null

        // We don't want imgui to handle open/closing at all, so we hack out the behaviour
        val doubleClicked = ImGui.io.mouseDoubleClicked[0]
        ImGui.io.mouseDoubleClicked[0] = false

        var clickedLeft = false
        var clickedRight = false

        fun updateClicked() {
            clickedLeft = isItemClicked(if (Settings.swapModuleListButtons) MouseButton.Left else MouseButton.Right)
            clickedRight = isItemClicked(if (Settings.swapModuleListButtons) MouseButton.Right else MouseButton.Left)
        }

        val open = treeNodeEx(label, nodeFlags, module.name)
        dragDropTarget {
            acceptDragDropPayload(KAMI_MODULE_PAYLOAD)?.let {
                val payload = it.data!! as ModulePayload
                payload.moveTo(source, sourceGroup)
            }
        }
        if (open) {
            updateClicked()
            showModuleSettings(module) {
                dragDropSource(DragDropFlag.SourceAllowNullID.i) {
                    setDragDropPayload(KAMI_MODULE_PAYLOAD, ModulePayload(mutableSetOf(module), source))
                    text("Merge")
                }

                popupContextItem("$label-popup") {
                    menuItem("Detach") {
                        moduleWindow = ModuleWindow(module.name, module = module)
                    }
                }
            }

//            treePop()
        } else updateClicked()

        // Restore state
        ImGui.io.mouseDoubleClicked[0] = doubleClicked

        if (clickedLeft) {
            module.toggle()
        } else if (clickedRight) {
            val id = currentWindow.getID(label)
            val open = treeNodeBehaviorIsOpen(id, nodeFlags)
            val window = currentWindow
            window.dc.stateStorage[id] = !open
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.ToggledOpen
        }
        
        return moduleWindow
    }

    operator fun invoke() {
        if (modulesOpen) {
            windows.removeIf(ModuleWindow::draw)
            if (windows.addAll(newWindows)) {
                newWindows.clear()
            }

            ModuleWindowsEditor()
        }
    }

    private fun getDefaultWindows() = mutableListOf(
        ModuleWindow("All modules", groups = FeatureManager.features.filterIsInstance<Module>().groupBy {
            it.category.getName()
        }.mapValuesTo(mutableMapOf(), { entry -> entry.value.toMutableList() }))
    )
    
    fun reset() {
        windows = getDefaultWindows()
    }

    class ModuleWindow(internal var title: String, val pos: Vec2? = null, var groups: Map<String, MutableList<Module>> = mapOf()) {

        constructor(title: String, pos: Vec2? = null, module: Module) : this(title, pos, mapOf(Pair("Group 1", mutableListOf(module))))

        var closed = false

        fun draw(): Boolean {
            pos?.let {
                setNextWindowPos(pos, Cond.Appearing)
            }
            
            fun iterateModules(list: MutableList<Module>, group: String): Boolean {
                return list.removeIf {
                    val moduleWindow = collapsibleModule(it, this, group)
                    moduleWindow?.let {
                        newWindows.add(moduleWindow)
                        return@removeIf true
                    }
                    return@removeIf false
                }
            }

            window("$title###${hashCode()}"){
                when {
                    groups.isEmpty() -> {
                        return true // close this window
                    }
                    groups.size == 1 -> {
                        val entry = groups.entries.stream().findAny().get()
                        val group = entry.value
                        if (group.isEmpty()) {
                            return true // close this window
                        }

                        iterateModules(group, entry.key)
                    }
                    else -> {
                        for ((group, list) in groups) {
                            if (list.isEmpty()) {
                                continue
                            }

                            if (collapsingHeader(group, TreeNodeFlag.SpanFullWidth.i)) {
                                iterateModules(list, group)
                            }
                        }
                    }
                }
            }

            return closed
        }

    }
}

private fun showModuleSettings(module: Module, block: () -> Unit) {
    // We introduce a generic <T> to allow displayImGui to be called even though we don't actually know the type of ConfigLeaf we have!
    // the flattenedStream() method returns a stream over ConfigLeaf<*>, where <*> is any type.
    // Because the interface (SettingInterface<T>) requires a ConfigLeaf<T> and we only have a ConfigLeaf<*> and SettingInterface<*> (and <*> != <*>), calling displayImGui is illegal.
    // Instead, we can just avoid this by introducing a generic method that 'proves' (by implicit casting) that our type of ConfigLeaf is the same as its interface.
    // Can't really do this inline (or I don't know how to), so I made a method to do it instead.
    fun <T> ConfigLeaf<T>.displayImGui() {
        this.getInterface().displayImGui(this)
    }

    val editMarkerShown = Settings.oldModuleEditMode
    if (!Settings.hideModuleDescriptions) {
        ImGui.pushStyleColor(Col.Text, Vec4(.7f, .7f, .7f, 1f))
        ImGui.textWrapped("%s", module.description)
        ImGui.popStyleColor()
        if (editMarkerShown)
            ImGui.sameLine()
    }
    if (editMarkerShown) {
        demoDebugInformations.helpMarker("Start dragging from this question mark to merge this module into another module window. Right click this question mark and press 'Detach' to seperate it into a new window.")
    }
    block()

    module.config.flattenedStream().filter {
        it.getAttributeValue(FiberId("kami", "setting_visibility"), visibilityType).map { vis ->
            vis.isVisible()
        }.orElse(true)
    }.forEach {
        it.displayImGui()
    }
}