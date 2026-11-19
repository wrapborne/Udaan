// File: app/src/main/java/com/viplove/licadvisornative/ui/viewmodel/NonMedicalCheckerViewModel.kt
package com.viplove.licadvisornative.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.viplove.licadvisornative.util.MedicalUnderwriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar

/** Medical checker: inputs as typed, and the last result (same rules as the Udaan website). */
class NonMedicalCheckerViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val dob: String = "",
        val gender: String = "male",
        val planCode: String = "736",
        val sumAssured: String = "1000000",
        val priorSumAssured: String = "0",
        val singlePremium: String = "",
        val plan749Option: String = "1",
        val residency: String = "resident",
        val occupation: String = "salariedListed",
        val qualification: String = "graduate",
        val income: String = "600000",
        val adverseHabits: Boolean = false,
        val incomeProof: Boolean = true,
        val husbandInsured: Boolean = false,
        val minorWithPwb: Boolean = false,
        val cihr: String = "",
        val termRider: String = "",
        val pwb: Boolean = false,
        val errors: List<String> = emptyList(),
        val result: MedicalUnderwriter.Result? = null,
    )

    private val charts by lazy { MedicalUnderwriter.charts(getApplication()) }
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun update(change: (UiState) -> UiState) = _uiState.update { change(it).copy(result = null, errors = emptyList()) }

    /** Age from the typed date of birth (for showing minor-only questions), or null. */
    fun ageFor(state: UiState): Int? = parseDob(state.dob)?.let { MedicalUnderwriter.ageNearerBirthday(it, Calendar.getInstance()) }

    fun check() {
        val s = _uiState.value
        val dob = parseDob(s.dob)
        val input = MedicalUnderwriter.Input(
            dob = dob,
            gender = s.gender,
            planCode = s.planCode,
            sumAssured = amount(s.sumAssured),
            priorSumAssured = amount(s.priorSumAssured),
            singlePremium = amount(s.singlePremium),
            plan749Option = s.plan749Option,
            residency = s.residency,
            occupation = s.occupation,
            qualification = s.qualification,
            income = if (s.occupation == "housewife") 0 else amount(s.income),
            adverseHabits = s.adverseHabits,
            incomeProof = s.incomeProof,
            husbandInsured = s.husbandInsured,
            minorWithPwb = s.minorWithPwb,
            cihr = amount(s.cihr),
            termRider = amount(s.termRider),
            pwb = s.pwb,
        )
        val check = MedicalUnderwriter.check(charts, input)
        val errors = if (s.dob.isNotBlank() && dob == null) listOf("Enter the date of birth as DD-MM-YYYY.") else check.errors
        _uiState.update { it.copy(result = check.result, errors = errors) }
    }

    companion object {
        fun amount(text: String): Long = text.filter { it.isDigit() }.toLongOrNull() ?: 0

        /** "14-03-1996", "14/03/1996" or "14.03.1996". */
        fun parseDob(text: String): Calendar? {
            val parts = text.trim().split('-', '/', '.').map { it.trim() }
            if (parts.size != 3) return null
            val (d, m, y) = parts.map { it.toIntOrNull() ?: return null }
            if (y < 1900 || m !in 1..12 || d !in 1..31) return null
            val cal = Calendar.getInstance().apply { isLenient = false; clear(); set(y, m - 1, d) }
            return try { cal.timeInMillis; if (cal.after(Calendar.getInstance())) null else cal } catch (e: Exception) { null }
        }
    }
}
