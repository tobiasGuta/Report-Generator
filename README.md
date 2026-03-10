# Report Generator

Report Generator is a Burp Suite extension built with the Montoya API that streamlines bug bounty report creation. Right-click on any request to add it as a finding, fill in the vulnerability details, and generate a professionally formatted Markdown report with a single click.

## Features

* **One-Click Finding Capture:** Right-click any request in Proxy, Repeater, or other Burp tools to instantly add it to your report.
* **Structured Finding Input:** Dialog prompts for Title, Severity, Description, Impact, Steps to Reproduce, and Remediation.
* **Severity Color Coding:** Findings are color-coded (Critical, High, Medium, Low, Info) in the findings table.
* **Full Request/Response Capture:** Automatically captures and includes the HTTP request and response in your report.
* **Markdown Export:** Generates clean, professional Markdown reports ready for submission.
* **Executive Summary:** Auto-generates a summary table and severity breakdown.
* **Finding Management:** Edit, delete, and reorder findings before generating your report.
* **Additional Notes:** Include extra context or testing notes in your report.

## Prerequisites

* Burp Suite Professional or Community Edition (requires Montoya API support)
* Java 11 or higher
* Maven (for building from source)

## Building the Extension

```bash
git clone https://github.com/tobiasGuta/Report-Generator.git
cd Report-Generator
mvn clean package
```

The compiled extension will be output to `target/report-generator.jar`.

## Installation

1. Open Burp Suite.
2. Navigate to the **Extensions** tab -> **Installed**.
3. Click the **Add** button.
4. Set the **Extension Type** to **Java**.
5. Select the compiled `report-generator.jar` file from your `target/` directory.
6. Click **Next** to load the extension.

## Usage

1. Browse your target through Burp Suite as usual.
2. When you find a vulnerability, right-click on the request in Proxy History, Repeater, or any other tool.
3. Select **Add to Report** from the context menu.
4. Fill in the finding details (Title, Severity, Description, Impact, Steps, Remediation).
5. Repeat for all findings during your session.
6. Go to the **Report Generator** tab.
7. Fill in the Program Name and Reporter fields.
8. Add any additional notes if needed.
9. Click **Generate Report** to save your Markdown report.

## Report Format

The generated report includes:

* Header with program name, reporter, date, and total findings
* Executive summary table
* Severity breakdown
* Detailed findings with:
  * Title and severity
  * Endpoint information
  * Description and impact
  * Steps to reproduce
  * Full HTTP request/response
  * Remediation recommendations

<img width="1919" height="1023" alt="Screenshot 2026-03-09 203934" src="https://github.com/user-attachments/assets/c07f8e4b-4292-4d3e-9442-d20bdc5d32c1" />

<img width="1915" height="1025" alt="Screenshot 2026-03-09 203437" src="https://github.com/user-attachments/assets/bde8a071-2837-4766-a119-08d2c5cb1f6c" />

<img width="1917" height="1026" alt="Screenshot 2026-03-09 203610" src="https://github.com/user-attachments/assets/d7eaf6e4-f92d-4ea9-9b2d-89abd6c2fcf4" />

<img width="1918" height="1029" alt="Screenshot 2026-03-09 203619" src="https://github.com/user-attachments/assets/ea3c9787-4c64-4449-85c2-ea5774d7ca19" />

<img width="1916" height="1027" alt="Screenshot 2026-03-09 203629" src="https://github.com/user-attachments/assets/74232157-6d7f-4fa4-856b-69646c76bcf9" />

<img width="1919" height="1033" alt="Screenshot 2026-03-09 203836" src="https://github.com/user-attachments/assets/5be85da1-7556-4993-be88-46920d54e046" />
