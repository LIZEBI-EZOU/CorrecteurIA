package com.correcteur.ia
import org.junit.Assert.assertEquals
import org.junit.Test
class OfflineFrenchCorrectorTest {
    @Test fun correctsCommonFrenchErrors(){assertEquals("Je voudrais vous remercier, ça va bien.",OfflineFrenchCorrector.correct("je voudrai vous remercier, ca va bien.").text)}
    @Test fun removesExtraSpacesBeforePunctuation(){assertEquals("Bonjour! Comment allez-vous?",OfflineFrenchCorrector.correct("Bonjour   !Comment allez-vous ?").text)}
    @Test fun doesNotInventContent(){val source="Le rendez-vous est demain.";assertEquals(source,OfflineFrenchCorrector.correct(source).text)}
}
