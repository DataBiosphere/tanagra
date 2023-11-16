
    SELECT
        t.T_DISP_condition AS T_DISP_condition,
        t.T_DISP_visit_type AS T_DISP_visit_type,
        t.age_at_occurrence AS age_at_occurrence,
        t.condition AS condition,
        t.end_date AS end_date,
        t.id AS id,
        t.person_id AS person_id,
        t.source_criteria_id AS source_criteria_id,
        t.source_value AS source_value,
        t.start_date AS start_date,
        t.stop_reason AS stop_reason,
        t.visit_occurrence_id AS visit_occurrence_id,
        t.visit_type AS visit_type 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_conditionOccurrence AS t 
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
                        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_conditionOccurrence AS t 
                    WHERE
                        t.condition IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_condition AS t 
                            WHERE
                                t.id = 201826
                        )
                    )
            ) LIMIT 30