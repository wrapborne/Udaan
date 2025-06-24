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
#render_sidebar()



def login_view():
    st.title("🔐 LIC Udaan Login")

    with st.form("login_form"):
        login_code = st.text_input("Agency Code / DO Code")
        password = st.text_input("Password", type="password")
        submitted = st.form_submit_button("Login")

        if submitted:
            user = check_credentials(login_code, password)  # Fetch from 'users' table in 'lic-db'

            if user:
                role = user["role"]
                agency_code = user.get("agency_code", login_code)
                do_code = user.get("do_code", "")  # ✅ Pulled from users table

                # Determine correct database
                if role == "admin":
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

                # ✅ Store session details
                st.session_state.update({
                    "logged_in": True,
                    "username": user["login_code"],
                    "full_name": user.get("full_name", user["login_code"]),  # ✅ Added this line
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

                    
    # 🔓 This sidebar should not be inside the form
    if st.session_state.get("logged_in"):
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
                full_name = st.text_input("Full Name")
                do_code = st.text_input("DO Code")
                password = st.text_input("Password", type="password")
                 submitted = st.form_submit_button("Register")

                if submitted:
                    if not all([full_name, do_code, password]):
                        st.warning("⚠️ All fields are required.")
                    elif user_exists(do_code):  # Login via DO Code only now
                         st.error("❌ This DO code is already registered.")
                    else:
                         add_pending_user(
                             username=do_code,
                             password=password,
                             role="admin",
                             full_name=full_name,
                             agency_code="",        # not applicable for admin
                             admin_username=do_code  # DO Code acts as own admin username
                         )
                            st.success("✅ Admin registration submitted. Awaiting approval.")


    elif selected_role == "Agent":
        with st.form("agent_register_form"):
            full_name = st.text_input("Full Name")
            agency_code = st.text_input("Agency Code")
            password = st.text_input("Password", type="password")
            do_code = st.text_input("DO Code (provided by your Admin)")
            submitted = st.form_submit_button("Register")

            if submitted:
                if not all([full_name, agency_code, password, do_code]):
                    st.warning("⚠️ All fields are required.")
                elif user_exists(agency_code):  # Login via Agency Code only now
                    st.error("❌ This agency code is already registered.")
                else:
                    add_pending_user(
                        username=agency_code,
                        password=password,
                        role="agent",
                        full_name=full_name,
                        agency_code=agency_code,
                        admin_username=do_code.upper()
                    )
                    st.success("✅ Registration submitted. Awaiting approval.")


def route_dashboard():
    if not st.session_state.get("logged_in"):
        login_view()
    else:
        from layout import render_sidebar
        render_sidebar()  # ✅ Moved here, only shows after login

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
