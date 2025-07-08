SELECT concept_id, concept_synonym_name
FROM `${omopDataset}.concept_synonym`
WHERE REGEXP_CONTAINS(concept_synonym_name, r'[\x00-\x7F]+') --only English characters
