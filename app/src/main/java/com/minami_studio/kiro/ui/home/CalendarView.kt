package com.minami_studio.kiro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minami_studio.kiro.data.model.Entry
import com.minami_studio.kiro.data.model.PlaceCategory
import com.minami_studio.kiro.data.store.EntryStore
import com.minami_studio.kiro.ui.components.resolveIcon
import com.minami_studio.kiro.ui.theme.*
import com.minami_studio.kiro.util.AppLanguage
import com.minami_studio.kiro.util.LanguageManager
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WanderCalendar(
    entries: List<Entry>,
    entryStore: EntryStore,
    language: AppLanguage,
    langManager: LanguageManager,
    modifier: Modifier = Modifier,
    onDayEntriesClick: (List<Entry>) -> Unit = {}
) {
    var displayMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()

    val entriesByDay = remember(entries, displayMonth) {
        entries
            .filter { entry ->
                try {
                    val date = LocalDate.parse(entry.visitedAt.substring(0, 10))
                    YearMonth.from(date) == displayMonth
                } catch (e: Exception) {
                    false
                }
            }
            .groupBy { entry ->
                try {
                    LocalDate.parse(entry.visitedAt.substring(0, 10)).dayOfMonth
                } catch (e: Exception) {
                    0
                }
            }
    }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragOffset = 0f },
                    onDragCancel = { dragOffset = 0f },
                    onHorizontalDrag = { _, delta ->
                        dragOffset += delta
                        if (dragOffset > 80) {
                            displayMonth = displayMonth.minusMonths(1)
                            dragOffset = 0f
                        } else if (dragOffset < -80) {
                            displayMonth = displayMonth.plusMonths(1)
                            dragOffset = 0f
                        }
                    }
                )
            }
    ) {
        // 月份标题
        MonthHeader(
            displayMonth = displayMonth,
            language = language,
            langManager = langManager,
            onPrevious = { displayMonth = displayMonth.minusMonths(1) },
            onNext = { displayMonth = displayMonth.plusMonths(1) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 星期标题行
        key(language) { WeekdayHeader(langManager = langManager) }

        Spacer(modifier = Modifier.height(4.dp))

        // 日历网格
        CalendarGrid(
            displayMonth = displayMonth,
            today = today,
            entriesByDay = entriesByDay,
            entryStore = entryStore,
            onDayClick = { dayEntries ->
                if (dayEntries.isNotEmpty()) {
                    onDayEntriesClick(dayEntries)
                }
            }
        )
    }
}

@Composable
private fun MonthHeader(
    displayMonth: YearMonth,
    language: AppLanguage,
    langManager: LanguageManager,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ChevronLeft, "Previous", tint = WanderMuted, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (language == AppLanguage.english) {
                Text(
                    text = displayMonth.year.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = WanderMuted
                )
                Text(
                    text = displayMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                    fontSize = 22.sp,
                    fontFamily = Georgia,
                    color = WanderInk
                )
            } else {
                val locale = when (language) {
                    AppLanguage.simplifiedChinese -> Locale("zh", "CN")
                    AppLanguage.traditionalChinese -> Locale("zh", "TW")
                    AppLanguage.japanese -> Locale("ja", "JP")
                    AppLanguage.korean -> Locale("ko", "KR")
                    else -> Locale.ENGLISH
                }
                Text(
                    text = langManager.s.monthYear(displayMonth.year, displayMonth.monthValue),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WanderInk
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ChevronRight, "Next", tint = WanderMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun WeekdayHeader(langManager: LanguageManager) {
    Row(modifier = Modifier.fillMaxWidth()) {
        langManager.weekdayAbbreviations.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = WanderMuted
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    displayMonth: YearMonth,
    today: LocalDate,
    entriesByDay: Map<Int, List<Entry>>,
    entryStore: EntryStore,
    onDayClick: (List<Entry>) -> Unit
) {
    val firstDayOfMonth = displayMonth.atDay(1)
    val firstWeekday = firstDayOfMonth.dayOfWeek.value % 7 // 0=Sunday
    val daysInMonth = displayMonth.lengthOfMonth()

    Column {
        var dayCounter = 1
        val rows = ((firstWeekday + daysInMonth + 6) / 7)

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < firstWeekday || dayCounter > daysInMonth) {
                        // 空白格
                        Spacer(modifier = Modifier.weight(1f).height(52.dp))
                    } else {
                        val day = dayCounter
                        val dayEntries = entriesByDay[day] ?: emptyList()
                        val isToday = today.year == displayMonth.year &&
                                today.monthValue == displayMonth.monthValue &&
                                today.dayOfMonth == day

                        CalendarDayCell(
                            day = day,
                            entries = dayEntries,
                            entryStore = entryStore,
                            isToday = isToday,
                            onClick = { onDayClick(dayEntries) },
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    entries: List<Entry>,
    entryStore: EntryStore,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasEntries = entries.isNotEmpty()

    Column(
        modifier = modifier
            .height(52.dp)
            .clickable(enabled = hasEntries, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (entries.size >= 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(
                    imageVector = resolveIcon(entryStore.categoryIcon(entries[0])) ?: entries[0].category.materialIcon,
                    contentDescription = null,
                    tint = WanderAccent,
                    modifier = Modifier.size(13.dp)
                )
                Icon(
                    imageVector = resolveIcon(entryStore.categoryIcon(entries[1])) ?: entries[1].category.materialIcon,
                    contentDescription = null,
                    tint = WanderAccent,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else if (entries.size == 1) {
            Icon(
                imageVector = resolveIcon(entryStore.categoryIcon(entries[0])) ?: entries[0].category.materialIcon,
                contentDescription = null,
                tint = WanderAccent,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }

        Text(
            text = day.toString(),
            fontSize = if (hasEntries) 10.sp else 14.sp,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isToday -> WanderAccent
                hasEntries -> WanderMuted
                else -> Color(0xFFC8C4BE)
            }
        )
    }
}

/**
 * 自定义日期选择器对话框（多语言支持，复用 MonthHeader + WeekdayHeader）
 */
@Composable
fun SimpleDatePickerDialog(
    langManager: LanguageManager,
    initialDate: LocalDate = LocalDate.now(),
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDay by remember { mutableStateOf(initialDate.dayOfMonth) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(WanderWarm)
                .padding(20.dp)
        ) {
            Column {
                // 月份标题（已支持多语言）
                MonthHeader(
                    displayMonth = displayMonth,
                    language = langManager.s.lang,
                    langManager = langManager,
                    onPrevious = { displayMonth = displayMonth.minusMonths(1) },
                    onNext = { displayMonth = displayMonth.plusMonths(1) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 星期标题（已支持多语言）
                WeekdayHeader(langManager = langManager)

                Spacer(modifier = Modifier.height(4.dp))

                // 日期网格
                val firstDayOfMonth = displayMonth.atDay(1)
                val firstWeekday = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = displayMonth.lengthOfMonth()

                Column {
                    var dayCounter = 1
                    val rows = ((firstWeekday + daysInMonth + 6) / 7)

                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                if (cellIndex < firstWeekday || dayCounter > daysInMonth) {
                                    Spacer(modifier = Modifier.weight(1f).height(40.dp))
                                } else {
                                    val day = dayCounter
                                    val isSelected = day == selectedDay &&
                                            displayMonth.year == YearMonth.from(initialDate).year &&
                                            displayMonth.monthValue == YearMonth.from(initialDate).monthValue
                                    val isToday = day == LocalDate.now().dayOfMonth &&
                                            displayMonth.year == LocalDate.now().year &&
                                            displayMonth.monthValue == LocalDate.now().monthValue

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) WanderInk
                                                else if (isToday) WanderBlush
                                                else Color.Transparent
                                            )
                                            .clickable { selectedDay = day },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) WanderCream else if (isToday) WanderAccent else WanderInk
                                        )
                                    }
                                    dayCounter++
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 确认 / 取消
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        langManager.s.cancel,
                        fontSize = 14.sp,
                        color = WanderMuted,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(WanderInk)
                            .clickable {
                                val result = try {
                                    displayMonth.atDay(selectedDay)
                                } catch (e: Exception) {
                                    LocalDate.now()
                                }
                                onConfirm(result)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(langManager.s.confirm, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = WanderCream)
                    }
                }
            }
        }
    }
}
