SELECT
    participant_id,
    age_at_baseline as age,
    sex,
    race,
    ethnicity

FROM `${omopDataset}.Demographics` d
