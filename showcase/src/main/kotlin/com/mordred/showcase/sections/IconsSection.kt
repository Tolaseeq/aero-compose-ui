package com.mordred.showcase.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mordred.aero.components.input.AeroSearchField
import com.mordred.aero.components.overlay.AeroToastHostState
import com.mordred.aero.icons.AeroIcons
import com.mordred.aero.icons.`internal`.Archive
import com.mordred.aero.icons.`internal`.ArrowBendUpLeft
import com.mordred.aero.icons.`internal`.ArrowClockwise
import com.mordred.aero.icons.`internal`.ArrowCounterClockwise
import com.mordred.aero.icons.`internal`.ArrowDown
import com.mordred.aero.icons.`internal`.ArrowLeft
import com.mordred.aero.icons.`internal`.ArrowRight
import com.mordred.aero.icons.`internal`.ArrowSquareOut
import com.mordred.aero.icons.`internal`.ArrowUp
import com.mordred.aero.icons.`internal`.ArrowUpRight
import com.mordred.aero.icons.`internal`.ArrowsDownUp
import com.mordred.aero.icons.`internal`.BatteryEmpty
import com.mordred.aero.icons.`internal`.BatteryFull
import com.mordred.aero.icons.`internal`.BatteryLow
import com.mordred.aero.icons.`internal`.Bell
import com.mordred.aero.icons.`internal`.BellSlash
import com.mordred.aero.icons.`internal`.Bluetooth
import com.mordred.aero.icons.`internal`.BookmarkSimple
import com.mordred.aero.icons.`internal`.Broom
import com.mordred.aero.icons.`internal`.Bug
import com.mordred.aero.icons.`internal`.Calendar
import com.mordred.aero.icons.`internal`.CalendarBlank
import com.mordred.aero.icons.`internal`.Camera
import com.mordred.aero.icons.`internal`.CaretDown
import com.mordred.aero.icons.`internal`.CaretLeft
import com.mordred.aero.icons.`internal`.CaretRight
import com.mordred.aero.icons.`internal`.CaretUp
import com.mordred.aero.icons.`internal`.ChatCircle
import com.mordred.aero.icons.`internal`.ChatCircleText
import com.mordred.aero.icons.`internal`.Check
import com.mordred.aero.icons.`internal`.CheckCircle
import com.mordred.aero.icons.`internal`.Clipboard
import com.mordred.aero.icons.`internal`.Clock
import com.mordred.aero.icons.`internal`.Cloud
import com.mordred.aero.icons.`internal`.CloudArrowDown
import com.mordred.aero.icons.`internal`.CloudArrowUp
import com.mordred.aero.icons.`internal`.Code
import com.mordred.aero.icons.`internal`.Copy
import com.mordred.aero.icons.`internal`.Cpu
import com.mordred.aero.icons.`internal`.Database
import com.mordred.aero.icons.`internal`.DesktopTower
import com.mordred.aero.icons.`internal`.DotsThree
import com.mordred.aero.icons.`internal`.DotsThreeVertical
import com.mordred.aero.icons.`internal`.Download
import com.mordred.aero.icons.`internal`.Envelope
import com.mordred.aero.icons.`internal`.Eye
import com.mordred.aero.icons.`internal`.EyeSlash
import com.mordred.aero.icons.`internal`.FastForward
import com.mordred.aero.icons.`internal`.File
import com.mordred.aero.icons.`internal`.FileText
import com.mordred.aero.icons.`internal`.Files
import com.mordred.aero.icons.`internal`.Flag
import com.mordred.aero.icons.`internal`.FloppyDisk
import com.mordred.aero.icons.`internal`.Folder
import com.mordred.aero.icons.`internal`.FolderOpen
import com.mordred.aero.icons.`internal`.FolderPlus
import com.mordred.aero.icons.`internal`.FrameCorners
import com.mordred.aero.icons.`internal`.Funnel
import com.mordred.aero.icons.`internal`.Gear
import com.mordred.aero.icons.`internal`.GearSix
import com.mordred.aero.icons.`internal`.Globe
import com.mordred.aero.icons.`internal`.HardDrive
import com.mordred.aero.icons.`internal`.Hash
import com.mordred.aero.icons.`internal`.Heart
import com.mordred.aero.icons.`internal`.House
import com.mordred.aero.icons.`internal`.Image
import com.mordred.aero.icons.`internal`.Info
import com.mordred.aero.icons.`internal`.Key
import com.mordred.aero.icons.`internal`.Lightbulb
import com.mordred.aero.icons.`internal`.Lightning
import com.mordred.aero.icons.`internal`.Link
import com.mordred.aero.icons.`internal`.LinkSimple
import com.mordred.aero.icons.`internal`.List
import com.mordred.aero.icons.`internal`.Lock
import com.mordred.aero.icons.`internal`.LockOpen
import com.mordred.aero.icons.`internal`.MagnifyingGlass
import com.mordred.aero.icons.`internal`.MagnifyingGlassMinus
import com.mordred.aero.icons.`internal`.MagnifyingGlassPlus
import com.mordred.aero.icons.`internal`.MapPin
import com.mordred.aero.icons.`internal`.Microphone
import com.mordred.aero.icons.`internal`.MicrophoneSlash
import com.mordred.aero.icons.`internal`.Minus
import com.mordred.aero.icons.`internal`.MinusCircle
import com.mordred.aero.icons.`internal`.Monitor
import com.mordred.aero.icons.`internal`.MusicNote
import com.mordred.aero.icons.`internal`.MusicNotes
import com.mordred.aero.icons.`internal`.PaperPlane
import com.mordred.aero.icons.`internal`.Paperclip
import com.mordred.aero.icons.`internal`.Pause
import com.mordred.aero.icons.`internal`.PencilSimple
import com.mordred.aero.icons.`internal`.Phone
import com.mordred.aero.icons.`internal`.Play
import com.mordred.aero.icons.`internal`.Plus
import com.mordred.aero.icons.`internal`.PlusCircle
import com.mordred.aero.icons.`internal`.Power
import com.mordred.aero.icons.`internal`.Printer
import com.mordred.aero.icons.`internal`.Prohibit
import com.mordred.aero.icons.`internal`.Question
import com.mordred.aero.icons.`internal`.Rewind
import com.mordred.aero.icons.`internal`.Scissors
import com.mordred.aero.icons.`internal`.ShareNetwork
import com.mordred.aero.icons.`internal`.Shield
import com.mordred.aero.icons.`internal`.ShieldWarning
import com.mordred.aero.icons.`internal`.SignIn
import com.mordred.aero.icons.`internal`.SignOut
import com.mordred.aero.icons.`internal`.SkipBack
import com.mordred.aero.icons.`internal`.SkipForward
import com.mordred.aero.icons.`internal`.Sliders
import com.mordred.aero.icons.`internal`.SlidersHorizontal
import com.mordred.aero.icons.`internal`.SortAscending
import com.mordred.aero.icons.`internal`.SortDescending
import com.mordred.aero.icons.`internal`.SpeakerHigh
import com.mordred.aero.icons.`internal`.SpeakerLow
import com.mordred.aero.icons.`internal`.SpeakerX
import com.mordred.aero.icons.`internal`.Spinner
import com.mordred.aero.icons.`internal`.Square
import com.mordred.aero.icons.`internal`.Star
import com.mordred.aero.icons.`internal`.Stop
import com.mordred.aero.icons.`internal`.TerminalWindow
import com.mordred.aero.icons.`internal`.Trash
import com.mordred.aero.icons.`internal`.TrashSimple
import com.mordred.aero.icons.`internal`.Upload
import com.mordred.aero.icons.`internal`.User
import com.mordred.aero.icons.`internal`.UserCheck
import com.mordred.aero.icons.`internal`.UserCircle
import com.mordred.aero.icons.`internal`.UserMinus
import com.mordred.aero.icons.`internal`.UserPlus
import com.mordred.aero.icons.`internal`.Users
import com.mordred.aero.icons.`internal`.VideoCamera
import com.mordred.aero.icons.`internal`.Warning
import com.mordred.aero.icons.`internal`.WarningCircle
import com.mordred.aero.icons.`internal`.WarningDiamond
import com.mordred.aero.icons.`internal`.WarningOctagon
import com.mordred.aero.icons.`internal`.WifiHigh
import com.mordred.aero.icons.`internal`.WifiSlash
import com.mordred.aero.icons.`internal`.Wrench
import com.mordred.aero.icons.`internal`.X
import com.mordred.aero.icons.`internal`.XCircle
import com.mordred.aero.theme.AeroTheme
import com.mordred.aero.theme.glassSurface
import kotlinx.coroutines.launch

private data class IconEntry(val name: String, val vector: ImageVector)

// Mirror of library/src/main/kotlin/com/mordred/aero/icons/internal/*.kt — alphabetized.
// Update both when adding a new icon. (Reflection over AeroIcons::class returns empty
// because these are file-scope extension properties; see 06-RESEARCH.md "Don't Hand-Roll".)
private val ICONS: List<IconEntry> = listOf(
    IconEntry("Archive", AeroIcons.Archive),
    IconEntry("ArrowBendUpLeft", AeroIcons.ArrowBendUpLeft),
    IconEntry("ArrowClockwise", AeroIcons.ArrowClockwise),
    IconEntry("ArrowCounterClockwise", AeroIcons.ArrowCounterClockwise),
    IconEntry("ArrowDown", AeroIcons.ArrowDown),
    IconEntry("ArrowLeft", AeroIcons.ArrowLeft),
    IconEntry("ArrowRight", AeroIcons.ArrowRight),
    IconEntry("ArrowSquareOut", AeroIcons.ArrowSquareOut),
    IconEntry("ArrowUp", AeroIcons.ArrowUp),
    IconEntry("ArrowUpRight", AeroIcons.ArrowUpRight),
    IconEntry("ArrowsDownUp", AeroIcons.ArrowsDownUp),
    IconEntry("BatteryEmpty", AeroIcons.BatteryEmpty),
    IconEntry("BatteryFull", AeroIcons.BatteryFull),
    IconEntry("BatteryLow", AeroIcons.BatteryLow),
    IconEntry("Bell", AeroIcons.Bell),
    IconEntry("BellSlash", AeroIcons.BellSlash),
    IconEntry("Bluetooth", AeroIcons.Bluetooth),
    IconEntry("BookmarkSimple", AeroIcons.BookmarkSimple),
    IconEntry("Broom", AeroIcons.Broom),
    IconEntry("Bug", AeroIcons.Bug),
    IconEntry("Calendar", AeroIcons.Calendar),
    IconEntry("CalendarBlank", AeroIcons.CalendarBlank),
    IconEntry("Camera", AeroIcons.Camera),
    IconEntry("CaretDown", AeroIcons.CaretDown),
    IconEntry("CaretLeft", AeroIcons.CaretLeft),
    IconEntry("CaretRight", AeroIcons.CaretRight),
    IconEntry("CaretUp", AeroIcons.CaretUp),
    IconEntry("ChatCircle", AeroIcons.ChatCircle),
    IconEntry("ChatCircleText", AeroIcons.ChatCircleText),
    IconEntry("Check", AeroIcons.Check),
    IconEntry("CheckCircle", AeroIcons.CheckCircle),
    IconEntry("Clipboard", AeroIcons.Clipboard),
    IconEntry("Clock", AeroIcons.Clock),
    IconEntry("Cloud", AeroIcons.Cloud),
    IconEntry("CloudArrowDown", AeroIcons.CloudArrowDown),
    IconEntry("CloudArrowUp", AeroIcons.CloudArrowUp),
    IconEntry("Code", AeroIcons.Code),
    IconEntry("Copy", AeroIcons.Copy),
    IconEntry("Cpu", AeroIcons.Cpu),
    IconEntry("Database", AeroIcons.Database),
    IconEntry("DesktopTower", AeroIcons.DesktopTower),
    IconEntry("DotsThree", AeroIcons.DotsThree),
    IconEntry("DotsThreeVertical", AeroIcons.DotsThreeVertical),
    IconEntry("Download", AeroIcons.Download),
    IconEntry("Envelope", AeroIcons.Envelope),
    IconEntry("Eye", AeroIcons.Eye),
    IconEntry("EyeSlash", AeroIcons.EyeSlash),
    IconEntry("FastForward", AeroIcons.FastForward),
    IconEntry("File", AeroIcons.File),
    IconEntry("FileText", AeroIcons.FileText),
    IconEntry("Files", AeroIcons.Files),
    IconEntry("Flag", AeroIcons.Flag),
    IconEntry("FloppyDisk", AeroIcons.FloppyDisk),
    IconEntry("Folder", AeroIcons.Folder),
    IconEntry("FolderOpen", AeroIcons.FolderOpen),
    IconEntry("FolderPlus", AeroIcons.FolderPlus),
    IconEntry("FrameCorners", AeroIcons.FrameCorners),
    IconEntry("Funnel", AeroIcons.Funnel),
    IconEntry("Gear", AeroIcons.Gear),
    IconEntry("GearSix", AeroIcons.GearSix),
    IconEntry("Globe", AeroIcons.Globe),
    IconEntry("HardDrive", AeroIcons.HardDrive),
    IconEntry("Hash", AeroIcons.Hash),
    IconEntry("Heart", AeroIcons.Heart),
    IconEntry("House", AeroIcons.House),
    IconEntry("Image", AeroIcons.Image),
    IconEntry("Info", AeroIcons.Info),
    IconEntry("Key", AeroIcons.Key),
    IconEntry("Lightbulb", AeroIcons.Lightbulb),
    IconEntry("Lightning", AeroIcons.Lightning),
    IconEntry("Link", AeroIcons.Link),
    IconEntry("LinkSimple", AeroIcons.LinkSimple),
    IconEntry("List", AeroIcons.List),
    IconEntry("Lock", AeroIcons.Lock),
    IconEntry("LockOpen", AeroIcons.LockOpen),
    IconEntry("MagnifyingGlass", AeroIcons.MagnifyingGlass),
    IconEntry("MagnifyingGlassMinus", AeroIcons.MagnifyingGlassMinus),
    IconEntry("MagnifyingGlassPlus", AeroIcons.MagnifyingGlassPlus),
    IconEntry("MapPin", AeroIcons.MapPin),
    IconEntry("Microphone", AeroIcons.Microphone),
    IconEntry("MicrophoneSlash", AeroIcons.MicrophoneSlash),
    IconEntry("Minus", AeroIcons.Minus),
    IconEntry("MinusCircle", AeroIcons.MinusCircle),
    IconEntry("Monitor", AeroIcons.Monitor),
    IconEntry("MusicNote", AeroIcons.MusicNote),
    IconEntry("MusicNotes", AeroIcons.MusicNotes),
    IconEntry("PaperPlane", AeroIcons.PaperPlane),
    IconEntry("Paperclip", AeroIcons.Paperclip),
    IconEntry("Pause", AeroIcons.Pause),
    IconEntry("PencilSimple", AeroIcons.PencilSimple),
    IconEntry("Phone", AeroIcons.Phone),
    IconEntry("Play", AeroIcons.Play),
    IconEntry("Plus", AeroIcons.Plus),
    IconEntry("PlusCircle", AeroIcons.PlusCircle),
    IconEntry("Power", AeroIcons.Power),
    IconEntry("Printer", AeroIcons.Printer),
    IconEntry("Prohibit", AeroIcons.Prohibit),
    IconEntry("Question", AeroIcons.Question),
    IconEntry("Rewind", AeroIcons.Rewind),
    IconEntry("Scissors", AeroIcons.Scissors),
    IconEntry("ShareNetwork", AeroIcons.ShareNetwork),
    IconEntry("Shield", AeroIcons.Shield),
    IconEntry("ShieldWarning", AeroIcons.ShieldWarning),
    IconEntry("SignIn", AeroIcons.SignIn),
    IconEntry("SignOut", AeroIcons.SignOut),
    IconEntry("SkipBack", AeroIcons.SkipBack),
    IconEntry("SkipForward", AeroIcons.SkipForward),
    IconEntry("Sliders", AeroIcons.Sliders),
    IconEntry("SlidersHorizontal", AeroIcons.SlidersHorizontal),
    IconEntry("SortAscending", AeroIcons.SortAscending),
    IconEntry("SortDescending", AeroIcons.SortDescending),
    IconEntry("SpeakerHigh", AeroIcons.SpeakerHigh),
    IconEntry("SpeakerLow", AeroIcons.SpeakerLow),
    IconEntry("SpeakerX", AeroIcons.SpeakerX),
    IconEntry("Spinner", AeroIcons.Spinner),
    IconEntry("Square", AeroIcons.Square),
    IconEntry("Star", AeroIcons.Star),
    IconEntry("Stop", AeroIcons.Stop),
    IconEntry("TerminalWindow", AeroIcons.TerminalWindow),
    IconEntry("Trash", AeroIcons.Trash),
    IconEntry("TrashSimple", AeroIcons.TrashSimple),
    IconEntry("Upload", AeroIcons.Upload),
    IconEntry("User", AeroIcons.User),
    IconEntry("UserCheck", AeroIcons.UserCheck),
    IconEntry("UserCircle", AeroIcons.UserCircle),
    IconEntry("UserMinus", AeroIcons.UserMinus),
    IconEntry("UserPlus", AeroIcons.UserPlus),
    IconEntry("Users", AeroIcons.Users),
    IconEntry("VideoCamera", AeroIcons.VideoCamera),
    IconEntry("Warning", AeroIcons.Warning),
    IconEntry("WarningCircle", AeroIcons.WarningCircle),
    IconEntry("WarningDiamond", AeroIcons.WarningDiamond),
    IconEntry("WarningOctagon", AeroIcons.WarningOctagon),
    IconEntry("WifiHigh", AeroIcons.WifiHigh),
    IconEntry("WifiSlash", AeroIcons.WifiSlash),
    IconEntry("Wrench", AeroIcons.Wrench),
    IconEntry("X", AeroIcons.X),
    IconEntry("XCircle", AeroIcons.XCircle),
)

@Composable
fun IconsSection(toastState: AeroToastHostState) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) ICONS
        else ICONS.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Icons", color = colors.onBackground, style = typography.title)

        AeroSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search icons",
            modifier = Modifier.width(400.dp)
        )
        Text(
            text = "${filtered.size} of ${ICONS.size}",
            color = colors.labelText,
            style = typography.label
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No icons match '$query'",
                    color = colors.onBackground,
                    style = typography.bodyMedium
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(80.dp),
                modifier = Modifier.fillMaxWidth().height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.name }) { entry ->
                    IconCell(
                        entry = entry,
                        onClick = {
                            clipboard.setText(AnnotatedString("AeroIcons.${entry.name}"))
                            scope.launch { toastState.showToast("Copied AeroIcons.${entry.name}") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun IconCell(entry: IconEntry, onClick: () -> Unit) {
    val colors = AeroTheme.colors
    val typography = AeroTheme.typography
    Column(
        modifier = Modifier
            .height(88.dp)
            .glassSurface(cornerRadius = 6.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = entry.vector,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = entry.name,
            color = colors.labelText,
            style = typography.label,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
