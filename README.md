# Extractly

> AI-powered video-to-SOW automation for Aether Automation · CMPT 276 Group 13

---

## Abstract

Extractly is a browser-based web application that converts recorded client meeting videos into Statements of Work. An admin user uploads an MP4 of a client call, the app transcribes it, extracts key project information using AI, and pushes completed SOWs into Aether Automation's existing Zoho Creator form for immediate download.

---

## Background

Aether Automation representatives currently produce SOWs manually: re-listening to recordings, extracting details, and filling in fields in Zoho Creator by hand. Client calls run 30–60 minutes, are unstructured, and arrive as MP4 files. There is typically one call per SOW, though some SOWs span two calls. No existing tool addresses this workflow end-to-end — from raw video to a downloadable SOW in a CRM.

---

## Solution

Extractly automates the workflow in a single browser interface. It accepts an MP4 upload, produces a filled and editable SOW draft for review, and submits it directly to Zoho Creator. The tool is intended for internal Aether Automation employees only.

---

## Epics

The project is organized into one main feature decomposed into five subproblems, one per team member.

| # | Epic | Description | Owner |
|---|------|-------------|-------|
| 1 | **API Key Management** | UI for rotating AI API credentials without touching the codebase | Sukh |
| 2 | **MP4 Upload** | File upload interface accepting MP4 only, with upload confirmation and handoff to the transcription pipeline | Taj |
| 3 | **Transcription** | AI transcription service integration converting MP4 audio to text; handles 30–60 min recordings and multi-call SOWs | Praise |
| 4 | **NLP Extraction + Zoho Field Mapping** | AI extraction of SOW fields from the transcript, mapped to Aether's Zoho Creator schema | April |
| 5 | **Editable Review UI + Zoho Submission** | Editable draft form for reviewing extracted fields before finalizing and pushing the SOW to Zoho Creator | Aaril |