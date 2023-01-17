/**
 * MIT License
 * <p>
 * Copyright (c) 2021 - 2023 Kyle Hicks
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package me.untouchedodin0.kotlin.mine.type

import org.bukkit.Material
import redempt.redlib.config.annotations.ConfigMappable
import redempt.redlib.config.annotations.ConfigPath

@ConfigMappable
class MineType {

    @ConfigPath
    val name: String? = null
    val file: String? = null
    val resetTime: Int = 0
    val expand: Int = 0
    val resetPercentage: Double = 0.0
    val upgradeCost: Double = 0.0
    val upgradeCurrency: String? = null
    val useOraxen: Boolean = false
    val useItemsAdder: Boolean = false
    val materials: Map<Material, Double>? = null
    val oraxen: Map<String, Double>? = null
    val itemsAdder: Map<String, Double>? = null
    val maxPlayers: Int = 0
    val maxMineSize: Int = 0
    val flags: Map<String, Boolean>? = null
    val fullFlags: Map<String, Boolean>? = null
}
