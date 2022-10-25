INSERT INTO case_data (uuid, created, type, reference, data, primary_topic_uuid, primary_correspondent_uuid,
                       case_deadline, date_received, deleted)
VALUES ('14915b78-6977-42db-b343-0915a7f412a1', '2018-12-14 12:00:00', 'TEST', 'TEST/9990190/18',
        '{"DateReceived": "2018-01-01"}',
        null, null, '2018-01-29', '2018-01-01', false),
    ('24915b78-6977-42db-b343-0915a7f412a1', '2018-12-14 12:00:00', 'TESTA', 'TESTA/9990190/18',
            '{"DateReceived": "2018-01-01"}',
            null, null, '2018-01-29', '2018-01-01', false),
    ('34915b78-6977-42db-b343-0915a7f412a1', '2018-12-14 12:00:00', 'TESTB', 'TESTB/9990190/18',
            '{"DateReceived": "2018-01-01"}',
            null, null, '2018-01-29', '2018-01-01', false);

INSERT INTO correspondent (uuid, created, type, case_uuid, fullname, postcode, address1, address2, address3, country,
                           telephone, email, reference, deleted, external_key)
VALUES ('2c9e1eb9-ee78-4f57-a626-b8b75cf3b937', '2018-12-14 12:00:00', 'Member', '14915b78-6977-42db-b343-0915a7f412a1',
        'Bob Someone', 'S1 1DJ', '1 SomeWhere Close', 'Sheffield', 'South Yorkshire', 'England', '01142595959',
        'a@a.com', '1', false, 'external_key_1');

INSERT INTO topic (uuid, created, case_uuid, text, text_uuid, deleted)
VALUES ('d472a1a9-d32d-46cb-a08a-56c22637c584', '2018-12-14 12:00:00', '14915b78-6977-42db-b343-0915a7f412a1',
        'SomeText', '66800cca-4e77-4345-85fc-c9624fa255cd', false);

INSERT INTO somu_item (uuid, somu_uuid, case_uuid, data) VALUES
('d472a1a9-d32d-46cb-a08a-56c22637c584', '66800cca-4e77-4345-85fc-c9624fa255cd',
 '14915b78-6977-42db-b343-0915a7f412a1', '{"SomeData": "some_data_1"}');

UPDATE case_data SET primary_topic_uuid = 'd472a1a9-d32d-46cb-a08a-56c22637c584' WHERE uuid = '14915b78-6977-42db-b343-0915a7f412a1';
UPDATE case_data SET primary_correspondent_uuid = '2c9e1eb9-ee78-4f57-a626-b8b75cf3b937' WHERE uuid = '14915b78-6977-42db-b343-0915a7f412a1';


