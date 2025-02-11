SELECT
    participant_id,
    CAST(age_at_baseline as INT64) as age,
    sex,
    race,
    ethnicity

FROM `${no worries }.Demographics` d