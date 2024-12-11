SELECT DISTINCT vid AS variant_id, flattened_person_id
FROM `${omopDataset}.variant_to_person`)
CROSS JOIN UNNEST(person_ids) AS flattened_person_id
