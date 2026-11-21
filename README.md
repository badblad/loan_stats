# Loan statistics

Spring Boot serves loan statistics from PostgreSQL, and a React/Vite frontend lets you group loan amounts by a supported CSV attribute. Each result includes the attribute value and the minimum, maximum, average, and median loan amount in that group.
The frontend also charts a histogram of group-average loan amounts; each bar counts attribute groups in an average-loan-amount range.

Supported search attributes are `addr_state`, `application_type`, `grade`, `home_ownership`, `loan_status`, `purpose`, `sub_grade`, `term`, and `verification_status`. Searching for any other attribute returns HTTP 400 with the message `Not implemented`.

For a purpose of this exercise, I did not spend too much time on beautification of this codebase. The frontend is a simple React/Vite app, and the backend is a Spring Boot app with a single REST controller. Everything can be changed and improved, but I believe that this is a good starting point for having a conversation about the codebase and its design.

## Run locally

1. Start PostgreSQL and create the CSV-shaped table:

   ```sh
   docker compose up -d db
   ```

   The database schema is initialized from `src/main/resources/schema.sql`, which contains all column names from `LoanStats_securev1_2017Q4.csv`.

2. Load the CSV. The import skips its first-line prospectus note, stops before the footer summary, and uses the next row as the CSV header:

   ```sh
   bash scripts/import-csv.sh
   ```

3. Start the backend:

   ```sh
   ./mvnw spring-boot:run
   ```

4. In another terminal, start the frontend:

   ```sh
   cd frontend
   npm install
   npm run dev
   ```

   Open `http://localhost:5173`. The frontend calls `http://localhost:8080/api/loans?attribute=grade`. Override the API base URL with `VITE_API_URL` if needed.

The database defaults are configured in `docker-compose.yml` and `src/main/resources/application.properties`; the backend also accepts `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.

Docker publishes PostgreSQL on host port `5433` by default to avoid conflicts with a local PostgreSQL server on `5432`. Set `POSTGRES_PORT` to change the published port, and update `DATABASE_URL` to match when running the backend outside Docker.
