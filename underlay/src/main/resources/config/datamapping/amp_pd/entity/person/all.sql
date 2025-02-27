SELECT
    d.participant_id,
    CAST(d.age_at_baseline as INT64) as age,
    d.sex,
    d.race,
    d.ethnicity,
    c.diagnosis_at_baseline,
    c.diagnosis_latest,
    c.case_control_other_at_baseline,
    c.case_control_other_latest
FROM `${omopDataset}.Demographics` d
LEFT JOIN `${omopDataset}.amp_pd_case_control` c on c.participant_id=d.participant_id