SELECT ingredient_occurrence_alias2.drug_exposure_id AS drug_exposure_id, ingredient_occurrence_alias2.person_id AS person_id, ingredient_occurrence_alias2.drug_concept_id AS drug_concept_id, ingredient_occurrence_alias2.drug_exposure_start_date AS drug_exposure_start_date, ingredient_occurrence_alias2.drug_exposure_end_date AS drug_exposure_end_date, ingredient_occurrence_alias2.stop_reason AS stop_reason, ingredient_occurrence_alias2.refills AS refills, ingredient_occurrence_alias2.days_supply AS days_supply, ingredient_occurrence_alias2.visit_occurrence_id AS visit_occurrence_id, ingredient_occurrence_alias2.drug_source_value AS drug_source_value, ingredient_occurrence_alias2.drug_source_concept_id AS drug_source_concept_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.drug_exposure AS ingredient_occurrence_alias2 WHERE ingredient_occurrence_alias2.person_id IN (SELECT person_alias.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.person AS person_alias WHERE person_alias.person_id IN (SELECT ingredient_occurrence_alias1.person_id FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.drug_exposure AS ingredient_occurrence_alias1 WHERE ingredient_occurrence_alias1.drug_concept_id IN (SELECT ingredient_alias.concept_id FROM (SELECT * FROM `broad-tanagra-dev.aou_synthetic_SR2019q4r4`.concept WHERE domain_id = 'Drug' AND valid_end_date > '2022-01-01' AND ((concept_class_id = 'Ingredient' AND standard_concept = 'S' AND (vocabulary_id = 'RxNorm' OR vocabulary_id = 'RxNorm Extension')) OR (vocabulary_id = 'RxNorm' AND concept_class_id = 'Precise Ingredient') OR (vocabulary_id = 'ATC' AND standard_concept = 'C' AND (concept_class_id = 'ATC 1st' OR concept_class_id = 'ATC 2nd' OR concept_class_id = 'ATC 3rd' OR concept_class_id = 'ATC 4th' OR concept_class_id = 'ATC 5th')))) AS ingredient_alias WHERE ingredient_alias.concept_id = 1177480)))
