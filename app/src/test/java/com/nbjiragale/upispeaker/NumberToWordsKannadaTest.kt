package com.nbjiragale.upispeaker

import com.nbjiragale.upispeaker.tts.NumberToWordsKannada
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberToWordsKannadaTest {

    @Test fun zero() = assertEquals("ಸೊನ್ನೆ", NumberToWordsKannada.kannadaIndian(0))
    @Test fun one() = assertEquals("ಒಂದು", NumberToWordsKannada.kannadaIndian(1))
    @Test fun two() = assertEquals("ಎರಡು", NumberToWordsKannada.kannadaIndian(2))
    @Test fun five() = assertEquals("ಐದು", NumberToWordsKannada.kannadaIndian(5))
    @Test fun nine() = assertEquals("ಒಂಬತ್ತು", NumberToWordsKannada.kannadaIndian(9))
    @Test fun ten() = assertEquals("ಹತ್ತು", NumberToWordsKannada.kannadaIndian(10))
    @Test fun eleven() = assertEquals("ಹನ್ನೊಂದು", NumberToWordsKannada.kannadaIndian(11))
    @Test fun twelve() = assertEquals("ಹನ್ನೆರಡು", NumberToWordsKannada.kannadaIndian(12))
    @Test fun thirteen() = assertEquals("ಹದಿಮೂರು", NumberToWordsKannada.kannadaIndian(13))
    @Test fun fourteen() = assertEquals("ಹದಿನಾಲ್ಕು", NumberToWordsKannada.kannadaIndian(14))
    @Test fun fifteen() = assertEquals("ಹದಿನೈದು", NumberToWordsKannada.kannadaIndian(15))
    @Test fun nineteen() = assertEquals("ಹತ್ತೊಂಬತ್ತು", NumberToWordsKannada.kannadaIndian(19))
    @Test fun twenty() = assertEquals("ಇಪ್ಪತ್ತು", NumberToWordsKannada.kannadaIndian(20))
    @Test fun twentyOne() = assertEquals("ಇಪ್ಪತ್ತು ಒಂದು", NumberToWordsKannada.kannadaIndian(21))
    @Test fun fifty() = assertEquals("ಐವತ್ತು", NumberToWordsKannada.kannadaIndian(50))
    @Test fun hundred() = assertEquals("ಒಂದುನೂರ", NumberToWordsKannada.kannadaIndian(100))

    @Test fun twoFifty() {
        assertEquals("ಎರಡುನೂರ ಐವತ್ತು", NumberToWordsKannada.kannadaIndian(250))
    }

    @Test fun fiveHundred() {
        assertEquals("ಐದುನೂರ", NumberToWordsKannada.kannadaIndian(500))
    }

    @Test fun nineNineNine() {
        assertEquals("ಒಂಬತ್ತುನೂರ ತೊಂಬತ್ತು ಒಂಬತ್ತು", NumberToWordsKannada.kannadaIndian(999))
    }

    @Test fun thousand() {
        assertEquals("ಒಂದು ಸಾವಿರ", NumberToWordsKannada.kannadaIndian(1000))
    }

    @Test fun twelveFiveHundred() {
        assertEquals("ಹನ್ನೆರಡು ಸಾವಿರ ಐದುನೂರ", NumberToWordsKannada.kannadaIndian(12500))
    }

    @Test fun lakh() {
        assertEquals("ಒಂದು ಲಕ್ಷ", NumberToWordsKannada.kannadaIndian(100_000))
    }

    @Test fun fifteenLakh() {
        assertEquals("ಹದಿನೈದು ಲಕ್ಷ", NumberToWordsKannada.kannadaIndian(1_500_000))
    }

    @Test fun crore() {
        assertEquals("ಒಂದು ಕೋಟಿ", NumberToWordsKannada.kannadaIndian(10_000_000))
    }

    @Test fun negative() {
        assertEquals("ಮೈನಸ್ ಐದು", NumberToWordsKannada.kannadaIndian(-5))
    }

    @Test fun complexNumber() {
        // 12,34,567 = 12 lakh 34 thousand 5 hundred and 67
        assertEquals(
            "ಹನ್ನೆರಡು ಲಕ್ಷ ಮೂವತ್ತು ನಾಲ್ಕು ಸಾವಿರ ಐದುನೂರ ಅರವತ್ತು ಏಳು",
            NumberToWordsKannada.kannadaIndian(1_234_567)
        )
    }
}
