package com.viplove.licadvisornative.util

import android.content.Context
import com.google.gson.Gson
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * Medical and financial underwriting for a new LIC proposal. Same rules and data as the Udaan
 * website's medical checker (udaan-gmail/client/src/calculators/medical/underwriter.js).
 * Data: assets/medical_charts.json, from LIC's "Non-Medical Chart for Ready Reference".
 * Replace that file when LIC issues a new underwriting circular.
 */
object MedicalUnderwriter {

    // ---------------- Data ----------------
    data class LimitRow(val grp1: Long? = null, val grp2: Long? = null, val vmer: Long? = null, val cihr: Long? = null, val withMer: Long? = null)
    data class SinglePremiumRow(val ages: List<Int> = emptyList(), val standard: Long? = null, val plan749Option2: Long? = null, val withMer: Boolean? = null)
    data class MinorLimit(val ages: List<Int> = emptyList(), val nonMedical: Long = 0)
    data class MinorDocument(val ages: List<Int> = emptyList(), val text: String = "")
    data class MinorPwb(val withoutPwb: Long = 0, val withPwb: Long = 0)
    data class NoProofCap(val ssc: Long = 0, val hsc: Long = 0)
    data class ReportChart(val bands: List<Long?> = emptyList(), val rows: List<List<String>> = emptyList())
    data class IncomeMultiple(val ages: List<Int> = emptyList(), val savings: Int? = null, val cihr: Int? = null, val term: Int? = null)
    data class IncomeProof(val upTo: Long? = null, val text: String = "")

    data class Charts(
        val groupIIPlans: List<String> = emptyList(),
        val groupIPlans: List<String> = emptyList(),
        val groupUnconfirmedPlans: List<String>? = null,
        val singlePremiumPlans: List<String> = emptyList(),
        val termPlans: List<String> = emptyList(),
        val chart2NotApplicable: List<String> = emptyList(),
        val rsa125Plans: List<String> = emptyList(),
        val cihrPlans: List<String> = emptyList(),
        val ageBands: List<List<Int>> = emptyList(),
        val limits: Map<String, List<LimitRow?>> = emptyMap(),
        val nmgFemaleNoProofCap: NoProofCap = NoProofCap(),
        val singlePremiumLimits: List<SinglePremiumRow> = emptyList(),
        val minorLimits: List<MinorLimit> = emptyList(),
        val minorDocuments: List<MinorDocument> = emptyList(),
        val minorPwb: MinorPwb = MinorPwb(),
        val reportCodes: Map<String, List<String>> = emptyMap(),
        val chart1: ReportChart = ReportChart(),
        val chart2: ReportChart = ReportChart(),
        val incomeMultiples: List<IncomeMultiple> = emptyList(),
        val incomeProof: List<IncomeProof> = emptyList(),
    )

    const val ASSET_NAME = "medical_charts.json"
    @Volatile private var cached: Charts? = null

    fun parse(json: String): Charts = Gson().fromJson(json, Charts::class.java)

    fun charts(context: Context): Charts = cached ?: synchronized(this) {
        cached ?: parse(context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }).also { cached = it }
    }

    // ---------------- Inputs / outputs ----------------
    data class Option(val id: String, val label: String)

    val OCCUPATIONS = listOf(
        Option("salariedListed", "Salaried · Govt / PSU / listed company"),
        Option("salaried", "Salaried · other employer"),
        Option("professional", "Professional (doctor, CA, advocate, engineer, PG...)"),
        Option("business", "Business / self-employed"),
        Option("unearned", "Farming, rent or pension income"),
        Option("housewife", "Housewife / no income"),
        Option("student", "Student (studying in India)"),
    )

    val QUALIFICATIONS = listOf(
        Option("below10", "Below 10th"), Option("ssc", "10th (SSC)"), Option("hsc", "12th (HSC)"),
        Option("diploma", "Engineering diploma"), Option("graduate", "Graduate"), Option("postgrad", "Post-graduate"),
    )

    val PLANS = listOf(
        "736" to "Jeevan Labh", "714" to "New Endowment", "715" to "New Jeevan Anand", "733" to "Jeevan Lakshya",
        "720" to "New Money Back 20 yrs", "721" to "New Money Back 25 yrs", "748" to "Bima Shree", "745" to "Jeevan Umang",
        "771" to "Jeevan Utsav", "881" to "Bima Lakshmi", "912" to "Nav Jeevan Shree (limited)", "770" to "Bima Platinum",
        "890" to "New Bima Jyoti", "888" to "New Jeevan Sathi (single)", "889" to "New Jeevan Sathi (limited)",
        "732" to "New Children's Money Back", "734" to "Jeevan Tarun", "774" to "Amritbaal",
        "717" to "Single Premium Endowment", "883" to "Jeevan Utsav (single)", "911" to "Nav Jeevan Shree (single)",
        "749" to "Nivesh Plus (ULIP, single)", "752" to "SIIP (ULIP)", "873" to "Index Plus (ULIP)", "886" to "Protection Plus (ULIP)",
        "867" to "New Pension Plus (ULIP)", "955" to "New Jeevan Amar (term)", "887" to "Bima Kavach (term)",
        "894" to "Jeevan Raksha (term)", "875" to "Yuva Term", "877" to "Yuva Credit Life",
    )

    val CATEGORY_LABELS = mapOf(
        "PNM" to "Preferred Non-Medical (PNM)", "SNM" to "Special Non-Medical (SNM)", "NMG" to "General Non-Medical (NMG)",
        "STUDENT" to "Major student", "MINOR" to "Minor life", "NONE" to "Not eligible for non-medical",
    )

    val RIDER_LABELS = mapOf("cihr" to "Critical illness rider (CIHR)", "termRider" to "Term rider", "pwb" to "Premium waiver (PWB)")

    data class Input(
        val dob: Calendar?,
        val onDate: Calendar = Calendar.getInstance(),
        val gender: String = "male",
        val planCode: String = "736",
        val sumAssured: Long = 0,
        val priorSumAssured: Long = 0,
        val singlePremium: Long = 0,
        val plan749Option: String = "1",
        val residency: String = "resident", // resident | nriGroupV | nriOther
        val occupation: String = "salariedListed",
        val qualification: String = "graduate",
        val income: Long = 0,
        val adverseHabits: Boolean = false,
        val incomeProof: Boolean = true,
        val husbandInsured: Boolean = false,
        val minorWithPwb: Boolean = false,
        val cihr: Long = 0,
        val termRider: Long = 0,
        val pwb: Boolean = false,
    )

    data class Rider(val id: String, val status: String, val text: String) // status: ok | vmer | mer | no

    data class Financial(
        val income: Long, val multiple: Int?, val maxInsurance: Long?, val trsa: Long, val withinIncomeLimit: Boolean?,
        val proof: String, val cihrMax: Long? = null, val cihrMultiple: Int? = null, val cihrWithin: Boolean? = null,
    )

    data class Result(
        val age: Int, val male: Boolean, val plan: String, val group: Int, val groupConfirmed: Boolean,
        val single: Boolean, val term: Boolean, val suc: Long, val prior: Long, val category: String,
        val reasons: List<String>, val limit: Long?, val verdict: String, val reports: List<String>, val chart: Int?,
        val notes: List<String>, val financial: Financial?, val riders: List<Rider>, val roomLeft: Long?,
    )

    data class Check(val errors: List<String>, val result: Result?)

    // ---------------- Helpers ----------------
    private const val LAKH = 100_000L
    private fun isGraduate(q: String) = q == "graduate" || q == "postgrad" || q == "diploma"
    private fun atLeast10th(q: String) = q != "below10"
    private fun List<Int>.covers(age: Int) = size >= 2 && age >= this[0] && age <= this[1]
    private val SEVERITY = mapOf("nonMedical" to 0, "vmer" to 1, "mer" to 2, "jfmr" to 2)

    fun money(value: Long): String = "₹" + NumberFormat.getIntegerInstance(Locale("en", "IN")).format(value)

    /** Age nearer birthday (insurance age): after six months past a birthday, the next age. */
    fun ageNearerBirthday(dob: Calendar, onDate: Calendar): Int {
        fun day(c: Calendar) = Calendar.getInstance().apply { clear(); set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)) }
        val birth = day(dob)
        val on = day(onDate)
        var years = on.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        val birthdayThisYear = Calendar.getInstance().apply { clear(); set(on.get(Calendar.YEAR), birth.get(Calendar.MONTH), birth.get(Calendar.DAY_OF_MONTH)) }
        if (on.before(birthdayThisYear)) years -= 1
        val halfYear = Calendar.getInstance().apply {
            clear(); set(birth.get(Calendar.YEAR) + years, birth.get(Calendar.MONTH), birth.get(Calendar.DAY_OF_MONTH)); add(Calendar.MONTH, 6)
        }
        return if (!on.before(halfYear)) years + 1 else years
    }

    fun planGroup(c: Charts, plan: String) = if (plan in c.groupIIPlans) 2 else 1

    private fun reportColumn(age: Int) = when {
        age <= 35 -> 0; age <= 45 -> 1; age <= 50 -> 2; age <= 55 -> 3; else -> 4
    }

    /** Expand a chart cell such as "MER+F", "Nil/#" or "MER+X+PSA" into a list of requirements. */
    fun expandReports(c: Charts, cell: String?, group: Int, male: Boolean): List<String> {
        if (cell.isNullOrEmpty() || cell == "Nil") return emptyList()
        if (cell == "Nil/#") return if (group == 2) listOf("MER") else emptyList()
        val out = mutableListOf<String>()
        for (part in cell.split("+")) {
            when (part) {
                "MER" -> out += "MER"
                "PSA" -> if (male) out += "PSA"
                else -> out += c.reportCodes[part] ?: listOf(part)
            }
        }
        return out.distinct()
    }

    data class Special(val chart: Int, val cell: String, val reports: List<String>)

    /** Chart 1 for PNM/SNM up to ₹50 lakh (age 60 and below), chart 2 otherwise. */
    fun specialReports(c: Charts, category: String, age: Int, suc: Long, group: Int, male: Boolean): Special {
        val useChart1 = (category == "PNM" || category == "SNM") && suc <= 50 * LAKH && age <= 60
        val chart = if (useChart1) c.chart1 else c.chart2
        var row = chart.bands.indexOfFirst { it == null || suc <= it }
        if (row < 0) row = chart.bands.size - 1
        val cell = chart.rows[minOf(row, chart.rows.size - 1)][reportColumn(age)]
        return Special(if (useChart1) 1 else 2, cell, expandReports(c, cell, group, male))
    }

    data class Category(val category: String, val reasons: List<String>)

    /** Best non-medical category: PNM, then SNM, then NMG. */
    fun categorise(input: Input, age: Int): Category {
        if (age < 18) return Category("MINOR", listOf("Below 18: minor life rules apply."))
        val nriAllowed = input.residency == "resident" || input.residency == "nriGroupV"
        val earned = input.occupation !in listOf("unearned", "housewife", "student")
        val special = nriAllowed && !input.adverseHabits
        val income = input.income
        if (special && earned && isGraduate(input.qualification) && income > 10 * LAKH &&
            input.occupation in listOf("salariedListed", "salaried", "professional", "business")) {
            return Category("PNM", listOf("Graduate / engineering diploma", "Earned income above ₹10 lakh"))
        }
        if (special && earned && income > 250_000 && (input.occupation == "salariedListed" || input.occupation == "professional")) {
            return Category("SNM", listOf(if (input.occupation == "professional") "Professional" else "Govt / PSU / listed company employee", "Income above ₹2.5 lakh"))
        }
        if (special && input.occupation == "business" && atLeast10th(input.qualification) && income > 10 * LAKH) {
            return Category("SNM", listOf("Business, 10th or above", "Income above ₹10 lakh"))
        }
        if (input.occupation == "student" && input.residency == "resident") return Category("STUDENT", listOf("Resident student studying in India"))
        if (input.residency != "resident") {
            return Category("NONE", listOf(
                if (input.residency == "nriGroupV") "NRI: only PNM/SNM allow non-medical, and the conditions are not met"
                else "NRI outside Group V countries: non-medical not available"
            ))
        }
        val reasons = mutableListOf<String>()
        if (input.adverseHabits) reasons += "Smoking / alcohol / tobacco rules out PNM and SNM"
        if (input.occupation == "unearned") reasons += "Farming, rent and pension income do not count for PNM/SNM"
        return Category("NMG", reasons.ifEmpty { listOf("Resident Indian") })
    }

    private data class WomanRule(val cap: Long?, val notes: List<String>, val blocked: Boolean)

    /** NMG women: conditions A(ii), A(iii), B(i), B(ii) of the chart. */
    private fun nmgWomanRule(c: Charts, input: Input, limit: Long): WomanRule {
        val notes = mutableListOf<String>()
        if (input.occupation == "housewife") {
            notes += if (input.husbandInsured) "Husband's matching insurance is in place (required for housewives)."
            else "Needs the husband's matching insurance (required for housewives)."
            if (!isGraduate(input.qualification)) notes += "Critical illness rider only with VMER; term rider and PWB not allowed."
            return WomanRule(limit, notes, !input.husbandInsured)
        }
        var cap = limit
        if (!input.incomeProof) {
            val qualCap = when (input.qualification) {
                "hsc" -> c.nmgFemaleNoProofCap.hsc
                "ssc", "below10" -> c.nmgFemaleNoProofCap.ssc
                else -> null
            }
            if (qualCap != null) cap = minOf(cap, qualCap)
            notes += "Without income proof: CIHR, term rider and PWB not allowed."
        } else {
            notes += "Proof of income required."
        }
        return WomanRule(cap, notes, false)
    }

    /** Rider eligibility. status: ok (granted with the policy's requirement) | vmer | mer | no. */
    fun riderChecks(c: Charts, category: String, age: Int, row: LimitRow?, input: Input, plan: String, term: Boolean): List<Rider> {
        val out = mutableListOf<Rider>()
        val female = input.gender == "female"
        val housewife = input.occupation == "housewife"
        val nmgWomanNoProof = category == "NMG" && female && !housewife && !input.incomeProof
        val special = category == "PNM" || category == "SNM"

        if (input.cihr > 0) {
            val cihrLimit = row?.cihr
            out += when {
                age < 18 -> Rider("cihr", "no", "Not available for minors.")
                plan !in c.cihrPlans -> Rider("cihr", "no", "At present only on plans ${c.cihrPlans.joinToString(" and ")}.")
                nmgWomanNoProof -> Rider("cihr", "no", "Not allowed for women without proof of income (NMG).")
                category == "NONE" -> Rider("cihr", "mer", "Only with full medical (MER).")
                cihrLimit != null && input.cihr <= cihrLimit -> Rider("cihr", "vmer", "Allowed with Video MER up to ${money(cihrLimit)} (not added to SUC).")
                else -> Rider("cihr", "mer", if (cihrLimit != null) "Above ${money(cihrLimit)}: MER needed." else "At this age: MER needed.")
            }
        }
        for (id in listOf("termRider", "pwb")) {
            val selected = if (id == "termRider") input.termRider > 0 else input.pwb
            if (!selected) continue
            out += when {
                age < 18 -> Rider(id, "no", if (id == "pwb") "For a child's policy use the PWB choice above (it is on the proposer)." else "Not available for minors.")
                id == "termRider" && term -> Rider(id, "no", "Not taken with a term plan.")
                nmgWomanNoProof -> Rider(id, "no", "Not allowed for women without proof of income (NMG).")
                category == "NMG" && housewife && !isGraduate(input.qualification) -> Rider(id, "no", "Not allowed for undergraduate housewives (NMG).")
                special -> Rider(id, "ok", "Granted within the category's SUC limit.")
                category == "NONE" -> Rider(id, "mer", "Only with full medical (MER).")
                else -> Rider(id, "vmer", "Allowed with Video MER or MER.")
            }
        }
        return out
    }

    // ---------------- Full check ----------------
    fun check(c: Charts, input: Input): Check {
        val errors = mutableListOf<String>()
        if (input.dob == null) errors += "Enter the date of birth."
        if (input.sumAssured <= 0) errors += "Enter the sum assured."
        if (input.planCode.isBlank()) errors += "Choose the plan."
        if (errors.isNotEmpty() || input.dob == null) return Check(errors, null)

        val age = ageNearerBirthday(input.dob, input.onDate)
        val male = input.gender != "female"
        val plan = input.planCode
        val group = planGroup(c, plan)
        val single = plan in c.singlePremiumPlans
        val term = plan in c.termPlans
        val prior = if (single) 0 else input.priorSumAssured
        val termRiderSa = if (!term && !single) input.termRider else 0
        val suc = input.sumAssured + prior + termRiderSa
        val notes = mutableListOf<String>()
        val (category, reasons) = categorise(input, age)
        if (termRiderSa > 0) notes += "Term rider sum assured ${money(termRiderSa)} is counted in the SUC."
        var limitRow: LimitRow? = null
        var verdict: String
        var limit: Long? = null
        var reports: List<String> = emptyList()
        var chart: Int? = null

        if (category == "MINOR") {
            val row = c.minorLimits.first { it.ages.covers(age) }
            limit = row.nonMedical
            verdict = if (suc <= row.nonMedical) "nonMedical" else "jfmr"
            c.minorDocuments.firstOrNull { it.ages.covers(age) }?.let { notes += it.text }
            if (age > 10) notes += "Declaration of Good Health (DGH) required."
            val pwbCap = if (input.minorWithPwb) c.minorPwb.withPwb else c.minorPwb.withoutPwb
            if (suc > pwbCap) notes += "Above ${money(pwbCap)} ${if (input.minorWithPwb) "" else "without PWB "}the proposer's matching insurance is required."
            if (verdict == "jfmr") reports = listOf("Juvenile Full Medical Report (JFMR)")
        } else if (term) {
            verdict = "mer"
            notes += "Term plans cannot be taken non-medical."
            if (plan !in c.chart2NotApplicable) {
                val sp = specialReports(c, "NMG", age, suc, group, male)
                chart = sp.chart
                reports = if ("MER" in sp.reports) sp.reports else listOf("MER") + sp.reports
            } else {
                reports = listOf("MER")
                notes += "Special reports as per this plan's own underwriting chart."
            }
        } else if (single) {
            val row = c.singlePremiumLimits.firstOrNull { it.ages.covers(age) }
            val option2 = plan == "749" && input.plan749Option == "2"
            limit = row?.let { if (option2) it.plan749Option2 else it.standard }
            val risk = maxOf(0, input.sumAssured - input.singlePremium)
            notes += "Single premium plan: counted on its own (not added to earlier policies), as sum assured minus single premium = ${money(risk)}."
            if (limit != null && risk <= limit && row?.withMer != true) verdict = "nonMedical"
            else if (limit != null && risk <= limit) { verdict = "mer"; reports = listOf("MER or VMER") }
            else {
                verdict = "mer"
                val sp = specialReports(c, if (category == "NONE") "NMG" else category, age, risk, group, male)
                chart = sp.chart
                reports = listOf("MER") + sp.reports.filter { it != "MER" }
            }
        } else {
            val index = c.ageBands.indexOfFirst { it.covers(age) }
            val row = if (index >= 0) c.limits[category]?.getOrNull(index) else null
            limitRow = row
            var cap: Long? = row?.let { if (it.grp1 != null) (if (group == 2) it.grp2 else it.grp1) else null }
            if (category == "NMG" && input.gender == "female" && cap != null) {
                val rule = nmgWomanRule(c, input, cap)
                cap = if (rule.blocked) null else rule.cap
                notes += rule.notes
            }
            limit = cap
            val chartCategory = if (category == "NONE" || category == "STUDENT") "NMG" else category
            val sp = specialReports(c, chartCategory, age, suc, group, male)
            if (cap != null && suc <= cap) {
                verdict = "nonMedical"
            } else if (row?.vmer != null && cap != null && suc <= row.vmer && sp.reports.isEmpty()) {
                verdict = "vmer"
                reports = listOf("Video MER (VMER)")
            } else {
                verdict = "mer"
                chart = sp.chart
                reports = when {
                    sp.reports.isEmpty() -> listOf("MER")
                    "MER" in sp.reports -> sp.reports
                    else -> listOf("MER") + sp.reports
                }
                if (row?.withMer != null && suc <= row.withMer) notes += "${CATEGORY_LABELS[category]}: up to ${money(row.withMer)} with MER at this age."
            }
            if (age > 60) notes += "Above 60: non-medical is not available."
            else if (category == "NMG" && age >= 56) notes += "NMG: non-medical is not available from age 56."
        }

        // Riders can raise the medical requirement.
        val riders = riderChecks(c, category, age, limitRow, input, plan, term)
        for (rider in riders) {
            if (rider.status != "vmer" && rider.status != "mer") continue
            if ((SEVERITY[rider.status] ?: 0) > (SEVERITY[verdict] ?: 0)) {
                notes += "${RIDER_LABELS[rider.id]} needs ${if (rider.status == "vmer") "Video MER" else "MER"}."
                verdict = rider.status
                if (verdict == "vmer") reports = listOf("Video MER (VMER)")
                if (verdict == "mer" && reports.none { it.startsWith("MER") }) reports = listOf("MER") + reports.filter { !it.startsWith("Video") }
            }
        }

        // Financial underwriting.
        val multipleRow = c.incomeMultiples.firstOrNull { it.ages.covers(age) }
        val multiple = multipleRow?.let { if (term) it.term else it.savings }
        val rsa125 = plan in c.rsa125Plans
        val trsa = Math.round(input.sumAssured * (if (rsa125) 1.25 else 1.0)) + prior
        val proof = c.incomeProof.first { it.upTo == null || trsa <= it.upTo }
        var financial: Financial? = if (category == "MINOR") null else Financial(
            income = input.income,
            multiple = multiple,
            maxInsurance = if (input.income > 0 && multiple != null) input.income * multiple else null,
            trsa = trsa,
            withinIncomeLimit = if (input.income > 0 && multiple != null) trsa <= input.income * multiple else null,
            proof = proof.text,
        )
        if (rsa125) notes += "This plan counts 1.25 × sum assured for financial underwriting."
        val groupConfirmed = plan !in (c.groupUnconfirmedPlans ?: emptyList())
        if (!groupConfirmed) notes.add(0, "Plan group (I or II) is not confirmed for this plan yet: the result uses Group I limits. Group II limits are lower.")
        if (financial != null && input.cihr > 0 && multipleRow?.cihr != null && input.income > 0) {
            val cihrMax = input.income * multipleRow.cihr
            financial = financial.copy(cihrMax = cihrMax, cihrMultiple = multipleRow.cihr, cihrWithin = input.cihr <= cihrMax)
        }

        return Check(emptyList(), Result(
            age = age, male = male, plan = plan, group = group, groupConfirmed = groupConfirmed, single = single, term = term,
            suc = suc, prior = prior, category = category, reasons = reasons, limit = limit, verdict = verdict, reports = reports,
            chart = chart, notes = notes, financial = financial, riders = riders, roomLeft = limit?.let { it - suc },
        ))
    }
}
