SELECT
    d.SUBJID,
    CAST(d.age_at_enrollment as INT64) as age_at_enrollment,
    d.SEX as sex,
    d.RACE as race,
    phq.VISIT,
    phq.questionnaire_version,
    phq.phq9_1,
    phq.phq9_2,
    phq.phq9_3,
    phq.phq9_4

FROM `${omopDataset}.DM` d

LEFT JOIN (
    SELECT
        p.*
    FROM
        `${omopDataset}.PHQ9A` p
    JOIN
        (SELECT SUBJID, MAX(VISITNUM) AS MaxVisit FROM `${omopDataset}.PHQ9A` GROUP BY SUBJID) AS max_visits
    ON
        p.SUBJID = max_visits.SUBJID AND p.VISITNUM = max_visits.MaxVisit
) phq
ON phq.SUBJID = d.SUBJID

