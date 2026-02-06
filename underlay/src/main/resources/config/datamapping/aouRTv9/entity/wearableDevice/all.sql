SELECT
    row_id,
    person_id,
    device_id,
    device_date,
    battery,
    battery_level,
    device_version,
    device_type,
    TIMESTAMP(last_sync_time) AS last_sync_time,
    src_id
FROM `${omopDataset}.device`
