import streamlit as st
from non_medical_checker.logic_engine import load_chart_data, determine_category, get_sa_limits
st.set_page_config(page_title="Non-Medical Checker", layout="centered")

import sys
import os
sys.path.append(os.path.abspath("."))
from number_to_indian_words import number_to_indian_words

def show_eligibility_form(current_user=None):
    st.header("🩺 Non-Medical Eligibility Checker")

    if "show_result" not in st.session_state:
        st.session_state.show_result = False

    chart = load_chart_data()
    grp_1_plans = chart.get("grp_1_plans", [])
    grp_2_plans = chart.get("grp_2_plans", [])
    all_plans = sorted(set(grp_1_plans + grp_2_plans))


    with st.form("eligibility_form"):
        st.subheader("Step 1: Personal & Income Details")
        name = st.text_input("Full Name")
        age = st.number_input("Age", min_value=1, max_value=100, value=30)
        gender = st.selectbox("Gender", ["male", "female"])
        is_resident_indian = st.checkbox("Are you a Resident Indian?", value=True)
        is_minor = st.checkbox("Is the Life Assured a Minor?", value=False)
        is_pwb = st.checkbox("Is Proposer different from Life Assured (PWB)?", value=False)
        trsa_required = st.checkbox("Is TRSA Required?", value=False)

        st.markdown("---")
        st.subheader("Step 2: Qualification & Occupation Details")

        qualification = st.selectbox("Qualification", [
            "Post Graduate", "Graduate", "Professional", "HSC / Plus 2", "SSC / 10th", "Others"
        ])

        profession = st.selectbox("Profession", [
            "Employed", "Business", "Professionals", "Others", "Major Student"
        ])

        income = st.number_input("Annual Income (₹)", step=10000, value=500000)
        st.caption(f"💸 {number_to_indian_words(income).capitalize()} rupees only")


        st.markdown("---")
        st.subheader("Step 3: Plan Details")
        plan_number = st.selectbox("Select Plan Number", all_plans)
        plan_group = "Grp I" if plan_number in grp_1_plans else "Grp II"
        st.caption(f"ℹ️ Plan {plan_number} belongs to **{plan_group}**")

        submitted = st.form_submit_button("Check Eligibility")

        if submitted:
            st.session_state.show_result = True
            st.session_state.form_data = {
                "name": name,
                "age": age,
                "gender": gender,
                "qualification": qualification,
                "profession": profession,
                "income": income,
                "is_resident_indian": is_resident_indian,
                "group": plan_group,
                "plan": plan_number,
                "is_minor": is_minor,
                "is_pwb": is_pwb,
                "trsa_required": trsa_required
            }

    if st.session_state.get("show_result") and "form_data" in st.session_state:
        data = st.session_state.form_data
        category = determine_category(data, chart)
        st.success(f"✅ Eligible under **{category}** category." if category != "Ineligible" else "❌ Not eligible under any non-medical category.")

        if category != "Ineligible":
            sa_limit = get_sa_limits(category, data["age"], data["group"], chart, data)
            if isinstance(sa_limit, dict):
                st.info(f"📋 Special Rules: {sa_limit}")
            elif sa_limit:
                st.success(f"💰 Max Sum Assured for Plan {data['plan']} at age {data['age']}: ₹{sa_limit} Lakhs")
            else:
                st.warning("⚠️ No SA limit data found.")
