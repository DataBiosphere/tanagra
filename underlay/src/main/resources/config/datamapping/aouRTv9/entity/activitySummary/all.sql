SELECT
    row_id,
    person_id,
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
    very_active_minutes
FROM `${omopDataset}.activity_summary`
