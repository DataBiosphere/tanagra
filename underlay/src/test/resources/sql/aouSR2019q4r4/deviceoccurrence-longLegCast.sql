
    SELECT
        t.T_DISP_device AS T_DISP_device,
        t.age_at_occurrence AS age_at_occurrence,
        t.device AS device,
        t.end_date AS end_date,
        t.id AS id,
        t.person_id AS person_id,
        t.source_criteria_id AS source_criteria_id,
        t.source_value AS source_value,
        t.start_date AS start_date,
        t.visit_occurrence_id AS visit_occurrence_id 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_deviceOccurrence AS t 
    WHERE
        t.person_id IN (
            SELECT
                t.id 
            FROM
                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_person AS t 
            WHERE
                t.id IN (
                    SELECT
                        t.person_id 
                    FROM
                        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_deviceOccurrence AS t 
                    WHERE
                        t.device IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_device AS t 
                            WHERE
                                t.id = 4038664
                        )
                    )
            ) LIMIT 30
