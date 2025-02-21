SELECT
    SUBJID,
    CAST(age_at_enrollment as INT64) as age_at_enrollment,
    SEX as sex,
    RACE as race

FROM `${omopDataset}.DM` d