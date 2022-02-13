SELECT device_occurrence_alias2.device_exposure_id AS device_exposure_id, device_occurrence_alias2.person_id AS person_id, device_occurrence_alias2.device_concept_id AS device_concept_id, device_occurrence_alias2.device_exposure_start_date AS device_exposure_start_date, device_occurrence_alias2.device_exposure_end_date AS device_exposure_end_date, device_occurrence_alias2.visit_occurrence_id AS visit_occurrence_id, device_occurrence_alias2.device_source_value AS device_source_value, device_occurrence_alias2.device_source_concept_id AS device_source_concept_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.device_exposure AS device_occurrence_alias2 WHERE device_occurrence_alias2.person_id IN (SELECT person_alias.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.person AS person_alias WHERE person_alias.person_id IN (SELECT device_occurrence_alias1.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.device_exposure AS device_occurrence_alias1 WHERE device_occurrence_alias1.device_concept_id IN (SELECT device_alias.concept_id FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Device' AND standard_concept = 'S') AS device_alias WHERE device_alias.concept_id = 4038664)))
