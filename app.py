# app.py (clean and stable)
import streamlit as st
from login_router import route_dashboard

st.set_page_config(
    layout="centered",
    page_title="LIC Udaan Portal",
    page_icon="📄"
)

# Initialize session state defaults
if "logged_in" not in st.session_state:
    st.session_state.update({
        "logged_in": False,
        "username": "",
        "role": "",
        "start_date": "",
        "selected_year": "All Years",
        "fin_year": "All Financial Years",
        "show_pending": False,
        "show_registration_form": False,
    })

# 👇 Directly call router (let Streamlit show any real error)
route_dashboard()
