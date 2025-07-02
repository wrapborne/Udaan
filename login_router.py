# login_router.py
import streamlit as st
from layout import render_sidebar
from superadmin_app import superadmin_dashboard
from admin_app import admin_dashboard
from agent_app import agent_dashboard
from views.login_view import login_view
from non_medical_checker.app_ui import show_eligibility_form
from non_medical_checker.upload_chart_ui import show_upload_ui

def route_dashboard():
    if not st.session_state.get("logged_in"):
        login_view()
        return

    render_sidebar()

    # Default page if not already set
    if "page" not in st.session_state:
        st.session_state.page = "dashboard"

    role = st.session_state["role"]
    page = st.session_state.page

    # 🧭 Navigation logic
    if page == "eligibility":
        show_eligibility_form(current_user=st.session_state.get("username"))
    elif page == "upload_chart" and role == "superadmin":
        show_upload_ui()
    elif page == "dashboard":
        if role == "superadmin":
            superadmin_dashboard()
        elif role == "admin":
            admin_dashboard()
        elif role == "agent":
            agent_dashboard()
        else:
            st.error("❌ Unknown role. Contact support.")
    else:
        st.warning("⚠️ Invalid page state.")
