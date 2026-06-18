package com.mordred.showcase.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.datatable.AeroColumnWidth
import com.mordred.aero.components.datatable.AeroDataTable
import com.mordred.aero.components.datatable.AeroTableColumn
import com.mordred.aero.components.datatable.AeroTreeView
import com.mordred.aero.components.datatable.SelectionMode
import com.mordred.aero.components.list.AeroBadge
import com.mordred.aero.theme.AeroTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private data class SatSession(
    val id: Int,
    val name: String,
    val noradId: Int,
    val aosDate: LocalDate,
    val durationMin: Int,
    val elevationDeg: Int,
    val status: String   // "Active" | "Scheduled" | "Failed" | "Complete"
)

private data class TreeNode(val id: String, val name: String, val children: List<TreeNode> = emptyList())

@Composable
fun DataSection() {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography

    val sessions = remember {
        val statuses = listOf("Active", "Scheduled", "Failed", "Complete")
        (1..100).map { i ->
            SatSession(
                id = i,
                name = "SAT-${"%03d".format(i)}",
                noradId = 40000 + i,
                aosDate = LocalDate(2026, 1, 1).plus(i.toLong(), DateTimeUnit.DAY),
                durationMin = 5 + (i % 15),
                elevationDeg = 10 + (i % 80),
                status = statuses[i % 4]
            )
        }
    }

    // Active -> primary, Scheduled -> secondary, Failed -> error, Complete -> borderDefault
    val columns = remember {
        listOf(
            AeroTableColumn<SatSession>(
                header = "Name",
                width = AeroColumnWidth.Weight(2f),
                sortKey = { it.name },
                cell = { s -> Text(s.name, color = colors.onSurface) }
            ),
            AeroTableColumn<SatSession>(
                header = "NORAD ID",
                width = AeroColumnWidth.Fixed(110.dp),
                alignment = Alignment.End,
                sortKey = { it.noradId },
                cell = { s -> Text(s.noradId.toString(), color = colors.onSurface) }
            ),
            AeroTableColumn<SatSession>(
                header = "AOS Date",
                width = AeroColumnWidth.Fixed(120.dp),
                sortKey = { it.aosDate },
                cell = { s -> Text(s.aosDate.toString(), color = colors.onSurface) }
            ),
            AeroTableColumn<SatSession>(
                header = "Duration min",
                width = AeroColumnWidth.Fixed(110.dp),
                alignment = Alignment.End,
                sortKey = { it.durationMin },
                cell = { s -> Text(s.durationMin.toString(), color = colors.onSurface) }
            ),
            AeroTableColumn<SatSession>(
                header = "Elevation°",
                width = AeroColumnWidth.Fixed(100.dp),
                alignment = Alignment.End,
                sortKey = { it.elevationDeg },
                cell = { s -> Text("${s.elevationDeg}°", color = colors.onSurface) }
            ),
            AeroTableColumn<SatSession>(
                header = "Status",
                width = AeroColumnWidth.Fixed(110.dp),
                sortKey = { it.status },
                cell = { s ->
                    val (bg, fg) = when (s.status) {
                        "Active"    -> colors.primary to colors.onPrimary
                        "Scheduled" -> colors.secondary to colors.onSecondary
                        "Failed"    -> colors.error to colors.onError
                        else        -> colors.borderDefault to colors.onSurface
                    }
                    AeroBadge(text = s.status, color = bg, contentColor = fg)
                }
            )
        )
    }

    var selectedKeys by remember { mutableStateOf<Set<Any>>(emptySet()) }

    val groundStations = remember {
        listOf(
            TreeNode("gs-svalbard", "Svalbard", listOf(
                TreeNode("og-leo", "LEO group", listOf(TreeNode("s-1", "SAT-001"), TreeNode("s-2", "SAT-002"))),
                TreeNode("og-meo", "MEO group", listOf(TreeNode("s-3", "SAT-003")))
            )),
            TreeNode("gs-kiruna", "Kiruna", listOf(
                TreeNode("og-geo", "GEO group", listOf(TreeNode("s-4", "SAT-004"), TreeNode("s-5", "SAT-005")))
            ))
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Data", color = colors.onBackground, style = typography.title)

        Box(Modifier.fillMaxWidth().height(360.dp)) {
            AeroDataTable(
                data = sessions,
                columns = columns,
                key = { it.id },
                modifier = Modifier.fillMaxSize(),
                selectionMode = SelectionMode.Multi,
                selectedKeys = selectedKeys,
                onSelectionChange = { selectedKeys = it }
            )
        }

        Text("TreeView (lazy onExpand)", color = colors.labelText, style = typography.bodyMedium)

        Box(Modifier.fillMaxWidth().height(220.dp)) {
            AeroTreeView(
                rootNodes = groundStations,
                children = { it.children },
                isExpandable = { it.children.isNotEmpty() },
                key = { it.id },
                nodeContent = { Text(it.name, color = colors.onSurface) },
                modifier = Modifier.fillMaxSize(),
                onExpand = { println("onExpand fired for: ${it.name}") }
            )
        }
    }
}
