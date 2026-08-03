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
| 1 | **Login/registration, user roles + prompt retrieval** | User authentication/session handling, plus the Zoho API integration that pulls the extraction prompt from a Zoho Creator report at runtime | Taj |
| 2 | **Review UI + MP4 transcription** | HTML page for reviewing and editing extracted SOW fields before submission and the MP4-to-text transcription API integration | Praise |
| 3 | **NLP extraction + Zoho field mapping, per session API key handling** | NLP extraction + Zoho field mapping, per session API key handling – AI extraction of SOW fields from the transcript, mapped to Aether’s Zoho Creator schema using Gemini API | April |
| 4 | **Extraction Mapping Logic** | Mapping logic that maps Gemini’s extraction output onto the review form fields and Zoho API integration for the mapped fields | Aaril |
| 5 | **Zoho Creator API integration** | Integration with Aether’s Zoho Creator API for fetching deal IDs/accounts and submitting completed SOWs to Zoho Creator | Sukh |
