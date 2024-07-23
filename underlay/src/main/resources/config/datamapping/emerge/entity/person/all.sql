-- manually inserted
-- insert into `vumc-emerge-dev.em_20240603.demo_criteria`(criteria_meta_seq, parent_seq,name,is_root,is_leaf,label)
-- values('124','115','Resolve P Race_Epic',false,true,'Race_Equals_Resolve P Race_Epic');
-- update `vumc-emerge-dev.em_20240603.demo_criteria` set type='DEMO' where criteria_meta_seq='124'

SELECT
    s.ind_seq,
    CAST(FLOOR(TIMESTAMP_DIFF(CURRENT_TIMESTAMP, s.dob, DAY) / 365.25) AS INT64) AS age,
    CAST(PARSE_NUMERIC(g.criteria_meta_seq) AS INT64) as gender_id,
    g.gender_code,
    g.name as gender_name,
    CAST(PARSE_NUMERIC(r.criteria_meta_seq) AS INT64) as race_id,
    r.race_code,
    r.name as race_name,
    s.site_code,
    sc.site_name,
    CASE WHEN s.dataset_gwas = 1 THEN true ELSE false END AS dataset_gwas,
    CASE WHEN s.dataset_emergeseq = 1 THEN true ELSE false END AS dataset_emergeseq,
    CASE WHEN s.dataset_pgrnseq = 1 THEN true ELSE false END AS dataset_pgrnseq,
    CASE WHEN s.dataset_exomechip = 1 THEN true ELSE false END AS dataset_exomechip,
    CASE WHEN s.dataset_wgs = 1 THEN true ELSE false END AS dataset_wgs,
    CASE WHEN s.dataset_wes = 1 THEN true ELSE false END AS dataset_wes,
    CASE WHEN s.dataset_gira = 1 THEN true ELSE false END AS dataset_gira
FROM `${omopDataset}.sd_record` s
LEFT JOIN (
    SELECT
        criteria_meta_seq,
        CASE WHEN name = 'Asian/Pacific' THEN 'A'
             WHEN name = 'African American' THEN 'B'
             WHEN name = 'Hispanic' THEN 'H'
             WHEN name = 'Native American' THEN 'I'
             WHEN name = 'Other' THEN 'N'
             WHEN name = 'Unknown' THEN 'U'
             WHEN name = 'Caucasian' THEN 'W'
             WHEN name = 'Pacific Islander' THEN 'P'
            END as race_code,
        name
    FROM `${omopDataset}.demo_criteria`
    WHERE starts_with(label,'Race_Equals_')) r on s.race_epic = r.race_code
LEFT JOIN (
    SELECT
        criteria_meta_seq,
        CASE WHEN name = 'Male' THEN 'M'
             WHEN name = 'Female' THEN 'F'
             WHEN name = 'Unknown' THEN 'U'
            END as gender_code,
        name
    FROM `${omopDataset}.demo_criteria`
    WHERE starts_with(label,'Gender_Equals_')) g on s.gender_epic = g.gender_code
LEFT JOIN `${omopDataset}.sites` sc on s.site_code = cast(cast(sc.site as numeric) as int64)
