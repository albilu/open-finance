-- V50: Add i18n name_key column to categories table
-- Phase 3.1 of i18n & Localization feature
-- name_key stores the messages.properties key for system categories so their
-- display name can be resolved in the user's preferred language.

ALTER TABLE categories ADD COLUMN name_key VARCHAR(100);

-- Backfill name_key for all system categories using their English name as the lookup.
-- These names match exactly what CategorySeeder inserts.

-- INCOME categories
UPDATE categories SET name_key = 'category.employment.income' WHERE name = 'Employment Income' AND is_system = TRUE;
UPDATE categories SET name_key = 'category.salary'             WHERE name = 'Salary'             AND is_system = TRUE;
UPDATE categories SET name_key = 'category.bonus'              WHERE name = 'Bonus'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.commission'         WHERE name = 'Commission'         AND is_system = TRUE;
UPDATE categories SET name_key = 'category.self.employment'    WHERE name = 'Self Employment'    AND is_system = TRUE;
UPDATE categories SET name_key = 'category.freelance.work'     WHERE name = 'Freelance Work'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.consulting'         WHERE name = 'Consulting'         AND is_system = TRUE;
UPDATE categories SET name_key = 'category.investments'        WHERE name = 'Investments'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.dividends'          WHERE name = 'Dividends'          AND is_system = TRUE;
UPDATE categories SET name_key = 'category.interest.income'    WHERE name = 'Interest Income'    AND is_system = TRUE;
UPDATE categories SET name_key = 'category.capital.gains'      WHERE name = 'Capital Gains'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.rental.income'      WHERE name = 'Rental Income'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.residential.rent.income' WHERE name = 'Residential Rent' AND is_system = TRUE;
UPDATE categories SET name_key = 'category.commercial.rent.income'  WHERE name = 'Commercial Rent'  AND is_system = TRUE;
UPDATE categories SET name_key = 'category.government.benefits' WHERE name = 'Government Benefits' AND is_system = TRUE;
UPDATE categories SET name_key = 'category.pension'            WHERE name = 'Pension'            AND is_system = TRUE;
UPDATE categories SET name_key = 'category.social.security'    WHERE name = 'Social Security'    AND is_system = TRUE;
UPDATE categories SET name_key = 'category.unemployment'       WHERE name = 'Unemployment'       AND is_system = TRUE;
UPDATE categories SET name_key = 'category.retirement.income'  WHERE name = 'Retirement Income'  AND is_system = TRUE;
UPDATE categories SET name_key = 'category.401k.withdrawal'    WHERE name = '401k Withdrawal'    AND is_system = TRUE;
UPDATE categories SET name_key = 'category.ira.withdrawal'     WHERE name = 'IRA Withdrawal'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.gifts'              WHERE name = 'Gifts'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.inheritance'        WHERE name = 'Inheritance'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.other.income'       WHERE name = 'Other Income'       AND is_system = TRUE;

-- EXPENSE categories
UPDATE categories SET name_key = 'category.groceries'          WHERE name = 'Groceries'          AND is_system = TRUE;
UPDATE categories SET name_key = 'category.supermarkets'       WHERE name = 'Supermarkets'       AND is_system = TRUE;
UPDATE categories SET name_key = 'category.convenience.stores' WHERE name = 'Convenience Stores' AND is_system = TRUE;
UPDATE categories SET name_key = 'category.organic.foods'      WHERE name = 'Organic Foods'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.dining.out'         WHERE name = 'Dining Out'         AND is_system = TRUE;
UPDATE categories SET name_key = 'category.fast.food'          WHERE name = 'Fast Food'          AND is_system = TRUE;
UPDATE categories SET name_key = 'category.casual.dining'      WHERE name = 'Casual Dining'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.fine.dining'        WHERE name = 'Fine Dining'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.coffee.shops'       WHERE name = 'Coffee Shops'       AND is_system = TRUE;
UPDATE categories SET name_key = 'category.bars.nightlife'     WHERE name = 'Bars and Nightlife' AND is_system = TRUE;
UPDATE categories SET name_key = 'category.auto.expenses'      WHERE name = 'Auto Expenses'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.gas.fuel'           WHERE name = 'Gas/Fuel'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.auto.maintenance'   WHERE name = 'Auto Maintenance'   AND is_system = TRUE;
UPDATE categories SET name_key = 'category.auto.insurance'     WHERE name = 'Auto Insurance'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.parking'            WHERE name = 'Parking'            AND is_system = TRUE;
UPDATE categories SET name_key = 'category.tolls'              WHERE name = 'Tolls'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.car.payment'        WHERE name = 'Car Payment'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.public.transit'     WHERE name = 'Public Transit'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.bus.metro'          WHERE name = 'Bus/Metro'          AND is_system = TRUE;
UPDATE categories SET name_key = 'category.taxi.rideshare'     WHERE name = 'Taxi/Rideshare'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.train'              WHERE name = 'Train'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.air.travel'         WHERE name = 'Air Travel'         AND is_system = TRUE;
UPDATE categories SET name_key = 'category.airlines'           WHERE name = 'Airlines'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.hotels'             WHERE name = 'Hotels'             AND is_system = TRUE;
UPDATE categories SET name_key = 'category.shopping'           WHERE name = 'Shopping'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.department.stores'  WHERE name = 'Department Stores'  AND is_system = TRUE;
UPDATE categories SET name_key = 'category.clothing'           WHERE name = 'Clothing'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.electronics'        WHERE name = 'Electronics'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.home.goods'         WHERE name = 'Home Goods'         AND is_system = TRUE;
UPDATE categories SET name_key = 'category.entertainment'      WHERE name = 'Entertainment'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.movies'             WHERE name = 'Movies'             AND is_system = TRUE;
UPDATE categories SET name_key = 'category.music'              WHERE name = 'Music'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.gaming'             WHERE name = 'Gaming'             AND is_system = TRUE;
UPDATE categories SET name_key = 'category.events'             WHERE name = 'Events'             AND is_system = TRUE;
UPDATE categories SET name_key = 'category.subscriptions'      WHERE name = 'Subscriptions'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.streaming.services' WHERE name = 'Streaming Services' AND is_system = TRUE;
UPDATE categories SET name_key = 'category.software'           WHERE name = 'Software'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.memberships'        WHERE name = 'Memberships'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.housing'            WHERE name = 'Housing'            AND is_system = TRUE;
UPDATE categories SET name_key = 'category.rent'               WHERE name = 'Rent'               AND is_system = TRUE;
UPDATE categories SET name_key = 'category.mortgage'           WHERE name = 'Mortgage'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.property.tax'       WHERE name = 'Property Tax'       AND is_system = TRUE;
UPDATE categories SET name_key = 'category.utilities'          WHERE name = 'Utilities'          AND is_system = TRUE;
UPDATE categories SET name_key = 'category.electricity'        WHERE name = 'Electricity'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.water'              WHERE name = 'Water'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.gas.heating'        WHERE name = 'Gas/Heating'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.internet'           WHERE name = 'Internet'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.phone'              WHERE name = 'Phone'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.cable.tv'           WHERE name = 'Cable TV'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.healthcare'         WHERE name = 'Healthcare'         AND is_system = TRUE;
UPDATE categories SET name_key = 'category.doctor.visits'      WHERE name = 'Doctor Visits'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.dental'             WHERE name = 'Dental'             AND is_system = TRUE;
UPDATE categories SET name_key = 'category.vision'             WHERE name = 'Vision'             AND is_system = TRUE;
UPDATE categories SET name_key = 'category.prescriptions'      WHERE name = 'Prescriptions'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.health.insurance'   WHERE name = 'Health Insurance'   AND is_system = TRUE;
UPDATE categories SET name_key = 'category.insurance'          WHERE name = 'Insurance'          AND is_system = TRUE;
UPDATE categories SET name_key = 'category.life.insurance'     WHERE name = 'Life Insurance'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.home.insurance'     WHERE name = 'Home Insurance'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.disability.insurance' WHERE name = 'Disability Insurance' AND is_system = TRUE;
UPDATE categories SET name_key = 'category.education'          WHERE name = 'Education'          AND is_system = TRUE;
UPDATE categories SET name_key = 'category.tuition'            WHERE name = 'Tuition'            AND is_system = TRUE;
UPDATE categories SET name_key = 'category.books.supplies'     WHERE name = 'Books/Supplies'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.online.courses'     WHERE name = 'Online Courses'     AND is_system = TRUE;
UPDATE categories SET name_key = 'category.personal.care'      WHERE name = 'Personal Care'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.haircut.salon'      WHERE name = 'Haircut/Salon'      AND is_system = TRUE;
UPDATE categories SET name_key = 'category.spa'                WHERE name = 'Spa'                AND is_system = TRUE;
UPDATE categories SET name_key = 'category.gym.fitness'        WHERE name = 'Gym/Fitness'        AND is_system = TRUE;
UPDATE categories SET name_key = 'category.pets'               WHERE name = 'Pets'               AND is_system = TRUE;
UPDATE categories SET name_key = 'category.pet.food'           WHERE name = 'Pet Food'           AND is_system = TRUE;
UPDATE categories SET name_key = 'category.veterinary'         WHERE name = 'Veterinary'         AND is_system = TRUE;
UPDATE categories SET name_key = 'category.charity'            WHERE name = 'Charity'            AND is_system = TRUE;
UPDATE categories SET name_key = 'category.taxes'              WHERE name = 'Taxes'              AND is_system = TRUE;
UPDATE categories SET name_key = 'category.other.expenses'     WHERE name = 'Other Expenses'     AND is_system = TRUE;
