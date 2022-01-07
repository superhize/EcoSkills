package com.willfp.ecoskills.skills

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskMaterials
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.StringUtils
import com.willfp.ecoskills.getSkillLevel
import com.willfp.ecoskills.getSkillProgress
import com.willfp.ecoskills.getSkillProgressRequired
import com.willfp.ecoskills.getSkillProgressToNextLevel
import com.willfp.ecoskills.gui.SkillGUI
import org.apache.commons.lang.WordUtils
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import kotlin.math.ceil

class SkillGUI(
    plugin: EcoPlugin,
    skill: Skill
) {
    val slot: Slot = Slot.builder { player: Player ->
        ItemStackBuilder(
            Items.lookup(skill.config.getString("gui.item")).item
        ).addItemFlag(
            ItemFlag.HIDE_ATTRIBUTES
        ).setDisplayName(
            plugin.configYml.getFormattedString("gui.skill-icon.name")
                .replace("%skill%", skill.name)
                .replace(
                    "%level%",
                    player.getSkillLevel(skill).toString()
                )
                .replace(
                    "%level_numeral%",
                    NumberUtils.toNumeral(player.getSkillLevel(skill))
                )
        ).addLoreLines {
            val currentXP = player.getSkillProgress(skill)
            val requiredXP = skill.getExpForLevel(player.getSkillLevel(skill) + 1)
            val requiredXPString = if (requiredXP == Int.MAX_VALUE) plugin.langYml.getFormattedString("infinity") else NumberUtils.format(requiredXP.toDouble())
            val lore = mutableListOf<String>()
            for (string in plugin.configYml.getStrings("gui.skill-icon.lore", false)) {
                lore.add(
                    StringUtils.format(
                        string.replace("%description%", skill.description)
                            .replace("%current_xp%", NumberUtils.format(currentXP))
                            .replace(
                                "%required_xp%",
                                requiredXPString
                            )
                            .replace(
                                "%percentage_progress%",
                                NumberUtils.format((currentXP / requiredXP) * 100) + "%"
                            ),
                        player
                    )
                )
            }
            val skillSpecificIndex = lore.indexOf("%skill_specific%")
            if (skillSpecificIndex != -1) {
                lore.removeAt(skillSpecificIndex)
                lore.addAll(skillSpecificIndex, skill.getGUILore(player))
            }

            val wrappedLore = mutableListOf<String>()

            for (line in lore) {
                val indent = " ".repeat(line.length - line.trim().length)
                val wrapped = WordUtils.wrap(
                    line.trim(),
                    plugin.configYml.getInt("gui.line-wrap"),
                    "\n${plugin.langYml.getFormattedString("line-wrap-color")}", false
                ).split("\n").toMutableList()

                wrapped.replaceAll { "$indent$it" }

                wrappedLore.addAll(wrapped)
            }

            wrappedLore
        }.build()
    }.onLeftClick { event, _, _ ->
        levels.open(event.whoClicked as Player)
    }.build()

    val levels: Menu

    init {
        val maskPattern = plugin.configYml.getStrings("level-gui.mask.pattern", false).toTypedArray()
        val maskMaterials: Array<Material> = plugin.configYml
            .getStrings("level-gui.mask.materials", false)
            .stream()
            .map { string: String -> Material.getMaterial(string.uppercase()) }
            .filter(Objects::nonNull)
            .toArray { length -> arrayOfNulls(length) }

        val progressionOrder = "123456789abcdefghijklmnopqrstuvwxyz"
        val progressionPattern = plugin.configYml.getStrings("level-gui.progression-slots.pattern", false)

        val progressionSlots = mutableMapOf<Int, Pair<Int, Int>>()

        var x = 0
        for (row in progressionPattern) {
            x++
            var y = 0
            for (char in row) {
                y++
                if (char == '0') {
                    continue
                }

                val pos = progressionOrder.indexOf(char)

                if (pos == -1) {
                    continue
                }

                progressionSlots[pos + 1] = Pair(x, y)
            }
        }

        val pages = ceil(skill.maxLevel / progressionSlots.size.toDouble()).toInt()
        val levelsPerPage = progressionSlots.size

        val closeMaterial = Items.lookup(plugin.configYml.getString("level-gui.progression-slots.close.material", false)).item
        val homeMaterial = Items.lookup(plugin.configYml.getString("level-gui.progression-slots.prev-page.material", false)).item
        val nextMaterial = Items.lookup(plugin.configYml.getString("level-gui.progression-slots.next-page.material", false)).item

        val pageKey = plugin.namespacedKeyFactory.create("page")

        levels = Menu.builder(plugin.configYml.getInt("level-gui.rows"))
            .setTitle(skill.name)
            .setMask(
                FillerMask(
                    MaskMaterials(
                        *maskMaterials
                    ),
                    *maskPattern
                )
            )
            .modfiy { builder ->
                for ((level, value) in progressionSlots) {
                    builder.setSlot(
                        value.first,
                        value.second,
                        Slot.builder(ItemStack(Material.BLACK_STAINED_GLASS_PANE))
                            .setModifier { player, menu, item ->
                                var page = menu.readData(player, pageKey, PersistentDataType.INTEGER)
                                if (page == null) {
                                    menu.writeData(player, pageKey, PersistentDataType.INTEGER, 1)
                                    page = 1
                                }

                                val slotLevel = ((page - 1) * levelsPerPage) + level

                                val meta = item.itemMeta!!
                                meta.lore = emptyList()
                                val lore = mutableListOf<String>()

                                item.amount = 1

                                if (slotLevel > skill.maxLevel) {
                                    item.type = maskMaterials[0]
                                    meta.setDisplayName("")
                                    item.itemMeta = meta
                                } else {
                                    if (plugin.configYml.getBool("level-gui.progression-slots.level-as-amount")) {
                                        item.amount = slotLevel
                                    }

                                    when {
                                        slotLevel <= player.getSkillLevel(skill) -> {
                                            item.type = Material.getMaterial(
                                                plugin.configYml.getString("level-gui.progression-slots.unlocked.material")
                                                    .uppercase()
                                            )!!
                                            meta.setDisplayName(
                                                plugin.configYml.getString("level-gui.progression-slots.unlocked.name")
                                                    .replace("%skill%", skill.name)
                                                    .replace("%level%", slotLevel.toString())
                                                    .replace("%level_numeral%", NumberUtils.toNumeral(slotLevel))
                                            )

                                            lore.addAll(plugin.configYml.getStrings("level-gui.progression-slots.unlocked.lore"))
                                        }
                                        slotLevel == player.getSkillLevel(skill) + 1 -> {
                                            item.type = Material.getMaterial(
                                                plugin.configYml.getString("level-gui.progression-slots.in-progress.material")
                                                    .uppercase()
                                            )!!
                                            meta.setDisplayName(
                                                plugin.configYml.getString("level-gui.progression-slots.in-progress.name")
                                                    .replace("%skill%", skill.name)
                                                    .replace("%level%", slotLevel.toString())
                                                    .replace("%level_numeral%", NumberUtils.toNumeral(slotLevel))
                                            )

                                            lore.addAll(plugin.configYml.getStrings("level-gui.progression-slots.in-progress.lore"))
                                        }
                                        else -> {
                                            item.type = Material.getMaterial(
                                                plugin.configYml.getString("level-gui.progression-slots.locked.material")
                                                    .uppercase()
                                            )!!
                                            meta.setDisplayName(
                                                plugin.configYml.getString("level-gui.progression-slots.locked.name")
                                                    .replace("%skill%", skill.name)
                                                    .replace("%level%", slotLevel.toString())
                                                    .replace("%level_numeral%", NumberUtils.toNumeral(slotLevel))
                                            )

                                            lore.addAll(plugin.configYml.getStrings("level-gui.progression-slots.locked.lore"))
                                        }
                                    }

                                    val currentXP = player.getSkillProgress(skill)
                                    val requiredXP = player.getSkillProgressRequired(skill)
                                    lore.replaceAll { string ->
                                        string.replace("%current_xp%", NumberUtils.format(currentXP))
                                            .replace(
                                                "%required_xp%",
                                                NumberUtils.format(requiredXP.toDouble())
                                            )
                                            .replace(
                                                "%percentage_progress%",
                                                NumberUtils.format(player.getSkillProgressToNextLevel(skill) * 100) + "%"
                                            )
                                    }

                                    val skillSpecificIndex = lore.indexOf("%rewards%")
                                    if (skillSpecificIndex != -1) {
                                        lore.removeAt(skillSpecificIndex)
                                        skill.getGUIRewardsMessages(player, slotLevel) // scary
                                        lore.addAll(skillSpecificIndex, skill.getGUIRewardsMessages(player, slotLevel))
                                    }

                                    val wrappedLore = mutableListOf<String>()

                                    for (line in lore) {
                                        val indent = " ".repeat(line.length - line.trim().length)
                                        val wrapped = WordUtils.wrap(
                                            line.trim(),
                                            plugin.configYml.getInt("gui.line-wrap"),
                                            "\n${plugin.langYml.getString("line-wrap-color")}", false
                                        ).split("\n").toMutableList()

                                        wrapped.replaceAll { "$indent$it" }

                                        wrappedLore.addAll(wrapped)
                                    }

                                    meta.lore = wrappedLore
                                    item.itemMeta = meta
                                }
                            }
                            .build()
                    )
                }
            }
            .setSlot(
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.prev-page.location.column"),
                Slot.builder(
                    ItemStackBuilder(homeMaterial)
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.prev-page.name"))
                        .build()
                ).onLeftClick { event, _, menu ->
                    val player = event.whoClicked as Player
                    var page = menu.readData(player, pageKey, PersistentDataType.INTEGER) ?: 1
                    page--
                    menu.writeData(player, pageKey, PersistentDataType.INTEGER, page)
                    if (page == 0) {
                        SkillGUI.homeMenu.open(event.whoClicked as Player)
                    }
                }.build()
            )
            .setSlot(
                plugin.configYml.getInt("level-gui.progression-slots.next-page.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.next-page.location.column"),
                Slot.builder(
                    ItemStackBuilder(nextMaterial)
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.next-page.name"))
                        .build()
                ).onLeftClick { event, _, menu ->
                    val player = event.whoClicked as Player
                    var page = menu.readData(player, pageKey, PersistentDataType.INTEGER) ?: 1
                    if (page < pages) {
                        page++
                    }
                    menu.writeData(player, pageKey, PersistentDataType.INTEGER, page)

                }.build()
            )
            .setSlot(
                plugin.configYml.getInt("level-gui.progression-slots.close.location.row"),
                plugin.configYml.getInt("level-gui.progression-slots.close.location.column"),
                Slot.builder(
                    ItemStackBuilder(closeMaterial)
                        .setDisplayName(plugin.configYml.getString("level-gui.progression-slots.close.name"))
                        .build()
                ).onLeftClick { event, _ ->
                    event.whoClicked.closeInventory()
                }.build()
            )
            .build()
    }

}