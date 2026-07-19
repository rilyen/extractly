// holds most recently extracted JSON object
let lastJSON = null;

// Fields stored in arrays for mapping elements of same type
// maps each JSON key to the form element's id.
const textField = [ "assignedDesigner", "dealName", "resources", "projectID", "designLink", "companyWebsite", "companyAbout", "productPurpose", "projectCostModifier",
    "designDocuments", "allocatedBudget", "usedHours", "productionNotes", "customerConcerns", "qcTurnaroundTime", "pulledSOWEstimatedTime", "devEstimatedTime"];

const Checkboxes = ["reviewProjectDocs", "draftRoadmap", "customerFeedback", "updateRoadmap", "createUpdateUserstories", "internalReview", "submittedForApproval",
    "onTrackIATs", "onTrackEmailsVideos", "flagProblem", "fullProjectIAT", "qcTestingCompleted", "demoVideoRecorded", "editPackageVideo", "cleanUpDatabase"];

const DateFields = ["devStartDate", "designDueDate", "designCompletionDate", "pullClosingDate", "projectedDeliveryDate", "negotiatedDeliveryDueDate", "projectedCompletionDate"
];

// The options allowed for each dropdown field.
const Stage_dropdown = ["Internal Testing", "Choice 2", "Choice 3", "Won"];
const ProjectClass_dropdown = ["Small (one week)", "Medium (multi week)", "Large (over X weeks)"];



// triggered by the "Extract JSON" button
async function extract() {
    const transcript = document.getElementById('transcript').value.trim();

    if (!transcript) {
        setStatus('Paste a transcript first.');
        return;
    }

    setStatus('Calling Gemini...');
    // document.getElementById('jsonOutput').textContent = '';

    try {
        // A Post request to an "/extract" endpoint
        // Backend controller returns a JSON object response
        // res is a variable to hold the Response object
        const res = await fetch('/extract', {                   // fetch is a built-in function to make HTTP requests. The request is sent to the "/extract"
            method: 'POST',                                     // Post request to the endpoint. 
            headers: { 'Content-Type': 'application/json' },    // The request body is in JSON format, so we set the Content-Type header to application/json
            body: JSON.stringify({ transcript: transcript })    // { transcript: transcript } JavaScript first wraps text into an object and flattens it so it can travel over the internet. The text is sent with a string identifier named 'transcript'. Then JSON.stringify to convert the JS object to a JSON string
        });                                                     // text is sent like "{\"transcript\":\"We had a meeting with Towels Direct. Bobby is the designer.\"}"
        // A ResponseEntity(that the controller returns) is an entire HTTP response. The JSON is the body inside that response. Like the paper inside an envelope letter.

        // parse response body as JSON
        // The body that arrives is still is a string and in the format that Gemini returned(including the candidate, content, parts, text etc). It looks like this "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"integration\\\":\\\"Towels Direct\\\"}\"}]}}]}"
        // Below now parses it which converts it to somewhat JSON format and removes the "". Allowing us to acces the data in text later
        const data = await res.json();

        // if request fails throw to catch block below
        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }

        // Check to make sure Gemini returned a candidate result.
        if (!data.candidates || !data.candidates[0]) {
            throw new Error('Gemini returned no result. Try again or shorten the transcript.');
        }
        const raw = data.candidates[0].content.parts[0].text;
        // log raw text to inspect formatting issues
        console.log('Raw Gemini text:', raw);

        // strip markdown code so the string can be parsed as valid JSON and trim whitespace
        const clean = raw.replace(/```json|```/g, '').trim();

        // convert clean string to JS object
        lastJSON = JSON.parse(clean);


        // display the parsed result with 2-space indentation
        // enable action buttons
        // document.getElementById('jsonOutput').textContent = JSON.stringify(lastJSON, null, 2);

        // Pushes the values from the JSON object into the form fields. 
        // The mapping function
        fillForm(lastJSON);

        setStatus('Form has been filled. Review before submitting.');

    } catch (err) {
        setStatus('Error: ' + err.message);
        console.error(err);
    }
}

// Maps the values from the JSON object to the corresponding zohoForm fields(based on the Field Type)
function fillForm(map) {

    // Map the text box fields
    textField.forEach(key => {
        const element = document.getElementById(key);
        // Only set the value if the element exists and the key is present in the map
        if (element && map[key] != null) {
            element.value = map[key];
        }
    });

    // Map the checkboxes. Set the box to checked if true. Fasle or missing values leave the box unchecked.
    Checkboxes.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            element.checked = map[key] === true;
        }
    });

    // Map THE dropdown fields. It is set only if the value is one of the allowed options. Otherwise, it is left as the default option
    setDropdown("stage", map.stage, Stage_dropdown);
    setDropdown("projectClass", map.projectClass, ProjectClass_dropdown);
    setDropdown("integration", map.integration, null);          // This set dynamic. There are too many options to pick

    // map the date field to dd-mm-yyyy format
    DateFields.forEach(key => {
        const element = document.getElementById(key);
        if (element && map[key] != null) {
            element.value = dateFormat(map[key]);
        }
    });
}

//  dropdown is set only if the value is one of the allowed options. Otherwise, it is left as the default option
function setDropdown(id, field, options) {
    const element = document.getElementById(id);
    if (options && !options.includes(field)) {
        console.warn(`Invalid value ${id} from the LLM:`, field);
        return;
    }
    element.value = field;
}

// Convert date format from YYYY-MM-DD to dd-MMM-YYYY (example. 2026-03-15 to 15-Mar-2026)
function dateFormat(set_date) {

    // parse the date string to a Date object and check if it worked
    const date = new Date(set_date);
    if (isNaN(date)) {
        return set_date;
    }
    // set the date to specific format using Intl.DateTimeFormat
    const parts = new Intl.DateTimeFormat("en-GB", {          // en-GB is british english format which is dd-mm-yyyy
        day: "2-digit",
        month: "short",
        year: "numeric",
        timeZone: "UTC"
    }).formatToParts(date);                                   // formatToParts returns an array of objects with type and value(like a key-value)
    const day = parts.find(key => key.type === "day").value;
    const month = parts.find(key => key.type === "month").value;
    const year = parts.find(key => key.type === "year").value;
    return `${day}-${month}-${year}`;
}

// triggered by the "Copy" button
// copies the last extracted JSON to the clipboard
function copyJSON() {
    if (!lastJSON) return;
    navigator.clipboard.writeText(JSON.stringify(lastJSON, null, 2))
        .then(() => setStatus('Copied to clipboard!'))
        .catch(() => setStatus('Could not copy.'));
}

// update status message shown to the user
function setStatus(msg) {
    document.getElementById('status').textContent = msg;
}