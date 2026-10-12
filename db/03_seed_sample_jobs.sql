-- Sample companies for the Jobs page. Names and numbers are MADE UP to show the layout.
-- Every row has is_sample = true so the page can label it. Remove these before launch.

INSERT INTO companies (name, sector, city, weather, openings, avg_days, toughness, fresher_band, mid_band, referral, notice, roles, is_sample) VALUES
('Howrah Bridge Systems',   'IT services', 'Kolkata',      'SUNNY',  42, 18, 3, 'Rs 3.5 to 5 LPA', 'Rs 9 to 16 LPA',  'Rs 15,000 after 90 days',  '60 days',                'Backend, Testing, Support', true),
('Tram & Tracks Fintech',   'Fintech',     'Bengaluru',    'CLOUDY', 11, 25, 4, 'Rs 6 to 9 LPA',   'Rs 18 to 30 LPA', 'Rs 30,000 after 6 months', '90 days, buyout possible', 'Data, Payments, Risk',    true),
('Ilish Foods Pvt Ltd',     'FMCG',        'Kolkata',      'FROZEN',  0, 30, 2, 'Rs 4 to 6 LPA',   'Rs 10 to 14 LPA', 'None',                     '30 days',                'Sales, Supply chain',      true),
('Rosogolla Retail Tech',   'E-commerce',  'Remote first', 'STORMY',  6, 12, 2, 'Rs 3 to 4.5 LPA', 'Rs 8 to 12 LPA',  'Rs 10,000 after 90 days',  '30 days',                'Operations, Design',       true),
('Sandesh Health Labs',     'Healthtech',  'Pune',         'SUNNY',  28, 21, 3, 'Rs 4 to 6 LPA',   'Rs 10 to 18 LPA', 'Rs 25,000 after 6 months', '60 days',                'Clinical data, QA, Sales', true),
('Puchka Games Studio',     'Gaming',      'Hyderabad',    'CLOUDY',  9, 16, 3, 'Rs 3.5 to 6 LPA', 'Rs 10 to 20 LPA', 'None',                     '45 days',                'Game dev, Art, QA',        true);

INSERT INTO company_steps (company_id, ord, name, detail)
SELECT c.id, s.ord, s.name, s.detail FROM companies c JOIN (VALUES
 ('Howrah Bridge Systems', 1, 'Online test',          'Aptitude and basic coding. Day 1 to 3.'),
 ('Howrah Bridge Systems', 2, 'Technical interview',  'One engineer, 45 minutes. Day 5 to 8.'),
 ('Howrah Bridge Systems', 3, 'Manager chat',         'Team fit and past projects. Day 10 to 12.'),
 ('Howrah Bridge Systems', 4, 'HR and offer',         'Documents and salary talk. Day 14 to 18.'),
 ('Tram & Tracks Fintech', 1, 'Screening call',       'A recruiter and 20 minutes of questions.'),
 ('Tram & Tracks Fintech', 2, 'Take-home task',       'Three days given to finish it.'),
 ('Tram & Tracks Fintech', 3, 'Panel, two rounds',    'System design and a values chat.'),
 ('Tram & Tracks Fintech', 4, 'Offer',                'Salary talk only after the panel.'),
 ('Ilish Foods Pvt Ltd',   1, 'Campus drive',         'Most hires come in through campuses.'),
 ('Ilish Foods Pvt Ltd',   2, 'Group discussion',     'Current affairs and a case.'),
 ('Ilish Foods Pvt Ltd',   3, 'Interview',            'Regional manager, 30 minutes.'),
 ('Ilish Foods Pvt Ltd',   4, 'Offer',                'Letter in about a week.'),
 ('Rosogolla Retail Tech', 1, 'Portfolio or case',    'Send past work or solve a small case.'),
 ('Rosogolla Retail Tech', 2, 'Interview',            'Two people, one hour.'),
 ('Rosogolla Retail Tech', 3, 'Founder chat',         'Short and decisive.'),
 ('Rosogolla Retail Tech', 4, 'Offer',                'Within two days of the chat.'),
 ('Sandesh Health Labs',   1, 'Screening',            'Resume check and a phone call.'),
 ('Sandesh Health Labs',   2, 'Domain test',          'Healthcare basics, 60 minutes.'),
 ('Sandesh Health Labs',   3, 'Panel',                'Two senior people.'),
 ('Sandesh Health Labs',   4, 'HR round',             'Notice period and joining date.'),
 ('Sandesh Health Labs',   5, 'Background check',     'About a week.'),
 ('Sandesh Health Labs',   6, 'Offer',                'After the check clears.'),
 ('Puchka Games Studio',   1, 'Portfolio review',     'Show shipped or finished work.'),
 ('Puchka Games Studio',   2, 'Tech or art test',     'Five days to finish.'),
 ('Puchka Games Studio',   3, 'Team fit chat',        'Meet the people you would work with.'),
 ('Puchka Games Studio',   4, 'Offer',                'Fast once the team says yes.')
) AS s(company, ord, name, detail) ON s.company = c.name;

INSERT INTO company_whispers (company_id, body, pakki, gappa)
SELECT c.id, w.body, w.pakki, w.gappa FROM companies c JOIN (VALUES
 ('Howrah Bridge Systems', 'The online test is mostly aptitude. Practise speed, not tricks.', 61, 7),
 ('Howrah Bridge Systems', 'Offers usually go out on Fridays after 5pm.', 34, 22),
 ('Tram & Tracks Fintech', 'The take-home task takes closer to 8 hours than the 3 they promise.', 80, 6),
 ('Tram & Tracks Fintech', 'Nobody discusses salary before the panel round.', 45, 10),
 ('Ilish Foods Pvt Ltd',   'Hiring is paused till January. Only replacements.', 55, 20),
 ('Ilish Foods Pvt Ltd',   'The campus drive happens in January, same as last year.', 40, 15),
 ('Rosogolla Retail Tech', 'Restructuring whispers are going around. Ask about runway before accepting.', 38, 41),
 ('Rosogolla Retail Tech', 'The founder chat is friendly and quick.', 62, 5),
 ('Sandesh Health Labs',   'The background check is slow. Do not resign before it clears.', 72, 4),
 ('Sandesh Health Labs',   'They like candidates who ask about patient data privacy.', 29, 18),
 ('Puchka Games Studio',   'Finished small projects beat big unfinished ones in the portfolio round.', 66, 8),
 ('Puchka Games Studio',   'Crunch before launch is real. Ask about it directly.', 58, 12)
) AS w(company, body, pakki, gappa) ON w.company = c.name;
