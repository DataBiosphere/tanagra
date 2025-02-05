SELECT
    SUBJID as participant_id,
    CAST(age_at_enrollment as INT64) as age,
    SEX,
    RACE

FROM `${omopDataset}.DM` d