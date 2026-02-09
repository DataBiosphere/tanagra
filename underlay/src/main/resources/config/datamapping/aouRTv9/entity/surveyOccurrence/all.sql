-- wrap around and use row_number as observation_id: test data has duplicates
-- with same observation_id in Cope Surveys & Cope Minute Vaccine Surveys
SELECT
    ROW_NUMBER() OVER() AS observation_id,
        person_id,
    survey_datetime,
    value_as_number,
    survey_item_id,
    survey_concept_id,
    survey_concept_name,
    question_concept_id,
    question_concept_name,
    answer_concept_id,
    answer_concept_name,
    survey_version_concept_id,
    survey_version_name
FROM (
         SELECT distinct
             o.observation_id,
             o.person_id,
             o.observation_datetime as survey_datetime,
             o.value_as_number,
             ps.id AS survey_item_id,
             ps_survey.concept_id AS survey_concept_id,
             sc.concept_name AS survey_concept_name,
             o.observation_source_concept_id AS question_concept_id,
             qc.name AS question_concept_name,
             o.value_source_concept_id AS answer_concept_id,
             ac.name AS answer_concept_name,
             sv.survey_concept_id as survey_version_concept_id,
             svc.concept_name as survey_version_name
         FROM `${omopDataset}.observation` AS o
                  JOIN `${staticTablesDataset}.prep_survey` AS ps
                       ON ps.concept_id = o.observation_source_concept_id
                           AND (
                              (CAST(ps.value AS INT64) = o.value_source_concept_id)
                                  OR
                              (ps.value IS NULL AND o.value_source_concept_id IS NULL)
                              )
                           AND ps.subtype = 'ANSWER'
                  JOIN `${staticTablesDataset}.prep_survey` AS ps_survey
                       ON ps_survey.survey = ps.survey
                           AND ps_survey.subtype = 'SURVEY'
                  LEFT JOIN `${omopDataset}.concept` AS sc
                            ON sc.concept_id = ps_survey.concept_id
                  LEFT JOIN `${staticTablesDataset}.prep_survey` AS qc
                            ON qc.concept_id = o.observation_source_concept_id
                                AND qc.subtype = 'QUESTION'
                  LEFT JOIN `${staticTablesDataset}.prep_survey` AS ac
                            ON ac.concept_id = o.observation_source_concept_id
                                AND CAST(ac.value AS INT64) = o.value_source_concept_id
                                AND ac.subtype = 'ANSWER'
                  LEFT JOIN `${omopDataset}.survey_conduct` AS sv
                            ON sv.survey_conduct_id = o.questionnaire_response_id
                  LEFT JOIN `${omopDataset}.concept` AS svc
                            ON svc.concept_id = sv.survey_concept_id
         WHERE ps.survey IN ('Basics',
                             'Lifestyle',
                             'OverallHealth',
                             'HealthCareAccessUtiliza',
                             'SocialDeterminantsOfHea')

         UNION ALL

         -- PFHH Survey
         SELECT
             ROW_NUMBER() OVER() AS observation_id,
                 po.person_id,
             po.observation_datetime as survey_datetime,
             po.value_as_number,
             ps.id AS survey_item_id,
             ps_survey.concept_id AS survey_concept_id,
             sc.concept_name AS survey_concept_name,
             po.observation_source_concept_id AS question_concept_id,
             qc.name AS question_concept_name,
             po.value_source_concept_id AS answer_concept_id,
             ac.name AS answer_concept_name,
             NULL as survey_version_concept_id,
             CAST(NULL as string) as survey_version_name
         FROM `${staticTablesDataset}.prep_pfhh_observation` AS po
                  JOIN `${staticTablesDataset}.prep_survey` AS ps
                       ON ps.concept_id = po.observation_source_concept_id
                           AND (
                              (CAST(ps.value AS INT64) = po.value_source_concept_id)
                                  OR
                              (ps.value IS NULL AND po.value_source_concept_id IS NULL)
                              )
                           AND ps.subtype = 'ANSWER'
                  JOIN `${staticTablesDataset}.prep_survey` AS ps_survey
                       ON ps_survey.survey = ps.survey
                           AND ps_survey.subtype = 'SURVEY'
                  LEFT JOIN `${omopDataset}.concept` AS sc
                            ON sc.concept_id = ps_survey.concept_id
                  LEFT JOIN `${staticTablesDataset}.prep_survey` AS qc
                            ON qc.concept_id = po.observation_source_concept_id
                                AND qc.subtype = 'QUESTION'
                  LEFT JOIN `${staticTablesDataset}.prep_survey` AS ac
                            ON ac.concept_id = po.observation_source_concept_id
                                AND CAST(ac.value AS INT64) = po.value_source_concept_id
                                AND ac.subtype = 'ANSWER'
         WHERE ps.survey = 'PersonalAndFamilyHealth'

         UNION ALL

         -- Cope Surveys & Cope Minute Vaccine Surveys (using enhanced table)
         SELECT distinct
             o.observation_id,
             o.person_id,
             o.observation_datetime as survey_datetime,
             o.value_as_number,
             ps.id AS survey_item_id,
             ps_survey.concept_id AS survey_concept_id,
             CASE
                 WHEN ps.survey_type = 'COVID_VACCINE' THEN 'COVID-19 Vaccine Survey'
                 WHEN ps.survey_type = 'COPE' THEN 'COVID-19 Participant Experience (COPE) Survey'
                 ELSE ps_survey.name
                 END AS survey_concept_name,
             o.observation_source_concept_id AS question_concept_id,
             ps_question.name AS question_concept_name,
             o.value_source_concept_id AS answer_concept_id,
             ps_answer.name AS answer_concept_name,
             ps.survey_version_concept_id as survey_version_concept_id,
             ps.survey_version_name as survey_version_name
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
                           AND ps.survey_type IN ('COPE', 'COVID_VACCINE')
                  JOIN `${staticTablesDataset}.prep_survey_enhanced` AS ps_survey
                       ON ps_survey.survey = ps.survey
                           AND ps_survey.subtype = 'SURVEY'
                  LEFT JOIN `${staticTablesDataset}.prep_survey_enhanced` AS ps_question
                            ON ps_question.concept_id = o.observation_source_concept_id
                                AND ps_question.subtype = 'QUESTION'
                                AND ps_question.survey = ps.survey
                  LEFT JOIN `${staticTablesDataset}.prep_survey_enhanced` AS ps_answer
                            ON ps_answer.concept_id = o.observation_source_concept_id
                                AND CAST(ps_answer.value AS INT64) = o.value_source_concept_id
                                AND ps_answer.subtype = 'ANSWER'
                                AND ps_answer.survey = ps.survey
     )
