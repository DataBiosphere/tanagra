SELECT DISTINCT vid, flattened_person_id
FROM `${omopDataset}.cb_variant_to_person`
CROSS JOIN UNNEST(person_ids) AS flattened_person_id
