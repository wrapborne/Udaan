import streamlit as st
import json
import os

CHART_PATH = os.path.join(os.path.dirname(__file__), "chart_data.json")

def show_upload_ui():
    st.header("📤 Upload or Edit Non-Medical Chart Logic")

    st.markdown("### Option 1: Upload New JSON Logic File")
    uploaded_file = st.file_uploader("Upload a JSON file to replace existing chart logic", type=["json"])

    if uploaded_file is not None:
        try:
            new_data = json.load(uploaded_file)
            with open(CHART_PATH, "w") as f:
                json.dump(new_data, f, indent=4)
            st.success("✅ Chart logic updated successfully.")
        except Exception as e:
            st.error(f"❌ Failed to process uploaded file: {e}")

    st.markdown("---")
    st.markdown("### Option 2: Edit Current Chart Logic Manually")

    try:
        with open(CHART_PATH, "r") as f:
            current_data = json.load(f)
        editable_text = st.text_area("Current Logic (edit with caution)", json.dumps(current_data, indent=4), height=400)

        if st.button("💾 Save Changes"):
            try:
                parsed = json.loads(editable_text)
                with open(CHART_PATH, "w") as f:
                    json.dump(parsed, f, indent=4)
                st.success("✅ Chart logic saved.")
            except json.JSONDecodeError as e:
                st.error(f"❌ Invalid JSON: {e}")
    except FileNotFoundError:
        st.warning("⚠️ Chart logic file not found.")
