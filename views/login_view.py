# views/login_view.py
import streamlit as st
from sqlalchemy import text
from datetime import datetime
from db_utils import (
    check_credentials,
    user_exists,
    log_failed_attempt,
    reset_failed_attempts,
    add_pending_user,
)
from utils import get_mysql_connection, log_login
from views.forms import forgot_password_form, registration_form

def ensure_approved_users_table_exists(db_name):
    engine = get_mysql_connection(db_name)
    with engine.connect() as conn:
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS approved_users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(255),
                name VARCHAR(100),
                role VARCHAR(20),
                agency_code VARCHAR(50),
                admin_username VARCHAR(50),
                do_code VARCHAR(50),
                start_date DATE
            );
        """))

def login_view():
    st.title("🔐 LIC Udaan Login")

    with st.form("login_form"):
        login_code = st.text_input("Agency Code / DO Code / Superadmin Username")
        password = st.text_input("Password", type="password")
        submitted = st.form_submit_button("Login")

        if submitted:
            user = check_credentials(login_code, password)

            if user:
                role = user["role"]
                agency_code = user.get("agency_code", login_code)
                do_code = user.get("do_code", "")

                if role == "superadmin":
                    db_name = "lic-db"
                elif role == "admin":
                    db_name = f"lic_{login_code.upper()}"
                elif role == "agent":
                    if do_code:
                        db_name = f"lic_{do_code.upper()}"
                    else:
                        st.error("Agent's DO Code is missing in the database.")
                        return
                else:
                    st.error("Invalid user role.")
                    return

                st.session_state.update({
                    "logged_in": True,
                    "username": login_code,
                    "full_name": user.get("name", login_code.upper()),
                    "role": role,
                    "start_date": user.get("start_date", ""),
                    "admin_username": user.get("admin_username", ""),
                    "db_name": db_name,
                    "agency_code": agency_code,
                })

                reset_failed_attempts(login_code)
                log_login(login_code)
                st.rerun()
            else:
                st.error("Invalid username or password.")
                if user_exists(login_code):
                    log_failed_attempt(login_code)

    st.markdown("---")
    st.session_state.setdefault("show_registration_form", False)
    st.session_state.setdefault("show_forgot_form", False)

    col1, col2 = st.columns(2)
    with col1:
        if st.button("🌞 New Registration"):
            st.session_state["show_registration_form"] = not st.session_state["show_registration_form"]
            st.session_state["show_forgot_form"] = False

    with col2:
        if st.button("🔐 Forgot Password?"):
            st.session_state["show_forgot_form"] = not st.session_state["show_forgot_form"]
            st.session_state["show_registration_form"] = False

    if st.session_state["show_forgot_form"]:
        forgot_password_form()

    if st.session_state["show_registration_form"]:
        registration_form()
