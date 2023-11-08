
    SELECT
        t.T_DISP_note AS T_DISP_note,
        t.T_DISP_visit_type AS T_DISP_visit_type,
        t.age_at_occurrence AS age_at_occurrence,
        t.date AS date,
        t.id AS id,
        t.note AS note,
        t.note_text AS note_text,
        t.person_id AS person_id,
        t.source_value AS source_value,
        t.title AS title,
        t.visit_occurrence_id AS visit_occurrence_id,
        t.visit_type AS visit_type 
    FROM
        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_noteOccurrence AS t 
    WHERE
        t.person_id IN (
            SELECT
                t.id 
            FROM
                `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_person AS t 
            WHERE
                t.id IN (
                    SELECT
                        t.person_id 
                    FROM
                        `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_noteOccurrence AS t 
                    WHERE
                        t.note IN (
                            SELECT
                                t.id 
                            FROM
                                `verily-tanagra-dev.sd20230331_index_110623`.T_ENT_note AS t 
                            WHERE
                                t.id = 44814638
                        )
                    )
            ) LIMIT 30
