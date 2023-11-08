
    SELECT
        t.T_DISP_ingredient AS T_DISP_ingredient,
        t.T_DISP_visit_type AS T_DISP_visit_type,
        t.age_at_occurrence AS age_at_occurrence,
        t.days_supply AS days_supply,
        t.end_date AS end_date,
        t.id AS id,
        t.ingredient AS ingredient,
        t.person_id AS person_id,
        t.refills AS refills,
        t.source_criteria_id AS source_criteria_id,
        t.source_value AS source_value,
        t.start_date AS start_date,
        t.stop_reason AS stop_reason,
        t.visit_occurrence_id AS visit_occurrence_id,
        t.visit_type AS visit_type 
    FROM
        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_ingredientOccurrence AS t 
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
                        `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_ingredientOccurrence AS t 
                    WHERE
                        t.ingredient IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.aouSR2019q4r4_index_110623`.T_ENT_ingredient AS t 
                            WHERE
                                t.id = 1177480
                        )
                    )
            ) LIMIT 30
