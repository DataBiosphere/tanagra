SELECT parent_id AS parent, id AS child
FROM `${omopDataset}.prep_survey`
WHERE parent_id != 0 AND survey = 'SocialDeterminantsOfHea'
