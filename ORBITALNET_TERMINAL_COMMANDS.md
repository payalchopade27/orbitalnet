# OrbitalNet Terminal Commands Reference

This guide provides Windows PowerShell commands for setting up, running, building, and inspecting OrbitalNet locally.

---

## 1. Repository Clone & Branch Checkout

Clone the repository and check out the verified integration branch:

```powershell
# Clone the repository
git clone https://github.com/payalchopade27/orbitalnet.git

# Navigate into the project folder
cd orbitalnet

# Check out the verified integration branch (DO NOT checkout test-member1)
git checkout test-member3-integration

# Verify current branch, status, and latest commit
git branch -v
git status
git log -1 --stat
```

---

## 2. Environment Setup

### A. Java 21 & Maven 3.10
Ensure your active shell points to Java 21 and Maven 3.10:

```powershell
# Example: Set JAVA_HOME if not globally configured (adjust path if different)
$env:JAVA_HOME = "C:\Users\asmch\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_21.0.11.v20260515-1531\jre"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# Verify versions
java -version
mvn -version
```

### B. Securely Set Database Password (In-Memory Only)
To avoid saving database passwords in plain text or shell history, use PowerShell's secure string input:

```powershell
# Prompt securely for password (hidden input)
$secPassword = Read-Host -Prompt "Enter Supabase DB Password" -AsSecureString

# Marshal securely into the process environment variable
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secPassword)
$env:SUPABASE_DB_PASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

# Verify presence without printing the password value
if ([string]::IsNullOrWhiteSpace($env:SUPABASE_DB_PASSWORD)) {
    Write-Warning "SUPABASE_DB_PASSWORD is NOT set."
} else {
    Write-Host "SUPABASE_DB_PASSWORD successfully loaded in current process." -ForegroundColor Green
}
```

---

## 3. Starting the Backend Service

Run the Spring Boot application locally on port `8080` from the repository root:

```powershell
# From the repository root (orbitalnet/):
mvn spring-boot:run -pl backend
```
*The backend connects to Supabase PostgreSQL and serves REST endpoints at `http://localhost:8080`.*

---

## 4. Starting the Frontend Dashboard

Run the React/Vite development server on port `5173`:

```powershell
# Navigate to the frontend directory
cd frontend

# Install exact locked dependencies cleanly (no package-lock changes)
npm ci

# Start the local development server
npm run dev
```
*Open your browser and navigate to `http://localhost:5173`.*

---

## 5. Building the Frontend for Production

Compile production-ready static assets into `frontend/dist/`:

```powershell
# From frontend/ directory
npm run build
```
*(This produces bundled assets in `frontend/dist/`, which is ignored by Git and does not modify tracked files).*

---

## 6. Safe Read-Only Verification Endpoints

The following read-only commands query the backend without performing database writes:

```powershell
# List existing scenarios stored in Supabase
Invoke-RestMethod -Uri "http://localhost:8080/api/scenarios" -Method GET | ConvertTo-Json -Depth 3

# Check state of an existing simulation run
# Example with verified test run: 6be3f475-8123-45e8-8089-573e6832c103
Invoke-RestMethod -Uri "http://localhost:8080/api/simulations/6be3f475-8123-45e8-8089-573e6832c103/state" -Method GET | ConvertTo-Json -Depth 3

# Check state of maneuver simulation run: d42872d3-32fc-4df1-8dcd-2399c26b9511
Invoke-RestMethod -Uri "http://localhost:8080/api/simulations/d42872d3-32fc-4df1-8dcd-2399c26b9511/state" -Method GET | ConvertTo-Json -Depth 3
```

---

## 7. Important Warnings Regarding Database-Writing Commands

> [!CAUTION]
> **Commands That Write to Supabase (DO NOT RUN AS ROUTINE SETUP):**  
> The following operations perform persistent database `INSERT` or `UPDATE` queries. Do not execute them casually:
> * `POST /api/scenarios/generate` &rarr; Inserts scenario configuration.
> * `POST /api/simulations/{scenarioId}/run` &rarr; Inserts `simulation_run` and `space_object` rows.
> * `POST /api/simulations/{runId}/advance` (or `/step`) &rarr; Advances simulation, inserts `encounter` and `risk_assessment` rows.
> * `POST /api/encounters/{id}/maneuvers` &rarr; Generates and inserts `maneuver_candidate` rows.
> * `POST /api/encounters/{id}/maneuvers/{cid}/apply` &rarr; Inserts maneuver `simulation_run` and `final_object_state` rows.
> * `mvn test -Dtest=SimulationAdvanceIntegrationTest` &rarr; Creates runs and inserts records in Supabase.

### Integration Test Policy
* **Testing Phase is Complete:** All unit (215/215) and end-to-end integration tests have been approved, executed, and audited.
* **Do NOT execute `SimulationAdvanceIntegrationTest`** unless explicitly requested by team leadership.
* **Do NOT execute database cleanup scripts (`DELETE`, `TRUNCATE`, `DROP`).** Existing test run records are required for audit trail and integration validation.
