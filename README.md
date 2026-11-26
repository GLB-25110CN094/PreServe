# PreServe - AI Food Waste Prevention (Spring Boot + HTML/CSS/JS)

## Run (Java 17+ and Maven required)
Option A - MySQL (default, real database): create nothing; just make sure MySQL runs on localhost:3306
and edit user/password in `src/main/resources/application.properties` (default root/root). The `preserve` DB is created automatically.

    mvn spring-boot:run

Option B - zero setup file database (H2):

    mvn spring-boot:run -Dspring-boot.run.profiles=h2

Open http://localhost:8080  -  Demo login: **demo@preserve.com / demo123**
Or click "Create account": registering creates your user and your first shop (empty, add products yourself).

## What happens on first start
Your Excel (`data/PreServe_Supermarket_Sample_Dataset.xlsx`, converted to CSV in `src/main/resources/data`) is loaded into the DB:
30 products + stock (shop S005), 1,800 sales records, 4 past interventions.
Sales dates are shifted so the newest sale is yesterday, and expiry = today + Days_Remaining, so the demo always looks live.

## Pages
login, register, dashboard, inventory (add product / add stock / record sale / CSV import), product (AI detail + charts + start action),
actions (tracking), insights (seasonality), impact (+QR), shops (add shops), store.html?code=... (public impact profile).
Dark/Light and English/Hindi toggles are in the top bar.

## AI model (ForecastService.java)
Exponential-smoothing level + damped trend, learned day-of-week factors, learned promotion uplift, category x month seasonal index
(seasonality.csv - editable baseline assumption), last-year adjustment when 28+28 days of last-year data exist, forecast variance ->
waste probability, FEFO batch simulation, discount elasticity learned from completed interventions.
New product with no history: uses your daily-sales estimate, else the category average from your shop.

## Notes
- The dataset has only ~2 months of history, so last-year comparison activates only after you add older sales data.
- NGO/redistribution is intentionally left out. Add product CSV format: name,category,price,quantity,days_remaining,est_daily
