CREATE TABLE IF NOT EXISTS users(id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), email VARCHAR(150) UNIQUE, password_hash VARCHAR(100));
CREATE TABLE IF NOT EXISTS shops(id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30), name VARCHAR(150), city VARCHAR(80), owner_id BIGINT);
CREATE TABLE IF NOT EXISTS products(id BIGINT AUTO_INCREMENT PRIMARY KEY, code VARCHAR(30), name VARCHAR(150), category VARCHAR(60), price DOUBLE, shop_id BIGINT, est_daily DOUBLE);
CREATE TABLE IF NOT EXISTS stock_batches(id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT, quantity INT, remaining INT, added_on DATE, expiry_date DATE);
CREATE TABLE IF NOT EXISTS sales(id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT, sale_date DATE, units INT, price DOUBLE, promo INT);
CREATE TABLE IF NOT EXISTS interventions(id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT, shop_id BIGINT, type VARCHAR(30), discount_pct INT, start_date DATE, stock_before INT, predicted_leftover INT, baseline_daily DOUBLE, status VARCHAR(15), units_sold INT, units_saved INT, kg_saved DOUBLE, value_recovered DOUBLE, completed_date DATE);
