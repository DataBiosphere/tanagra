
    SELECT
        e.T_DISP_ingredient AS T_DISP_ingredient,
        e.age_at_occurrence AS age_at_occurrence,
        e.days_supply AS days_supply,
        e.end_date AS end_date,
        e.id AS id,
        e.ingredient AS ingredient,
        e.person_id AS person_id,
        e.refills AS refills,
        e.source_criteria_id AS source_criteria_id,
        e.source_value AS source_value,
        e.start_date AS start_date,
        e.stop_reason AS stop_reason,
        e.visit_occurrence_id AS visit_occurrence_id 
    FROM
        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_ingredientOccurrence AS e 
    WHERE
        e.person_id IN (
            SELECT
                e.id 
            FROM
                `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_person AS e 
            WHERE
                e.id IN (
                    SELECT
                        e.person_id 
                    FROM
                        `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_ingredientOccurrence AS e 
                    WHERE
                        e.ingredient IN (
                            SELECT
                                e.id 
                            FROM
                                `verily-tanagra-dev.cmssynpuf_index_110623`.ENT_ingredient AS e 
                            WHERE
                                e.id = 1177480
                        )
                    )
            ) LIMIT 30
