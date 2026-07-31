// holds most recently extracted JSON object (the whole { Products: [...] })
let lastJSON = null;
// Fields stored in arrays for mapping elements of same type
// maps each JSON key to the form element's id.
const textField = ["Deal_ID", "What_is_the_name_of_this_Product", "Product_Description1",
    "Product_Cost", "Estimated_Duration_to_Implement_days",
    "General_Comments", "Task_ID"];

const Checkboxes = ["Passed_IAT", "User_Story_Created"];

const DateFields = ["Latest_Review_Date"];
//DeliveryRate needed
const Dropdowns = ["ReviewerApprover"];

// Default
const DEFAULT_STANDARD_SERVICE = "4003860000000356135";

// Standard service names loaded from Zoho instead of hardcoding them
//let StandardServicesOptions = [DEFAULT_STANDARD_SERVICE];

// tracks the fetch so extract() / extractFromVideo() can wait for it
let standardServicesReady = null;

async function loadStandardServices() {
    standardServicesReady = fetch('/standard-services')
        .then(res => res.json())
        .then(data => {
            const products = (data && data.data) || [];
            //console.log("Products: " + products);
            StandardServicesOptions = new Map();
            products.forEach(p => {
                if (!p.ID) {
                    return;
                }
                if (!StandardServicesOptions.has(p.ID)) {
                    StandardServicesOptions.set(p.ID, p.Output_Name);
                }
            });
        })
        .then(() => console.log('Standard Services' + StandardServicesOptions))
        .catch(err => {
            console.error('Could not load standard services, using fallback only:', err);
        });
}

// which product is currently loaded into the form.
// Ive assigned them (and im using them) as an index and treated them as sigle objects
let currentProduct = 0;

// On page load, ask server whether a Gemini key is already saved for this session and
// indicate in the status line. Only returns boolean (not the key)
document.addEventListener('DOMContentLoaded', () => {
    refreshGeminiKeyStatus();
})

async function refreshGeminiKeyStatus() {
    const statusEl = document.getElementById('geminiKeyStatus');
    try {
        const res = await fetch('/gemini-key/status');
        const data = await res.json();
        statusEl.textContent = data.hasKey
            ? 'A Gemini API key is saved for this session.'
            : 'No Gemini API key saved yet. Paste Gemini API key and click "Save Key".';
    } catch (err) {
        statusEl.textContent = 'Could not check Gemini API key status.';
    }
}

// triggered by the "Save Key" button. Sends whatever is in the field to the server (POST /gemini-key),
// which stores it in this login session. On success, the field is cleared immediately so the plaintext key
// is no longer present in the page. Only the server session holds it. Paste a different key and click
// "Save Key" again anytime in the same session to replace it.
async function saveGeminiKey() {
    const input = document.getElementById('geminiApiKey');
    const geminiApiKey = input.value.trim();

    if (!geminiApiKey) {
        setStatus('Paste a Gemini API key first.');
        return;
    }

    try {
        const res = await fetch('/gemini-key', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ geminiApiKey: geminiApiKey })
        });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || 'Could not save Gemini API key.');
        }

        // Clear the field now that the key is in the session
        input.value = '';
        setStatus('Gemini API key saved for this session.');
        refreshGeminiKeyStatus();
    } catch (err) {
        setStatus('Error: ' + err.message);
        console.error(err);
    }
}

// triggered by the "Clear key" button. Empties the field and tells the server to drop
// the key from the session (POST /gemini-key/clear), without logging the user out
async function clearGeminiKey() {
    document.getElementById('geminiApiKey').value = '';
    try {
        await fetch('/gemini-key/clear', { method: 'POST' });
    } catch (err) {
        console.error('Failed to clear Gemini API key on the server: ', err);
    }
    setStatus('Gemini API key cleared.');
    refreshGeminiKeyStatus();
}

// triggered by the "Extract JSON" button
async function extract() {
    const transcript = document.getElementById('transcript').value.trim();
    
    if (!transcript) {
        setStatus('Paste a transcript first.');
        return;
    }

    const formData = new FormData();
    formData.append('transcript', document.getElementById('transcript').value.trim());

    setStatus('Calling Gemini...');

    // call Gemini to map data to form fields
    await mapTranscript(formData);
}


async function mapTranscript(formData) {
    // to make sure the standard services list has finished
    if (standardServicesReady) {
        await standardServicesReady;
    }

    try {
        // A Post request to /extract endpoint
        // Backend controller returns a JSON object response
        const res = await fetch('/extract', {
            method: 'POST',
            body: formData,
        });

        // parse response body as JSON
        const data = await res.json();

        // if request fails throw to catch block below
        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }

        // convert clean string to JS object
        lastJSON = parseGeminiResponse(data);

        //   console.log('data:', lastJSON.data[0]);
        // display the parsed result with 2-space indentation
        // enable action buttons
        // document.getElementById('jsonOutput').textContent = JSON.stringify(lastJSON, null, 2);

        // Pushes the values from the JSON object into the form fields. 
        // The mapping function
        // fillForm(lastJSON);

        // showJSON(lastJSON);

        // fill the product dropdown and load the first product into the form
        selectProduct(lastJSON);

        setStatus('Extraction complete. Review and edit before submitting.');

    } catch (err) {
        setStatus('Error: ' + err.message);
        console.error(err);
    }
}

function parseGeminiResponse(data) {
    // Check to make sure Gemini returned a candidate result.
    if (!data.candidates || !data.candidates[0]) {
        throw new Error('Gemini returned no result. Try again or shorten the transcript.');
    }
    const candidate = data.candidates[0];
    // join ALL parts, long responses may be split across several
    const parts = (candidate.content && candidate.content.parts) || [];
    const raw = parts.map(p => p.text || '').join('');
    // log raw text and finish reason to inspect formatting issues
    console.log('Gemini finishReason:', candidate.finishReason);
    console.log('Raw Gemini text:', raw);
    // strip markdown code fences so the string can be parsed as valid JSON
    const clean = raw.replace(/```json|```/g, '').trim();
    try {
        // convert clean string to JS object
        return JSON.parse(clean);
    } catch (err) {
        if (candidate.finishReason === 'MAX_TOKENS') {
            throw new Error('Gemini hit its output token limit, the JSON is cut off. Shorten the transcript or raise maxOutputTokens.');
        }
        throw new Error('Could not parse Gemini output as JSON: ' + err.message);
    }
}

// Print extracted JSON object into #jsonOutput block on the page
function showJSON(obj) {
    const output = document.getElementById('jsonOutput');
    if (output) {
        output.textContent = JSON.stringify(obj, null, 2);
    }
}

// gets the selceted video and uploads it
function getVideoFile() {
    const fileInput = document.getElementById("videoInput");
    const file = fileInput.files[0];

    if (!file) {
        alert("Please select an mp4 file first.");
        return null;
    }

    return file
}

async function uploadToAssemblyAI(file) {
    // fetch AssemblyAI API key 
    const keyRes = await fetch('/api/assembly-key');
    const { apiKey } = await keyRes.json();

    if (!keyRes.ok) throw new Error('Failed to fetch API key');

    // upload straight from the browser to AssemblyAI
    const uploadRes = await fetch('https://api.assemblyai.com/v2/upload', {
        method: 'POST',
        headers: {
            'Authorization': apiKey,
            'Content-Type': 'application/octet-stream'
        },
        body: file
    });

    const data = await uploadRes.json();

    if (!uploadRes.ok) {
        throw new Error(data.error || 'AssemblyAI upload failed');
    }

    // return the temporary URL AssemblyAI generated
    return data.upload_url; 
}

// triggered by the "Create Transcript" button
// AssemblyAI creates a trancript. Transcript is placed in a text box for review
async function createTranscriptFromVideo() {
    // get the selected video file
    const file = getVideoFile();

    // if getVideoFile() returned null, stop this function immediately
    if (!file) return;

    setStatus('Transcribing video...');

    // to make sure the standard services list has finished loading 
    if (standardServicesReady) {
        await standardServicesReady;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        setStatus('Uploading to AssemblyAI...this may take a few minutes.');

        // upload MP4 file to AssemblyAI
        const videoUrl = await uploadToAssemblyAI(file);

        setStatus('Transcribing video...');

        // package the new URL string
        const formData = new FormData();
        formData.append("videoUrl", videoUrl);


        const res = await fetch("/extract", {
            method: "POST",
            body: formData,
        });

        // parse response body as JSON
        const data = await res.json();

        // if request fails throw to catch block below
        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }

        // Place tthe returned transcript into the text box for review
        document.getElementById("transcript").value = data.transcript;

        setStatus('Transcript generated successfully! Review the text below.');
    } catch (err) {
        console.error('Video transcription failed:', err);
        setStatus(err.message || 'Something went wrong.');
    }

}

async function autoFillFromVideo() {
    // get the selected video file
    const file = getVideoFile();

    // If getVideoFile() returned null, stop this function immediately
    if (!file) return;

    try {
        setStatus('Uploading to AssemblyAI...this may take a few minutes');

        // upload MP4 file to AssemblyAI
        const videoUrl = await uploadToAssemblyAI(file);

        setStatus('Video uploaded! Extracting transcript...');

        // package the new URL string
        const formData = new FormData();
        formData.append("videoUrl", videoUrl);

        const res = await fetch("/extract", {
            method: "POST",
            body: formData,
        });

        // parse response body as JSON
        const data = await res.json();

        // if request fails throw to catch block below
        if (!res.ok) {
            throw new Error(data.error || 'Request failed.');
        }

        setStatus('Calling Gemini...');

        const textFormData = new FormData();
        textFormData.append('transcript', data.transcript);

        // call Gemini to map data to form fields
        await mapTranscript(textFormData);

    } catch (err) {
        console.error('Autofill failed:', err);
        setStatus(err.message || 'Something went wrong.');
    }
}


// Function to fill the product dropdown field with each of the menttioned product name, then load the product data into the field
function selectProduct(data) {
    const products = (data && data.data) || [];
    const productSelected = document.getElementById('productSelected');
    const table = document.getElementById('productOptions');
    if (!productSelected) return;
    productSelected.innerHTML = '';

    // add one option per product
    for (let i = 0; i < products.length; i++) {
        const option = document.createElement('option');
        option.value = i;
        option.textContent = products[i].What_is_the_name_of_this_Product || ('Product ' + (i + 1));
        productSelected.appendChild(option);
    }

    // shows the products in the drop down
    if (products.length > 0) {
        table.style.display = 'block';
        currentProduct = 0;
        productSelected.value = 0;
        fillForm(products[0]);
    } else {
        table.style.display = 'none';
    }
}

// called when the user picks a different product from the dropdown
function ProductChosen() {
    saveFormToProduct(currentProduct);          // Function I wrote below that saves any changes/ edits on current product before switching to a different product
    const productSelected = document.getElementById('productSelected');
    currentProduct = parseInt(productSelected.value, 10);
    fillForm(lastJSON.data[currentProduct]);
}


// Maps the values from the JSON object to the corresponding zohoForm fields(based on the Field Type)
function fillForm(map) {
    if (!map) return;

    // Map the text box fields
    textField.forEach(key => {
        const element = document.getElementById(key);
        // Only set the value if the element exists and the key is present in the map
        if (element && map[key] != null) {
            element.value = map[key];
        } else if (element) {
            element.value = '';
        }
    });

    // Map the checkboxes. Set the box to checked if true. Fasle or missing values leave the box unchecked.
    Checkboxes.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            element.checked = map[key] === true;
        }
    });

    // Map the date fields to dd-MMM-yyyy format
    DateFields.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            if (map[key] != null) {
                element.value = dateFormat(map[key]);
            } else {
                element.value = '';
            }
        }
    });

    // We are always setting the Service type to be "Custom Functions"(we need it for the IF and THENs)
    // const Custom = document.getElementById("ServiceType");
    // if (Custom) {
    //     Custom.value = "Custom Functions";
    // }

    // Map THE dropdown fields. It is set only if the value is one of the allowed options. Otherwise, it is left as the default option
    // setDropdown("Project", map.Project);
    // setDropdown("DealNameAccountContact", map.Deal_Name_Account_Contact);
    const service = document.getElementById("Service_Types");
    if (service) {
        service.value = "Custom Functions"
    }

    const select = document.getElementById("dealSelect");
    if (select) {
        select.value = map.Deal_ID || '';
    }

    const dealIdField = document.getElementById("Deal_ID");
    if (dealIdField) {
        dealIdField.value = map.Deal_ID || '';
    }

    // creates the IFs and THENs tables for the product
    AutomationTriggerTable(map.Automation_Triggers || []);
    StandardFunctionOutput(map.Standard_Function_Outputs || []);
}


function AutomationTriggerTable(triggers) {             // IF TABLE
    const body = document.getElementById('IfTable');
    body.innerHTML = '';
    for (let i = 0; i < triggers.length; i++) {
        addIfEntry(triggers[i]);
    }
}

// The function adds a new entry to IFs Automation trigger
// Dynamically adds a entire row of entry
function addIfEntry(entry) {
    if (!entry) {
        entry = {};
    }
    const body = document.getElementById('IfTable');
    const row = document.createElement('tr');

    // adds a cell column to table entry and matches the text format in zoho creator for easier zoho mapping
    const numberCell = CreateTextInput(entry.Trigger_Number);
    const appCell = CreateTextInput(entry.Name_of_Trigger_Application);
    const zohoCell = makeYesNoCell(entry.Is_the_application_a_Zoho_App);
    const eventCell = CreateTextArea(entry.Trigger_Event_Description);
    const filtersCell = CreateTextArea(entry.Filters);
    const assumeCell = CreateTextArea(entry.Trigger_Assumptions);
    const hoursCell = CreateTextInput(entry.Hours);
    const deleteEntry = DeleteCellButton();

    // Dynamically adds a entire row of entry
    row.appendChild(numberCell);
    row.appendChild(appCell);
    row.appendChild(zohoCell);
    row.appendChild(eventCell);
    row.appendChild(filtersCell);
    row.appendChild(assumeCell);
    row.appendChild(hoursCell);
    row.appendChild(deleteEntry);

    body.appendChild(row);
}


function StandardFunctionOutput(outputs) {                           // THEN TABLE
    const body = document.getElementById('ThenTable');
    body.innerHTML = '';
    for (let i = 0; i < outputs.length; i++) {
        addThenEntry(outputs[i]);
    }
}

// The function adds a new entry to Standard Function Outputs (THENs)
// Dynamically adds a entire row of entry
function addThenEntry(entry) {
    if (!entry) {
        entry = {};
    }
    const body = document.getElementById('ThenTable');
    const row = document.createElement('tr');

    const triggerCell = CreateTextInput(entry.Select_Trigger);
    const servicesCell = makeServiceDropdownCell(entry.Standard_Services);
    const inclCell = CreateTextArea(entry.Inclusions);
    const exclCell = CreateTextArea(entry.Exclusions);
    const descCell = CreateTextArea(entry.Detailed_Description);
    const hoursCell = CreateTextInput(entry.Estimated_Hours);
    const deleteEntry = DeleteCellButton();
    row.appendChild(triggerCell);
    row.appendChild(servicesCell);
    row.appendChild(inclCell);
    row.appendChild(exclCell);
    row.appendChild(descCell);
    row.appendChild(hoursCell);
    row.appendChild(deleteEntry);
    body.appendChild(row);
}


// Functions that dynamically create(replicate) and set html text command formats and assign values based on IFs and THENs in the JSON
// table data with a text input inside
function CreateTextInput(entry) {
    const td = document.createElement('td');
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'form-control';
    if (entry != null) input.value = entry;
    input.oninput = saveEdits;              // save into the JSON whenever the user types in this cell
    td.appendChild(input);
    return td;
}

// table data with a textarea inside
function CreateTextArea(entry) {
    const td = document.createElement('td');
    const area = document.createElement('textarea');
    area.className = 'form-control';
    area.rows = 2;
    if (entry != null) area.value = entry;
    area.oninput = saveEdits;               // save into the JSON whenever the user types in this cell
    td.appendChild(area);
    return td;
}

// table data with a dropdown that will display Yes/No dropdown
function makeYesNoCell(entry) {
    const td = document.createElement('td');
    const select = document.createElement('select');
    select.className = 'form-select';
    select.innerHTML =
        '<option value="">-Select-</option>' +
        '<option value="Yes">Yes</option>' +
        '<option value="No">No</option>';
    if (entry != null) select.value = entry;
    select.onchange = saveEdits;            // save into the JSON when the user picks Yes/No
    td.appendChild(select);
    return td;
}

// a dropdown listing Zoho's standard service names
// Defaults to "Unique Automation (or service not listed below)" if the value is missing or doesn't match.
function makeServiceDropdownCell(entry) {
    const td = document.createElement('td');
    const select = document.createElement('select');
    select.className = 'form-select';

    // build one <option> per known standard service
    let optionsHtml = '';
    StandardServicesOptions.forEach((name, id) => {
        optionsHtml += `<option value = "${id}" >  ${name}  </option>`;
    });
    select.innerHTML = optionsHtml;

    const saveId = entry && typeof entry === 'object' ? entry.ID : entry;

    // pick the matching option, or fall back to the default catch-all
    if (saveId != null && StandardServicesOptions.has(saveId)) {
        select.value = saveId;
    } else {
        select.value = DEFAULT_STANDARD_SERVICE;
    }

    select.onchange = saveEdits;            // save into the JSON when the user picks a service
    td.appendChild(select);
    return td;
}

// delete button that removes entire row
function DeleteCellButton() {
    const td = document.createElement('td');
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-sm btn-outline-danger';
    button.textContent = 'x';
    button.onclick = function () {
        const row = button.parentElement.parentElement;
        row.remove();
        saveEdits();                        // save the JSON again now that the row is gone
    };
    td.appendChild(button);
    return td;
}


// Runs once when the page loads.
// Adds a listener to every form field so that any edit the user makes is saved into lastJSON right away
function FieldListner() {
    textField.forEach(key => addSaveListener(key));
    Checkboxes.forEach(key => addSaveListener(key));
    DateFields.forEach(key => addSaveListener(key));
    Dropdowns.forEach(key => addSaveListener(key));
}

// attaches the save function to one field, found by its id
function addSaveListener(id) {
    const element = document.getElementById(id);
    if (!element) return;
    element.oninput = saveEdits;            // fires while typing in a text box
    element.onchange = saveEdits;           // fires for checkboxes and dropdowns
}

// Saves the form into the JSON and prints the updated JSON so we can review it
function saveEdits() {
    saveFormToProduct(currentProduct);
    showJSON(lastJSON);
    //console.log('Updated JSON after edit:', JSON.stringify(lastJSON, null, 2));
}

// run the listener
window.addEventListener('DOMContentLoaded', FieldListner);


// Function that saves any changes made to the web pages fields so that when we flip to different product the edits still remain
// makes the edits directly into lastJSON
// its run everytime before we switch products
function saveFormToProduct(index) {
    if (!lastJSON || !lastJSON.data || !lastJSON.data[index]) return;
    const product = lastJSON.data[index];           // product object to update and store below values

    // the text fields
    textField.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            product[key] = element.value !== '' ? element.value : null;
        }
    });

    // the drop down
    Checkboxes.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            product[key] = element.checked;
        }
    });

    // the date fields
    DateFields.forEach(key => {
        const element = document.getElementById(key);
        if (element) {
            product[key] = element.value !== '' ? element.value : null;
        }
    });

    // the drop downs
    product.Project = readValue("Project");
    product.Deal_Name_Account_Contact = readValue("DealNameAccountContact");
    product.Delivery_Rate = readValue("Delivery_Rate");
    product.Reviewer_Approver = readValue("ReviewerApprover");
    product.Service_Types = ["Custom Functions"];

    // the IF/THEN tables
    product.Automation_Triggers = saveIfEntries();
    product.Standard_Function_Outputs = saveThenEntries();
}

// save all IF rows back into an array of objects and turns then into trigger objects
function saveIfEntries() {
    const rows = document.getElementById('IfTable').children;
    const Triggerlist = [];
    // loop over each row and stores each entry in the cell
    for (let i = 0; i < rows.length; i++) {
        const cells = rows[i].children;                 // read each cell's value into fixed column
        const trigger = {
            Trigger_Number: ColumnValue(cells[0]),
            Name_of_Trigger_Application: ColumnValue(cells[1]),
            Is_the_application_a_Zoho_App: ColumnValue(cells[2]),
            Trigger_Event_Description: ColumnValue(cells[3]),
            Filters: ColumnValue(cells[4]),
            Trigger_Assumptions: ColumnValue(cells[5]),
            Hours: ColumnValue(cells[6])
        };
        Triggerlist.push(trigger);
    }
    return Triggerlist;        // All the triggers are stored on the product
}

// repeat for all the THEN rows. Save back into an array of objects
function saveThenEntries() {
    const rows = document.getElementById('ThenTable').children;
    const Triggerlist = [];
    for (let i = 0; i < rows.length; i++) {
        const entry = rows[i].children;
        const output = {
            Select_Trigger: ColumnValue(entry[0]),
            Standard_Services: ColumnValue(entry[1]),
            Inclusions: ColumnValue(entry[2]),
            Exclusions: ColumnValue(entry[3]),
            Detailed_Description: ColumnValue(entry[4]),
            Estimated_Hours: ColumnValue(entry[5])
        };
        Triggerlist.push(output);
    }
    return Triggerlist;
}

// function to get the value of the input inside the table column
function ColumnValue(cell) {
    const field = cell.children[0];        // the input or select or the textarea
    if (!field) return null;
    return field.value !== '' ? field.value : null;
}


// dropdown is set only if the value is not empty
function setDropdown(id, field) {
    const element = document.getElementById(id);
    if (!element || field == null) return;
    element.value = field;
}

function readValue(id) {
    const element = document.getElementById(id);
    if (element && element.value !== '') {
        return element.value;
    }
    return null;
}


// Convert date format from YYYY-MM-DD to dd-MMM-YYYY (example. 2026-03-15 to 15-Mar-2026)
function dateFormat(set_date) {

    // parse the date string to a Date object and check if it worked
    const date = new Date(set_date);
    if (isNaN(date)) {
        return set_date;
    }
    // set the date to specific format using Intl.DateTimeFormat
    const parts = new Intl.DateTimeFormat("en-GB", {          // en-GB is british format dd-mm-yyyy
        day: "2-digit",
        month: "short",
        year: "numeric",
        timeZone: "UTC"
    }).formatToParts(date);
    const day = parts.find(key => key.type === "day").value;
    const month = parts.find(key => key.type === "month").value;
    const year = parts.find(key => key.type === "year").value;
    return `${day}-${month}-${year}`;
}


// triggered by the "Copy" button
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
function setZohoStatus(msg) {
    document.getElementById('zohoStatus').textContent = msg;
}
// logout
async function logout() {
    try {
        await fetch('/logout', { method: 'POST' });
        window.location.href = '/login.html';
    } catch (err) {
        console.error('Logout failed:', err);
        setStatus('Logout failed. Try again.');
    }
}

// Submit to Zoho: save current form edits into the JSON first, then send everything
async function sendJsonToZoho() {
    if (!lastJSON) {
        setStatus('Enter the Transcript.');
        return;
    }

    // save edits from the form on screen before sending
    saveFormToProduct(currentProduct);

    const dealId = document.getElementById('dealSelect').value;
    if (!dealId) {
        setStatus('Select a deal first.');
        return;
    }

    // lastJSON.data.forEach(product => {
    //     product.Deal_ID = dealId;
    //     product.Deal_Name = dealId;
    // })

    const payload = { data: lastJSON.data };
    setZohoStatus('Sending to Zoho...');

    try {
        const res = await fetch('/send-to-service', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)

        });
        const result = await res.json();
        setZohoStatus(result.code === 3000 ? 'Message sent successfully' : 'Fail: ' + JSON.stringify(result));
    } catch (err) {
        setZohoStatus('Error: ' + err.message);
    }


}
async function zohoPreview() {
    if (!lastJSON) {
        setStatus('Enter the Transcript.');
        return;
    }

    // save edits from the form on screen before sending
    saveFormToProduct(currentProduct);

    const dealId = document.getElementById('dealSelect').value;

    if (!dealId) {
        setStatus('Select a deal first.');
        return;
    }

    const product = lastJSON.data[currentProduct];
    product.Deal_ID = dealId;
    product.Deal_Name = dealId;

    // lastJSON.data.forEach(product => {
    //     product.Deal_ID = dealId;
    //     product.Deal_Name = dealId;
    // })

    const payload = { data: lastJSON.data };
    console.log(JSON.stringify(payload, null, 2));


}

async function getProjectNameID() {
    const res = await fetch('/deals');
    const deal = await res.json();

    const deals = new Map();

    deal.data.forEach(d => {
        if (!d.Integration) return;
        if (d.Integration.ID) {
            deals.set(d.Integration.ID, d.Integration.zc_display_value);
        }
    });

    const select = document.getElementById('dealSelect');
    select.innerHTML = '<option value="">-- Select -- </option>';
    deals.forEach((name, id) => {
        select.innerHTML += `<option value ="${id}">${name} ${id} </option>`;
    });

    document.getElementById('dealSelect').addEventListener('change', (event) => {
        const id = document.getElementById('Deal_ID');
        if (id) {
            id.value = event.target.value;
        }
        if (lastJSON && lastJSON.data && lastJSON.data[currentProduct]) {
            lastJSON.data[currentProduct].Deal_ID = event.target.value;
            lastJSON.data[currentProduct].Deal_Name = event.target.value;
        }
    });
    // deal.data.forEach(d => {
    //     if (!d.Deal_ID || !d.Deal_Name || !d.Deal_Name.Account_Name) return;
    //     if (d.Deal_ID) {
    //         deals.set(d.Deal_Name.Potential_Name, d.Deal_Name.ID, d.Deal_Name.Account_Name);
    //     }
    // });

    // const select = document.getElementById('dealSelect');
    // select.innerHTML = '<option value="">-- Select -- </option>';
    // deals.forEach((deal, name, id) => {
    //     select.innerHTML += `<option value ="${id}">${deal} ${id} ${name}</option>`;
    // });

    // document.getElementById('dealSelect').addEventListener('change', (event) => {
    //     const id = document.getElementById('Deal_ID');
    //     if (id) {
    //         id.value = event.target.value;
    //     }
    // });
}

getProjectNameID();
loadStandardServices();
