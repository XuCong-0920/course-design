package com.xucong.wordmemory.domain.dict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailySentencePickerTest {

    private fun entries(count: Int): List<DictEntry> = (0 until count).map {
        DictEntry(term = "w$it", definitions = listOf("释义 $it"))
    }

    @Test
    fun `同一天多次调用选中同一个词`() {
        val list = entries(5)
        val day = DailySentencePicker.dayIndex(1_793_000_000_000L)

        val first = DailySentencePicker.pick(list, day)
        val second = DailySentencePicker.pick(list, day)

        assertEquals(first?.term, second?.term)
    }

    @Test
    fun `隔天下标前进一位`() {
        val list = entries(5)
        val day = DailySentencePicker.dayIndex(1_793_000_000_000L)

        val today = list.indexOfFirst { it.term == DailySentencePicker.pick(list, day)?.term }
        val tomorrow = list.indexOfFirst { it.term == DailySentencePicker.pick(list, day + 1)?.term }

        assertEquals((today + 1) % list.size, tomorrow)
    }

    @Test
    fun `空列表返回 null`() {
        assertNull(DailySentencePicker.pick(emptyList(), 12_345L))
    }

    @Test
    fun `只有一个词时天天都是它`() {
        val only = entries(1)
        assertEquals("w0", DailySentencePicker.pick(only, 1L)?.term)
        assertEquals("w0", DailySentencePicker.pick(only, 987_654_321L)?.term)
    }

    @Test
    fun `负数下标也不会越界`() {
        val list = entries(3)
        val picked = DailySentencePicker.pick(list, -7L)
        assertEquals("w2", picked?.term)
    }

    @Test
    fun `dayIndex 按天数向下取整`() {
        val dayMillis = 86_400_000L
        assertEquals(0L, DailySentencePicker.dayIndex(0L, dayMillis))
        assertEquals(0L, DailySentencePicker.dayIndex(dayMillis - 1, dayMillis))
        assertEquals(1L, DailySentencePicker.dayIndex(dayMillis, dayMillis))
        assertEquals(2L, DailySentencePicker.dayIndex(2 * dayMillis + 5, dayMillis))
    }
}
