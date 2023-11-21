SELECT io.drug_exposure_id, pc.id AS cpt4_id
FROM `${omopDataset}.drug_exposure` AS io

JOIN `${staticTablesDataset}.prep_cpt` AS pc
    ON pc.concept_id = io.drug_source_concept_id

WHERE pc.type = 'CPT4'
