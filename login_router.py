# login_router.py
import streamlit as st
from superadmin_app import superadmin_dashboard
from admin_app import admin_dashboard
from agent_app import agent_dashboard
from db_utils import check_credentials, user_exists, log_failed_attempt, reset_failed_attempts
from datetime import datetime
from utils import log_login
from utils import handle_registration  # or from auth_utils if you moved it there
from layout import render_sidebar
render_sidebar()



def login_view():
    st.title("🔐 LIC Udaan Login")

    with st.form("login_form"):
        username = st.text_input("Agency Code (Username)")
        password = st.text_input("Password", type="password")
        submitted = st.form_submit_button("Login")

        if submitted:
            user = check_credentials(username, password)
            if user:
                db_name = user.get("db_name", f"lic_{username.upper()}").replace(".db", "")

                st.session_state.update({
                    "logged_in": True,
                    "username": user["username"],
                    "role": user["role"],
                    "start_date": user.get("start_date", ""),
                    "admin_username": user.get("admin_username", ""),
                    "db_name": db_name,
                    "agency_code": user.get("agency_code", user["username"]) if user["role"] == "agent" else ""
                })
                reset_failed_attempts(username)
                log_login(username)
                st.rerun()
            else:
                st.error("Invalid username or password.")
                if user_exists(username):
                    log_failed_attempt(username)
                    
# 🔓 This sidebar should not be inside the form
if st.session_state.get("logged_in"):
            from layout import render_sidebar
            render_sidebar()
            
col1, col2 = st.columns(2)

with col1:
    st.button("🔑 Forgot Password", on_click=lambda: st.info("Coming soon."))

with col2:
    if st.button("🌞 New Registration"):
        st.session_state.show_registration_form = not st.session_state.show_registration_form

    # 🚪 Show registration form if toggled
    if st.session_state.get("show_registration_form", False):
        st.markdown("#### 📝 New Registration")

        selected_role = st.selectbox("Registering as:", ["-- Select --", "Agent", "Admin"], key="role_selector")

        if selected_role == "Admin":
            with st.form("admin_register_form"):
                full_name = st.text_input("Full Name", key="admin_name")
                username = st.text_input("Create Username", key="admin_username")
                password = st.text_input("Password", type="password", key="admin_pass")
                do_code = st.text_input("Create a DO Code", key="admin_do_code")

                if st.form_submit_button("Submit Admin Registration"):
                    handle_registration(
                        username=username,
                        password=password,
                        do_code=do_code,
                        role="admin",
                        name=full_name
                    )

        elif selected_role == "Agent":
            with st.form("agent_register_form"):
                full_name = st.text_input("Full Name", key="agent_name")
                agency_code = st.text_input("Agency Code", key="agent_agency")
                username = st.text_input("Create Username", key="agent_username")
                password = st.text_input("Password", type="password", key="agent_pass")
                do_code = st.text_input("DO Code (provided by your Admin)", key="agent_do_code")

                if st.form_submit_button("Submit Agent Registration"):
                    handle_registration(
                        username=username,
                        password=password,
                        do_code=do_code,
                        role="agent",
                        name=full_name,
                        agency_code=agency_code
                    )


def route_dashboard():
    if not st.session_state.get("logged_in"):
        login_view()
    else:
        role = st.session_state.get("role")
        if role == "superadmin":
            superadmin_dashboard()
        elif role == "admin":
            admin_dashboard()
        elif role == "agent":
            agent_dashboard()
        else:
            st.error("❌ Unknown role. Contact support.")

# Add this to main app.py
# from login_router import route_dashboard
# route_dashboard()
