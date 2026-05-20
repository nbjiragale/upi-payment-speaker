package com.nbjiragale.upispeaker

import com.nbjiragale.upispeaker.tts.NumberToWords
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberToWordsTest {

    @Test fun zero() = assertEquals("zero", NumberToWords.englishIndian(0))
    @Test fun single() = assertEquals("seven", NumberToWords.englishIndian(7))
    @Test fun teen() = assertEquals("nineteen", NumberToWords.englishIndian(19))
    @Test fun twenty() = assertEquals("twenty", NumberToWords.englishIndian(20))
    @Test fun twentyOne() = assertEquals("twenty one", NumberToWords.englishIndian(21))
    @Test fun hundred() = assertEquals("one hundred", NumberToWords.englishIndian(100))
    @Test fun twoFifty() = assertEquals("two hundred and fifty", NumberToWords.englishIndian(250))
    @Test fun twelveFive() = assertEquals("twelve thousand five hundred", NumberToWords.englishIndian(12500))
    @Test fun lakh() = assertEquals("one lakh", NumberToWords.englishIndian(100_000))
    @Test fun fifteenLakh() = assertEquals("fifteen lakh", NumberToWords.englishIndian(1_500_000))
    @Test fun crore() = assertEquals("one crore", NumberToWords.englishIndian(10_000_000))
}
