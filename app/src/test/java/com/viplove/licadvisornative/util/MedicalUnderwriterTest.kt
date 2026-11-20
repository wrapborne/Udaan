package com.viplove.licadvisornative.util

import com.viplove.licadvisornative.util.MedicalUnderwriter.Input
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar

/** Same cases as the website's underwriter.test.js, so the app and the website agree. */
class MedicalUnderwriterTest {
    private val c = MedicalUnderwriter.parse(File("src/main/assets/medical_charts.json").readText())
    private val on: Calendar = Calendar.getInstance().apply { clear(); set(2026, 10, 19) }
    private fun dobForAge(age: Int): Calendar = Calendar.getInstance().apply { clear(); set(2026 - age, 10, 1) }
    private fun date(y: Int, m: Int, d: Int): Calendar = Calendar.getInstance().apply { clear(); set(y, m - 1, d) }
    private val base = Input(dob = null, onDate = on, gender = "male", planCode = "736", residency = "resident",
        occupation = "salariedListed", qualification = "graduate", income = 600_000)
    private fun run(input: Input) = MedicalUnderwriter.check(c, input).result!!

    @Test fun ageNearerBirthday() {
        assertEquals(31, MedicalUnderwriter.ageNearerBirthday(date(1996, 3, 14), on))
        assertEquals(30, MedicalUnderwriter.ageNearerBirthday(date(1996, 9, 14), on))
    }

    @Test fun categories() {
        assertEquals("PNM", MedicalUnderwriter.categorise(base.copy(income = 1_200_000), 30).category)
        assertEquals("SNM", MedicalUnderwriter.categorise(base, 30).category)
        assertEquals("NMG", MedicalUnderwriter.categorise(base.copy(occupation = "salaried"), 30).category)
        assertEquals("SNM", MedicalUnderwriter.categorise(base.copy(occupation = "business", qualification = "ssc", income = 1_500_000), 30).category)
        assertEquals("NMG", MedicalUnderwriter.categorise(base.copy(income = 1_200_000, adverseHabits = true), 30).category)
        assertEquals("NONE", MedicalUnderwriter.categorise(base.copy(residency = "nriOther"), 30).category)
        assertEquals("NMG", MedicalUnderwriter.categorise(base.copy(occupation = "unearned", income = 2_000_000), 30).category)
    }

    @Test fun example1SnmNonMedical() {
        val r = run(base.copy(dob = dobForAge(30), sumAssured = 1_500_000, priorSumAssured = 500_000))
        assertEquals("SNM", r.category); assertEquals(3_000_000L, r.limit); assertEquals("nonMedical", r.verdict)
        assertEquals(21_000_000L, r.financial!!.maxInsurance); assertTrue(r.financial!!.proof.startsWith("Not needed"))
    }

    @Test fun example2NmgMerReports() {
        val r = run(base.copy(dob = dobForAge(48), occupation = "salaried", income = 400_000, sumAssured = 2_500_000))
        assertEquals("NMG", r.category); assertEquals(500_000L, r.limit); assertEquals("mer", r.verdict)
        assertEquals(listOf("MER", "ECG", "SBT-13", "RUA", "HB%"), r.reports)
        assertEquals(8_000_000L, r.financial!!.maxInsurance); assertTrue(r.financial!!.proof.startsWith("Not needed"))
        val more = run(base.copy(dob = dobForAge(48), occupation = "salaried", income = 400_000, sumAssured = 3_000_000))
        assertTrue(more.financial!!.proof.contains("PFQ"))
    }

    @Test fun vmerBand() {
        assertEquals("mer", run(base.copy(dob = dobForAge(30), income = 3_000_000, sumAssured = 8_000_000)).verdict)
        val vmer = run(base.copy(dob = dobForAge(40), income = 3_000_000, sumAssured = 3_000_000))
        assertEquals(2_500_000L, vmer.limit); assertEquals("vmer", vmer.verdict)
    }

    @Test fun groupTwoAndHash() {
        val r = run(base.copy(dob = dobForAge(30), occupation = "salaried", planCode = "715", sumAssured = 1_000_000))
        assertEquals(2, r.group); assertEquals(800_000L, r.limit); assertEquals("mer", r.verdict)
        assertEquals(listOf("MER"), MedicalUnderwriter.expandReports(c, "Nil/#", 2, true))
        assertEquals(emptyList<String>(), MedicalUnderwriter.expandReports(c, "Nil/#", 1, true))
    }

    @Test fun nmgFrom56AndFrom51() {
        val old = run(base.copy(dob = dobForAge(57), occupation = "salaried", sumAssured = 200_000))
        assertEquals("mer", old.verdict); assertEquals(listOf("MER", "ECG", "FBS"), old.reports)
        assertEquals("mer", run(base.copy(dob = dobForAge(52), occupation = "salaried", sumAssured = 200_000)).verdict)
    }

    @Test fun nmgWomen() {
        val hw = run(base.copy(gender = "female", dob = dobForAge(30), occupation = "housewife", income = 0, sumAssured = 500_000))
        assertEquals("mer", hw.verdict); assertTrue(hw.notes.any { it.contains("husband") })
        assertEquals("nonMedical", run(base.copy(gender = "female", dob = dobForAge(30), occupation = "housewife", income = 0, husbandInsured = true, sumAssured = 500_000)).verdict)
        assertEquals(800_000L, run(base.copy(gender = "female", dob = dobForAge(40), occupation = "salaried", qualification = "ssc", incomeProof = false, sumAssured = 800_000)).limit)
    }

    @Test fun minors() {
        val child = run(base.copy(dob = dobForAge(12), occupation = "student", sumAssured = 2_000_000))
        assertEquals("MINOR", child.category); assertEquals(1_500_000L, child.limit); assertEquals("jfmr", child.verdict)
        assertTrue(child.notes.any { it.contains("Good Health") })
        val baby = run(base.copy(dob = dobForAge(2), sumAssured = 3_000_000))
        assertEquals("nonMedical", baby.verdict); assertTrue(baby.notes.any { it.contains("matching insurance") })
    }

    @Test fun singlePremium() {
        val r = run(base.copy(dob = dobForAge(40), occupation = "salaried", planCode = "717", sumAssured = 2_000_000, singlePremium = 1_200_000, priorSumAssured = 5_000_000))
        assertEquals(0L, r.prior); assertEquals(1_000_000L, r.limit); assertEquals("nonMedical", r.verdict)
    }

    @Test fun termPlans() {
        val r = run(base.copy(dob = dobForAge(30), planCode = "955", sumAssured = 5_000_000))
        assertEquals("mer", r.verdict); assertEquals(25, r.financial!!.multiple)
    }

    @Test fun chart2TopBandPsa() {
        assertEquals("PSA", MedicalUnderwriter.specialReports(c, "NMG", 60, 500_000_000, 1, true).reports.last())
        assertFalse(MedicalUnderwriter.specialReports(c, "NMG", 60, 500_000_000, 1, false).reports.contains("PSA"))
    }

    @Test fun ridersCihrAndNmg() {
        assertEquals("no", run(base.copy(dob = dobForAge(30), sumAssured = 1_000_000, cihr = 500_000)).riders[0].status)
        val snm714 = run(base.copy(dob = dobForAge(30), planCode = "714", sumAssured = 1_000_000, cihr = 2_000_000))
        assertEquals("vmer", snm714.riders[0].status); assertEquals("vmer", snm714.verdict); assertEquals(7_200_000L, snm714.financial!!.cihrMax)
        assertEquals("mer", run(base.copy(dob = dobForAge(40), planCode = "714", sumAssured = 1_000_000, cihr = 4_000_000)).riders[0].status)
        val nmgMale = run(base.copy(dob = dobForAge(30), occupation = "salaried", sumAssured = 500_000, termRider = 500_000, pwb = true))
        assertEquals(1_000_000L, nmgMale.suc); assertEquals(listOf("vmer", "vmer"), nmgMale.riders.map { it.status }); assertEquals("vmer", nmgMale.verdict)
    }

    @Test fun ridersWomen() {
        val noProof = run(base.copy(gender = "female", dob = dobForAge(30), occupation = "salaried", incomeProof = false, sumAssured = 500_000, termRider = 200_000, pwb = true))
        assertEquals(listOf("no", "no"), noProof.riders.map { it.status })
        val hw = run(base.copy(gender = "female", dob = dobForAge(30), occupation = "housewife", qualification = "hsc", husbandInsured = true, income = 0, planCode = "714", sumAssured = 500_000, cihr = 500_000, pwb = true))
        assertEquals("vmer", hw.riders.first { it.id == "cihr" }.status); assertEquals("no", hw.riders.first { it.id == "pwb" }.status)
        val snm = run(base.copy(dob = dobForAge(30), sumAssured = 1_000_000, termRider = 1_000_000))
        assertEquals("ok", snm.riders[0].status); assertEquals("nonMedical", snm.verdict)
    }

    @Test fun unconfirmedGroupPlans() {
        val r = run(base.copy(dob = dobForAge(30), planCode = "890", sumAssured = 1_000_000))
        assertEquals(1, r.group); assertFalse(r.groupConfirmed); assertTrue(r.notes[0].contains("not confirmed"))
        assertTrue(run(base.copy(dob = dobForAge(30), sumAssured = 1_000_000)).groupConfirmed)
    }

    @Test fun dateOfBirthTypedAsDigitsOrWithDashes() {
        val parse = com.viplove.licadvisornative.ui.viewmodel.NonMedicalCheckerViewModel.Companion::parseDob
        for (text in listOf("12101995", "12-10-1995", "12/10/1995")) {
            val cal = parse(text)!!
            assertEquals(listOf(12, 9, 1995), listOf(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH), cal.get(Calendar.YEAR)))
        }
        assertEquals(null, parse("31021995")) // 31 February
        assertEquals(null, parse("1210199"))
    }
}
