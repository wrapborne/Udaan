# db_utils.py
import mysql.connector
import pandas as pd
import streamlit as st
from db_config import DB_CONFIG

# === Connection Helper ===
def get_mysql_connection(db_name=None):
    db_config = DB_CONFIG.copy()
    if db_name:
        db_config["database"] = db_name
    return mysql.connector.connect(**db_config)

def get_db_connection():
    if "db_name" not in st.session_state:
        st.error("No database selected. Please log in again.")
        st.stop()

    db_config = DB_CONFIG.copy()
    db_config["database"] = st.session_state["db_name"]
    return mysql.connector.connect(**db_config)



# === Initialization ===
def init_db():
    with get_mysql_connection("lic-db") as conn:
        cursor = conn.cursor()

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(100) PRIMARY KEY,
                password VARCHAR(100) NOT NULL,
                role VARCHAR(50) NOT NULL,
                start_date DATE,
                admin_username VARCHAR(100),
                db_name VARCHAR(200),
                do_code VARCHAR(100),
                agency_code VARCHAR(100),
                name VARCHAR()
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS pending_users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(100),
                password VARCHAR(100),
                role VARCHAR(50),
                admin_username VARCHAR(100),
                db_name VARCHAR(200),
                do_code VARCHAR(100),
                agency_code VARCHAR(100),
                name VARCHAR(),
                approved BOOLEAN DEFAULT 0
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS failed_attempts (
                username VARCHAR(100) PRIMARY KEY,
                attempts INT DEFAULT 0
            )
        """)
        conn.commit()

def check_credentials(username, password):
    db_config = DB_CONFIG.copy()
    db_config["database"] = "lic-db"  # Central DB where all user credentials are stored

    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor(dictionary=True)

    cursor.execute(
        "SELECT * FROM users WHERE username = %s AND password = %s",
        (username, password)
    )
    user = cursor.fetchone()
    conn.close()

    if user:
        do_code = user['do_code']
        st.session_state['username'] = user['username']
        st.session_state['role'] = user['role']
        st.session_state['agency_code'] = user['agency_code']
        st.session_state['current_db'] = f"lic_{do_code}"  # 👈 DO’s DB
        return user  # ✅ return user dict
    else:
        return None


def user_exists(username):
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM users WHERE username = %s", (username.upper(),))
        return cursor.fetchone() is not None

def get_admin_by_do_code(do_code):
    with get_mysql_connection() as conn:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM users WHERE role = 'admin' AND do_code = %s", (do_code,))
        return cursor.fetchone()


# === Users Management ===
def get_user(username):
    with get_mysql_connection() as conn:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM users WHERE username = %s", (username.upper(),))
        return cursor.fetchone()

def ensure_user_columns_exist(cursor):
    cursor.execute("SHOW COLUMNS FROM users")
    columns = [col[0] for col in cursor.fetchall()]
    alterations = []

    if 'do_code' not in columns:
        alterations.append("ADD COLUMN do_code VARCHAR(50)")
    if 'agency_code' not in columns:
        alterations.append("ADD COLUMN agency_code VARCHAR(50)")
    if 'name' not in columns:
        alterations.append("ADD COLUMN name VARCHAR(100)")

    if alterations:
        alter_sql = f"ALTER TABLE users {', '.join(alterations)}"
        cursor.execute(alter_sql)


def add_user(username, password, role, start_date=None, admin_username=None, db_name=None, do_code=None, agency_code=None, name=None):
    from mysql.connector import ProgrammingError

    # 1️⃣ Add to lic-db (master)
    with get_mysql_connection("lic-db") as conn:
        cursor = conn.cursor()
        ensure_user_columns_exist(cursor)
        cursor.execute("""
            INSERT INTO users (username, password, role, start_date, admin_username, db_name, do_code, agency_code, name)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (username.upper(), password, role, start_date, admin_username, db_name, do_code, agency_code, name))
        conn.commit()

    # 2️⃣ Add to admin's own DB
    if db_name:
        with get_mysql_connection(db_name) as admin_conn:
            admin_cursor = admin_conn.cursor()

            # ✅ Ensure the extra columns exist here as well
            ensure_user_columns_exist(admin_cursor)

            admin_cursor.execute("""
                INSERT INTO users (username, password, role, start_date, admin_username, db_name, do_code, agency_code, name)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """, (username.upper(), password, role, start_date, admin_username, db_name, do_code, agency_code, name))
            admin_conn.commit()



def add_user_to_db(username, password, role, admin_username, db_name, do_code, agency_code, name):
    with get_mysql_connection("lic-db") as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO users (username, password, role, admin_username, db_name, do_code, agency_code, name)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """, (username.upper(), password, role, admin_username, db_name, do_code, agency_code, name))
        conn.commit()
        

def delete_user(username):
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM users WHERE username = %s", (username.upper(),))
        conn.commit()


#from .db_connection import get_mysql_connection  # Adjust path as needed

def get_all_users(admin_filter=None):
    conn = get_mysql_connection()
    cursor = conn.cursor()

    if admin_filter:
        query = """
            SELECT username, role, start_date, do_code, name, agency_code, admin_username
            FROM users
            WHERE admin_username = %s AND role = 'agent'
        """
        cursor.execute(query, (admin_filter,))
    else:
        query = """
            SELECT username, role, start_date, do_code, name, agency_code, admin_username
            FROM users
        """
        cursor.execute(query)

    results = cursor.fetchall()
    conn.close()
    return results



  

# === Pending Registration ===
def add_pending_user(username, password, role, admin_username, db_name, do_code = None, agency_code = None, name = None):
    # Step 1: Auto-create DO DB if role is admin and do_code is given
    if role == "admin" and do_code:
        create_do_database(do_code)  # ✅ ensures lic_<do_code> is created before anything else
    
    # Step 2: Add to pending_users table (in lic-db)
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            INSERT INTO pending_users (username, password, role, admin_username, db_name, do_code, agency_code, name)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """, (username.upper(), password, role, admin_username, db_name, do_code, agency_code, name))
        conn.commit()

def get_pending_users():
    with get_mysql_connection("lic-db") as conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, username, password, role, admin_username, db_name, do_code, agency_code, name, approved
            FROM pending_users
        """)
        return cursor.fetchall()

def delete_pending_user(rowid):
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM pending_users WHERE id = %s", (rowid,))
        conn.commit()

# === Login Tracking ===
def log_failed_attempt(username):
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM failed_attempts WHERE username = %s", (username.upper(),))
        if cursor.fetchone():
            cursor.execute("UPDATE failed_attempts SET attempts = attempts + 1 WHERE username = %s", (username.upper(),))
        else:
            cursor.execute("INSERT INTO failed_attempts (username, attempts) VALUES (%s, 1)", (username.upper(),))
        conn.commit()

def reset_failed_attempts(username):
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("DELETE FROM failed_attempts WHERE username = %s", (username.upper(),))
        conn.commit()

# === Utilities ===
def load_users():
    with get_mysql_connection() as conn:
        return pd.read_sql("SELECT * FROM users", conn)

def get_all_users(admin_filter=None):
    conn = get_mysql_connection()
    cursor = conn.cursor()

    if admin_filter:
        query = """
            SELECT username, role, start_date, do_code, name, agency_code, admin_username
            FROM users
            WHERE admin_username = %s
        """
        cursor.execute(query, (admin_filter,))
    else:
        query = """
            SELECT username, role, start_date, do_code, name, agency_code, admin_username
            FROM users
        """
        cursor.execute(query)

    results = cursor.fetchall()
    conn.close()
    return results

def update_user_role_and_start(username, new_role, new_start_date):
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute("""
            UPDATE users
            SET role = %s, start_date = %s
            WHERE username = %s
        """, (new_role, new_start_date, username.upper()))
        conn.commit()
        
# === Auto-create DO DB ===
def create_do_database(do_code):
    db_name = f"lic_{do_code.upper()}"

    # Step 1: Create the database
    with get_mysql_connection() as conn:
        cursor = conn.cursor()
        cursor.execute(f"CREATE DATABASE IF NOT EXISTS {db_name}")
        conn.commit()

    # Step 2: Create tables in the new DO DB
    with get_mysql_connection(db_name) as conn:
        cursor = conn.cursor()

        # lic_data
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS lic_data (
                policy_no VARCHAR(50) PRIMARY KEY,
                name VARCHAR(255),
                plan VARCHAR(50),
                mode VARCHAR(50),
                doc DATE,
                ananda VARCHAR(10)
            )
        """)

        # premium_summary
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS premium_summary (
                agency_code VARCHAR(100),
                report_month VARCHAR(20),
                total_premium FLOAT,
                fp_sch_prem FLOAT,
                fy_sch_prem FLOAT,
                uploaded_by VARCHAR(100)
            )
        """)

        # approved_users
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS approved_users (
                username VARCHAR(100) PRIMARY KEY,
                password VARCHAR(100),
                role VARCHAR(50),
                start_date DATE,
                admin_username VARCHAR(100),
                db_name VARCHAR(200),
                do_code VARCHAR(100),
                agency_code VARCHAR(100),
                full_name VARCHAR(255)
            )
        """)
        conn.commit()
