# login_router.py
import streamlit as st
from layout import render_sidebar
from superadmin_app import superadmin_dashboard
from admin_app import admin_dashboard
from agent_app import agent_dashboard
from views.login_view import login_view

def route_dashboard():
    if not st.session_state.get("logged_in"):
        login_view()
        return

    render_sidebar()
    role = st.session_state["role"]

    if role == "superadmin":
        superadmin_dashboard()
    elif role == "admin":
        admin_dashboard()
    elif role == "agent":
        agent_dashboard()
    else:
        st.error("\u274c Unknown role. Contact support.")
