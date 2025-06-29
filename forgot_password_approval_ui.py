import streamlit as st
from sqlalchemy import text
from utils import get_mysql_connection

def show_forgot_password_approval_ui(current_user=None):
    st.header("🔐 Password Reset Approvals")

    engine = get_mysql_connection("lic-db")

    with engine.connect() as conn:
        if current_user:
            result = conn.execute(text("""
                SELECT id, username, role, new_password, approve_by, submitted_at 
                FROM forgot_password_requests 
                WHERE approve_by = :approver AND approved = 0
            """), {"approver": current_user})
        else:
            result = conn.execute(text("""
                SELECT id, username, role, new_password, approve_by, submitted_at 
                FROM forgot_password_requests 
                WHERE approved = 0
            """))

        requests = result.fetchall()

    if not requests:
        st.success("✅ No password reset requests pending.")
        return

    with st.expander(f"📬 {len(requests)} Pending Password Reset Request(s)", expanded=True):
        # Table Header
        header = st.columns([1.5, 1.2, 2, 2.5, 1.5, 1.2, 1.2])
        header[0].markdown("**👤 Username**")
        header[1].markdown("**🧩 Role**")
        header[2].markdown("**🔑 New Password**")
        header[3].markdown("**🕒 Requested At**")
        header[4].markdown("**👨‍✈️ Approver**")
        header[5].markdown("**✅ Approve**")
        header[6].markdown("**❌ Reject**")

        for req in requests:
            req_id, username, role, new_password, approve_by, submitted_at = req
            row = st.columns([1.5, 1.2, 2, 2.5, 1.5, 1.2, 1.2])

            row[0].code(username)
            row[1].code(role)
            row[2].code(new_password)
            row[3].markdown(f"`{submitted_at.strftime('%Y-%m-%d %H:%M:%S')}`")
            row[4].code(approve_by)

            # ✅ Approve
            if row[5].button("✅", key=f"approve_{req_id}", help=f"Approve {username}"):
                try:
                    db_name = "lic-db" if role == "admin" else f"lic_{approve_by.upper()}"
                    target_engine = get_mysql_connection(db_name)

                    with target_engine.connect() as conn2:
                        conn2.execute(text("""
                            UPDATE approved_users
                            SET password = :new_password
                            WHERE username = :username AND role = :role
                        """), {
                            "new_password": new_password,
                            "username": username,
                            "role": role
                        })

                    with engine.begin() as conn:
                        conn.execute(text("""
                            UPDATE forgot_password_requests
                            SET approved = 1
                            WHERE id = :id
                        """), {"id": req_id})

                    st.success(f"✅ Password reset for `{username}` approved.")
                    st.rerun()

                except Exception as e:
                    st.error(f"❌ Error: {e}")

            # ❌ Reject
            if row[6].button("❌", key=f"reject_{req_id}", help=f"Reject {username}"):
                try:
                    with engine.begin() as conn:
                        conn.execute(text("""
                            DELETE FROM forgot_password_requests
                            WHERE id = :id
                        """), {"id": req_id})

                    st.warning(f"🗑️ Request by `{username}` rejected.")
                    st.rerun()

                except Exception as e:
                    st.error("❌ Error occurred while rejecting:")
                    st.code(str(e))
