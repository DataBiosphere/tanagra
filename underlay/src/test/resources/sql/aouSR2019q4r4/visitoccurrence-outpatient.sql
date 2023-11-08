
    SELECT
        t.T_DISP_visit AS T_DISP_visit,
        t.age_at_occurrence AS age_at_occurrence,
        t.end_date AS end_date,
        t.id AS id,
        t.person_id AS person_id,
        t.source_criteria_id AS source_criteria_id,
        t.source_value AS source_value,
        t.start_date AS start_date,
        t.visit AS visit 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_visitOccurrence AS t 
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
                        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_visitOccurrence AS t 
                    WHERE
                        t.visit IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_visit AS t 
                            WHERE
                                t.id = 9202
                        )
                    )
            ) LIMIT 30
