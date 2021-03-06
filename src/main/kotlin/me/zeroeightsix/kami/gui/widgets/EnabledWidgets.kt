package me.zeroeightsix.kami.gui.widgets

import imgui.ImGui.separator
import imgui.dsl.checkbox
import imgui.dsl.menu
import imgui.dsl.menuItem

object EnabledWidgets {

    var hideAll = false

    private var informationVisible = true
    private var coordinatesVisible = true
    private var activeModulesVisible = true

    internal val widgets = mapOf(
        Information to ::informationVisible,
        Coordinates to ::coordinatesVisible,
        ActiveModules to ::activeModulesVisible
    )

    operator fun invoke() = menu("Overlay") {
        checkbox("Hide all", EnabledWidgets::hideAll) {}
        separator()
        enabledButtons()
        separator()
        menuItem("Pin all") {
            widgets.keys.forEach {
                it.pinned = true
            }
        }
        menuItem("Unpin all") {
            widgets.keys.forEach {
                it.pinned = false
            }
        }
    }

    fun enabledButtons() {
        for ((widget, open) in widgets) {
            menuItem(widget.name, "", open.get(), !hideAll) {
                open.set(!open.get())
            }
        }
    }

}