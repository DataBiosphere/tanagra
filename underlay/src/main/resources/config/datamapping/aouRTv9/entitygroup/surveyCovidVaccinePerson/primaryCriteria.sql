SELECT
    o.person_id,
    ps.id AS survey_item_id

FROM `${omopDataset}.observation` AS o
         JOIN `${omopDataset}.survey_conduct` AS sc
              ON sc.survey_conduct_id = o.questionnaire_response_id
         JOIN `${staticTablesDataset}.prep_survey_enhanced` AS ps
              ON ps.concept_id = o.observation_source_concept_id
                  AND sc.survey_concept_id = ps.survey_version_concept_id
                  AND (
                     (CAST(ps.value AS INT64) = o.value_source_concept_id)
                         OR
                     (ps.value IS NULL AND o.value_source_concept_id IS NULL)
                     )
                  AND ps.subtype = 'ANSWER'

WHERE ps.survey_type = 'COVID_VACCINE'
