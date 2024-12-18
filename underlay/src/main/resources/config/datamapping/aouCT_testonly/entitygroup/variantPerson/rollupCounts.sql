SELECT vid AS variant_id, ARRAY_LENGTH(person_ids) AS num_persons
/* Wrap variant_to_person table in a SELECT DISTINCT because there is a duplicate row in the test data. */
FROM (SELECT DISTINCT vid, person_ids FROM `${omopDataset}.variant_to_person` WHERE REGEXP_CONTAINS(vid, r"{indexIdRegex}") )
