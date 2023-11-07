SELECT
  ROW_NUMBER() OVER (ORDER BY xfh.uniq) AS family_history_id,
  xfh.person_id

FROM `${omopDataset}.x_family_history` AS xfh
