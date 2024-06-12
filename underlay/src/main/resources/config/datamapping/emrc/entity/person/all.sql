-- manually inserted
-- insert into `vumc-emerge-dev.em_20240603.demo_criteria`(criteria_meta_seq, parent_seq,name,is_root,is_leaf,label)
-- values('124','115','Resolve P Race_Epic',false,true,'Race_Equals_Resolve P Race_Epic');
-- update `vumc-emerge-dev.em_20240603.demo_criteria` set type='DEMO' where criteria_meta_seq='124'

SELECT
    p.ind_seq,
    CAST(p.age AS INT64) AS age,
    cast(cast(g.criteria_meta_seq as numeric) as int64) as gender_id,
    g.gender_code,
    g.name as gender_name,
    cast(cast(r.criteria_meta_seq as numeric) as int64) as race_id,
    r.race_code,
    concat(r.race_code,'-',r.name) as race_name,
    CASE WHEN s.dataset_gwas = 1 THEN true ELSE false END AS dataset_gwas,
    CASE WHEN s.dataset_emergeseq = 1 THEN true ELSE false END AS dataset_emergeseq,
    CASE WHEN s.dataset_pgrnseq = 1 THEN true ELSE false END AS dataset_pgrnseq,
    CASE WHEN s.dataset_exomechip = 1 THEN true ELSE false END AS dataset_exomechip,
    CASE WHEN s.dataset_wgs = 1 THEN true ELSE false END AS dataset_wgs,
    CASE WHEN s.dataset_wes = 1 THEN true ELSE false END AS dataset_wes,
    false as dataset_gira
FROM `${omopDataset}.person_demo` p
LEFT JOIN `${omopDataset}.sd_record` s ON p.ind_seq = s.ind_seq
LEFT JOIN (select
    criteria_meta_seq,
    CASE WHEN name = 'Asian/Pacific' THEN 'A'
         WHEN name = 'African American' THEN 'B'
         WHEN name = 'Hispanic' THEN 'H'
         WHEN name = 'Native American' THEN 'I'
         WHEN name = 'Other' THEN 'N'
         WHEN name = 'Unknown' THEN 'U'
         WHEN name = 'Caucasian' THEN 'W'
         WHEN name = 'Resolve P Race_Epic' THEN 'P'
        END as race_code,
    name
FROM `${omopDataset}.demo_criteria` WHERE starts_with(label,'Race_Equals_')) r on p.race = r.race_code
LEFT JOIN (select
    criteria_meta_seq,
    CASE WHEN name = 'Male' THEN 'M'
         WHEN name = 'Female' THEN 'F'
         WHEN name = 'Unknown' THEN 'U'
        END as gender_code,
    name
FROM `${omopDataset}.demo_criteria` WHERE starts_with(label,'Gender_Equals_')) g on p.gender = g.gender_code
