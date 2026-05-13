package com.viplove.licadvisornative.model

data class PlanData(
    val name: String,
    val planNumber: String,
    // A map where the Key is the Policy Term and the Value is the Premium Paying Term (PPT).
    val termsAndPpts: Map<Int, Int>
)