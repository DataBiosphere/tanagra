SELECT
    SUBJID as participant_id,
    CAST(age_at_enrollment as INT64) as age,
    SEX as sex,
    RACE as race

FROM `${omopDataset}.DM` d