import streamlit as st
from sqlalchemy import text
from datetime import datetime
from utils import get_mysql_connection, database_exists  # make sure this is defined in utils.py
from db_utils import add_pending_user


def forgot_password_form():
    st.markdown("#### 📝 Forgot Password")
    role = st.selectbox("Role", ["agent", "admin"], key="forgot_role")
    do_code_placeholder = st.empty()

    with st.form("forgot_form"):
        username = st.text_input("Username")
        new_password = st.text_input("New Password", type="password")

        if role == "agent":
            do_code = do_code_placeholder.text_input("DO Code (of your DO)").strip().upper()
        else:
            do_code_placeholder.empty()
            do_code = ""

        submit = st.form_submit_button("Submit Request")

        if submit:
            if not username or not new_password:
                st.warning("⚠️ Please fill in both Username and New Password.")
            elif role == "agent" and not do_code:
                st.warning("⚠️ Please provide your DO Code.")
            else:
                try:
                    if role == "agent":
                        db_name = f"lic_{do_code}"

                        if not database_exists(db_name):
                            st.error("❌ No such DO Code found. Please check the code or contact your DO.")
                            return

                        engine_check = get_mysql_connection(db_name)
                        with engine_check.connect() as conn_check:
                            result = conn_check.execute(text("""
                                SELECT 1 FROM approved_users 
                                WHERE username = :username AND role = 'agent'
                            """), {"username": username.upper()}).fetchone()

                        if not result:
                            st.error("❌ No such agent found under the provided DO Code.")
                            return

                        approve_by = do_code
                    else:
                        approve_by = "SUPERADMIN"

                    engine = get_mysql_connection("lic-db")
                    with engine.connect() as conn:
                        conn.execute(text("""
                            INSERT INTO forgot_password_requests 
                            (username, role, new_password, approve_by, submitted_at, approved)
                            VALUES (:username, :role, :new_password, :approve_by, :submitted_at, :approved)
                        """), {
                            "username": username.upper(),
                            "role": role,
                            "new_password": new_password,
                            "approve_by": approve_by,
                            "submitted_at": datetime.now(),
                            "approved": 0
                        })

                    approver = "Superadmin" if role == "admin" else f"Admin ({do_code})"
                    st.success(f"🔐 Password reset request submitted. Wait for approval by {approver}.")

                except Exception as e:
                    import traceback
                    st.error("❌ Failed to insert request:")
                    st.code(traceback.format_exc())


def registration_form():
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
                else:
                    db_name = f"lic_{do_code.upper()}"
                    if database_exists(db_name):
                        st.error("❌ This DO Code is already registered.")
                    else:
                        add_pending_user(
                            username=do_code,
                            password=password,
                            role="admin",
                            name=full_name,
                            agency_code="",
                            admin_username=do_code,
                            db_name=db_name,
                            do_code=do_code
                        )
                        st.success("✅ Admin registration submitted. Awaiting approval.")

    elif selected_role == "Agent":
        with st.form("agent_register_form"):
            full_name = st.text_input("Full Name")
            agency_code = st.text_input("Agency Code")
            password = st.text_input("Password", type="password")
            do_code = st.text_input("DO Code")

            submitted = st.form_submit_button("Register")

            if submitted:
                if not all([full_name, agency_code, password, do_code]):
                    st.warning("⚠️ All fields are required.")
                else:
                    db_name = f"lic_{do_code.upper()}"

                    if not database_exists(db_name):
                        st.error("❌ This DO Code is not yet approved. Please contact your DO.")
                        return

                    try:
                        engine_check = get_mysql_connection(db_name)
                        with engine_check.connect() as conn_check:
                            result = conn_check.execute(text("""
                                SELECT 1 FROM approved_users 
                                WHERE username = :username AND role = 'agent'
                            """), {"username": agency_code.upper()}).fetchone()

                        if not result:
                            st.error("❌ No such agent found under the provided DO Code. Please contact your DO.")
                            return

                        add_pending_user(
                            username=agency_code,
                            password=password,
                            role="agent",
                            name=full_name,
                            do_code=do_code,
                            agency_code=agency_code,
                            admin_username=do_code.upper(),
                            db_name=db_name
                        )
                        st.success("✅ Agent registration submitted. Awaiting approval.")

                    except Exception as e:
                        st.error(f"❌ Failed to validate: {e}")
