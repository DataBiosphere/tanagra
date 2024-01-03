SELECT
  de.person_id,
  de.drug_concept_id
FROM `${omopDataset}.drug_exposure` AS de
WHERE de.drug_concept_id
  IN (SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Drug'
        AND c.vocabulary_id = 'HCPCS'
        AND c.standard_concept = 'S'
      UNION DISTINCT
      SELECT concept_id
      FROM `${omopDataset}.concept` c
      WHERE c.domain_id = 'Drug'
        AND c.vocabulary_id = 'CVX'
        AND c.standard_concept = 'S')
