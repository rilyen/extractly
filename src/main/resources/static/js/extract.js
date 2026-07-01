// holds most recently extracted JSON object
let lastJSON = null;

// triggered by the "Extract JSON" button
// sends the pasted transcript to the backend, which will send it to Gemini
// then parses and displays the JSON Gemini extracts
async function extract() {
    // grab transcript text from textarea, trimming leading/trailing whitespace
    const transcript = document.getElementById('transcript').value.trim();

    // don't call backend if there is no transcript
    if (!transcript) {
        setStatus('Paste a transcript first.');
        return;
    }

    // show loading state and clear previous output
    // disable buttons until result is ready
    setStatus('Calling Gemini...');
    document.getElementById('jsonOutput').textContent = '';

    try {
        // POST transcript to backend /extract endpoint as JSON
        const res = await fetch('/extract', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ transcript:transcript })
        });

        // prase response body as JSON
        const data = await res.json();

        // if request fails throw to catch block below
        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }

        // Parse the extracted text from Gemini's response
        const raw = data.candidates[0].content.parts[0].text;

        // log raw text to inspect formatting issues
        console.log('Raw Gemini text:', raw);

        // strip markdown code so the string can be parsed as valid JSON and trim whitespace
        const clean = raw.replace(/```json|```/g, '').trim();

        // convert clean string to JS object
        lastJSON = JSON.parse(clean);

        // display the parsed result with 2-space indentation
        // enable action buttons
        document.getElementById('jsonOutput').textContent = JSON.stringify(lastJSON, null, 2);
        setStatus('Done');

    } catch (err) {
        // Show error message and logs details to consol
        setStatus('Error: ' + err.message);
        console.error(err);
    }
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