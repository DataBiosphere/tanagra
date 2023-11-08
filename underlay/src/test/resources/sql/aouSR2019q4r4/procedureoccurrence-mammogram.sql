
    SELECT
        t.T_DISP_procedure AS T_DISP_procedure,
        t.T_DISP_visit_type AS T_DISP_visit_type,
        t.age_at_occurrence AS age_at_occurrence,
        t.date AS date,
        t.id AS id,
        t.person_id AS person_id,
        t.procedure AS procedure,
        t.source_criteria_id AS source_criteria_id,
        t.source_value AS source_value,
        t.visit_occurrence_id AS visit_occurrence_id,
        t.visit_type AS visit_type 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_procedureOccurrence AS t 
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
                        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_procedureOccurrence AS t 
                    WHERE
                        t.procedure IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_procedure AS t 
                            WHERE
                                t.id = 4324693
                        )
                    )
            ) LIMIT 30
