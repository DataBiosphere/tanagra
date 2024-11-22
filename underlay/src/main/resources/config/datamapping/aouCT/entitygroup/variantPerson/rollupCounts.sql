SELECT vid as id, ARRAY_LENGTH(person_ids) AS num_persons
FROM (SELECT DISTINCT vid, person_ids FROM `${omopDataset}.cb_variant_to_person`)
