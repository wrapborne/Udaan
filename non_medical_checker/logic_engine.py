import json

def load_chart_data():
    try:
        with open("non_medical_checker/chart_data.json", "r") as file:
            return json.load(file)
    except FileNotFoundError:
        return {
            "grp_1_plans": ["717", "732", "736", "734", "745", "748", "760", "761", "768", "771", "873"],
            "grp_2_plans": ["714", "715", "720", "721", "733"],
            "limits": {
                "PNM": {
                    "Grp I": [
                        {"age_range": [18, 35], "max_suc": 100},
                        {"age_range": [36, 45], "max_suc": 50},
                        {"age_range": [46, 50], "max_suc": 20}
                    ],
                    "Grp II": [
                        {"age_range": [18, 35], "max_suc": 100},
                        {"age_range": [36, 45], "max_suc": 50},
                        {"age_range": [46, 50], "max_suc": 20}
                    ]
                },
                "SNM": {
                    "Grp I": [
                        {"age_range": [18, 35], "max_suc": 50},
                        {"age_range": [36, 45], "max_suc": 30},
                        {"age_range": [46, 50], "max_suc": 15}
                    ],
                    "Grp II": [
                        {"age_range": [18, 35], "max_suc": 50},
                        {"age_range": [36, 45], "max_suc": 30},
                        {"age_range": [46, 50], "max_suc": 15}
                    ]
                },
                "NMG": {
                    "Grp I": [
                        {"age_range": [18, 35], "max_suc": 12},
                        {"age_range": [36, 45], "max_suc": 8},
                        {"age_range": [46, 50], "max_suc": 5}
                    ],
                    "Grp II": [
                        {"age_range": [18, 35], "max_suc": 12},
                        {"age_range": [36, 45], "max_suc": 8},
                        {"age_range": [46, 50], "max_suc": 5}
                    ]
                },
                "X": {  # Major Student
                    "Grp I": [{"age_range": [18, 30], "max_suc": 30}],
                    "Grp II": [{"age_range": [18, 30], "max_suc": 30}]
                },
                "N": {  # Minor
                    "Grp I": [{"age_range": [0, 4], "max_suc": 40}, {"age_range": [5, 17], "max_suc": 15}],
                    "Grp II": [{"age_range": [0, 4], "max_suc": 40}, {"age_range": [5, 17], "max_suc": 15}]
                }
            }
        }

def determine_category(data, chart_data):
    age = data.get("age", 0)
    income = data.get("income", 0)
    qualification = data.get("qualification", "").lower()
    profession = data.get("profession", "").lower()
    is_resident = data.get("is_resident_indian", True)
    is_minor = data.get("is_minor", False)
    is_student = data.get("is_student", False)

    if is_minor:
        return "N" if age <= 17 else "Ineligible"

    if is_student:
        if qualification in ["hsc / plus 2", "graduate", "post graduate", "professional"] and age <= 30:
            return "X"
        return "Ineligible"

    if not is_resident or income <= 0:
        return "Ineligible"

    is_professional = qualification in [
        "graduate", "post graduate", "professional"
    ] and profession in [
        "professionals", "employed"
    ] and income >= 10_00_000

    if is_professional:
        return "PNM"

    is_special_prof = (
        qualification in ["graduate", "post graduate", "professional", "hsc / plus 2", "ssc / 10th"]
        and income >= 2_50_000
    )

    if profession in ["employed", "professionals"] and is_special_prof:
        return "SNM"

    if profession == "business" and qualification in ["hsc / plus 2", "ssc / 10th"] and income >= 10_00_000:
        return "SNM"

    if income > 0:
        return "NMG"

    return "Ineligible"

def get_sa_limits(category, age, plan_group, chart_data, form_data=None):
    limits = chart_data.get("limits", {}).get(category, {}).get(plan_group, [])
    for entry in limits:
        age_from, age_to = entry["age_range"]
        if age_from <= age <= age_to:
            return entry["max_suc"]
    return 0
