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
