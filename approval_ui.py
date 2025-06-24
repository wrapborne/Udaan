# approval_ui.py

import streamlit as st
from db_utils import get_pending_users, delete_pending_user, add_user_to_db  # ya aapka jo actual import ho

def show_approval_ui():
    st.header("🚦 Pending User Approvals")

    pending_users = get_pending_users()

    if not pending_users:
        st.success("✅ No users pending approval.")
        return

    for user in pending_users:
        row_id, username, password, role, admin_username, db_name, do_code, agency_code, name = user

        with st.expander(f"👤 {name.title()} | {agency_code} - {role.capitalize()}"):
            st.text(f"Username: {username}")
            st.text(f"Role: {role}")
            st.text(f"Admin: {admin_username}")
            st.text(f"DO Code: {do_code or 'N/A'}")
            st.text(f"DB Name: {db_name}")
            
            if st.button(f"✅ Approve {username}", key=f"approve_{row_id}"):                   
                try:
                    add_user_to_db(
                        username=username.upper(),
                        password=password,
                        role=role,
                        admin_username=admin_username,
                        db_name=db_name,
                        do_code=do_code,
                        agency_code=agency_code,
                        name=name
                    )
                    # ✅ Create DO DB if approved user is admin
                    if role == "admin":
                        create_do_database(do_code)

                    # Step 3: Insert into DO-specific DB (for both agent & admin)
                    try:
                        from sqlalchemy import create_engine
                        from db_config import DB_CONFIG
                
                        db_name = f"lic_{do_code.upper()}"
                        db_url = f"mysql+mysqlconnector://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}/{db_name}"
                        engine = create_engine(db_url)

                        pd.DataFrame([{
                            "username": username,
                            "password": password,
                            "role": role,
                            "start_date": pd.Timestamp.today().date(),
                            "admin_username": admin_username,
                            "db_name": db_name,
                            "do_code": do_code,
                            "agency_code": agency_code,
                            "full_name": name
                        }]).to_sql("approved_users", con=engine, if_exists="append", index=False)

                    except Exception as e:
                        st.error(f"❌ Failed to approve {username}: {e}")

                    delete_pending_user(row_id)
                    st.success(f"✅ {username} approved and added successfully!")
                    st.experimental_rerun()

