package io.github.supermonster003.autojs6.plugin.three.chime.alarm

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Whether a node matching [matcher] is shown, for the conditions of `waitUntil` right after a screen opens or closes
 * (roadmap T106): between two screens (the edit page finished, the settings screen not resumed yet) the app may have no
 * Compose hierarchy at all, and the default query then throws "No compose hierarchies found" at once instead of letting
 * the wait go on. Here no hierarchy counts as "not shown yet".
 * zh-CN: 是否显示了与 [matcher] 匹配的节点, 用于界面刚打开或关闭时 `waitUntil` 的条件 (路线图 T106): 两个界面之间 (编辑页已结束, 设置页尚未恢复) 应用可能
 * 完全没有 Compose 层级, 默认查询这时会立即抛出 "No compose hierarchies found", 而不是继续等待. 这里把没有层级视为 "尚未显示".
 */
internal fun ComposeTestRule.shows(matcher: SemanticsMatcher): Boolean =
    onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
