SELECT
  ROW_NUMBER() OVER (ORDER BY xbp.person_id, xbp.measurement_datetime) AS id,
  xbp.person_id

FROM `${omopDataset}.x_blood_pressure` AS xbp
