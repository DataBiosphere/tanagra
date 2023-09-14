SELECT
    ROW_NUMBER() OVER() AS row_id,
    date,
    activity_calories,
    calories_bmr,
    calories_out,
    elevation,
    fairly_active_minutes,
    floors,
    lightly_active_minutes,
    marginal_calories,
    sedentary_minutes,
    steps,
    very_active_minutes,
    person_id
FROM `all-of-us-ehr-dev.SR2023Q3R1.activity_summary`
